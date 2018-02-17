package nars.concept;

import jcog.math.FloatSupplier;
import nars.NAR;
import nars.Task;
import nars.concept.builder.ConceptBuilder;
import nars.task.DerivedTask;
import nars.task.signal.SignalTask;
import nars.task.util.PredictionFeedback;
import nars.term.Term;
import nars.truth.Truth;
import nars.util.signal.ScalarSignal;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.function.LongSupplier;

import static nars.Op.BELIEF;


/**
 * primarily a collector for believing time-changing input signals
 */
public class SensorConcept extends WiredConcept implements FloatFunction<Term>, FloatSupplier {

    public final ScalarSignal sensor;
    public FloatSupplier signal;
    private float currentValue = Float.NaN;

    private transient short cause = -1;

    public SensorConcept(Term c, NAR n, FloatSupplier signal) {
        this(c, n.conceptBuilder, signal);
        sensor.pri(() -> n.priDefault(BELIEF));
    }

    public SensorConcept(Term c, ConceptBuilder b, FloatSupplier signal) {
        super(c,
                //new SensorBeliefTable(n.conceptBuilder.newTemporalBeliefTable(c)),
                null,
                null, b);

        this.sensor = new ScalarSignal(c, this, ()->SensorConcept.this.resolution.asFloat()) {
            @Override
            protected LongSupplier stamp(Truth currentBelief,  NAR nar) {
                return SensorConcept.this.nextStamp(nar);
            }
        };

        this.signal = signal;

    }

    /**
     * returns a new stamp for a sensor task
     */
    protected LongSupplier nextStamp(NAR nar) {
        return nar.time::nextStamp;
    }


    public SensorConcept signal(FloatSupplier signal) {
        this.signal = signal;
        return this;
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
    public boolean add(Task t, NAR n) {

        //feedback prefilter non-signal beliefs

        if (cause >= 0) { //HACK wait for the cause id to come from a task
            if (t instanceof DerivedTask) { //TODO should not apply to: SignalTask, RevisionTask for now just apply to Derived
                if (t.isBelief()) {
                    PredictionFeedback.feedbackNewBelief(cause, t, beliefs, n);
                    if (t.isDeleted())
                        return false;
                }
            }
        } else {
            //HACK snoop this concept's cause channel from a signal task inserted to it
            if (t instanceof SignalTask) {
                short[] c = ((SignalTask) t).cause;
                if (c.length > 0)
                    this.cause = c[0];
            }
        }

        return super.add(t, n);
    }

    @Nullable
    public Task update(FloatFloatToObjectFunction<Truth> truther, long time, int dur, NAR n) {
        Task x = sensor.update(this, truther, n, time, dur);

        PredictionFeedback.feedbackNewSignal(sensor.get() /* get() again in case x is stretched it will be null */, beliefs, n);

        return x;
    }


    public SensorConcept resolution(float r) {
        resolution.set(r);
        return this;
    }

    public SensorConcept pri(FloatSupplier pri) {
        sensor.pri(pri);
        return this;
    }

}
