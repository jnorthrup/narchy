package nars.derive.op;

import jcog.Util;
import jcog.util.ArrayUtil;
import nars.$;
import nars.NAL;
import nars.NAR;
import nars.Task;
import nars.control.MetaGoal;
import nars.derive.model.Derivation;
import nars.derive.model.DerivationFailure;
import nars.derive.rule.PremiseRuleProto;
import nars.task.DebugDerivedTask;
import nars.task.DerivedTask;
import nars.term.Neg;
import nars.term.ProxyTerm;
import nars.term.Term;
import nars.time.Tense;
import nars.truth.Truth;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static nars.NAL.derive.DERIVE_FILTER_SIMILAR_TO_PARENTS;
import static nars.Op.*;
import static nars.derive.model.DerivationFailure.Success;
import static nars.time.Tense.*;

public class Taskify extends ProxyTerm {

    private final static Logger logger = LoggerFactory.getLogger(Taskify.class);
    /**
     * destination of any derived tasks; also may be used to communicate backpressure
     * from the recipient.
     */
    public final PremiseRuleProto.RuleWhy channel;
    final Termify termify;

    public Taskify(Termify termify, PremiseRuleProto.RuleWhy channel) {
        super($.pFast(termify, $.the(channel.id)));
        this.termify = termify;
        this.channel = channel;
    }


    boolean spam(Derivation d, int cost) {
        d.use(cost);

        MetaGoal.Futile.learn(cost, d.nar.control.why, channel.id);

        return true;
    }

    void eternalTask(Term x, Derivation d) {

//        byte punc = d.concPunc;
//        if ((punc == BELIEF || punc == GOAL) && x.hasXternal()) { // && !d.taskTerm.hasXternal() && !d.beliefTerm.hasXternal()) {
//            //HACK this is for deficiencies in the temporal solver that can be fixed
//
//            x = Retemporalize.retemporalizeXTERNALToDTERNAL.apply(x);
//
//            if (!DerivationFailure.failure(x, d.concPunc)) {
//                d.nar.emotion.deriveFailTemporal.increment();
//                spam(d, NAL.derive.TTL_COST_DERIVE_TASK_FAIL);
//                return;
//            }
//        }

        taskify(x, ETERNAL, ETERNAL, d);
    }

    void temporalTask(Term x, Occurrify.OccurrenceSolver time, Derivation d) {



        boolean neg = false;
        Term xx = x;
        if (x instanceof Neg && (!d.taskTerm.hasAny(NEG) && !d.beliefTerm.hasAny(NEG))) {
            //HACK semi-auto-unneg to help occurrify
            x = x.unneg();
            neg = true;
        }

        Pair<Term, long[]> timing = time.occurrence(x, d);
        if (timing == null) {
            d.nar.emotion.deriveFailTemporal.increment();
            return;
        }

        Term y = timing.getOne();

        long[] occ = timing.getTwo();

        if (!((occ[0] != TIMELESS) && (occ[1] != TIMELESS) &&
                (occ[0] == ETERNAL) == (occ[1] == ETERNAL) &&
                (occ[1] >= occ[0])) || (occ[0] == ETERNAL && !d.occ.validEternal()))
            throw new RuntimeException("bad occurrence result: " + Arrays.toString(occ));

        if (NAL.derive.DERIVE_QUESTION_FROM_AMBIGUOUS_BELIEF_OR_GOAL && (d.concPunc == BELIEF || d.concPunc == GOAL)) {
            if (DerivationFailure.failure(y, d.concPunc)) {

                //as a last resort, try forming a question from the remains
                byte qPunc = d.concPunc == BELIEF ? QUESTION : QUEST;
                d.concPunc = qPunc;
                if (DerivationFailure.failure(y, d) == Success) {
                    d.concPunc = qPunc;
                    d.concTruth = null;
                } else {
                    d.nar.emotion.deriveFailTemporal.increment();
                    return; //fail
                }

            } //else: ok
        } else {
            if (DerivationFailure.failure(y, d) != Success) {
                d.nar.emotion.deriveFailTemporal.increment();
                return;
            }
        }

        if (NAL.test.DEBUG_ENSURE_DITHERED_DT)
            assertDithered(y, d.ditherDT);

        taskify(y.negIf(neg), occ[0], occ[1], d);
    }



    /**
     * note: the return value here shouldnt matter so just return true anyway
     */
    protected void taskify(Term x0, long start, long end, Derivation d) {

        final byte punc = d.concPunc;
        if (punc == 0)
            throw new RuntimeException("no punctuation assigned");

        /** un-anon */
        Term x1 = d.anon.get(x0);
        if (x1 == null)
            throw new NullPointerException("could not un-anonymize " + x0 + " with " + d.anon);

        NAR nar = d.nar();

        @Nullable ObjectBooleanPair<Term> xn = Task.tryTaskTerm(x1, punc, !NAL.test.DEBUG_EXTRA);
        if (xn == null) {
            nar.emotion.deriveFailTaskify.increment();
            spam(d, NAL.derive.TTL_COST_DERIVE_TASK_FAIL);
            return;
        }
        Term x = xn.getOne();

//        Op xo = x.op();






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

        boolean neg = xn.getTwo();

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
            if (dither > 1) {
                S = Tense.dither(start, dither, -1);
                E = Tense.dither(end, dither, +1);
            } else {
                S = start; E = end;
            }

        } else {
            S = E = ETERNAL;
        }



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

        //abbreviate TODO combine this with anon step by editing the substitution map
//        if (x.volume() > d.termVolMax/2)
//            x = Abbreviation.abbreviate(x, nar);

        DerivedTask t = //Task.tryTask(x, punc, tru, (C, tr) -> {
                //return
                NAL.DEBUG ?
                            new DebugDerivedTask(x, punc, tru, S, E, d) :
                            new DerivedTask(x, punc, tru, d.time(), S, E, d.evidence());
                //};
        //);




        float priority = d.what.derivePri.pri(t, d);
        if (priority != priority) {
            nar.emotion.deriveFailPrioritize.increment();
            spam(d, NAL.derive.TTL_COST_DERIVE_TASK_UNPRIORITIZABLE);
            return;
        }

        //these must be applied before possible merge on input to derivedTask bag
        t.cause(ArrayUtil.add(d.parentCause(), channel.id));

        if (d.concSingle) //|| (NAL.OVERLAP_DOUBLE_SET_CYCLIC && d.overlapDouble))
            t.setCyclic(true);

        t.pri(priority);

        int cost;
        Task u = d.add(t);
        if (u != t) {

            nar.emotion.deriveFailDerivationDuplicate.increment();
            cost = NAL.derive.TTL_COST_DERIVE_TASK_REPEAT;

        } else {

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
