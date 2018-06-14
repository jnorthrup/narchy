package nars.util.signal;

import com.google.common.collect.Iterables;
import jcog.Util;
import jcog.learn.LivePredictor;
import nars.NAR;
import nars.Task;
import nars.control.DurService;
import nars.control.channel.BufferedCauseChannel;
import nars.task.signal.SignalTask;
import nars.term.Termed;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.eclipse.collections.api.block.function.primitive.LongToFloatFunction;
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
    //final List<ITask> currentPredictions = new FasterList<>();
    private final BufferedCauseChannel predict;
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

        this.predict = nar.newChannel(this).buffered();

        this.on = DurService.on(nar, this::predict);
    }

    protected synchronized void predict() {

        long now = nar.time();

        double[] p = predictor.next(now);

        now += sampleDur;
        believe(now, p);

        for (int i = 0; i < projections; i++) {

            p = predictor.project(p);

            now += sampleDur;
            believe(now, p);
        }


//        currentPredictions.forEach(ITask::delete); //disarm last cycles predictions
//        currentPredictions.clear();
//
//        predict.input(currentPredictions);

        predict.commit();

    }

    private void believe(long when, double[] predFreq) {

        float evi = c2w(conf.floatValue() * nar.beliefConfDefault.floatValue());

        for (int i = 0; i < predFreq.length; i++) {

            double f0 = predFreq[i];
            if (!Double.isFinite(f0))
                continue;

            float f = (float)Util.unitize(f0);

            PreciseTruth t = Truth.theDithered(f, evi, nar);
            if (t == null)
                continue; 

            Task p = new SignalTask(predicted[i].term(), BELIEF,
                    t,
                    when - sampleDur/2, when + sampleDur/2,
                    nar.evidence()[0]
            ).pri(nar.priDefault(BELIEF));

            predict.input(
                    p
            );
        }
    }

    LongToFloatFunction freqSupplier(Termed c, NAR nar) {
        return (when) -> {
            long start = Math.round(when - sampleDur/2f);
            long end = Math.round(when + sampleDur/2f);
            @Nullable Truth t = nar.truth(c, BELIEF, start, end);
            if (t == null)
                return
                        0.5f;
                        
            else
                return t.freq();
        };
    }
}
