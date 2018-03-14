package nars.concept.scalar;

import jcog.math.FloatSupplier;
import nars.NAR;
import nars.Task;
import nars.concept.Sensor;
import nars.concept.dynamic.ScalarBeliefTable;
import nars.concept.util.ConceptBuilder;
import nars.task.DerivedTask;
import nars.task.signal.SignalTask;
import nars.task.util.PredictionFeedback;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.function.LongSupplier;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;


/**
 * primarily a collector for belief-generating time-changing input scalar (1-d real value) signals
 */
public class Scalar extends Sensor implements FloatFunction<Term>, FloatSupplier {

//    public final ScalarSignal sensor;

    @Deprecated public final ScalarBeliefTable sensor;
    //@Deprecated public final ScalarBeliefTable goal;

    public FloatSupplier signal;

    private volatile float currentValue = Float.NaN;

    private transient short cause = -1;

    public Scalar(Term c, NAR n, FloatSupplier signal) {
        this(c, n.conceptBuilder, signal, n.random()::nextLong);
        ((ScalarBeliefTable)beliefs()).pri(() -> n.priDefault(BELIEF));
        ((ScalarBeliefTable)goals()).pri(() -> n.priDefault(GOAL));
    }

    private Scalar(Term c, ConceptBuilder b, FloatSupplier signal, LongSupplier stamp) {
        super(c,
                new ScalarBeliefTable(c, true, b.newTemporalTable(c), stamp.getAsLong()),
                new ScalarBeliefTable(c, false, b.newTemporalTable(c), stamp.getAsLong()),
                b);

        this.sensor = ((ScalarBeliefTable)beliefs());

//        this.sensor = new ScalarSignal(c, this, ()->Scalar.this.resolution.asFloat()) {
//            @Override
//            protected LongSupplier stamp(Truth currentBelief,  NAR nar) {
//                return Scalar.this.nextStamp(nar);
//            }
//        };

        this.signal = signal;

    }

    /**
     * returns a new stamp for a sensor task
     */
    protected LongSupplier nextStamp(NAR nar) {
        return nar.time::nextStamp;
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
//        Task x = sensor.update(this, truther, n, time, dur);
//
//        PredictionFeedback.feedbackNewSignal(
//                sensor.get() /* get() again in case x is stretched it will be null */, beliefs, n);
//
//        return x;

        sensor.update(truther.value(currentValue, floatValueOf(term)), time, dur);

        return sensor.matchDynamic(time-dur/2, time+dur/2, null, n);
        //return null;
    }


    public Scalar resolution(float r) {
        resolution.set(r);
        return this;
    }

    public Scalar pri(FloatSupplier pri) {
        sensor.pri(pri);
        return this;
    }

}
