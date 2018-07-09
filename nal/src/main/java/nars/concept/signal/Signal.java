package nars.concept.signal;

import jcog.math.FloatSupplier;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.concept.Sensor;
import nars.concept.dynamic.SignalBeliefTable;
import nars.concept.util.ConceptBuilder;
import nars.control.DurService;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.LongSupplier;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;


/**
 * primarily a collector for belief-generating time-changing (live) input scalar (1-d real value) signals
 *
 *
 * In vector analysis, a scalar quantity is considered to be a quantity that has magnitude or size, but no motion. An example is pressure; the pressure of a gas has a certain value of so many pounds per square inch, and we can measure it, but the notion of pressure does not involve the notion of movement of the gas through space. Therefore pressure is a scalar quantity, and it's a gross, external quantity since it's a scalar. Note, however, the dramatic difference here between the physics of the situation and mathematics of the situation. In mathematics, when you say something is a scalar, you're just speaking of a number, without having a direction attached to it. And mathematically, that's all there is to it; the number doesn't have an internal structure, it doesn't have internal motion, etc. It just has magnitude - and, of course, location, which may be attachment to an object.
 * http://www.cheniere.org/misc/interview1991.htm#Scalar%20Detector
 */
public class Signal extends Sensor implements FloatFunction<Term>, FloatSupplier {

    /**
     * update directly with next value
     */
    public static Function<FloatSupplier, FloatFloatToObjectFunction<Truth>> SET = (conf) ->
            ((p, n) -> n == n ? $.t(n, conf.asFloat()) : null);
    /**
     * first order difference
     */
    public static Function<FloatSupplier, FloatFloatToObjectFunction<Truth>> DIFF = (conf) ->
            ((p, n) -> (n == n) ? ((p == p) ? $.t((n - p) / 2f + 0.5f, conf.asFloat()) : $.t(0.5f, conf.asFloat())) : $.t(0.5f, conf.asFloat()));
    public final FloatSupplier signal;

    private volatile float currentValue = Float.NaN;

    public Signal(Term c, FloatSupplier signal, NAR n) {
        this(c, BELIEF, signal, n);
    }

    public Signal(Term c, byte punc, FloatSupplier signal, NAR n) {
        this(c, punc, signal, n.conceptBuilder);
        pri(() -> n.priDefault(punc));
        ((SignalBeliefTable) beliefs()).res(resolution);
        n.on(this);
    }

    private Signal(Term term, byte punc, FloatSupplier signal, ConceptBuilder b) {
        super(term,
                punc == BELIEF ? new SignalBeliefTable(term, true, b) : b.newTable(term, true),
                punc == GOAL ? new SignalBeliefTable(term, false, b) : b.newTable(term, false),
                b);

        this.signal = signal;

    }

    /**
     * returns a new stamp for a sensor task
     */
    protected static LongSupplier nextStamp(NAR nar) {
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

    @Deprecated
    public final DurService auto(NAR n) {
        return auto(n, 1);
    }

    @Deprecated
    public DurService auto(NAR n, float durs) {
        FloatFloatToObjectFunction<Truth> truther =
                (prev, next) -> $.t(next, n.confDefault(BELIEF));

        return DurService.on(n, nn ->
                nn.input(update(truther, n))).durs(durs);
    }

    @Nullable
    @Deprecated
    public final Task update(FloatFloatToObjectFunction<Truth> truther, NAR n) {
        return update(truther, n.time(), n.dur(), n);
    }

    @Nullable
    @Deprecated
    public Task update(FloatFloatToObjectFunction<Truth> truther, long time, int dur, NAR n) {
        return update(time - dur / 2, time + dur / 2, truther, dur, n);
    }

    @Nullable
    public Task update(long start, long end, FloatFloatToObjectFunction<Truth> truther, int dur, NAR n) {

        float prevValue = currentValue;
        float nextValue = floatValueOf(term);
        if (nextValue == nextValue /* not NaN */) {
            Truth nextTruth = truther.value(prevValue, nextValue);
            if (nextTruth != null) {


                return ((SignalBeliefTable) beliefs()).add(nextTruth,
                        start, end, dur, this, n);
            }
        }
        return null;

    }


    public nars.concept.signal.Signal resolution(float r) {
        resolution.set(r);
        return this;
    }

    public nars.concept.signal.Signal pri(FloatSupplier pri) {
        ((SignalBeliefTable) beliefs()).pri(pri);
        return this;
    }

}
