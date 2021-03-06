package nars.derive.action.op;

import jcog.Util;
import jcog.pri.ScalarValue;
import jcog.signal.meter.Use;
import nars.$;
import nars.NAL;
import nars.NAR;
import nars.Task;
import nars.derive.Derivation;
import nars.derive.rule.RuleCause;
import nars.derive.util.DerivationFailure;
import nars.task.DebugDerivedTask;
import nars.task.DerivedTask;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.ProxyTerm;
import nars.term.Term;
import nars.term.util.TermException;
import nars.term.util.transform.VariableTransform;
import nars.term.var.VarIndep;
import nars.time.Tense;
import nars.truth.Truth;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static nars.NAL.derive.DERIVE_FILTER_SIMILAR_TO_PARENTS;
import static nars.Op.*;
import static nars.time.Tense.*;

public class Taskify extends ProxyTerm {

    private static final Logger logger = LoggerFactory.getLogger(Taskify.class);
    /**
     * destination of any derived tasks; also may be used to communicate backpressure
     * from the recipient.
     */
    public final RuleCause rule;
    public final Termify termify;

    final Occurrify.OccurrenceSolver time;

    public Taskify(Termify termify, Occurrify.OccurrenceSolver time, RuleCause rule) {
        super($.INSTANCE.pFast(termify, $.INSTANCE.the((int) rule.id)));
        this.termify = termify;
        this.time = time;
        this.rule = rule;
    }


    static boolean spam(Derivation d, int cost) {
        d.use(cost);

        //MetaGoal.Futile.learn(cost, d.nar.control.why, channel.id);

        return true;
    }


    private static Term postFilter(Term y, Derivation d) {

        if (y instanceof Compound) {

//            Term yc = y.concept();
//            if (yc == Null) {
//                if (NAL.DEBUG)
//                    throw new TermTransformException(y, yc, "unconceptualizable");
//                else
//                    return Null;
//            }

            //if ((d.concPunc==QUESTION || d.concPunc==QUEST)  && !VarIndep.validIndep(y, true)) {
            if (!VarIndep.validIndep(y, true)) {
                //convert orphaned indep vars to query/dep variables
                byte punc = d.punc;
                y = y.transform(
                        ((int) punc == (int) QUESTION || (int) punc == (int) QUEST) ?
                                VariableTransform.indepToQueryVar
                                :
                                VariableTransform.indepToDepVar
                );
            }

            //if (!d.single)
//            if (!d.overlapSingle)
//                y = Image.imageNormalize(y);
        }
        return y;
    }

    private @Nullable Task taskTemporal(Term x, Derivation d) {

        Pair<Term, long[]> o;
        try (Use.SafeAutocloseable __ = d.nar.emotion.derive_E_Run3_Taskify_1_Occurrify.time()) {
            o = occurrify(x, d);
        }
        if (o == null)
            return null;

        long[] occ = o.getTwo();
        return task(o.getOne(), occ[0], occ[1], d);
    }

    private @Nullable Pair<Term, long[]> occurrify(Term x, Derivation d) {
        Pair<Term, long[]> timing = time.occurrence(x, d);
        if (timing == null) {
            if (NAL.test.DEBUG_OCCURRIFY)
                throw new TermException("occurify failure:\n" + d + '\n' + d.occ, x);
            else {
                d.nar.emotion.deriveFailTemporal.increment();
            }
            return null;
        }


        long[] occ = timing.getTwo();
        assertOccValid(d, occ);

        Term y = timing.getOne();

        if (NAL.derive.DERIVE_QUESTION_FROM_AMBIGUOUS_BELIEF_OR_GOAL && ((int) d.punc == (int) BELIEF || (int) d.punc == (int) GOAL)) {
            if (DerivationFailure.failure(y, d.punc)) {

                //as a last resort, try forming a question from the remains
                byte qPunc = (int) d.punc == (int) BELIEF ? QUESTION : QUEST;
                d.punc = qPunc;
                if (DerivationFailure.failure(y, d) == null) {
                    d.punc = qPunc;
                    d.truth.clear(); //may be unnecessary
                } else {
                    d.nar.emotion.deriveFailTemporal.increment();
                    return null;
                }

            } //else: ok
        } else {
            if (DerivationFailure.failure(y, d) != null) {
                d.nar.emotion.deriveFailTemporal.increment();
                return null;
            }
        }

        if (NAL.test.DEBUG_ENSURE_DITHERED_DT)
            assertDithered(y, d.ditherDT);
        return timing;
    }

    private static void assertOccValid(Derivation d, long[] occ) {
        if (!((occ[0] != TIMELESS) && (occ[1] != TIMELESS) &&
                (occ[0] == ETERNAL) == (occ[1] == ETERNAL) &&
                (occ[1] >= occ[0])) || (occ[0] == ETERNAL && !d.occ.validEternal()))
            throw new RuntimeException("invalid occurrence result: " + Arrays.toString(occ));


//        long tRange = d._task.isEternal() ? Long.MAX_VALUE : d._task.range();
//        long maxRange;
//        if (d._belief!=null && !d.concSingle) {
////            long bRange = d._belief.isEternal() ? Long.MAX_VALUE : d._belief.range();
//            maxRange = !d._belief.isEternal() ? d.taskBelief_TimeIntersection[1] - d.taskBelief_TimeIntersection[0] : tRange;
//        } else
//            maxRange = tRange;
//        long oRange = occ[0] == ETERNAL ? Long.MAX_VALUE : occ[1] - occ[0];
//        if (oRange < maxRange - (d.ditherDT)) {
//            throw new RuntimeException("occurrence range diminished from max=" + tRange + ": " + Arrays.toString(occ) + "=" + oRange);
//        }
    }


