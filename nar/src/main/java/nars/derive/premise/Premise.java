/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package nars.derive.premise;

import jcog.WTF;
import jcog.signal.meter.FastCounter;
import nars.*;
import nars.derive.model.Derivation;
import nars.derive.model.PreDerivation;
import nars.table.BeliefTable;
import nars.task.proxy.ImageTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.util.Image;
import nars.time.Tense;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static nars.Op.BELIEF;
import static nars.Op.VAR_QUERY;
import static nars.term.atom.Bool.Null;
import static nars.time.Tense.ETERNAL;

/**
 * Defines the conditions used in an instance of a derivation
 * Contains the information necessary for generating derivation Tasks via reasoning rules.
 * <p>
 * It is meant to be disposable and should not be kept referenced longer than necessary
 * to avoid GC loops, so it may need to be weakly referenced.
 * <p>
 * note: Comparable as implemented here is not 100% consistent with Task.equals and Term.equals.  it is
 * sloppily consistent for its purpose in collating Premises in optimal sorts during hypothesizing
 */
public class Premise implements Comparable<Premise> {

    public static final Premise[] EmptyArray = new Premise[0];
    /**
     * variable types unifiable in premise formation
     */
    final static int var =
            //Op.VAR_QUERY.bit | Op.VAR_DEP.bit
            VAR_QUERY.bit
            //Op.Variable //all
            ;
    public final Task task;
    public final Term beliefTerm;
    public final long hash;


