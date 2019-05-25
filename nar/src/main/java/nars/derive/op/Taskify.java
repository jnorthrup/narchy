package nars.derive.op;

import jcog.Util;
import jcog.util.ArrayUtil;
import nars.*;
import nars.control.MetaGoal;
import nars.derive.model.Derivation;
import nars.derive.model.DerivationFailure;
import nars.derive.rule.PremiseRuleProto;
import nars.op.mental.Abbreviation;
import nars.task.DebugDerivedTask;
import nars.task.DerivedTask;
import nars.term.ProxyTerm;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.time.Tense;
import nars.truth.Truth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nars.NAL.derive.DERIVE_FILTER_SIMILAR_TO_PARENTS;
import static nars.Op.*;
import static nars.derive.model.DerivationFailure.Success;
import static nars.time.Tense.ETERNAL;

public class Taskify extends ProxyTerm {

    private final static Logger logger = LoggerFactory.getLogger(Taskify.class);
    private static final Atomic TASKIFY = Atomic.the("Taskify");
    /**
     * destination of any derived tasks; also may be used to communicate backpressure
     * from the recipient.
     */
    public final PremiseRuleProto.RuleWhy channel;
    final Termify termify;

    public Taskify(Termify termify, PremiseRuleProto.RuleWhy channel) {
        this($.funcFast(TASKIFY, termify, $.the(channel.id)), termify, channel);
    }


    private Taskify(Term id, Termify termify, PremiseRuleProto.RuleWhy channel) {
        super(id);
        this.termify = termify;
        this.channel = channel;
    }


//    /** applies variable introduction between Termify and Taskify step */
//    public static class VarTaskify extends Taskify {
//
//        private static final IntroVars introVars = new IntroVars();
//
//        public VarTaskify(Termify termify, PremiseRuleProto.RuleCause channel) {
//            super($.funcFast(TASKIFY, $.func(IntroVars.VarIntro, termify), $.the(channel.id)), termify, channel);
//        }
//
//        public Term test(Term x, Derivation d) {
//            Term y = termify.test(x, d);
//            if (y!=null) {
//                Term z = introVars.test(y, d);
//                return (z != null) ? (taskify(z, d) ? z : null) : null; //HACK
//            } else
//                return null;
//        }
//
//    }

    boolean spam(Derivation d, int cost) {
        d.use(cost);

        MetaGoal.Futile.learn(cost, d.nar.control.why, channel.id);

        return true;
    }

    public void apply(Term x, Derivation d) {

        d.nar.emotion.deriveTermify.increment();

        DerivationFailure fail = DerivationFailure.failure(x,
                (byte) 0 /* dont consider punc consequences until after temporalization */,
                d);

        if (fail == Success) {
            if (d.temporal)
                Occurrify.temporalTask(x, termify.time, this, d);
            else
                Occurrify.eternalTask(x, this, d);
        }

    }

    /**
     * use special eternal pattern if non-temporal belief or goal.  but questions always use the default temporal form (allowing, ex: (a ==>+- a)?
     */
    Term pattern(Derivation d) {
        return (d.temporal || (d.concPunc == QUESTION || d.concPunc == QUEST)) ? termify.pattern : termify.patternEternal;
    }

