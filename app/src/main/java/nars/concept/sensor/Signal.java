package nars.concept.sensor;

import jcog.Util;
import jcog.math.FloatRange;
import jcog.math.FloatSupplier;
import nars.$;
import nars.NAR;
import nars.attention.AttNode;
import nars.attention.AttVectorNode;
import nars.concept.PermanentConcept;
import nars.concept.TaskConcept;
import nars.control.channel.CauseChannel;
import nars.link.TermLinker;
import nars.table.dynamic.SensorBeliefTables;
import nars.table.dynamic.SeriesBeliefTable;
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

    public final AttNode attn;

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

    public Signal(Term term, FloatSupplier signal, NAR n) {
        this(term, BELIEF, signal, n.conceptBuilder.termlinker(term), n);
    }

    private Signal(Term term, byte punc, FloatSupplier signal, TermLinker linker, NAR n) {
        super(term,
                punc == BELIEF ? new SensorBeliefTables(term, true, n.conceptBuilder) : n.conceptBuilder.newTable(term, true),
                punc == GOAL ? new SensorBeliefTables(term, false, n.conceptBuilder) : n.conceptBuilder.newTable(term, false),
                linker,
                n.conceptBuilder);

        this.source = signal;

        this.attn = new AttVectorNode(term, List.of(this));

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

    @Nullable
    public SeriesBeliefTable.SeriesRemember update(long start, long end, float pri, FloatFloatToObjectFunction<Truth> truther, NAR n) {

        float prevValue = currentValue, nextValue = floatValueOf(term);

        return ((SensorBeliefTables) beliefs())
                .add(nextValue == nextValue ? truther.value(prevValue, nextValue) : null,
                        start, end, pri, this, n.dur(), n);
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
    public void update(long prev, long now, long next, NAR nar) {

        float pri = attn.supply.priElseZero();
        SeriesBeliefTable.SeriesRemember t = update(prev, now, pri, (tp, tn) -> $.t(Util.unitize(tn), nar.confDefault(BELIEF)), nar);
        if (t!=null) {
            attn.taken(t.input.priElseZero());
            in.input(t);
        }

    }


}
