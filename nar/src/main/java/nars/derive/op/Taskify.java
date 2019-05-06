package nars.derive.op;

import jcog.Util;
import jcog.data.list.FasterList;
import jcog.util.ArrayUtil;
import nars.*;
import nars.derive.Derivation;
import nars.derive.premise.PremiseRuleProto;
import nars.task.DebugDerivedTask;
import nars.task.DerivedTask;
import nars.term.ProxyTerm;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.util.transform.AbstractTermTransform;
import nars.time.Tense;
import nars.truth.Truth;
import nars.unify.unification.DeterministicUnification;
import nars.unify.unification.Termutifcation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static nars.NAL.derive.DERIVE_FILTER_SIMILAR_TO_PARENTS;
import static nars.NAL.derive.TermUnifyForkMax;
import static nars.Op.*;
import static nars.time.Tense.ETERNAL;

public class Taskify extends ProxyTerm {

    final Termify termify;


    public final boolean test(Termutifcation u, Derivation d) {
        List<DeterministicUnification> ii = u.listClone();
        int s = ii.size();
        if (s > 0) {
            if (s > 1)
                ((FasterList) ii).shuffleThis(d.random);

            int fanOut = Math.min(s, TermUnifyForkMax);
            for (int i = 0; i < fanOut; i++) {
                test(ii.get(i), d);
                if (!d.live())
                    return false;
            }
        }
        return true;
    }

    public final void test(DeterministicUnification xy, Derivation d) {
//        assert(d.transform.xy == null);
//            int start = d.size();
        d.transform.xy = xy::xy;
        d.retransform.clear();
        Term y = AbstractTermTransform.transform(termify.pattern, d.transform);
//      d.revert(start);
        d.transform.xy = null;
        test(y, d);
    }


    public void test(Term x, Derivation d) {
        termify.test(x, this, d);
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

    /**
     * note: the return value here shouldnt matter so just return true anyway
     */
    protected boolean taskify(Term x0, Derivation d) {

        final byte punc = d.concPunc;
        if (punc == 0)
            throw new RuntimeException("no punctuation assigned");


        Term x = Task.normalize(x0);

        Op xo = x.op();

        boolean neg = xo == NEG;
        if (neg) {
            x = x.unneg();
            xo = x.op();
        }
        if (!xo.taskable)
            return spam(d, NAL.derive.TTL_COST_DERIVE_TASK_FAIL);


        Truth tru;

        NAR nar = d.nar();
        if (punc == BELIEF || punc == GOAL) {

            //dither truth
            tru = d.concTruth.dither(nar.freqResolution.floatValue(), nar.confResolution.floatValue(), d.eviMin, neg);
            if (tru == null)
                return spam(d, NAL.derive.TTL_COST_DERIVE_TASK_UNPRIORITIZABLE);

        } else {
            tru = null; //questions and quests
        }


        long S, E;
        if (d.concOcc != null && d.concOcc[0] != ETERNAL) {

            int dither = d.ditherDT;
            if (dither > 1)
                Tense.dither(d.concOcc, dither);

            S = d.concOcc[0]; E = d.concOcc[1];

            assert (S <= E) : "task has reversed occurrence: " + S + ".." + E;
        } else {
            S = E = ETERNAL;
        }

        /** compares taskTerm before un-anon */
        if (same(x, punc, tru, S, E, d.taskTerm, d._task, nar)) {
            return parentDuplicate(d, nar);
        }

        /** un-anon */
        x = d.anon.get(x);
        if (x == null)
            throw new NullPointerException("could not un-anonymize " + x0 + " with " + d.anon);

        /** compares beliefTerm un-anon */
        if (d._belief != null && same(x, punc, tru, S, E, d._belief.term(), d._belief, nar)) {
            return parentDuplicate(d, nar);
        }

        DerivedTask t = Task.tryTask(x, punc, tru, (C, tr) ->
                NAL.DEBUG ?
                        new DebugDerivedTask(C, punc, tr, S, E, d) :
                        new DerivedTask(C, punc, tr, d.time(), S, E, d.evidence())
        );

        if (t == null) {
            nar.emotion.deriveFailTaskify.increment();
            return spam(d, NAL.derive.TTL_COST_DERIVE_TASK_FAIL);
        }


        float priority = d.what.derivePri.pri(t, d);

        if (priority != priority) {
            nar.emotion.deriveFailPrioritize.increment();
            return spam(d, NAL.derive.TTL_COST_DERIVE_TASK_UNPRIORITIZABLE);
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
        return d.use(cost);

    }

    private boolean parentDuplicate(Derivation d, NAR nar) {
        nar.emotion.deriveFailParentDuplicate.increment();
        return spam(d, NAL.derive.TTL_COST_DERIVE_TASK_SAME);
    }

    private final static Logger logger = LoggerFactory.getLogger(Taskify.class);
    /**
     * destination of any derived tasks; also may be used to communicate backpressure
     * from the recipient.
     */
    public final PremiseRuleProto.RuleWhy channel;

    private static final Atomic TASKIFY = Atomic.the("Taskify");


    public Taskify(Termify termify, PremiseRuleProto.RuleWhy channel) {
        this($.funcFast(TASKIFY, termify, $.the(channel.id)), termify, channel);
    }

    private Taskify(Term id, Termify termify, PremiseRuleProto.RuleWhy channel) {
        super(id);
        this.termify = termify;
        this.channel = channel;
    }


    static boolean spam(Derivation d, int cost) {
        d.use(cost);
        return true;
    }

    protected boolean same(Term derived, byte punc, Truth truth, long start, long end, Term parentTerm, Task parent, NAR n) {

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
