/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package nars.derive;

import com.netflix.servo.monitor.Counter;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import nars.*;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.op.mental.AliasConcept;
import nars.table.BeliefTable;
import nars.task.util.Answer;
import nars.term.Term;
import nars.time.Tense;
import nars.unify.UnifySubst;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

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

    public Term beliefTerm() {
        return termLink.get();
    }

    /**
     * variable types unifiable in premise formation
     */
    final static int var =
            Op.VAR_QUERY.bit;
    //Op.Variable; //all


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
    private Task match(Derivation d, int matchTTL) {


        Term taskTerm = task.term();

        boolean beliefConceptCanAnswerTaskConcept = false;
        boolean beliefTransformed = false;

        Term beliefTerm = beliefTerm();
        Op bo = beliefTerm.op();
        NAR n = d.nar;
        if (taskTerm.concept().equals(beliefTerm.concept())) {
            beliefConceptCanAnswerTaskConcept = true;
        } else {
//            if (taskTerm.op() == bo) {
//            } else {

            if ((bo.conceptualizable) && (beliefTerm.hasAny(var) || taskTerm.hasAny(var))) {

                Term _beliefTerm = beliefTerm;
                final Term[] unifiedBeliefTerm = new Term[]{null};
                UnifySubst u = new UnifySubst(var == VAR_QUERY.bit ? VAR_QUERY : null /* all */, n.random(), (y) -> {
                    if (y.op().conceptualizable) {
                        y = //y.normalize().unneg();
                                y.unneg();


                        if (!y.equals(_beliefTerm)) {
                            unifiedBeliefTerm[0] = y;
                            return false;  //done
                        }
                    }
                    return true;
                });

                u.symmetric = true;

                beliefConceptCanAnswerTaskConcept = u.transform(beliefTerm, beliefTerm, taskTerm, matchTTL) > 0;

                if (unifiedBeliefTerm[0] != null) {
                    beliefTerm = unifiedBeliefTerm[0];
                    beliefTransformed = true;
                }


            }
//            }
        }

        Task belief = match(d, beliefTerm, beliefConceptCanAnswerTaskConcept, beliefTransformed);

        d.reset(task, belief, belief != null ? belief.term() : beliefTerm.unneg());

        return belief;
    }

    @Nullable Task match(Derivation d, Term beliefTerm, boolean beliefConceptCanAnswerTaskConcept, boolean unifiedBelief) {

        NAR n = d.nar;

        Concept beliefConcept = beliefTerm.op().conceptualizable ?
                n.concept(beliefTerm)
                //n.conceptualize(beliefTerm)
                //n.conceptualizeDynamic(beliefTerm)
                :
                null;

        if (beliefConcept == null)
            return null;

        if (beliefConcept instanceof AliasConcept)
            beliefTerm = (beliefConcept = ((AliasConcept) beliefConcept).abbr).term(); //dereference alias

        if (!(beliefConcept instanceof TaskConcept))
            return null;

        Task belief = null;

        final BeliefTable beliefTable = beliefConcept.beliefs();
        final BeliefTable answerTable =
                (task.isQuest()) ?
                        beliefConcept.goals() :
                        beliefTable;

        if (answerTable == beliefTable) {
            if (beliefTable.isEmpty())
                return null; //no beliefs
        }

        if (beliefConceptCanAnswerTaskConcept && task.isQuestionOrQuest() && (task.isQuestion() || !answerTable.isEmpty()))
            belief = tryAnswer(beliefTerm, answerTable, d);

        if (belief == null)
            belief = tryMatch(beliefTerm, beliefTable, d);

        if (unifiedBelief && belief != null && Param.LINK_VARIABLE_UNIFIED_PREMISE) {
            linkVariable(unifiedBelief, d.nar, beliefConcept);
        }

        return belief;
    }

    private Predicate<Task> beliefFilter() {
        if (task.stamp().length == 0) {
            return t -> !t.equals(task) && t.stamp().length > 0; //dont allow derivation of 2 unstamped tasks - infinite feedback - dont cross the streams
        } else {
            return t -> !t.equals(task);//null; //stampFilter(d);
        }

    }

    private Task tryMatch(Term beliefTerm, BeliefTable bb, Derivation d) {
        long[] focus = timeFocus(d, beliefTerm);

        NAR nar = d.nar;
        Tense.dither(focus, nar);

        Predicate<Task> beliefFilter = beliefFilter();

        //dont dither because this task isnt directly input to the system.  derivations will be dithered at the end
        //TODO factor in the Task's stamp so it can try to avoid those tasks, thus causing overlap in double premise cases
        return Answer.relevance(true, Answer.TASK_LIMIT, focus[0], focus[1], beliefTerm, beliefFilter, nar)
                .match(bb)
                .task(false, true, false);
    }


    private Task tryAnswer(Term beliefTerm, BeliefTable answerTable, Derivation d) {


        if (!answerTable.isEmpty()) {

            Task match =
                    Answer.relevance(true, task.start(), task.end(), beliefTerm, null /*beliefFilter*/, d.nar)
                            .ditherTruth(true)
                            .match(answerTable).task(true, true, true);

            if (match != null) {
                //assert (task.isQuest() || match.punc() == BELIEF) : "quest answered with a belief but should be a goal";

                @Nullable Task answered = task.onAnswered(match, d.nar);
                if (answered != null) {

                    d.add(answered);

                    if (answered.isBelief())
                        return answered;
                }

            }
        }

        return null;
    }

    private long[] timeFocus(Derivation d, Term beliefTerm) {
        return d.deriver.timing.apply(task, beliefTerm);
    }

    private void linkVariable(boolean unifiedBelief, NAR n, Concept beliefConcept) {
        if (unifiedBelief) {
            Concept originalBeliefConcept = n.conceptualize(beliefTerm());
            if (originalBeliefConcept != null) {
                Concept taskConcept = n.concept(task.term(), true);


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

////    private boolean validMatch(@Nullable Task x) {
////        return x != null && !x.isDeleted() && !x.equals(task);
////    }
//
//    private Predicate<Task> stampFilter(Derivation d) {
//        ImmutableLongSet taskStamp =
//                Stamp.toSet(task);
//        return t -> !Stamp.overlapsAny(taskStamp, t.stamp());
//    }


    @Override
    public boolean equals(Object obj) {
        return this == obj ||
                (hashCode() == obj.hashCode() && ((Premise) obj).task.equals(task) && ((Premise) obj).beliefTerm().equals(beliefTerm()));
    }

    @Override
    public final int hashCode() {
        return (int) (hash >> 10) /* shift down about 10 bits to capture all 3 elements in the hash otherwise the task hash is mostly excluded */;
    }

    @Override
    public String toString() {
        return "Premise(" +
                task +
                " * " + beliefTerm() +
                ')';
    }


    public final void derive(Derivation d, int matchTTL, int deriveTTL) {

        if (task.isDeleted())
            return;

        Counter result;

        Emotion e = d.nar.emotion;

        match(d, matchTTL);

        if (d.deriver.rules.derivable(d)) {

            d.derive(deriveTTL);

            result = e.premiseFire; //premiseFired(p, d);

        } else {
            result = e.premiseUnderivable; //premiseUnderivable(p, d);
        }


        result.increment();


    }
}