    public Premise(Task task, Term beliefTerm) {
        super();

        //normalize the image if premise doesnt involve Image-specific derivation
        //TODO check for non-ImageTask images
        if (task instanceof ImageTask &&
                ((beliefTerm instanceof Compound && !beliefTerm.op().isAny(Op.INH.bit | Op.SIM.bit))
                        ||
                        (beliefTerm instanceof Atomic && task.term().containsRecursively(beliefTerm))
                )
        ) {
            task = ((ImageTask) task).task;
        }
        if (beliefTerm instanceof Compound) {
            if (!task.term().op().isAny(Op.INH.bit | Op.SIM.bit)) {
                Term ib = Image.imageNormalize(beliefTerm);
                if (ib != beliefTerm)
                    beliefTerm = ib;
            }
        }


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
    private static long premiseHash(Task task, Term beliefTerm) {
        //task's lower 23 bits in bits 40..64
        return (((long) task.hashCode()) << (64 - 24))
                | //task target's lower 20 bits in bits 20..40
                (((long) (task.term().hashCode() & 0b00000000000011111111111111111111)) << 20)
                | //termlink's lower 20 bits in bits 0..20
                ((beliefTerm.hashCode() & 0b00000000000011111111111111111111));
    }

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
    public boolean match(Derivation d, int matchTTL) {

        boolean beliefConceptUnifiesTaskConcept = false;

        Term beliefTerm = this.beliefTerm;
        Term taskTerm = task.term();
        if (taskTerm.equals(beliefTerm)) {
            beliefConceptUnifiesTaskConcept = true;
        } else if (taskTerm.op() == beliefTerm.op()) {

            if (taskTerm.equalsRoot(beliefTerm)) {
                //difference involving XTERNAL etc
                beliefConceptUnifiesTaskConcept = true;

            } else if (beliefTerm.hasAny(var) || taskTerm.hasAny(var)) {

                Term unifiedBeliefTerm = d.premiseUnify.unified(taskTerm, beliefTerm, matchTTL);

                if (unifiedBeliefTerm != null) {

                    unifiedBeliefTerm = d.random.nextBoolean() ?
                            unifiedBeliefTerm
                            :
                            unifiedBeliefTerm.normalize();

                    beliefTerm = unifiedBeliefTerm;

                    beliefConceptUnifiesTaskConcept = true;
                } else {
                    beliefConceptUnifiesTaskConcept = false;
                }

            }

        }


        Task belief = match(d, beliefTerm, beliefConceptUnifiesTaskConcept);

        if (task != belief && task.stamp().length == 0) {
            //only allow unstamped tasks to apply with stamped beliefs.
            //otherwise stampless tasks could loop forever in single premise or in interaction with another stampless task
            if (belief == null || belief.stamp().length == 0)
                return false;
        }

        Term nextBeliefTerm = belief != null ? belief.term() : beliefTerm;//.unneg();
        if (nextBeliefTerm.volume() > d.termVolMax)
            return false; //WTF

        if (!d.budget(task, belief))
            return false;

//        System.out.println(task + "\t" + belief + "\t" + nextBeliefTerm);

        d.reset(this.task, belief, nextBeliefTerm);

        return d.taskTerm != Null;
    }


    private @Nullable Task match(Derivation d, Term beliefTerm, boolean beliefConceptUnifiesTaskConcept) {

        if (beliefTerm.op().taskable && beliefTerm.isNormalized() && !beliefTerm.hasAny(VAR_QUERY)) {

            NAR n = d.nar();

            final BeliefTable beliefTable = n.tableDynamic(beliefTerm, true);

            boolean answerGoal = task.isQuest();

            if (beliefConceptUnifiesTaskConcept && task.isQuestionOrQuest()) {

                final BeliefTable answerTable = answerGoal ? n.tableDynamic(beliefTerm, false) : beliefTable;

                if (answerTable != null && !answerTable.isEmpty()) {
                    Task a = tryAnswer(beliefTerm, answerTable, d);
                    if (a != null) {
//                        if (a.conf() < d.confMin)
//                            a = null;
//                        else {
                            if (!(((!answerGoal && a.isBelief()) || (answerGoal && a.isGoal()))))
                                throw new WTF();

                            if (answerGoal)
                                d.what.accept(a);
                            else {
                                //if (task.isInput()) { }
                                d.what.emit(a);
                                //d.what.accept(a);
                            }
//                        }

                    }
                    if (!answerGoal)
                        return a;
                }

            }

            return beliefTable != null && !beliefTable.isEmpty() ?
                    tryMatch(beliefTerm, beliefTable, beliefConceptUnifiesTaskConcept, d) : null;

        } else
            return null;
    }

//        if (unifiedBelief && belief != null && Param.LINK_VARIABLE_UNIFIED_PREMISE) {
//            linkVariable(unifiedBelief, d.nar, beliefConcept);
//        }



    private Task tryMatch(Term beliefTerm, BeliefTable bb, boolean beliefConceptUnifiesTaskConcept, Derivation d) {

        Predicate<Task> beliefFilter =
                beliefConceptUnifiesTaskConcept && task.punc() == BELIEF ?
                        t -> !t.equals(task) :
                        null;

        long[] focus = timeFocus(beliefTerm, d);

        boolean topOrSample =
                true;
        //false;

        return bb.matching(focus[0], focus[1], beliefTerm, beliefFilter, d.dur(), d.nar())
                .task(topOrSample, false, false);
    }


    private Task tryAnswer(Term beliefTerm, BeliefTable answerTable, Derivation d) {

//        long ts = task.start(), te;
//        if (ts == ETERNAL) {
            long[] f = timeFocus(beliefTerm, d);
            long ts = f[0];
            long te = f[1];
            assert (ts != ETERNAL);
//        } else {
//            te = task.end();
//        }
        Task match = answerTable.matching(ts, te, beliefTerm,
                null, d.dur(), d.nar())
                .task(true, false, false);


        if (match != null) {
            //assert (task.isQuest() || match.punc() == BELIEF) : "quest answered with a belief but should be a goal";

            return task.onAnswered(match);

        }

        return null;
    }

    private long[] timeFocus(Term beliefTerm, Derivation d) {
        long[] l = d.deriver.timing.apply(d.what, task, beliefTerm);
        if (NAL.premise.PREMISE_FOCUS_TIME_DITHER && l[0] != ETERNAL) {
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


    @Override
    public int compareTo(Premise premise) {
        if (this == premise)
            return 0;

        int h = Long.compare(hash, premise.hash);
        if (h != 0)
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

    public void derive(Derivation d, int matchTTL, int deriveTTL) {
        FastCounter result;

        Emotion e = d.nar.emotion;

        int ttlUsed;

        if (match(d, matchTTL)) {

            result = PreDerivation.run(d, deriveTTL) ? e.premiseFire : e.premiseUnderivable;

            ttlUsed = Math.max(0, deriveTTL - d.ttl);

        } else {
            result = e.premiseUnbudgetable;
            ttlUsed = 0;
        }

        e.premiseTTL_used.recordValue(ttlUsed); //TODO handle negative amounts, if this occurrs.  limitation of HDR histogram
        result.increment();

    }
}
