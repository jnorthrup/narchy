package nars.derive.op;

import jcog.Util;
import jcog.data.list.FasterList;
import jcog.util.ArrayUtils;
import nars.*;
import nars.derive.Derivation;
import nars.derive.premise.PremiseRuleProto;
import nars.task.DebugDerivedTask;
import nars.task.DerivedTask;
import nars.task.ITask;
import nars.term.ProxyTerm;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.util.transform.AbstractTermTransform;
import nars.time.Tense;
import nars.truth.Truth;
import nars.unify.unification.DeterministicUnification;
import nars.unify.unification.Termutifcation;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static nars.Op.*;
import static nars.Param.derive.DERIVE_FILTER_SIMILAR_TO_PARENTS;
import static nars.Param.derive.TermUnifyForkMax;
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

    public final Term test(DeterministicUnification xy, Derivation d) {
//        assert(d.transform.xy == null);
//            int start = d.size();
        d.transform.xy = xy::xy;
        d.retransform.clear();
        Term y = AbstractTermTransform.transform(termify.pattern, d.transform);
//      d.revert(start);
        d.transform.xy = null;
        return test(y, d);
    }


    @Nullable
    public Term test(Term x, Derivation d) {
        Term y = termify.test(x, d);
        return y != null && taskify(y, d) ? y : null;
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
        if (x0 == null)
            return false; //HACK

        final byte punc = d.concPunc;
        if (punc == 0)
            throw new RuntimeException("no punctuation assigned");

        Term x1 = d.anon.get(x0);
        if (x1 == null)
            throw new NullPointerException("could not un-anonymize " + x0 + " with " + d.anon);


        Term x = Task.normalize(x1);

        Op xo = x.op();


        boolean neg = xo == NEG;
        if (neg) {
            x = x.unneg();
            xo = x.op();
        }
        if (!xo.taskable)
            return spam(d, Param.derive.TTL_COST_DERIVE_TASK_FAIL);


//        if (xo == INH && Param.DERIVE_AUTO_IMAGE_NORMALIZE && !d.concSingle) {
//            Term y = Image.imageNormalize(x);
//            if (y!=x) {
//                x = y;
//                //xo = x.op();
//            }
//        }

        Truth tru;

        if (punc == BELIEF || punc == GOAL) {

            //dither truth
            tru = d.concTruth.dither(d.nar().freqResolution.floatValue(), d.nar().confResolution.floatValue(), d.eviMin, neg);
            if (tru == null)
                return spam(d, Param.derive.TTL_COST_DERIVE_TASK_UNPRIORITIZABLE);

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


        if (same(x, punc, tru, S, E, d._task, d.nar()) ||
                (d._belief != null && same(x, punc, tru, S, E, d._belief, d.nar()))) {
            d.nar().feel.deriveFailParentDuplicate.increment();
            return spam(d, Param.derive.TTL_COST_DERIVE_TASK_SAME);
        }


        DerivedTask t = Task.tryTask(x, punc, tru, (C, tr) ->
                Param.DEBUG ?
                        new DebugDerivedTask(C, punc, tr, S, E, d) :
                        new DerivedTask(C, punc, tr, S, E, d)
        );

        if (t == null) {
            d.nar().feel.deriveFailTaskify.increment();
            return spam(d, Param.derive.TTL_COST_DERIVE_TASK_FAIL);
        }


        float priority = d.what.derivePri.pri(t, d);

        if (priority != priority) {
            d.nar().feel.deriveFailPrioritize.increment();
            return spam(d, Param.derive.TTL_COST_DERIVE_TASK_UNPRIORITIZABLE);
        }

        //these must be applied before possible merge on input to derivedTask bag
        t.cause(ArrayUtils.add(d.parentCause, channel.id));

        if ((d.concSingle) || (Param.OVERLAP_DOUBLE_SET_CYCLIC && d.overlapDouble))
            t.setCyclic(true);

        t.pri(priority);


        int cost;
        ITask u = d.add(t);
        if (u != t) {

            d.nar().feel.deriveFailDerivationDuplicate.increment();
            cost = Param.derive.TTL_COST_DERIVE_TASK_REPEAT;

        } else {

            if (Param.DEBUG)
                t.log(channel.ruleString);

            d.nar().feel.deriveTask.increment();
            cost = Param.derive.TTL_COST_DERIVE_TASK_SUCCESS;

        }
        return d.use(cost);

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

    static boolean valid(Term x, byte punc) {
        if (x == null || x instanceof Bool)
            return false;
        x = x.unneg();
        return x.op().taskable &&
                !x.hasAny(Op.VAR_PATTERN) &&
                ((punc != BELIEF && punc != GOAL) || (!x.hasVarQuery()));

    }

    static boolean spam(Derivation d, int cost) {
        d.use(cost);
        return true;
    }

    protected boolean same(Term derived, byte punc, Truth truth, long start, long end, Task parent, NAR n) {

        if (DERIVE_FILTER_SIMILAR_TO_PARENTS) {

            if (parent.isDeleted())
                return false;

            if (parent.punc() == punc) {
                if (parent.term().equals(derived.term())) { //TODO test for dtDiff
                    if (parent.containsSafe(start, end)) {

                        if ((punc == QUESTION || punc == QUEST) || (
                                Util.equals(parent.freq(), truth.freq(), n.freqResolution.floatValue()) &&
                                        parent.conf() >= truth.conf() - n.confResolution.floatValue() / 2
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
