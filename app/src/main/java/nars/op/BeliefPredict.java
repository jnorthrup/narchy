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
import nars.attention.What;
import nars.control.NARPart;
import nars.control.channel.CauseChannel;
import nars.task.util.signal.SignalTask;
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
public class BeliefPredict extends NARPart {

    public final FloatRange conf = new FloatRange(0.5f, 0, 1f);
    public final FloatRange confFadeFactor = new FloatRange(0.9f, 0, 1f);

    //final List<ITask> currentPredictions = new FasterList<>();
    private final CauseChannel<Task> predict;
    private final LivePredictor predictor;


    /** in cycles (not durs) */
    private final int sampleDur;
    private final Termed[] predicted;

    /** where the generated beliefs will be input */
    private final What target;


    int projections = 0;
    private List<PredictionTask> predictions = new FasterList();


    /** if the past and present set of monitored concepts are equal, then iterative projections
     * into the future are possible to compute each cycle.
     */
    public BeliefPredict(Iterable<? extends Termed> concepts, int history, int sampleDur, int extraProjections, Predictor m, What target) {
        this(concepts, history, sampleDur, concepts, m, target);
        this.projections = extraProjections;
    }

    public BeliefPredict(Iterable<? extends Termed> inConcepts, int history, int sampleDur, Iterable<? extends Termed> outConcepts, Predictor m, What target) {
        this(Iterables.toArray(inConcepts, Termed.class),
                history, sampleDur,
                Iterables.toArray(outConcepts, Termed.class), m, target);
    }

    public BeliefPredict(Termed[] pastSampling, int history, int sampleDur, Termed[] presentSampling, Predictor m, What target) {
        super(target.nar);

        this.sampleDur = sampleDur;
        this.predicted = presentSampling;

        this.target = target;

        predictor = new LivePredictor(m, new LivePredictor.DenseShiftFramer(
            map(c -> freqSupplier(c, nar), LongToFloatFunction[]::new, pastSampling),
            history,
            sampleDur,
            map(c -> freqSupplier(c, nar), LongToFloatFunction[]::new, presentSampling)
        ));

        this.predict = nar.newChannel(this);


    }

    @Override
    protected void starting(NAR nar) {
        super.starting(nar);
        on(nar.onDur(this::predict));
    }

    protected synchronized void predict() {



        long now = nar.time();

        double[] p = null;

        float c = conf.floatValue() * nar.beliefConfDefault.floatValue();
        float fade = confFadeFactor.floatValue();
        List<PredictionTask> nextPred = new FasterList((projections+1)*predictor.framer.outputCount());


        List<PredictionTask> oldPred = this.predictions;
        oldPred.forEach(x -> {
            x.delete(); //TODO also directly remove from table?
        });
        oldPred.clear();


        for (int i = 0; i < projections + 1; i++) {

            p = (i == 0) ? predictor.next(now-sampleDur) : predictor.project(p);

            believe(now + (i ) * sampleDur, p, c, nextPred);

            c *= fade;
        }


        predict.acceptAll(this.predictions = nextPred, target);


        //System.out.println(); System.out.println();

        //        currentPredictions.forEach(ITask::delete); //disarm last cycles predictions
//        currentPredictions.clear();
//
//        predict.input(currentPredictions);

//        predict.commit();

    }

    private void believe(long when, double[] predFreq, float conf, List<PredictionTask> predictions) {


        float evi = c2w(conf );

        //long eShared = nar.evidence()[0];
        long[] se = Tense.dither(new long[] { when, when+sampleDur }, nar);
//        long start = Tense.dither(when , nar);
//        long end = Tense.dither(when+sampleDur, nar);

        for (int i = 0; i < predFreq.length; i++) {

            double f0 = predFreq[i];
            if (!Double.isFinite(f0))
                continue;

            float f = (float)Util.unitize(f0);

            PreciseTruth t = Truth.theDithered(f, evi, nar);
            if (t == null)
                continue;

            //System.out.println("-> " + start + " " + end);
            PredictionTask p = new PredictionTask(predicted[i].term(), t, se[0], se[1], nar.evidence());
            p.pri(nar);

            predictions.add(p);

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
        public PredictionTask(Term term, PreciseTruth t, long start, long end, long[] evi) {
            super(term, Op.BELIEF, t, start, start, end, evi);
        }
    }

}
