package nars.concept;

import jcog.math.FloatSupplier;
import nars.NAR;
import nars.Task;
import nars.concept.builder.ConceptBuilder;
import nars.task.signal.SignalTask;
import nars.task.util.PredictionFeedback;
import nars.term.Term;
import nars.truth.Truth;
import nars.util.signal.ScalarSignal;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
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

    //static final Logger logger = LoggerFactory.getLogger(SensorConcept.class);

    public SensorConcept(Term c, NAR n, FloatSupplier signal, FloatToObjectFunction<Truth> truth) {
        this(c, n.terms.conceptBuilder, signal, truth);
        sensor.pri(() -> n.priDefault(BELIEF));
    }

    public SensorConcept(Term c, ConceptBuilder b, FloatSupplier signal, FloatToObjectFunction<Truth> truth) {
        super(c,
                //new SensorBeliefTable(n.conceptBuilder.newTemporalBeliefTable(c)),
                null,
                null, b);

        this.sensor = new ScalarSignal(c, this, truth, ()->SensorConcept.this.resolution.asFloat()) {
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
        if (!(t instanceof SignalTask) && cause >= 0) {
            if (t.isBelief()) {
                PredictionFeedback.feedbackNewBelief(cause, t, beliefs, n);
                if (t.isDeleted())
                    return false;
            }
        } else {
            this.cause = ((SignalTask) t).cause[0];
        }

        return super.add(t, n);
    }

    @Nullable
    public Task update(long time, int dur, NAR n) {
        Task x = sensor.update(this, n, time, dur);

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
