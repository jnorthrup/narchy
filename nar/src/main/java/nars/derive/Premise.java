/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package nars.derive;

import jcog.signal.meter.FastCounter;
import nars.*;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.op.mental.AliasConcept;
import nars.table.BeliefTable;
import nars.term.Term;
import nars.time.Tense;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static nars.time.Tense.ETERNAL;

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

    public final long hash;

    public Premise(Task task, Term beliefTerm) {
        super();

        this.task = task;

        this.beliefTerm = beliefTerm;

        this.hash = premiseHash(task, beliefTerm);
    }


    /**
     * specially constructed hash that is useful for sorting premises by:
     * a) task equivalency (hash)
     * a) task target equivalency (hash)
     * b) belief target equivalency (hash)
     * <p>
     * designed to maximize sequential repeat of derived task target
     */
    public static long premiseHash(Task task, Term beliefTerm) {
                //task's lower 23 bits in bits 40..64
        return (((long) task.hashCode()) << (64 - 24))
                | //task target's lower 20 bits in bits 20..40
                (((long) (task.term().hashCode() & 0b00000000000011111111111111111111)) << 20)
                | //termlink's lower 20 bits in bits 0..20
                ((beliefTerm.hashCode() & 0b00000000000011111111111111111111));
    }

    /**
     * variable types unifiable in premise formation
     */
    final static int var =
            //Op.VAR_QUERY.bit | Op.VAR_DEP.bit
            Op.VAR_QUERY.bit
            //Op.Variable //all
    ;

    /**
     * resolve the most relevant belief of a given target/concept
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

        boolean beliefConceptUnifiesTaskConcept = false;

        Term beliefTerm = this.beliefTerm;
        Term taskTerm = task.term();
        if (taskTerm.op() == beliefTerm.op()) {
            if (taskTerm.equals(beliefTerm)) {
                beliefConceptUnifiesTaskConcept = true;
            } else {

                if (beliefTerm.hasAny(var) || taskTerm.hasAny(var) || taskTerm.hasXternal() || beliefTerm.hasXternal()) {

                    Term unifiedBeliefTerm = d.unifyPremise.unified(taskTerm, beliefTerm, matchTTL);

                    if (unifiedBeliefTerm != null) {

                        if (beliefTerm!=unifiedBeliefTerm && (!unifiedBeliefTerm.isNormalized() && d.random.nextBoolean())) {
                            unifiedBeliefTerm = unifiedBeliefTerm.normalize();
                            beliefTerm = unifiedBeliefTerm;
                        }

                        beliefConceptUnifiesTaskConcept = true;
                    } else {
                        beliefConceptUnifiesTaskConcept = false;
                    }

                }
            }
        }

//        Term solved = Evaluation.solveFirst(beliefTerm, d.nar);
//        if (solved!=null && solved!=beliefTerm)
//            System.out.println(beliefTerm + " -> " + solved);

        Task belief = match(d, beliefTerm, beliefConceptUnifiesTaskConcept);

        if (task.stamp().length== 0) {
            //only allow unstamped tasks to apply with stamped beliefs.
            //otherwise stampless tasks could loop forever in single premise or in interaction with another stampless task
            if (belief==null || belief.stamp().length==0)
                return false;
        }

        Term nextBeliefTerm = belief != null ? belief.term() : beliefTerm.unneg();
        if (nextBeliefTerm.volume() > d.termVolMax)
            return false; //WTF

        if (!d.budget(task, belief))
            return false;

        Task task = this.task;
//        if (belief!=null) {
//            boolean te = task.isEternal(), be = belief.isEternal();
//            if (te ^ be) {
//                long now = d.time;
//                if (te) {
//
//                    //proxy task to now
//                    long[] nowOrBelief =
//                            (task.isGoal() || task.isQuest()) ?
//                                new long[] { now, now + belief.range() - 1 } //immediate
//                                //new long[] { d.dur + now, d.dur + now + belief.range() - 1 } //next dur
//                            :
//                                //Longerval.unionArray(belief.start(), belief.end(), now, now + belief.range() - 1);
//                                new long[] { belief.start(), belief.end() };
//                    nowOrBelief[0] = Tense.dither(nowOrBelief[0], d.ditherTime);
//                    nowOrBelief[1] = Tense.dither(nowOrBelief[1], d.ditherTime);
//                    task = new SpecialOccurrenceTask(task, nowOrBelief);
//
//                } else {
//                    //proxy belief to now
//                    long[] nowOrTask =
//                            new long[] { task.start(), task.end() };
//                            //Longerval.unionArray(task.start(), task.end(), now, now + task.range() - 1);
//                    nowOrTask[0] = Tense.dither(nowOrTask[0], d.ditherTime);
//                    nowOrTask[1] = Tense.dither(nowOrTask[1], d.ditherTime);
//
//                    belief = new SpecialOccurrenceTask(belief, nowOrTask);
//                }
//            }
//        }


        d.reset(task, belief, nextBeliefTerm);

        return true;
    }

    private @Nullable Task match(Derivation d, Term beliefTerm, boolean beliefConceptUnifiesTaskConcept) {

        NAR n = d.nar();

        Concept beliefConcept = beliefTerm.op().taskable ?
                n.conceptualizeDynamic(beliefTerm)
                //n.conceptualize(beliefTerm)
                //n.concept(beliefTerm)
                :
                null;

        /** dereference */
        if (beliefConcept instanceof AliasConcept)
            beliefTerm = (beliefConcept = ((AliasConcept) beliefConcept).abbr).term(); //dereference alias

        if (!(beliefConcept instanceof TaskConcept))
            return null;


        final BeliefTable beliefTable = beliefConcept.beliefs();

        if (beliefConceptUnifiesTaskConcept && task.isQuestionOrQuest()) {

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
                    if (answered.evi() >= d.eviMin) {

//                        d.addAt(answered); //TODO determine if inputting here is really only useful if revised or dynamic

//                        n.input(answered);

//                        if (answerGoal) {
//                            //store goals
//                            //d.add(answered);
//                        } else {
//                            //just emit if belief
                            n.eventTask.emit(answered);
//                        }
                    }

                }
                return answered;
            }
        }

        if (!beliefTable.isEmpty()) {
            return tryMatch(beliefTerm, beliefTable, d);
        }

