/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package nars.derive;

import jcog.pri.PLink;
import jcog.pri.PriReference;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.concept.Concept;
import nars.op.mental.AliasConcept;
import nars.table.BeliefTable;
import nars.term.Term;
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

    /**
     * specially constructed hash that is useful for sorting premises by:
     * a) task equivalency (hash)
     * a) task term equivalency (hash)
     * b) belief term equivalency (hash)
     * <p>
     * designed to maximize sequential repeat of derived task term
     */
    public final long hash;

    public Premise(Task task, PriReference<Term> termLink) {
        super();

        this.task = task;

        this.termLink = termLink;

        this.hash =
                //task's lower 23 bits in bits 40..64
                (((long) task.hashCode()) << (64 - 24))
                        | //task term's lower 20 bits in bits 20..40
                        (((long) (task.term().hashCode() & 0b00000000000011111111111111111111)) << 20)
                        | //termlink's lower 20 bits in bits 0..20
                        ((termLink.hashCode() & 0b00000000000011111111111111111111));
    }

    public Term term() {
        return termLink.get();
    }

    /**
     * variable types unifiable in premise formation
     */
    final static int var =
            //Op.VAR_QUERY.bit;
              Op.VariableBits; //all


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

        if (task.isDeleted())
            return false;

        Term taskTerm = task.term();


        boolean beliefConceptCanAnswerTaskConcept = false;
        boolean beliefTransformed = false;

        Term beliefTerm = term();
        Op bo = beliefTerm.op();
        if (taskTerm.concept().equals(beliefTerm.concept())) {
            beliefConceptCanAnswerTaskConcept = true;
        } else {
//            if (taskTerm.op() == bo) {
//            } else {

            if ((bo.conceptualizable) && (beliefTerm.hasAny(var) || taskTerm.hasAny(var))) {

                Term _beliefTerm = beliefTerm;
                final Term[] unifiedBeliefTerm = new Term[]{null};
                UnifySubst u = new UnifySubst(var == VAR_QUERY.bit ? VAR_QUERY : null /* all */, d.nar, (y) -> {
                    if (y.op().conceptualizable) {
                        y = //y.normalize().unneg();
                                y.unneg();


                        if (!y.equals(_beliefTerm)) {
                            unifiedBeliefTerm[0] = y;
                            return false;  //done
                        }
                    }
                    return true;
                }, matchTTL);

                u.symmetric = true;

                beliefConceptCanAnswerTaskConcept = u.unify(beliefTerm, taskTerm, true).matches() > 0;

                if (unifiedBeliefTerm[0] != null) {
                    beliefTerm = unifiedBeliefTerm[0];
                    beliefTransformed = true;
                }


            }
//            }
        }


        Task belief = match(d, beliefTerm, beliefConceptCanAnswerTaskConcept, beliefTransformed);
//        if (belief!=null) {
//            System.out.println(beliefTerm + " -> " + belief);
//        }

        return d.reset(task, belief, belief != null ? belief.term() : beliefTerm.unneg());
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
                beliefConcept = ((AliasConcept) beliefConcept).abbr;
                beliefTerm = beliefConcept.term();
            }

            long taskStart =
                    //Tense.dither(task.start(), n);
                    task.start();
            long taskEnd =
                    //Tense.dither(task.end(), n);
                    task.end();

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
                                    //try, allowing overlap
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

                    if (beliefFilter == null) beliefFilter = stampFilter(d);


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


        }

//        if (unifiedBelief && belief!=null) {
//            linkVariable(unifiedBelief, d.nar, beliefConcept);
//        }

        return belief;
    }

    private void linkVariable(boolean unifiedBelief, NAR n, Concept beliefConcept) {
        if (unifiedBelief) {
            Concept originalBeliefConcept = n.conceptualize(term());
            if (originalBeliefConcept != null) {
                Concept taskConcept = task.concept(n, true);


                float pri = termLink.priElseZero();


                Term moreConstantTerm = beliefConcept.term();
                Term lessConstantTerm = originalBeliefConcept.term();


                beliefConcept.termlinks().putAsync(new PLink<>(lessConstantTerm, pri / 2f));

                originalBeliefConcept.termlinks().putAsync(new PLink<>(moreConstantTerm, pri / 2f));


                if (taskConcept != null)
                    taskConcept.termlinks().putAsync(new PLink<>(moreConstantTerm, pri));


            }
        }
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
        return this == obj ||
                (hashCode() == obj.hashCode() && ((Premise) obj).task.equals(task) && ((Premise) obj).term().equals(term()));
    }

    @Override
    public final int hashCode() {
        return (int) (hash >> 10) /* shift down about 10 bits to capture all 3 elements in the hash otherwise the task hash is mostly excluded */;
    }

    @Override
    public String toString() {
        return "Premise(" +
                task +
                " * " + term() +
                ')';
    }


}
