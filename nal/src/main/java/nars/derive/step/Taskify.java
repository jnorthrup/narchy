package nars.derive.step;

import jcog.Util;
import nars.*;
import nars.derive.Derivation;
import nars.derive.premise.PremiseDeriverProto;
import nars.task.DebugDerivedTask;
import nars.task.DerivedTask;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;
import nars.time.Tense;
import nars.truth.Truth;
import org.apache.commons.lang3.ArrayUtils;
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
    public final PremiseDeriverProto.RuleCause channel;

    private static final Atomic TASKIFY = Atomic.the("taskify");

    public Taskify(PremiseDeriverProto.RuleCause channel) {
        super(  $.func(TASKIFY, $.the(channel.id)) );
        this.channel = channel;
    }

    public static boolean valid(Term x, byte punc) {
        return x != null &&
                x.unneg().op().conceptualizable &&
                !x.hasAny(Op.VAR_PATTERN) &&
                ((punc != BELIEF && punc != GOAL) || (!x.hasXternal() && x.varQuery() <= 0));
    }

    protected static boolean spam(Derivation p, int cost) {
        p.use(cost);
        return true;
    }

    /**
     * note: the return value here shouldnt matter so just return true anyway
     */
    @Override
    public boolean test(Derivation d) {

        Truth tru = d.concTruth;

        Term x0 = d.derivedTerm;

        Term x1 = d.anon.get(x0);
        Term x = Term.forceNormalizeForBelief(x1);
        if (!x.op().conceptualizable) {
            return spam(d, Param.TTL_DERIVE_TASK_FAIL);
        }


        long[] occ = d.concOcc;
        byte punc = d.concPunc;
        assert (punc != 0) : "no punctuation assigned";

        if (tru != null && tru.conf() < d.confMin) {
            return spam(d, Param.TTL_DERIVE_TASK_UNPRIORITIZABLE);
        }

        if (same(x, punc, tru, occ, d._task, d.nar) ||
                (d._belief != null && same(x, punc, tru, occ, d._belief, d.nar))) {
            d.nar.emotion.deriveFailParentDuplicate.increment();
            return spam(d, Param.TTL_DERIVE_TASK_SAME);
        }

        DerivedTask t = (DerivedTask) Task.tryTask(x, punc, tru, (C, tr) -> {

            long start = occ[0], end = occ[1];

            assert (end >= start) : "task has reversed occurrence: " + start + ".." + end;

            //dither time
            if (start != ETERNAL) {
                int dither = d.ditherTime;
                start = Tense.dither(start, dither);
                end = Tense.dither(end, dither);
            }

            //dither truth
            if (tr!=null) {
                tr = tr.dither(d.nar);
                if (tr == null)
                    return null;
            }

            return Param.DEBUG ?
                    new DebugDerivedTask(C, punc, tr, start, end, d) :
                    new DerivedTask(C, punc, tr, start, end, d);
        });

        if (t == null) {
            d.nar.emotion.deriveFailTaskify.increment();
            return spam(d, Param.TTL_DERIVE_TASK_FAIL);
        }


        float priority = d.deriver.prioritize.pri(t, d);
        if (priority != priority) {
            d.nar.emotion.deriveFailPrioritize.increment();
            return spam(d, Param.TTL_DERIVE_TASK_UNPRIORITIZABLE);
        }


        if (d.add(t) != t) {
            d.nar.emotion.deriveFailDerivationDuplicate.increment();
            spam(d, Param.TTL_DERIVE_TASK_REPEAT);
        } else {

            if (d.concSingle)
                t.setCyclic(true);

            t.priSet(priority);

            t.cause = ArrayUtils.addAll(d.parentCause, channel.id);

            if (Param.DEBUG)
                t.log(channel.ruleString);

            d.use(Param.TTL_DERIVE_TASK_SUCCESS);
        }

        return true;
    }

    protected boolean same(Term derived, byte punc, Truth truth, long[] occ, Task parent, NAR n) {
        if (parent.isDeleted())
            return false;

        if (FILTER_SIMILAR_DERIVATIONS) {

            if (parent.punc() == punc) {
                if (parent.term().equals(derived.term())) {
                    if (Tense.dither(parent.start(), n) == Tense.dither(occ[0], n) &&
                            Tense.dither(parent.end(), n) == Tense.dither(occ[1], n)) {

                        if ((punc == QUESTION || punc == QUEST) || (
                                Util.equals(parent.freq(), truth.freq(), n.freqResolution.floatValue()) &&
                                        parent.conf() <= truth.conf() - n.confResolution.floatValue() / 2 /* + epsilon to avid creeping confidence increase */
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