//        if (unifiedBelief && belief != null && Param.LINK_VARIABLE_UNIFIED_PREMISE) {
//            linkVariable(unifiedBelief, d.nar, beliefConcept);
//        }

        return null;
    }

    private Predicate<Task> beliefFilter() {
//        if (task.stamp().length == 0) {
//            return t -> !t.equals(task) && t.stamp().length > 0; //dont allow derivation of 2 unstamped tasks - infinite feedback - dont cross the streams
//        } else {
            return t -> !t.equals(task);//null; //stampFilter(d);
//        }

    }

    private Task tryMatch(Term beliefTerm, BeliefTable bb, Derivation d) {
        long[] focus = timeFocus(beliefTerm, d);

        Predicate<Task> beliefFilter =
                beliefTerm.equalsRoot(task.term()) ?
                    beliefFilter() :
                    null;

        return bb.matching(focus[0], focus[1], beliefTerm, beliefFilter, d.dur(), d.nar())
                    .task(true, false, true);
    }


    private Task tryAnswer(Term beliefTerm, BeliefTable answerTable, Derivation d) {

        long ts = task.start(), te;
        if (ts == ETERNAL) {
            long[] f = timeFocus(beliefTerm, d);
            ts = f[0];
            te = f[1];
            assert(ts!=ETERNAL);
        } else {
            te = task.end();
        }
        Task match = answerTable.matching(ts, te, beliefTerm,
                null, d.dur(), d.nar())
                .task(true, false, false);


        if (match != null) {
            //assert (task.isQuest() || match.punc() == BELIEF) : "quest answered with a belief but should be a goal";

            return task.onAnswered(match, d.nar());

        }

        return null;
    }

    private long[] timeFocus(Term beliefTerm, Derivation d) {
        long[] l = d.deriver.timing.apply(d.what, task, beliefTerm);
        if (NAL.premise.PREMISE_FOCUS_TIME_DITHER && l[0]!=ETERNAL) {
            Tense.dither(l, d.ditherDT);
        }
        return l;
    }

//    private void linkVariable(boolean unifiedBelief, NAR n, Concept beliefConcept) {
//        if (unifiedBelief) {
//            Concept originalBeliefConcept = n.conceptualize(beliefTerm());
//            if (originalBeliefConcept != null) {
//                Concept taskConcept = n.concept(task.target(), true);
//
//
//                float pri = termLink.priElseZero() * n.activateLinkRate.floatValue();
//
//
//                Term moreConstantTerm = beliefConcept.target();
//                Term lessConstantTerm = originalBeliefConcept.target();
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

        FastCounter result;

        Emotion e = d.nar().emotion;

        if (match(d, matchTTL)) {

            short[] can = d.deriver.what(d);

            if (can.length > 0) {

                d.derive(
                    //Util.lerp(Math.max(d.priDouble, d.priSingle), Param.TTL_MIN, deriveTTL)
                    deriveTTL, can
                );

                result = e.premiseFire;

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
