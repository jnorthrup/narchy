package nars.concept.sensor;

import jcog.Util;
import jcog.math.FloatRange;
import jcog.math.FloatSupplier;
import nars.$;
import nars.NAR;
import nars.concept.PermanentConcept;
import nars.concept.TaskConcept;
import nars.control.DurService;
import nars.control.channel.CauseChannel;
import nars.table.dynamic.SensorBeliefTables;
import nars.task.ITask;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;


/**
 * primarily a collector for belief-generating time-changing (live) input scalar (1-d real value) signals
 *
 *
 * In vector analysis, a scalar quantity is considered to be a quantity that has magnitude or size, but no motion. An example is pressure; the pressure of a gas has a certain value of so many pounds per square inch, and we can measure it, but the notion of pressure does not involve the notion of movement of the gas through space. Therefore pressure is a scalar quantity, and it's a gross, external quantity since it's a scalar. Note, however, the dramatic difference here between the physics of the situation and mathematics of the situation. In mathematics, when you say something is a scalar, you're just speaking of a number, without having a direction attached to it. And mathematically, that's all there is to it; the number doesn't have an internal structure, it doesn't have internal motion, etc. It just has magnitude - and, of course, location, which may be attachment to an object.
 * http://www.cheniere.org/misc/interview1991.htm#Scalar%20Detector
 */
public class Signal extends TaskConcept implements Sensor, FloatFunction<Term>, FloatSupplier, PermanentConcept {

    /**
     * update directly with next value
     */
    public static Function<FloatSupplier, FloatFloatToObjectFunction<Truth>> SET = (conf) ->
            ((prev, next) -> next == next ? $.t(next, conf.asFloat()) : null);
    /**
     * first order difference
     */
    public static Function<FloatSupplier, FloatFloatToObjectFunction<Truth>> DIFF = (conf) ->
            ((prev, next) -> (next == next) ? ((prev == prev) ? $.t((next - prev) / 2f + 0.5f, conf.asFloat()) : $.t(0.5f, conf.asFloat())) : $.t(0.5f, conf.asFloat()));

    public final FloatSupplier source;
    private final CauseChannel<ITask> in;

    private volatile float currentValue = Float.NaN;

    public Signal(Term c, FloatSupplier signal, NAR n) {
        this(c, BELIEF, signal, n);
    }

    public Signal(Term term, byte punc, FloatSupplier signal, NAR n) {
        super(term,
                punc == BELIEF ? new SensorBeliefTables(term, true, n.conceptBuilder) : n.conceptBuilder.newTable(term, true),
                punc == GOAL ? new SensorBeliefTables(term, false, n.conceptBuilder) : n.conceptBuilder.newTable(term, false),
                n.conceptBuilder);

        this.source = signal;

        setPri(FloatRange.unit(punc == BELIEF ? n.beliefPriDefault : n.goalPriDefault));
        ((SensorBeliefTables) beliefs()).resolution(FloatRange.unit(n.freqResolution));

        in = newChannel(n);
        n.on(this);
    }

    protected CauseChannel<ITask> newChannel(NAR n) {
        return n.newChannel(this);
    }

    @Override
    public Iterable<Termed> components() {
        return List.of(this);
    }

    @Override
    public final FloatRange resolution() {
        return ((SensorBeliefTables) beliefs()).resolution();
    }

    @Override
    public float floatValueOf(Term anObject /* ? */) {
        return this.currentValue = source.asFloat();
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
    public final ITask update(FloatFloatToObjectFunction<Truth> truther, NAR n) {
        return update(truther, n.time(), n.dur(), n);
    }

    @Nullable
    @Deprecated
    public ITask update(FloatFloatToObjectFunction<Truth> truther, long time, int dur, NAR n) {
        return update(time - dur / 2, time + Math.max(0, (dur / 2 - 1)), truther, dur, n);
    }

    @Nullable
    public final ITask update(long prev, long now, FloatFloatToObjectFunction<Truth> truther, NAR n) {
        return update(prev, now, truther, now - prev, n);
    }

    @Nullable
    public ITask update(long start, long end, FloatFloatToObjectFunction<Truth> truther, float dur, NAR n) {

        float prevValue = currentValue;
        float nextValue = floatValueOf(term);
        if (nextValue == nextValue /* not NaN */) {
            Truth nextTruth = truther.value(prevValue, nextValue);
            if (nextTruth != null) {
                SensorBeliefTables s = (SensorBeliefTables) beliefs();
                assert(dur > 0);
                return s.add(nextTruth, start, end, this, dur, n);
            }
        }
        return null;

    }


    public Signal resolution(float r) {
        resolution().set(r);
        return this;
    }
    public Signal setResolution(FloatRange r) {
        ((SensorBeliefTables) beliefs()).resolution(r);
        return this;
    }

    @Override
    public FloatRange pri() {
        return ((SensorBeliefTables) beliefs()).pri();
    }

    @Override
    public void update(long prev, long now, long next, NAR nar) {
        in.input(update(prev, now, (tp, tn) -> $.t(Util.unitize(tn), nar.confDefault(BELIEF)),nar));
    }

    public Signal setPri(FloatRange pri) {
        ((SensorBeliefTables) beliefs()).setPri(pri);
        return this;
    }

}
