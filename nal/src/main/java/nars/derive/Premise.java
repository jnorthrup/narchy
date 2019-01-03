/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package nars.derive;

import com.netflix.servo.monitor.Counter;
import nars.Emotion;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.op.mental.AliasConcept;
import nars.table.BeliefTable;
import nars.task.proxy.SpecialOccurrenceTask;
import nars.term.Term;
import nars.time.Tense;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Defines the conditions used in an instance of a derivation
 * Contains the information necessary for generating derivation Tasks via reasoning rules.
 * <p>
 * It is meant to be disposable and should not be kept referenced longer than necessary
 * to avoid GC loops, so it may need to be weakly referenced.
 *
 * note: Comparable as implemented here is not 100% consistent with Task.equals and Term.equals.  it is
 * sloppily consistent for its purpose in collating Premises in optimal sorts during hypothesizing
 */
public class Premise implements Comparable<Premise> {

    public final Task task;

    public final Term beliefTerm;

    /**
     * specially constructed hash that is useful for sorting premises by:
     * a) task equivalency (hash)
     * a) task term equivalency (hash)
     * b) belief term equivalency (hash)
     * <p>
     * designed to maximize sequential repeat of derived task term
     */
    public final long hash;

    public Premise(Task task, Term beliefTerm) {
        super();

        this.task = task;

        this.beliefTerm = beliefTerm;

        this.hash =
                //task's lower 23 bits in bits 40..64
                (((long) task.hashCode()) << (64 - 24))
                        | //task term's lower 20 bits in bits 20..40
                        (((long) (task.term().hashCode() & 0b00000000000011111111111111111111)) << 20)
                        | //termlink's lower 20 bits in bits 0..20
                        ((beliefTerm.hashCode() & 0b00000000000011111111111111111111));
    }

    /**
     * variable types unifiable in premise formation
     */
    final static int var =
            Op.VAR_QUERY.bit | Op.VAR_DEP.bit
            //Op.VAR_QUERY.bit
            //Op.Variable //all
    ;

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
    private boolean match(Derivation d, int matchTTL) {

        boolean beliefConceptCanAnswerTaskConcept = false;

        Term beliefTerm = this.beliefTerm;
        //if (task.isQuestionOrQuest())
        {
            Term taskTerm = task.term();
            if (taskTerm.op() == beliefTerm.op()) {
                if (taskTerm.equals(beliefTerm)) {
                    beliefConceptCanAnswerTaskConcept = true;
                } else {

                    if (beliefTerm.hasAny(var) || taskTerm.hasAny(var) || taskTerm.hasXternal() || beliefTerm.hasXternal()) {

                        Term unifiedBeliefTerm = d.unifyPremise.unified(taskTerm, beliefTerm, matchTTL);

                        if (unifiedBeliefTerm != null) {


                            if (!unifiedBeliefTerm.isNormalized() && d.random.nextBoolean())
                                unifiedBeliefTerm = unifiedBeliefTerm.normalize();

                            beliefTerm = unifiedBeliefTerm;
                            beliefConceptCanAnswerTaskConcept = true;
                        } else {
                            beliefConceptCanAnswerTaskConcept = false;
                        }

                    }
                }
            }
        }

//        Term solved = Evaluation.solveFirst(beliefTerm, d.nar);
//        if (solved!=null && solved!=beliefTerm)
//            System.out.println(beliefTerm + " -> " + solved);

        Task belief = match(d, beliefTerm, beliefConceptCanAnswerTaskConcept);

        Term nextBeliefTerm = belief != null ? belief.term() : beliefTerm.unneg();
        if (nextBeliefTerm.volume() > d.termVolMax)
            return false; //WTF

        if (!d.budget(task, belief))
            return false;

        Task task = this.task;
        if (belief!=null) {
            boolean te = task.isEternal(), be = belief.isEternal();
            if (te ^ be) {
                long now = d.time;
                if (te) {

                    //proxy task to now
                    long[] nowOrBelief =
                            (task.isGoal() || task.isQuest()) ?
                                new long[] { now, now + belief.range() - 1 } //immediate
                                //new long[] { d.dur + now, d.dur + now + belief.range() - 1 } //next dur
                            :
                                //Longerval.unionArray(belief.start(), belief.end(), now, now + belief.range() - 1);
                                new long[] { belief.start(), belief.end() };
                    nowOrBelief[0] = Tense.dither(nowOrBelief[0], d.ditherTime);
                    nowOrBelief[1] = Tense.dither(nowOrBelief[1], d.ditherTime);
                    task = new SpecialOccurrenceTask(task, nowOrBelief);

                } else {
                    //proxy belief to now
                    long[] nowOrTask =
                            new long[] { task.start(), task.end() };
                            //Longerval.unionArray(task.start(), task.end(), now, now + task.range() - 1);
                    nowOrTask[0] = Tense.dither(nowOrTask[0], d.ditherTime);
                    nowOrTask[1] = Tense.dither(nowOrTask[1], d.ditherTime);

                    belief = new SpecialOccurrenceTask(belief, nowOrTask);
                }
            }
        }


        d.reset(task, belief, nextBeliefTerm);

        return true;
    }

