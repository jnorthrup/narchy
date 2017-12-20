package nars.util.signal;

import jcog.Util;
import jcog.math.FloatSupplier;
import nars.NAR;
import nars.Task;
import nars.task.signal.SignalTask;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;

import static nars.Op.BELIEF;


/**
 * Generates temporal tasks in reaction to the change in a scalar numeric value
 *
 * when NAR wants to update the signal, it will call Function.apply. it can return
 * an update Task, or null if no change
 */
public class ScalarSignal extends Signal implements  DoubleSupplier {



    private final Term term;



    private final FloatFunction<Term> value;
    @NotNull
    private final FloatToObjectFunction<Truth> truthFloatFunction;


    public float currentValue = Float.NaN;


    public final static FloatToFloatFunction direct = n -> n;



    public ScalarSignal(NAR n, Term t, FloatFunction<Term> value, @Nullable FloatToObjectFunction<Truth> truthFloatFunction, FloatSupplier resolution) {
        super(BELIEF, resolution);

        pri(()->n.priDefault(BELIEF));
        this.term = t;
        this.value = value;
        this.truthFloatFunction = truthFloatFunction == null ? (v)->null : truthFloatFunction;
    }


    public byte punc() { return punc; }

//    /** clears timing information so it thinks it will need to input on next attempt */
//    public void ready() {
//        this.lastInputTime = nar.time() - minTimeBetweenUpdates;
//    }


    /** does not input the task, only generates it.
     *  the time is specified instead of obtained from NAR so that
     *  all sensor readings can be timed with perfect consistency within the same cycle
     * */
    public Task update( NAR nar, long now, int dur) {

        //long now = nar.time();

        //update previous task: extend its end time to current time
//        if (current!=null && current.isDeleted())  {
//            currentValue = Float.NaN; //force re-input
//        }

        //int timeSinceLastInput = (int) (now - lastInputTime);



            float nextRaw = value.floatValueOf(term);
            float cur = currentValue;

            float next = Util.unitize(Util.round(nextRaw, resolution.asFloat()));

            currentValue = (next);

            Truth truth = (next == next) ? truthFloatFunction.valueOf(next) : null;

            Task nextTask = set(term,
                    truth,
                    stamp(truth, nar),
                    now, dur, nar);
            return nextTask;
    }

    protected LongSupplier stamp(Truth currentBelief, NAR nar) {
        return nar.time::nextStamp;
    }





//    protected float conf(float v) {
//        return confFactor;
//    }
//    protected float freq(float v) {
//        return v;
//    }

    public float freq() {
        SignalTask t = get();
        if (t !=null)
            return t.freq();
        else
            return Float.NaN;
    }




    /** provides an immediate truth assessment with the last known signal value */
    @Nullable public final Truth truth() {
        Task t = get();
        return t!=null ? t.truth() : null;
    }

    //    public float pri(float v, long now, float prevV, long lastV) {
//        return pri;
//    }

    @NotNull
    public Term term() {
        return term;
    }


    @Override
    public double getAsDouble() {
        return currentValue;
    }


}