    /**
     * note: the return value here shouldnt matter so just return true anyway
     */
    protected void taskify(Term x0, long start, long end, Derivation d) {

        Term x = Task.normalize(x0);

        Op xo = x.op();


        NAR nar = d.nar();
        final byte punc = d.concPunc;
        if (punc == 0)
            throw new RuntimeException("no punctuation assigned");



//        if (punc == GOAL && d.taskPunc == GOAL) {
//            //check for contradictory goal derivation
//            if (LongInterval.minTimeTo(d._task, start, end) < d.dur() + d.taskTerm.eventRange() + x.unneg().eventRange()) {
//                Term posTaskGoal = d.taskTerm.negIf(d.taskTruth.isNegative());
//                Term antiTaskGoal = posTaskGoal.neg();
//                Term cc = x.negIf(d.concTruth.isNegative());
//
//                if (
//                    cc.equals(antiTaskGoal)
//                    //|| (cc.op() == CONJ && Conj.containsEvent(cc, antiTaskGoal))
//                    //|| (posTaskGoal.op() == CONJ && Conj.containsEvent(posTaskGoal, cc.neg()))
//                ) {
//                    nar.emotion.deriveFailTaskifyGoalContradiction.increment();
//                    spam(d, NAL.derive.TTL_COST_DERIVE_TASK_UNPRIORITIZABLE);
//                    return;
//                }
//            }
//        }

        boolean neg = xo == NEG;
        if (neg) {
            x = x.unneg();
            xo = x.op();
        }
        if (!xo.taskable) {
            spam(d, NAL.derive.TTL_COST_DERIVE_TASK_FAIL);
            return;
        }






        Truth tru;
        if (punc == BELIEF || punc == GOAL) {

            //dither truth
            tru = d.concTruth.dither(nar.freqResolution.floatValue(), nar.confResolution.floatValue(), d.eviMin, neg);
            if (tru == null) {
                nar.emotion.deriveFailTaskifyTruthUnderflow.increment();
                spam(d, NAL.derive.TTL_COST_DERIVE_TASK_UNPRIORITIZABLE);
                return;
            }

        } else {
            tru = null; //questions and quests
        }


        long S, E;
        if (start != ETERNAL) {
            assert (start <= end) : "reversed occurrence: " + start + ".." + end;

            int dither = d.ditherDT;
            S = Tense.dither(start, dither);
            E = Tense.dither(end, dither);

        } else {
            S = E = ETERNAL;
        }

//        /** compares taskTerm before un-anon */
//        if (isSame(x, punc, tru, S, E, d.taskTerm, d._task, nar)) {
//            same(d, nar);
//            return;
//        }
//        /** compares beliefTerm before un-anon */
//        if (d._belief != null && isSame(x, punc, tru, S, E, d.beliefTerm, d._belief, nar)) {
//            same(d, nar);
//            return;
//        }

        /** un-anon */
        x = d.anon.get(x);
        if (x == null)
            throw new NullPointerException("could not un-anonymize " + x0 + " with " + d.anon);

        //abbreviate TODO combine this with anon step by editing the substitution map
        x = Abbreviation.abbreviate(x, nar);

        /** compares taskTerm un-anon */
        if (isSame(x, punc, tru, S, E, d._task.term(), d._task, nar)) {
            same(d, nar);
            return;
        }
        /** compares beliefTerm un-anon */
        if (d._belief != null && isSame(x, punc, tru, S, E, d._belief.term(), d._belief, nar)) {
            same(d, nar);
            return;
        }

        DerivedTask t = Task.tryTask(x, punc, tru, (C, tr) ->
                NAL.DEBUG ?
                        new DebugDerivedTask(C, punc, tr, S, E, d) :
                        new DerivedTask(C, punc, tr, d.time(), S, E, d.evidence())
        );

        if (t == null) {
            nar.emotion.deriveFailTaskify.increment();
            spam(d, NAL.derive.TTL_COST_DERIVE_TASK_FAIL);
            return;
        }


        float priority = d.what.derivePri.pri(t, d);
        if (priority != priority) {
            nar.emotion.deriveFailPrioritize.increment();
            spam(d, NAL.derive.TTL_COST_DERIVE_TASK_UNPRIORITIZABLE);
            return;
        }

        //these must be applied before possible merge on input to derivedTask bag
        t.cause(ArrayUtil.add(d.parentCause, channel.id));

        if ((d.concSingle) || (NAL.OVERLAP_DOUBLE_SET_CYCLIC && d.overlapDouble))
            t.setCyclic(true);

        t.pri(priority);


        int cost;
        Task u = d.add(t);
        if (u != t) {

            nar.emotion.deriveFailDerivationDuplicate.increment();
            cost = NAL.derive.TTL_COST_DERIVE_TASK_REPEAT;

        } else {

            if (NAL.DEBUG)
                t.log(channel.ruleString);

            nar.emotion.deriveTask.increment();
            cost = NAL.derive.TTL_COST_DERIVE_TASK_SUCCESS;

        }

        d.use(cost);
    }

    private boolean same(Derivation d, NAR nar) {
        nar.emotion.deriveFailParentDuplicate.increment();
        return spam(d, NAL.derive.TTL_COST_DERIVE_TASK_SAME);
    }

    private boolean isSame(Term derived, byte punc, Truth truth, long start, long end, Term parentTerm, Task parent, NAR n) {

        if (DERIVE_FILTER_SIMILAR_TO_PARENTS) {

            if (parent.isDeleted())
                return false;

            if (parent.punc() == punc) {
                if (parentTerm.equals(derived)) { //TODO test for dtDiff
                    if (parent.containsSafe(start, end)) {

                        if ((punc == QUESTION || punc == QUEST) || (
                                Util.equals(parent.freq(), truth.freq(), n.freqResolution.floatValue()) &&
                                        parent.conf() >= truth.conf() - n.confResolution.floatValue() / 2
                                // / 2 /* + epsilon to avid creeping confidence increase */
                        )) {

                            if (NAL.DEBUG_SIMILAR_DERIVATIONS)
                                logger.warn("similar derivation to parent:\n\t{} {}\n\t{}", derived, parent, channel.ruleString);

                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    //    @Deprecated
//    protected boolean same(Task derived, Task parent, float truthResolution) {
//        if (parent.isDeleted())
//            return false;
//
//        if (derived.equals(parent)) return true;
//
//        if (FILTER_SIMILAR_DERIVATIONS) {
//
//            if (parent.target().equals(derived.target()) && parent.punc() == derived.punc() &&
//                    parent.start() == derived.start() && parent.end() == derived.end()) {
//                /*if (Arrays.equals(derived.stamp(), parent.stamp()))*/
//                if (parent.isQuestionOrQuest() ||
//                        (Util.equals(parent.freq(), derived.freq(), truthResolution) &&
//                                parent.evi() >= derived.evi())
//                ) {
//                    if (Param.DEBUG_SIMILAR_DERIVATIONS)
//                        logger.warn("similar derivation to parent:\n\t{} {}\n\t{}", derived, parent, channel.ruleString);
//
//
//                    return true;
//                }
//            }
//        }
//        return false;
//    }

}