    @Nullable Task match(Derivation d, Term beliefTerm, boolean beliefConceptCanAnswerTaskConcept) {

        NAR n = d.nar;

        Concept beliefConcept = beliefTerm.op().conceptualizable ?
                n.conceptualizeDynamic(beliefTerm)
                //n.concept(beliefTerm)
                //n.conceptualize(beliefTerm)
                :
                null;

        if (beliefConcept instanceof AliasConcept)
            beliefTerm = (beliefConcept = ((AliasConcept) beliefConcept).abbr).term(); //dereference alias

        if (!(beliefConcept instanceof TaskConcept))
            return null;

        Task belief = null;

        final BeliefTable beliefTable = beliefConcept.beliefs();

        if (beliefConceptCanAnswerTaskConcept && task.isQuestionOrQuest()) {

            boolean answerGoal = task.isQuest();

            final BeliefTable answerTable =
                    answerGoal ?
                            beliefConcept.goals() :
                            beliefTable;

            if (answerTable.isEmpty()) {
                if (!answerGoal)
                    return null; //no belief
            } else {
                Task answered = tryAnswer(beliefTerm, answerTable, d);
                if (answered!=null) {


//                    if (Param.INPUT_PREMISE_ANSWER_BELIEF || answered.isGoal()) // || (!(answered instanceof SignalTask) && !(answered instanceof DerivedTask)))
                    if (answered.isGoal())
                        n.input(answered);
////                        d.add(answered); //TODO inputting here is really only useful if revised or dynamic
                    else
                        n.eventTask.emit(answered, answered.punc());
//
//                        else //if (answered.isBelief())
//                            return answered;


                }
                if (answerGoal) {
                    //goal, already added in tryAnswer
                } else {
                    //belief
                    return answered;
                }
            }
        }

        if (belief == null && !beliefTable.isEmpty())
            belief = tryMatch(beliefTerm, beliefTable, d);

//        if (unifiedBelief && belief != null && Param.LINK_VARIABLE_UNIFIED_PREMISE) {
//            linkVariable(unifiedBelief, d.nar, beliefConcept);
//        }

        return belief;
    }

    private Predicate<Task> beliefFilter() {
//        if (task.stamp().length == 0) {
//            return t -> !t.equals(task) && t.stamp().length > 0; //dont allow derivation of 2 unstamped tasks - infinite feedback - dont cross the streams
//        } else {
            return t -> !t.equals(task);//null; //stampFilter(d);
//        }

    }

    private Task tryMatch(Term beliefTerm, BeliefTable bb, Derivation d) {
        long[] focus = timeFocus(d, beliefTerm);

        NAR nar = d.nar;
        Tense.dither(focus, nar);

        Predicate<Task> beliefFilter =
                beliefTerm.equalsRoot(task.term()) ?
                    beliefFilter() :
                    null;

        return bb.sample(focus[0], focus[1], beliefTerm, beliefFilter, nar);
    }


