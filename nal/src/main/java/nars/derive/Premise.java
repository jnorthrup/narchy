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
    public final PriReference<Term> termLink;
    private final int hash;

    //TODO make global param



    public Premise(Task task, PriReference<Term> termLink) {
        super();
//        if (task.isQuestionOrQuest())
//            System.out.println(task + " "+ termLink);
        this.task = task;
        this.termLink = termLink;
        this.hash = Util.hashCombine(task, term());
    }

    public Term term() {
        return termLink.get();
    }

    /** variable types unifiable in premise formation */
    final static int var =
            Op.VAR_QUERY.bit;
            //Op.VAR_QUERY.bit | Op.VAR_DEP.bit | Op.VAR_INDEP.bit;

    /**
     * resolve the most relevant belief of a given term/concept
     * <p>
     * patham9 project-eternalize
     * patham9 depending on 4 cases
     * patham9 https://github.com/opennars/opennars2/blob/a143162a559e55c456381a95530d00fee57037c4/src/nal/deriver/projection_eternalization.clj
     * sseehh__ ok ill add that in a bit
     * patham9 you need  project-eternalize-to
     * sseehh__ btw i disabled immediate eternalization entirely
     * patham9 so https://github.com/opennars/opennars2/blob/a143162a559e55c456381a95530d00fee57037c4/src/nal/deriver/projection_eternalization.clj#L31
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
                                return false; //stop
                            }
                        }
                        return true; //keep going
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

        //QUESTION ANSWERING and TERMLINK -> TEMPORALIZED BELIEF TERM projection
        Task belief = match(d, beliefTerm, beliefConceptCanAnswerTaskConcept, unifiedBelief);

        if (belief != null) {
            beliefTerm = belief.term(); //use the belief's actual, possibly-temporalized term
        } else {

            beliefTerm = beliefTerm.unneg();

            if (beliefTerm instanceof Bool)
                throw new RuntimeException("beliefTerm boolean; termLink=" + termLink + ", belief=" + belief);
        }

        return d.reset(task, belief, beliefTerm);
    }

    @Nullable Task match(Derivation d, Term beliefTerm, boolean beliefConceptCanAnswerTaskConcept, boolean unifiedBelief) {
        //        float timeFocus = n.timeFocus.floatValue();
//        int fRad = Math.round(Math.max(1,dur * timeFocus));

        NAR n = d.nar;

        Task belief = null;

        Concept beliefConcept = beliefTerm.op().conceptualizable ?
                n.conceptualize(beliefTerm) //conceptualize in case of dynamic concepts
                :
                null;

        if (beliefConcept != null) {
            if (beliefConcept instanceof AliasConcept) {
                beliefConcept = ((AliasConcept)beliefConcept).abbr;
                beliefTerm = beliefConcept.term();
            }

//            long[] focus = n.timeFocus(task.nearestPointInternal(n.time()));
//            long focusStart = focus[0];
//            long focusEnd = focus[1];
            long taskStart = Tense.dither(task.start(), n);
            long taskEnd = Tense.dither(task.end(), n);

            if (!beliefTerm.hasVarQuery()) { //doesnt make sense to look for a belief in a term with query var, it will have none

                final BeliefTable bb = beliefConcept.beliefs();
                Predicate<Task> beliefFilter = null;
                if (task.isQuestionOrQuest()) {
                    if (beliefConceptCanAnswerTaskConcept) {

                        final BeliefTable answerTable =
                                (task.isQuest()) ?
                                        beliefConcept.goals() :
                                        bb;

                        if (beliefFilter==null) beliefFilter = stampFilter(d); //lazy compute

                        if (!answerTable.isEmpty()) {
                            //try task start/end time
                            Task match = answerTable.answer(taskStart, taskEnd, beliefTerm, beliefFilter, n);
                            if (!validMatch(match)) match = null;
                            if (match == null) {

                                //try current moment
                                if (match == null) {
                                    long[] focus = n.timeFocus();
                                    if (focus[0] != taskStart && focus[1] != taskEnd) {
                                        //CURRENT MOMENT (stamp filtered)
                                        belief = answerTable.answer(focus[0], focus[1], beliefTerm, beliefFilter, n);
                                        if (!validMatch(match)) match = null; //force single
                                    }
                                }

                                if (match == null) {
                                    match = answerTable.answer(taskStart, taskEnd, beliefTerm, null, n); //retry without stamp filter
                                    if (!validMatch(match)) match = null;
                                }
                            }

                            if (match != null) {
                                assert (task.isQuest() || match.punc() == BELIEF) : "quest answered with a belief but should be a goal";

                                //add the answer to the derived tasks for eventual input
                                //((NALTask)match).causeMerge(task);
                                d.add(match);

                                if (match.isBelief()) {
                                    belief = match;
                                }

                                @Nullable Task answered = task.onAnswered(match, n);
                                if (answered != null) {
                                    n.emotion.onAnswer(task, answered);
                                }

                            }
                        }
                    }
                }

                if ((belief == null) && !bb.isEmpty()) {

                    if (beliefFilter==null) beliefFilter = stampFilter(d); //lazy compute


                    //TASK'S MOMENT (stamp filtered)
                    belief = bb.match(taskStart, taskEnd, beliefTerm, beliefFilter, n);
                    if (!validMatch(belief)) belief = null; //force single

                    if (belief == null) {

                        long[] focus = n.timeFocus();
                        if (focus[0] != taskStart && focus[1] != taskEnd) {
                            //CURRENT MOMENT (stamp filtered)
                            belief = bb.match(focus[0], focus[1], beliefTerm, beliefFilter, n);
                            if (!validMatch(belief)) belief = null; //force single
                        }
                    }

                    if (belief == null) {
                        //TASK's MOMENT (unfiltered)
                        belief = bb.match(taskStart, taskEnd, beliefTerm, null, n); //retry without stamp filter
                        if (!validMatch(belief)) belief = null; //force single
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
//        else {
//            if (x != null) {
//                boolean reallyEqualWTF = task.equals(x);
//                throw new RuntimeException(reallyEqualWTF + "=equal - shouldnt happen if stamp overlap filtered");
//            }
//        }
    }

    private Predicate<Task> stampFilter(Derivation d) {
        ImmutableLongSet taskStamp = //(d._task !=null && d._task.equals(task)) ? d.taskStamp :
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

        //fraction of the source termlink's budget
        float pri = termLink.priElseZero(); // * 0.5f;
        //termLink.priMult(0.5f);

//        /** creates a tasklink/termlink proportional to the tasklink's priority
//         *  and inversely proportional to the increase in term complexity of the
//         *  unified variable.  ie. $x -> (y)  would get a stronger link than  $x -> (y,z)
//         */
//        PriReference taskLink = this;
        Term moreConstantTerm = moreConstant.term();
        Term lessConstantTerm = lessConstant.term();
