package nars.util.signal;

import com.google.common.collect.Iterables;
import jcog.Util;
import jcog.learn.LivePredictor;
import nars.NAR;
import nars.Task;
import nars.control.DurService;
import nars.control.channel.CauseChannel;
import nars.task.ITask;
import nars.task.signal.SignalTask;
import nars.term.Termed;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.eclipse.collections.api.block.function.primitive.LongToFloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

import static jcog.Util.map;
import static nars.Op.BELIEF;
import static nars.truth.TruthFunctions.c2w;

/**
 * numeric prediction support
 */
public class BeliefPredict {

    final MutableFloat conf = new MutableFloat(0.5f);

    final DurService on;
    final Set<ITask> currentPredictions = new HashSet();
    private final CauseChannel<ITask> predict;
    private final LivePredictor predictor;
    private final NAR nar;
    private final int sampleDur;
    private final Termed[] predicted;
    int projections = 0;

    /** if the past and present set of monitored concepts are equal, then iterative projections
     * into the future are possible to compute each cycle.
     */
    public BeliefPredict(Iterable<Termed> concepts, int history, int sampleDur, int extraProjections, LivePredictor.Predictor m, NAR nar) {
        this(concepts, history, sampleDur, concepts, m, nar);
        this.projections = extraProjections;
    }

    public BeliefPredict(Iterable<Termed> inConcepts, int history, int sampleDur, Iterable<Termed> outConcepts, LivePredictor.Predictor m, NAR nar) {
        this(Iterables.toArray(inConcepts, Termed.class),
                history, sampleDur,
                Iterables.toArray(outConcepts, Termed.class), m, nar);
    }

    public BeliefPredict(Termed[] pastSampling, int history, int sampleDur, Termed[] presentSampling, LivePredictor.Predictor m, NAR nar) {

        this.nar = nar;
        this.sampleDur = sampleDur;
        this.predicted = presentSampling;

        predictor = new LivePredictor(m, new LivePredictor.DenseShiftFramer(
            map(c -> freqSupplier(c, nar), LongToFloatFunction[]::new, pastSampling),
            history,
            sampleDur,
            map(c -> freqSupplier(c, nar), LongToFloatFunction[]::new, presentSampling)
        ));

        this.predict = nar.newChannel(this);

        this.on = DurService.on(nar, this::predict);
    }

    protected synchronized void predict() {
        //delete all prediction tasks from the past cycle
        currentPredictions.forEach(ITask::delete);
        currentPredictions.clear();

        long now = nar.time();

        double[] p = predictor.next(now);
        believe(now, p);

        for (int i = 0; i < projections; i++) {
            now += sampleDur;
            p = predictor.project(now, p);
            believe(now, p);
        }
    }

    private void believe(long when, double[] predFreq) {
        long next = when + sampleDur;

        float evi = c2w(conf.floatValue() * nar.beliefConfDefault.floatValue());
        int dur = nar.dur();

        for (int i = 0; i < predFreq.length; i++) {

            double f0 = predFreq[i];
            if (!Double.isFinite(f0))
                continue;

            float f = (float)Util.unitize(f0);

            PreciseTruth t = Truth.theDithered(f, evi, nar);
            if (t == null)
                continue; //??

            Task p = new SignalTask(predicted[i].term(), BELIEF,
                    t,
                    next - dur, next, nar.time.nextStamp()
            ).pri(nar.priDefault(BELIEF));

            currentPredictions.add(p);

            predict.input(
                    p
            );
        }
    }

    static LongToFloatFunction freqSupplier(Termed c, NAR nar) {
        return (when) -> {
            float dur = Math.max(1, nar.dur());
            long start = Math.round(when - dur/2f);
            long end = Math.round(when + dur/2f);
            @Nullable Truth t = nar.truth(c, BELIEF, start, end);
            if (t == null)
                return 0.5f; //HACK
            else
                return t.freq();
        };
    }
}