    private Task tryAnswer(Term beliefTerm, BeliefTable answerTable, Derivation d) {

        Task match = answerTable.answer(task.start(), task.end(), beliefTerm, d.nar);

//        long qStart = task.start();
//        //int limit = qStart == ETERNAL ? Answer.TASK_LIMIT_ETERNAL_QUESTION : Answer.TASK_LIMIT_DEFAULT;
//        Task match =
//                Answer.relevance(true, limit,
//                        qStart, task.end(),
//                        beliefTerm, null /*beliefFilter*/, d.nar)
//                        //.ditherTruth(true)
//                        .match(answerTable)
//
//                        .task(true, true, true);

        if (match != null) {
            //assert (task.isQuest() || match.punc() == BELIEF) : "quest answered with a belief but should be a goal";

            @Nullable Task answered = task.onAnswered(match, d.nar);

            return answered;

        }

        return null;
    }

    private long[] timeFocus(Derivation d, Term beliefTerm) {
        return d.deriver.timing.apply(task, beliefTerm);
    }

//    private void linkVariable(boolean unifiedBelief, NAR n, Concept beliefConcept) {
//        if (unifiedBelief) {
//            Concept originalBeliefConcept = n.conceptualize(beliefTerm());
//            if (originalBeliefConcept != null) {
//                Concept taskConcept = n.concept(task.term(), true);
//
//
//                float pri = termLink.priElseZero() * n.activateLinkRate.floatValue();
//
//
//                Term moreConstantTerm = beliefConcept.term();
//                Term lessConstantTerm = originalBeliefConcept.term();
//
//
//                beliefConcept.termlinks().putAsync(new PLink<>(lessConstantTerm, pri / 2f));
//
//                originalBeliefConcept.termlinks().putAsync(new PLink<>(moreConstantTerm, pri / 2f));
//
//
//                if (taskConcept != null)
//                    taskConcept.termlinks().putAsync(new PLink<>(moreConstantTerm, pri));
//
//
//            }
//        }
//    }

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
                (hashCode() == obj.hashCode() && ((Premise) obj).task.equals(task) && ((Premise) obj).beliefTerm.equals(beliefTerm));
    }

    @Override
    public final int hashCode() {
        return (int) (hash >> 10) /* shift down about 10 bits to capture all 3 elements in the hash otherwise the task hash is mostly excluded */;
    }

    @Override
    public String toString() {
        return "Premise(" +
                task +
                " * " + beliefTerm +
                ')';
    }


    public final void derive(Derivation d, int matchTTL, int deriveTTL) {

        Counter result;

        Emotion e = d.nar.emotion;

        if (match(d, matchTTL)) {

            if (d.deriver.rules.derivable(d)) {

                d.derive(deriveTTL);

                result = e.premiseFire; //premiseFired(p, d);

            } else {
                result = e.premiseUnderivable;
            }
        } else {
            result = e.premiseUnbudgetable;
        }


        result.increment();



    }

    @Override
    public int compareTo(Premise premise) {
        if (this == premise)
            return 0;

        int h = Long.compare(hash, premise.hash);
        if (h!=0)
            return h;

        if (task.equals(premise.task) && beliefTerm.equals(premise.beliefTerm))
            return 0;

        //TODO since Task doesnt implement Comparable, they could be compared by their byte[] serialization
//        int t = Integer.compare(System.identityHashCode(task), System.identityHashCode(premise.task));
//        if (t!=0)
//            return t;
//
//        int b = Integer.compare(System.identityHashCode(beliefTerm.hashCode()), System.identityHashCode(premise.beliefTerm.hashCode()));
//        if (b!=0)
//            return b;

        return Integer.compare(System.identityHashCode(this), System.identityHashCode(premise));
    }

}