//        float pri = taskLink.priElseZero()
//                * (1f/lessConstantTerm.volume());
//                //* Util.unitize(lessConstantTerm.complexity() / ((float) moreConstantTerm.complexity()));
//
        //share the budget in 2 opposite links: specific -> general & general -> specific
        moreConstant.termlinks().putAsync(new PLink<>(lessConstantTerm, pri/2f));

        lessConstant.termlinks().putAsync(new PLink<>(moreConstantTerm, pri/2f));
//        //moreConstant.termlinks().putAsync(new PLink<>(taskConcept.term(), pri));

        if (taskConcept!=null)
            taskConcept.termlinks().putAsync(new PLink<>(moreConstantTerm, pri));
//
//
//        //Tasklinks.linkTask(this.task.get(), pri, moreConstant);

    }

    @Override
    public String toString() {
        return "Premise(" +
                task +
                " * " + term() +
                ')';
    }


//    public void merge(Premise incoming) {
//        //WARNING this isnt thread safe but collisions should be rare
//
//        Collection<Concept> target = this.links;
//        Collection<Concept> add = incoming.links;
//
//        if (target == add || add == null)
//            return; //same or no change
//
//        if (target == null || target.isEmpty()) {
//            this.links = add;
//            return; //just replace it
//        }
//
//        if (!(target instanceof Set)) {
//            Set<Concept> merge =
//                    new HashSet(target.size() + add.size());
//                    //Collections.newSetFromMap(new ConcurrentHashMap<>(target.size() + add.size()));
//            merge.addAll(target);
//            merge.addAll(add);
//            this.links = merge;
//        } else {
//            target.addAll(add);
//        }
//    }

}
