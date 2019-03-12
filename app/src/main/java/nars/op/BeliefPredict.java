package nars.op;

import com.google.common.collect.Iterables;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.learn.LivePredictor;
import jcog.learn.Predictor;
import jcog.math.FloatRange;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.time.event.DurService;
import nars.control.channel.BufferedCauseChannel;
import nars.task.signal.SignalTask;
import nars.term.Term;
import nars.term.Termed;
import nars.time.Tense;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.LongToFloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static jcog.Util.map;
import static nars.Op.BELIEF;
import static nars.truth.func.TruthFunctions.c2w;

/**
 * numeric prediction support
 * TODO implement this as an addable Scalar Belief table which recycles past prediction tasks as they change
 * TODO configuration parameters for projections
 *      --number
 *      --time width (duty cycle %)
 *      --confidence fade factor
 */
public class BeliefPredict {

    public final FloatRange conf = new FloatRange(0.5f, 0, 1f);
    public final FloatRange confFadeFactor = new FloatRange(0.9f, 0, 1f);

    final DurService on;
    //final List<ITask> currentPredictions = new FasterList<>();
    private final BufferedCauseChannel predict;
    private final LivePredictor predictor;
    private final NAR nar;

    /** in cycles (not durs) */
    private final int sampleDur;
    private final Termed[] predicted;
    int projections = 0;
    private List<Task> predictions = new FasterList();

    /** if the past and present set of monitored concepts are equal, then iterative projections
     * into the future are possible to compute each cycle.
     */
    public BeliefPredict(Iterable<Termed> concepts, int history, int sampleDur, int extraProjections, Predictor m, NAR nar) {
        this(concepts, history, sampleDur, concepts, m, nar);
        this.projections = extraProjections;
    }

    public BeliefPredict(Iterable<Termed> inConcepts, int history, int sampleDur, Iterable<Termed> outConcepts, Predictor m, NAR nar) {
        this(Iterables.toArray(inConcepts, Termed.class),
                history, sampleDur,
                Iterables.toArray(outConcepts, Termed.class), m, nar);
    }

    public BeliefPredict(Termed[] pastSampling, int history, int sampleDur, Termed[] presentSampling, Predictor m, NAR nar) {

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

    protected void predict() {

        synchronized (this) {
            predictions.forEach(x -> {
                x.delete();
                //TODO also directly remove from table?
            });
            predictions.clear();

            long now = nar.time();

            double[] p = null;

            float c = conf.floatValue() * nar.beliefConfDefault.floatValue();
            float fade = confFadeFactor.floatValue();
            for (int i = 0; i < projections + 1; i++) {

                p = (i == 0) ? predictor.next(now-sampleDur) : predictor.project(p);

                believe(now + (i ) * sampleDur, p, c);

                c *= fade;
            }
            //System.out.println(); System.out.println();

        }

//        currentPredictions.forEach(ITask::delete); //disarm last cycles predictions
//        currentPredictions.clear();
//
//        predict.input(currentPredictions);

        predict.commit();

    }

    private void believe(long when, double[] predFreq, float conf) {

        float evi = c2w(conf );

        long eShared = nar.evidence()[0];

        for (int i = 0; i < predFreq.length; i++) {

            double f0 = predFreq[i];
            if (!Double.isFinite(f0))
                continue;

            float f = (float)Util.unitize(f0);

            PreciseTruth t = Truth.theDithered(f, evi, nar);
            if (t == null)
                continue;


            long start = Tense.dither(when , nar);
            long end = Tense.dither(when+sampleDur, nar);
            //System.out.println("-> " + start + " " + end);
            Task p = new PredictionTask(predicted[i].term(), t, start, end, eShared).pri(nar);

            predictions.add(p);

            predict.input(
                    p
            );
        }
    }

    LongToFloatFunction freqSupplier(Termed c, NAR nar) {
        return (when) -> {
            long start = (when);
            long end = (when + sampleDur);
            //System.out.println("<- " + start + " " + end);
            @Nullable Truth t = nar.truth(c, BELIEF, start, end); //TODO filter predictions (PredictionTask's) from being used in this calculation
            if (t == null)
                return
                        0.5f;
                        
            else
                return t.freq();
        };
    }

    private static class PredictionTask extends SignalTask {
        public PredictionTask(Term term, PreciseTruth t, long start, long end, long eShared) {
            super(term, Op.BELIEF, t, start, end, eShared);
        }
    }

}
