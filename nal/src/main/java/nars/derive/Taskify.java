package nars.derive;

import jcog.Util;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.control.Derivation;
import nars.task.DebugDerivedTask;
import nars.task.DerivedTask;
import nars.task.NALTask;
import nars.term.Term;
import nars.truth.Truth;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.function.BiFunction;

import static nars.Param.FILTER_SIMILAR_DERIVATIONS;

public class Taskify extends AbstractPred<Derivation> {

    final static BiFunction<DerivedTask, DerivedTask, DerivedTask> DUPLICATE_DERIVATION_MERGE = (pp, tt) -> {
        pp.priMax(tt.pri());
        if (!Arrays.equals(pp.cause(), tt.cause())) //dont merge if they are duplicates, it's pointless here
            pp.causeMerge(tt);
        return pp;
    };
    private final static Logger logger = LoggerFactory.getLogger(Taskify.class);

    /**
     * destination of any derived tasks; also may be used to communicate backpressure
     * from the recipient.
     */
    public final Conclude.RuleCause channel;

    protected Taskify(Conclude.RuleCause channel) {
        super($.func("taskify", $.the(channel.id)));
        this.channel = channel;
    }

    @Override
    public boolean test(Derivation d) {

        Term x = d.derivedTerm.get();
        x = d.anon.get(x).normalize();

        long[] occ = d.concOcc;
        byte punc = d.concPunc;
        assert (punc != 0) : "no punctuation assigned";

        Truth tru = d.concTruth;
        if (tru!=null) {
            tru = d.concTruth.dither(d.freqRes, d.confRes, d.confMin, d.concEviFactor);
            if (tru == null)
                return false;
        }

        final NAR nar = d.nar;
        DerivedTask t = (DerivedTask) Task.tryTask(x, punc, tru, (C, tr) -> {

            long start = occ[0];
            long end = occ[1];
            assert (end >= start): "task has reversedoccurrence: " + start + ".." + end;


            long[] evi = d.single ? d.evidenceSingle() : d.evidenceDouble();

            long now = d.time;

            DerivedTask derived =
                    Param.DEBUG ?
                            new DebugDerivedTask(C, punc, tr, now, start, end, evi, d.task, !d.single ? d.belief : null) :
                            new DerivedTask(C, punc, tr, now, start, end, evi);


            return derived;
        });

        if (t == null) {
            return spam(d, Param.TTL_DERIVE_TASK_FAIL);
        }

//        if (same(t, d.task, d.freqRes) || (d.belief != null && same(t, d.belief, d.freqRes))) {
//            //created a duplicate of the task
//            return spam(d, Param.TTL_DERIVE_TASK_SAME);
//        }


        float priority = Param.derivationPriority(t, d)
                //* channel.amp()
        ;

        assert (priority == priority);

        t.priSet(priority);

        if (Param.DEBUG)
            t.log(channel.ruleString);

        short[] cause = ArrayUtils.addAll(d.parentCause, channel.id);
        t.cause = cause;

        if (d.derivations.merge(t, t, DUPLICATE_DERIVATION_MERGE) != t) {
            spam(d, Param.TTL_DERIVE_TASK_REPEAT);
        } else {
            d.use(Param.TTL_DERIVE_TASK_SUCCESS);
        }

        return true;
    }


    private static boolean spam(@NotNull Derivation p, int cost) {
        p.use(cost);
        return true; //just does
    }

    protected boolean same(Task derived, Task parent, float truthResolution) {
        if (parent.isDeleted())
            return false;

        if (derived.equals(parent)) return true;

        if (FILTER_SIMILAR_DERIVATIONS) {
            //test for same punc, term, start/end, freq, but different conf
            if (parent.term().equals(derived.term()) && parent.punc() == derived.punc() && parent.start() == derived.start() && parent.end() == derived.end()) {
                /*if (Arrays.equals(derived.stamp(), parent.stamp()))*/
                if (parent.isQuestOrQuestion() ||
                        (Util.equals(parent.freq(), derived.freq(), truthResolution) &&
                                parent.evi() >= derived.evi())
                        ) {
                    if (Param.DEBUG_SIMILAR_DERIVATIONS)
                        logger.warn("similar derivation to parent:\n\t{} {}\n\t{}", derived, parent, channel.ruleString);


                    if (parent instanceof DerivedTask) {
                        parent.priMax(derived.priElseZero());
                        ((NALTask) parent).causeMerge(derived); //merge cause
                    }
                    return true;
                }
            }
        }
        return false;
    }

}
