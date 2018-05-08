package nars.util.signal;

import com.google.common.collect.Iterables;
import jcog.Util;
import jcog.learn.LivePredictor;
import jcog.math.FloatSupplier;
import nars.NAR;
import nars.control.DurService;
import nars.control.channel.CauseChannel;
import nars.task.ITask;
import nars.task.signal.SignalTask;
import nars.term.Termed;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.Nullable;

import static jcog.Util.map;
import static nars.Op.BELIEF;
import static nars.truth.TruthFunctions.c2w;

/**
 * numeric prediction support
 */
public class BeliefPredict {

    final MutableFloat conf = new MutableFloat(0.5f);

    final DurService on;
    private final CauseChannel<ITask> predict;

    public BeliefPredict(Iterable<Termed> inConcepts, int lookAhead, int history, Iterable<Termed> outConcepts, LivePredictor.Predictor m, NAR nar) {
        this(Iterables.toArray(inConcepts, Termed.class),
                lookAhead, history,
                Iterables.toArray(outConcepts, Termed.class), m, nar);
    }

    public BeliefPredict(Termed[] inConcepts, int lookAhead, int history, Termed[] outConcepts, LivePredictor.Predictor m, NAR nar) {

        assert(lookAhead >= 1);
        assert(lookAhead <= history);

        LivePredictor p = new LivePredictor(m, new LivePredictor.HistoryFramer(
            map(c -> freqSupplier(c, -lookAhead, nar), FloatSupplier[]::new, inConcepts),
            history,
            map(c -> freqSupplier(c, 0, nar), FloatSupplier[]::new, outConcepts)
        ));

        this.predict = nar.newChannel(this);

        this.on = DurService.on(nar, () -> {
            double[] predFreq = p.next();
            long now = nar.time();
            int dur = nar.dur();
            long next = now + lookAhead * dur;

            float evi = c2w(conf.floatValue() * nar.beliefConfDefault.floatValue());

            for (int i = 0; i < predFreq.length; i++) {

                float f = (float)Util.unitize(predFreq[i]);

                PreciseTruth t = Truth.theDithered(f, evi, nar);
                if (t == null)
                    continue; //??

                predict.input(
                    new SignalTask(outConcepts[i].term(), BELIEF,
                            t,
                            next-dur, next, nar.time.nextStamp()
                    ).pri(nar.priDefault(BELIEF))
                );
            }
        });
    }

    static FloatSupplier freqSupplier(Termed c, int dDur, NAR nar) {
        return () -> {
            long now = nar.time();
            float dur = Math.max(1,nar.dur());
            long start = Math.round(now + dDur * dur - dur/2f);
            long end = Math.round(now + dDur * dur + dur/2f);
            @Nullable Truth t = nar.truth(c, BELIEF, start, end);
            if (t == null)
                return 0.5f; //HACK
            else
                return t.freq();
        };
    }
}
