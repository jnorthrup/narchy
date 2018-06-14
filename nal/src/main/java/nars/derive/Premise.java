/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package nars.derive;

import jcog.Util;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.concept.Concept;
import nars.op.mental.AliasConcept;
import nars.table.BeliefTable;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.time.Tense;
import nars.truth.Stamp;
import nars.unify.UnifySubst;
import org.eclipse.collections.api.set.primitive.ImmutableLongSet;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.function.Predicate;

import static nars.Op.BELIEF;
import static nars.Op.VAR_QUERY;

/**
 * Defines the conditions used in an instance of a derivation
 * Contains the information necessary for generating derivation Tasks via reasoning rules.
 * <p>
 * It is meant to be disposable and should not be kept referenced longer than necessary
 * to avoid GC loops, so it may need to be weakly referenced.
 */
public class Premise {

    public final Task task;
    /**
     * sloppy pre-sort of premises by task/task_term,
     * to maximize sequential repeat of derived task term
     */
    public static final Comparator<? super Premise> sortByTaskSloppy =
            Comparator
                    .comparingInt((Premise a) -> a.task.hashCode())
                    .thenComparingLong((Premise a) -> (a.task.term().hashCode() << 32) | a.term().hashCode()  )
                    //.thenComparingInt((Premise a) -> System.identityHashCode(a.task))
            ;

    final PriReference<Term> termLink;

    private final int hash;

    public Premise(Task task, PriReference<Term> termLink) {
        super();

        this.task = task;

        this.termLink = termLink;
        if (termLink.get() instanceof Bool) {
            throw new RuntimeException("beliefTerm boolean; termLink=" + termLink);
        }

        this.hash = Util.hashCombine(task, termLink /* should have same hash as: term()*/);
    }

    public Term term() {
        return termLink.get();
    }

    /** variable types unifiable in premise formation */
    final static int var =
            Op.VAR_QUERY.bit;

            

    /**
     * resolve the most relevant belief of a given term/concept
     * <p>
     * patham9 project-eternalize
     * patham9 depending on 4 cases
     * patham9 https:
     * sseehh__ ok ill add that in a bit
     * patham9 you need  project-eternalize-to
     * sseehh__ btw i disabled immediate eternalization entirely
     * patham9 so https:
     * patham9 especially try to understand the "temporal temporal" case
     * patham9 its using the result of higher confidence
     * <p>
     * returns ttl used, -1 if failed before starting
     *
     * @param matchTime - temporal focus control: determines when a matching belief or answer should be projected to
     */
    public boolean match(Derivation d, int matchTTL) {

        Term taskTerm = task.term();


        boolean beliefConceptCanAnswerTaskConcept = false;
        boolean unifiedBelief = false;

        Term beliefTerm = term();
        Op bo = beliefTerm.op();
        if (taskTerm.op() == bo) {
            if (taskTerm.concept().equals(beliefTerm.concept())) {
                beliefConceptCanAnswerTaskConcept = true;
            } else {

                if ((bo.conceptualizable) && (beliefTerm.hasAny(var) || taskTerm.hasAny(var))) {

                    Term _beliefTerm = beliefTerm;
                    final Term[] unifiedBeliefTerm = new Term[]{null};
                    UnifySubst u = new UnifySubst(var==VAR_QUERY.bit ? VAR_QUERY : null /* all */, d.nar, (y) -> {
                        if (y.op().conceptualizable) {
                            y = y.normalize().unneg();


                            if (!y.equals(_beliefTerm)) {
                                unifiedBeliefTerm[0] = y;
                                return false; 
                            }
                        }
                        return true; 
                    }, matchTTL);

                    u.varSymmetric = false;

                    beliefConceptCanAnswerTaskConcept = u.unify(taskTerm, beliefTerm, true).matches() > 0;

                    if (unifiedBeliefTerm[0] != null) {
                        beliefTerm = unifiedBeliefTerm[0];
                        unifiedBelief = true;
                    }

                }
            }
        }

        
        Task belief = match(d, beliefTerm, beliefConceptCanAnswerTaskConcept, unifiedBelief);

        if (belief != null) {
            beliefTerm = belief.term(); 
        } else {

            beliefTerm = beliefTerm.unneg();

            if (beliefTerm instanceof Bool) {
                throw new RuntimeException("beliefTerm boolean; termLink=" + termLink + ", belief=" + belief);
            }
        }

        return d.reset(task, belief, beliefTerm);
    }

