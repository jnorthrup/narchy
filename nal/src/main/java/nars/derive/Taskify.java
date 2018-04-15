package nars.derive;

import jcog.Util;
import nars.$;
import nars.Param;
import nars.Task;
import nars.task.DebugDerivedTask;
import nars.task.DerivedTask;
import nars.term.Term;
import nars.term.pred.AbstractPred;
import nars.time.Tense;
import nars.truth.Truth;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nars.Op.BOOL;
import static nars.Op.VAR_PATTERN;
import static nars.Param.FILTER_SIMILAR_DERIVATIONS;

public class Taskify extends AbstractPred<Derivation> {


    private final static Logger logger = LoggerFactory.getLogger(Taskify.class);

    /**
     * destination of any derived tasks; also may be used to communicate backpressure
     * from the recipient.
     */
    public final Conclude.RuleCause channel;

    protected Taskify(Conclude.RuleCause channel) {
        super(
                //$.func("taskify", $.the(channel.id))
                $.the("taskify" + channel.id)
        );
        this.channel = channel;
    }

    static final int PatternsOrBool = VAR_PATTERN.bit | BOOL.bit;
    public static boolean valid(Term x) {
        if ((x != null) && x.unneg().op().conceptualizable) {

            if (x.hasAny(PatternsOrBool))
                return false; //throw new RuntimeException("shouldnt happen");

            //return  Task.validTaskTerm(x);
            return true;
        }

        return false;
    }

    /** note: the return value here shouldnt matter so just return true anyway */
    @Override public boolean test(Derivation d) {

        Truth tru = d.concTruth;
        if (tru!=null) {
            float finalEvi = tru.evi() * d.concEviFactor;
            if (d.eviMin > finalEvi) {
                d.use(Param.TTL_EVI_INSUFFICIENT);
                return true;
            }

            tru = tru.withEvi(finalEvi);
        }

        Term x0 = d.derivedTerm.get();
        Term x = d.anon.get(x0).normalize();

        long[] occ = d.concOcc;
        byte punc = d.concPunc;
        assert (punc != 0) : "no punctuation assigned";

        DerivedTask t = (DerivedTask) Task.tryTask(x, punc, tru, (C, tr) -> {

            int dither = d.ditherTime;
            long start = Tense.dither(occ[0], dither);
            long end = Tense.dither(occ[1], dither);
            assert (end >= start): "task has reversed occurrence: " + start + ".." + end;

            return Param.DEBUG ?
                            new DebugDerivedTask(C, punc, tr, start, end, d) :
                            new DerivedTask(C, punc, tr, start, end, d);
        });

        if (t == null) {
            d.nar.emotion.deriveFailTaskify.increment();
            return spam(d, Param.TTL_DERIVE_TASK_FAIL);
        }

        if (same(t, d._task, d.freqRes) || (d._belief != null && same(t, d._belief, d.freqRes))) {
            d.nar.emotion.deriveFailParentDuplicate.increment();
            return spam(d, Param.TTL_DERIVE_TASK_SAME);
        }

        if (d.single)
            t.setCyclic(true);

        float priority = d.deriver.prioritize.pri(t, d);
        if (priority != priority) {
            d.nar.emotion.deriveFailPrioritize.increment();
            return spam(d, Param.TTL_DERIVE_TASK_PRIORITIZE);
        }

        t.priSet(priority);

        t.cause = ArrayUtils.addAll(d.parentCause, channel.id);

        if (d.add(t) != t) {
            d.nar.emotion.deriveFailDerivationDuplicate.increment();
            spam(d, Param.TTL_DERIVE_TASK_REPEAT);
        } else {

            if (Param.DEBUG)
                t.log(channel.ruleString);

            d.use(Param.TTL_DERIVE_TASK_SUCCESS);
        }

        return true;
    }




    private static boolean spam(Derivation p, int cost) {
        p.use(cost);
        return true; //just does
    }

    protected boolean same(Task derived, Task parent, float truthResolution) {
        if (parent.isDeleted())
            return false;

        if (derived.equals(parent)) return true;

        if (FILTER_SIMILAR_DERIVATIONS) {
            //test for same punc, term, start/end, freq, but different conf
            if (parent.term().equals(derived.term()) && parent.punc() == derived.punc() &&
                    parent.start() == derived.start() && parent.end() == derived.end()) {
                /*if (Arrays.equals(derived.stamp(), parent.stamp()))*/
                if (parent.isQuestionOrQuest() ||
                        (Util.equals(parent.freq(), derived.freq(), truthResolution) &&
                                parent.evi() >= derived.evi())
                        ) {
                    if (Param.DEBUG_SIMILAR_DERIVATIONS)
                        logger.warn("similar derivation to parent:\n\t{} {}\n\t{}", derived, parent, channel.ruleString);


//                    if (parent.isCyclic() && !derived.isCyclic())
//                        parent.setCyclic(false);
//                    if (parent instanceof DerivedTask) {
//                        parent.priMax(derived.priElseZero());
//                        //((NALTask) parent).causeMerge(derived); //merge cause
//                    }
                    return true;
                }
            }
        }
        return false;
    }

}
