package nars.concept;

import jcog.math.FloatSupplier;
import nars.NAR;
import nars.Task;
import nars.task.SignalTask;
import nars.task.util.PredictionFeedback;
import nars.term.Term;
import nars.truth.Truth;
import nars.util.signal.ScalarSignal;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.LongSupplier;


/**
 * primarily a collector for believing time-changing input signals
 */
public class SensorConcept extends WiredConcept implements FloatFunction<Term>, FloatSupplier {

    public final ScalarSignal sensor;
    public FloatSupplier signal;
    protected float currentValue = Float.NaN;

    static final Logger logger = LoggerFactory.getLogger(SensorConcept.class);


    public SensorConcept(@NotNull Term c, @NotNull NAR n, FloatSupplier signal, FloatToObjectFunction<Truth> truth) {
        super(c,
                //new SensorBeliefTable(n.conceptBuilder.newTemporalBeliefTable(c)),
                null,
                null, n);

        this.sensor = new ScalarSignal(n, c, this, truth, ()->SensorConcept.this.resolution.asFloat()) {
            @Override
            protected LongSupplier stamp(Truth currentBelief,  NAR nar) {
                return SensorConcept.this.nextStamp(nar);
            }
        };
        //((SensorBeliefTable)beliefs).sensor = sensor;

        this.signal = signal;

    }

    /**
     * returns a new stamp for a sensor task
     */
    protected LongSupplier nextStamp(NAR nar) {
        return nar.time::nextStamp;
    }


    public void setSignal(FloatSupplier signal) {
        this.signal = signal;
    }


    @Override
    public float floatValueOf(Term anObject /* ? */) {
        return this.currentValue = signal.asFloat();
    }


    @Override
    public float asFloat() {
        return currentValue;
    }


    @Override
    public void add(Task t, NAR n) {

        //feedback prefilter non-signal beliefs
        if (t.isBelief() && !(t instanceof SignalTask)) {
            PredictionFeedback.accept(t, beliefs, n);
            if (t.isDeleted())
                return; //rejected
        }

        super.add(t, n);
    }

    @Nullable
    public Task update(long time, int dur, NAR n) {
        Task x = sensor.update(n, time, dur);

        PredictionFeedback.accept(sensor.get() /* get() again in case x is stretched it will be null */, beliefs, n);

        return x;
    }


    public SensorConcept resolution(float r) {
        resolution = ()->r;
        return this;
    }

}