    @Nullable Task match(Derivation d, Term beliefTerm, boolean beliefConceptCanAnswerTaskConcept, boolean unifiedBelief) {
        


        NAR n = d.nar;

        Task belief = null;

        Concept beliefConcept = beliefTerm.op().conceptualizable ?
                n.conceptualize(beliefTerm) 
                :
                null;

        if (beliefConcept != null) {
            if (beliefConcept instanceof AliasConcept) {
                beliefConcept = ((AliasConcept)beliefConcept).abbr;
                beliefTerm = beliefConcept.term();
            }




            long taskStart = Tense.dither(task.start(), n);
            long taskEnd = Tense.dither(task.end(), n);

            if (!beliefTerm.hasVarQuery()) { 

                final BeliefTable bb = beliefConcept.beliefs();
                Predicate<Task> beliefFilter = null;
                if (task.isQuestionOrQuest()) {
                    if (beliefConceptCanAnswerTaskConcept) {

                        final BeliefTable answerTable =
                                (task.isQuest()) ?
                                        beliefConcept.goals() :
                                        bb;

                        beliefFilter = stampFilter(d); 

                        if (!answerTable.isEmpty()) {
                            
                            Task match = answerTable.answer(taskStart, taskEnd, beliefTerm, beliefFilter, n);
                            if (!validMatch(match)) match = null;
                            if (match == null) {

                                
                                long[] focus = n.timeFocus();
                                if (focus[0] != taskStart && focus[1] != taskEnd) {
                                    
                                    match = answerTable.answer(focus[0], focus[1], beliefTerm, beliefFilter, n);
                                    if (!validMatch(match)) match = null; 
                                }

                                if (match == null) {
                                    match = answerTable.answer(taskStart, taskEnd, beliefTerm, null, n); 
                                    if (!validMatch(match)) match = null;
                                }
                            }

                            if (match != null) {
                                assert (task.isQuest() || match.punc() == BELIEF) : "quest answered with a belief but should be a goal";

                                

                                @Nullable Task answered = task.onAnswered(match, n);
                                if (answered != null) {

                                    d.add(answered);

                                    if (answered.isBelief()) {
                                        belief = answered;
                                    }

                                    n.emotion.onAnswer(task, answered);
                                }

                            }
                        }
                    }
                }

                if ((belief == null) && !bb.isEmpty()) {

                    if (beliefFilter==null) beliefFilter = stampFilter(d); 


                    
                    belief = bb.match(taskStart, taskEnd, beliefTerm, beliefFilter, n);
                    if (!validMatch(belief)) belief = null; 

                    if (belief == null) {

                        long[] focus = n.timeFocus();
                        if (focus[0] != taskStart && focus[1] != taskEnd) {
                            
                            belief = bb.match(focus[0], focus[1], beliefTerm, beliefFilter, n);
                            if (!validMatch(belief)) belief = null; 
                        }
                    }

                    if (belief == null) {
                        
                        belief = bb.match(taskStart, taskEnd, beliefTerm, null, n); 
                        if (!validMatch(belief)) belief = null; 
                    }

                }
            }


            if (unifiedBelief) {
                Concept originalBeliefConcept = n.conceptualize(term());
                if (originalBeliefConcept != null) {
                    Concept taskConcept = task.concept(n, true);
                    linkVariable(taskConcept, originalBeliefConcept, beliefConcept);
                }
            }

        }
        return belief;
    }

    private boolean validMatch(@Nullable Task x) {
        return x != null && !x.isDeleted() && !x.equals(task);






    }

    private Predicate<Task> stampFilter(Derivation d) {
        ImmutableLongSet taskStamp = 
                Stamp.toSet(task);
        return t -> !Stamp.overlapsAny(taskStamp, t.stamp());
    }


    @Override
    public boolean equals(Object obj) {
        return hash == obj.hashCode() && ((Premise)obj).task.equals(task) && ((Premise)obj).term().equals(term());
    }

    @Override
    public int hashCode() {
        return hash;
    }

    /**
     * x has variables, y unifies with x and has less or no variables
     */
    private void linkVariable(Concept taskConcept, Concept lessConstant, Concept moreConstant) {

        
        float pri = termLink.priElseZero(); 
        






        Term moreConstantTerm = moreConstant.term();
        Term lessConstantTerm = lessConstant.term();




        
        moreConstant.termlinks().putAsync(new PLink<>(lessConstantTerm, pri/2f));

        lessConstant.termlinks().putAsync(new PLink<>(moreConstantTerm, pri/2f));


        if (taskConcept!=null)
            taskConcept.termlinks().putAsync(new PLink<>(moreConstantTerm, pri));




    }

    @Override
    public String toString() {
        return "Premise(" +
                task +
                " * " + term() +
                ')';
    }




























}
