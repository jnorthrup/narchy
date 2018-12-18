package nars.derive.op;

import jcog.Util;
import jcog.util.ArrayUtils;
import nars.*;
import nars.derive.Derivation;
import nars.derive.premise.PremiseRuleProto;
import nars.task.DebugDerivedTask;
import nars.task.DerivedTask;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;
import nars.time.Tense;
import nars.truth.Truth;
import nars.truth.polation.TruthIntegration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nars.Op.*;
import static nars.Param.FILTER_SIMILAR_DERIVATIONS;
import static nars.time.Tense.ETERNAL;

public class Taskify extends AbstractPred<Derivation> {


    private final static Logger logger = LoggerFactory.getLogger(Taskify.class);
    /**
     * destination of any derived tasks; also may be used to communicate backpressure
     * from the recipient.
     */
    public final PremiseRuleProto.RuleCause channel;

    private static final Atomic TASKIFY = Atomic.the("taskify");

    public Taskify(PremiseRuleProto.RuleCause channel) {
        super($.funcFast(TASKIFY, $.the(channel.id)));
        this.channel = channel;
    }

    static boolean valid(Term x, byte punc) {
        if (x == null)
            return false;
        x = x.unneg();
        return x.op().taskable &&
               !x.hasAny(Op.VAR_PATTERN) &&
               ((punc != BELIEF && punc != GOAL) || (!x.hasVarQuery()));
    }

    protected static boolean spam(Derivation d, int cost) {
        d.use(cost);
        d.concTerm = null; //erase immediately
        return true;
    }

    /**
     * note: the return value here shouldnt matter so just return true anyway
     */
    @Override
    public boolean test(Derivation d) {


        final byte punc = d.concPunc;
        if (punc == 0)
            throw new RuntimeException("no punctuation assigned");

        Term x0 = d.concTerm;


        Term x1 = d.anon.get(x0);
        if (x1 == null)
            throw new NullPointerException("could not un-anonymize " + x0 + " with " + d.anon);

//        //TEMPORARY
//        if (Param.DEBUG) {
//            if (x1.ORrecurse(t -> t instanceof Anom))
//                throw new WTF("Anom leak into Taskify content: " + x0 + " with " + d.anon);
//        }

        Term x = Task.forceNormalizeForBelief(x1);
        Op xo = x.op();
        if (!xo.conceptualizable)
            return spam(d, Param.TTL_DERIVE_TASK_FAIL);

        if (xo==NEG)
            x = x.unneg();

        Truth tru;

        if (punc == BELIEF || punc == GOAL) {

            tru = d.concTruth;

            //dither truth
            float f = tru.freq();
            if (xo == NEG) {
                f = 1 - f;
            }

            tru = Truth.theDithered(f, tru.evi(), d.eviMin, d.nar);
            if (tru == null)
                return spam(d, Param.TTL_DERIVE_TASK_UNPRIORITIZABLE);

        } else {
            tru = null; //questions and quests
        }


        long S, E;
        if (d.concOcc!=null && d.concOcc[0]!=ETERNAL) {
            long s = d.concOcc[0], e = d.concOcc[1];
            assert (e >= s) : "task has reversed occurrence: " + s + ".." + e;

            //dither time
            int dither = d.ditherTime;
            S = Tense.dither(s, dither);
            E = Tense.dither(e, dither);
        } else {
            S = E = ETERNAL;
        }


        if (same(x, punc, tru, S, E, d._task, d.nar) ||
                (d._belief != null && same(x, punc, tru, S, E, d._belief, d.nar))) {
            d.nar.emotion.deriveFailParentDuplicate.increment();
            return spam(d, Param.TTL_DERIVE_TASK_SAME);
        }



        DerivedTask t = Task.tryTask(x, punc, tru, (C, tr) ->
            Param.DEBUG ?
                new DebugDerivedTask(C, punc, tr, S, E, d) :
                new DerivedTask(C, punc, tr, S, E, d)
        );

        if (t == null) {
            d.nar.emotion.deriveFailTaskify.increment();
            return spam(d, Param.TTL_DERIVE_TASK_FAIL);
        }


        float priority =
                t.isBeliefOrGoal() ?
                        d.deriver.pri.pri(t, tru.freq(), TruthIntegration.evi(t), d) :
                        d.deriver.pri.pri(t, Float.NaN, Float.NaN, d);

        if (priority != priority) {
            d.nar.emotion.deriveFailPrioritize.increment();
            return spam(d, Param.TTL_DERIVE_TASK_UNPRIORITIZABLE);
        }

        //these must be applied before possible merge on input to derivedTask bag
        t.cause(ArrayUtils.add(d.parentCause, channel.id) );

        if ((d.concSingle) || (Param.OVERLAP_DOUBLE_SET_CYCLIC && d.overlapDouble))
            t.setCyclic(true);

        t.pri(priority);


        if (d.add(t) != t) {

            d.use(Param.TTL_DERIVE_TASK_REPEAT);
            d.nar.emotion.deriveFailDerivationDuplicate.increment();

        } else {

            if (Param.DEBUG)
                t.log(channel.ruleString);

            d.use(Param.TTL_DERIVE_TASK_SUCCESS);
            d.nar.emotion.deriveTask.increment();

        }

        return true;
    }

    protected boolean same(Term derived, byte punc, Truth truth, long start, long end, Task parent, NAR n) {
        if (parent.isDeleted())
            return false;

        if (FILTER_SIMILAR_DERIVATIONS) {

            if (parent.punc() == punc) {
                if (parent.term().equals(derived.term())) { //TODO test for dtDiff
                    if (parent.contains(start, end)) {

                        if ((punc == QUESTION || punc == QUEST) || (
                                Util.equals(parent.freq(), truth.freq(), n.freqResolution.floatValue()) &&
                                        parent.conf() >= truth.conf() - n.confResolution.floatValue()/2
                                // / 2 /* + epsilon to avid creeping confidence increase */
                        )) {

                            if (Param.DEBUG_SIMILAR_DERIVATIONS)
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
//            if (parent.term().equals(derived.term()) && parent.punc() == derived.punc() &&
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