    /**
     * note: the return value here shouldnt matter so just return true anyway
     */
    private @Nullable Task task(Term x0, long start, long end, Derivation d) {

        byte punc = d.punc;
        if ((int) punc == 0)
            throw new RuntimeException("no punctuation assigned");

        Term z = postFilter(x0, d);

        /** un-anon */
        Term x1 = d.anon.get(z);
        if (x1 == null)
            throw new NullPointerException("could not un-anonymize " + z + " with " + d.anon);

        NAR nar = d.nar;

        Term x = Task.taskTerm(x1, punc, !NAL.test.DEBUG_EXTRA);
        if (x == null) {
            nar.emotion.deriveFailTaskify.increment();
            spam(d, NAL.derive.TTL_COST_DERIVE_TASK_FAIL);
            return null;
        }

//        if (punc == GOAL && d.taskPunc == GOAL) {
//            //check for contradictory goal derivation
//            if (LongInterval.minTimeTo(d._task, start, end) < d.dur() + d.taskTerm.eventRange() + x.unneg().eventRange()) {
//                Term posTaskGoal = d._task.term().negIf(d.taskTruth.isNegative());
//                Term antiTaskGoal = posTaskGoal.neg();
//                Term cc = x.negIf(d.truth.isNegative());
//
//                if (
//                    cc.equals(antiTaskGoal)
//                    || (Conj.eventOf(cc, antiTaskGoal))
//                ) {
//                    System.err.println(d._task + " " + x);
////                    nar.emotion.deriveFailTaskifyGoalContradiction.increment();
////                    spam(d, NAL.derive.TTL_COST_DERIVE_TASK_UNPRIORITIZABLE);
////                    return;
//                }
//            }
//        }

        boolean neg = x instanceof Neg;


        Truth tru;
        if ((int) punc == (int) BELIEF || (int) punc == (int) GOAL) {

            //dither truth
            tru = Truth.dither(d.truth, d.eviMin, neg, nar);
            if (tru == null) {
                nar.emotion.deriveFailTaskifyTruthUnderflow.increment();
                spam(d, NAL.derive.TTL_COST_DERIVE_TASK_UNPRIORITIZABLE);
                return null;
            }

        } else {
            tru = null; //questions and quests
        }


        long S, E;
        if (start != ETERNAL) {
            assert (start <= end) : "reversed occurrence: " + start + ".." + end;

            int dither =
                d.ditherDT;

            int dur =
                d.unify.dtTolerance;
            long belowDur = (long) dur - (end - start);
            if (belowDur >= 2L) {
                //expand to perceptual dur since this is used in unification
                //TODO corresponding evidence dilution
                start -= belowDur/ 2L;
                end += belowDur/ 2L;
            }

            if (dither > 1) {
                S = Tense.dither(start, dither, -1);
                E = Tense.dither(end, dither, +1);
            } else {
                S = start;
                E = end;
            }

        } else {
            S = E = ETERNAL;
        }

        if (neg)
            x = x.unneg();

        /** compares taskTerm un-anon */
        if (isSame(x, punc, tru, S, E, d._taskTerm, d._task, nar)) {
            same(d, nar);
            return null;
        }
        /** compares beliefTerm un-anon */
        if (d._belief != null && isSame(x, punc, tru, S, E, d._beliefTerm, d._belief, nar)) {
            same(d, nar);
            return null;
        }


        DerivedTask t = //Task.tryTask(x, punc, tru, (C, tr) -> {
                //return
                NAL.DEBUG ?
                        new DebugDerivedTask(x, punc, tru, S, E, d) :
                        new DerivedTask(x, punc, tru, d.time, S, E, d.evidence());

        float priority = d.x.derivePri.pri(t, d);
        if (priority != priority) {
            nar.emotion.deriveFailPrioritize.increment();
            spam(d, NAL.derive.TTL_COST_DERIVE_TASK_UNPRIORITIZABLE);
            return null;
        }
        t.pri(Util.clamp(priority, ScalarValue.Companion.getEPSILON(), 1.0F));

        //these must be applied before possible merge on input to derivedTask bag
        t.why(rule.why(d));

        if (d.single) //|| (NAL.OVERLAP_DOUBLE_SET_CYCLIC && d.overlapDouble))
            t.setCyclic(true);

        return t;
    }

    private static boolean same(Derivation d, NAR nar) {
        nar.emotion.deriveFailParentDuplicate.increment();
        return spam(d, NAL.derive.TTL_COST_DERIVE_TASK_SAME);
    }

    private boolean isSame(Term derived, byte punc, Truth truth, long start, long end, Term parentTerm, Task parent, NAR n) {

        if (DERIVE_FILTER_SIMILAR_TO_PARENTS) {

            if (parent.isDeleted())
                return false;

            if ((int) parent.punc() == (int) punc) {
                if (parentTerm.equals(derived)) { //TODO test for dtDiff
                    if (parent.containsSafe(start, end)) {

                        if (((int) punc == (int) QUESTION || (int) punc == (int) QUEST) || (
                                Util.equals(parent.freq(), truth.freq(), n.freqResolution.floatValue()) &&
                                        parent.conf() >= truth.conf() - n.confResolution.floatValue() / 2.0F
                                // / 2 /* + epsilon to avid creeping confidence increase */
                        )) {

                            if (NAL.DEBUG_SIMILAR_DERIVATIONS)
                                logger.warn("similar derivation to parent:\n\t{} {}\n\t{}", derived, parent, rule.ruleString);

                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public final Task task(Term x, Derivation d) {
        Task y = d.temporal || x.hasXternal() /*Occurrify.temporal(y)*/ ?
            taskTemporal(x, d) : task(x, ETERNAL, ETERNAL, d);

        if (y!=null)
            d.unify.use(NAL.derive.TTL_COST_TASK_TASKIFY);

        return y;
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
