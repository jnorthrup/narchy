package nars.derive;

import jcog.Util;
import nars.$;
import nars.Param;
import nars.Task;
import nars.control.Derivation;
import nars.task.DebugDerivedTask;
import nars.task.DerivedTask;
import nars.task.NALTask;
import nars.term.Term;
import nars.term.pred.AbstractPred;
import nars.time.Tense;
import nars.truth.Truth;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nars.Param.FILTER_SIMILAR_DERIVATIONS;

public class Taskify extends AbstractPred<Derivation> {


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

        Truth tru = d.concTruth;
        if (tru!=null) {
            tru = tru.ditherDiscrete(d.freqRes, d.confRes, d.confMin,
                    //TimeFusion.eviEternalize(tru.evi(), d.concEviFactor)
                    tru.evi() * d.concEviFactor
            );
            if (tru == null) {
                d.nar.emotion.deriveFailTaskify.increment();
                return false;
            }
        }

        Term x0 = d.derivedTerm.get();
        Term x = d.anon.get(x0);
        if (x == null || !Conclusion.valid((x = x.normalize()))) {
            d.nar.emotion.deriveFailTaskify.increment();
            return false; //when the values were finally dereferenced, the result produced an invalid compound
            //throw new RuntimeException("un-anonymizing " + x0 + " produced " + x);
        }

        long[] occ = d.concOcc;
        byte punc = d.concPunc;
        assert (punc != 0) : "no punctuation assigned";



        DerivedTask t = (DerivedTask) Task.tryTask(x, punc, tru, (C, tr) -> {

            int dither = d.ditherTime;
            long start = Tense.dither(occ[0], dither);
            long end = Tense.dither(occ[1], dither);
            assert (end >= start): "task has reversed occurrence: " + start + ".." + end;

            long[] evi = d.single ? d.evidenceSingle() : d.evidenceDouble();
            long now = d.time;
            return Param.DEBUG ?
                            new DebugDerivedTask(C, punc, tr, now, start, end, evi, d._task, !d.single ? d._belief : null) :
                            new DerivedTask(C, punc, tr, now, start, end, evi);
        });

        if (t == null) {
            d.nar.emotion.deriveFailTaskify.increment();
            return spam(d, Param.TTL_DERIVE_TASK_FAIL);
        }

        if (d.single)
            t.setCyclic(true);

        if (same(t, d._task, d.freqRes) || (d._belief != null && same(t, d._belief, d.freqRes))) {
            d.nar.emotion.deriveFailParentDuplicate.increment();
            return spam(d, Param.TTL_DERIVE_TASK_SAME);
        }

        float priority = d.deriver.derivationPriority(t, d);
        assert (priority == priority);
        t.priSet(priority);

        if (Param.DEBUG)
            t.log(channel.ruleString);

        t.cause = ArrayUtils.addAll(d.parentCause, channel.id);

        if (d.add(t) != t) {
            d.nar.emotion.deriveFailDerivationDuplicate.increment();
            spam(d, Param.TTL_DERIVE_TASK_REPEAT);
        } else {
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
            if (parent.term().equals(derived.term()) && parent.punc() == derived.punc() && parent.start() == derived.start() && parent.end() == derived.end()) {
                /*if (Arrays.equals(derived.stamp(), parent.stamp()))*/
                if (parent.isQuestOrQuestion() ||
                        (Util.equals(parent.freq(), derived.freq(), truthResolution) &&
                                parent.evi() >= derived.evi())
                        ) {
                    if (Param.DEBUG_SIMILAR_DERIVATIONS)
                        logger.warn("similar derivation to parent:\n\t{} {}\n\t{}", derived, parent, channel.ruleString);


                    if (parent.isCyclic() && !derived.isCyclic())
                        parent.setCyclic(false);

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
