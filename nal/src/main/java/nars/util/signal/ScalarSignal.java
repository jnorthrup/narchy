package nars.util.signal;

import jcog.Util;
import jcog.math.FloatSupplier;
import nars.NAR;
import nars.Task;
import nars.concept.SensorConcept;
import nars.task.signal.SignalTask;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.LongSupplier;

import static nars.Op.BELIEF;


/**
 * Generates temporal tasks in reaction to the change in a scalar numeric value
 * <p>
 * when NAR wants to update the signal, it will call Function.apply. it can return
 * an update Task, or null if no change
 */
public class ScalarSignal extends Signal {


    private final Term term;


    private final FloatFunction<Term> value;


    public float currentValue = Float.NaN;


    public final static FloatToFloatFunction direct = n -> n;


    public ScalarSignal(Term t, FloatFunction<Term> value, FloatSupplier resolution) {
        super(BELIEF, resolution);
        this.term = t;
        this.value = value;
    }


    public byte punc() {
        return punc;
    }


    /**
     * does not input the task, only generates it.
     * the time is specified instead of obtained from NAR so that
     * all sensor readings can be timed with perfect consistency within the same cycle
     */
    public Task update(SensorConcept c, FloatFloatToObjectFunction<Truth> truthFloatFunction, NAR nar, long now, int dur) {

        float next = Util.unitize(Util.round(value.floatValueOf(term), resolution.asFloat()));

        float prevValue = currentValue;

        Truth truth = truthFloatFunction.value(prevValue, next);

        Task nextTask = set(c,
                truth,
                stamp(truth, nar),
                now, dur, nar);

        if (nextTask!=null) {
            currentValue = next;
        }

        return nextTask;
    }

    protected LongSupplier stamp(Truth currentBelief, NAR nar) {
        return nar.time::nextStamp;
    }


    public float freq() {
        SignalTask t = get();
        if (t != null)
            return t.freq();
        else
            return Float.NaN;
    }


    /**
     * provides an immediate truth assessment with the last known signal value
     */
    @Nullable
    public final Truth truth() {
        Task t = get();
        return t != null ? t.truth() : null;
    }

    //    public float pri(float v, long now, float prevV, long lastV) {
//        return pri;
//    }

    @NotNull
    public Term term() {
        return term;
    }



}
