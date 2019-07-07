package nars.concept.sensor;

import jcog.Util;
import jcog.math.FloatSupplier;
import nars.$;
import nars.NAR;
import nars.agent.Game;
import nars.concept.PermanentConcept;
import nars.concept.TaskConcept;
import nars.table.BeliefTable;
import nars.table.dynamic.SensorBeliefTables;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;

import java.util.List;
import java.util.function.Function;

import static nars.Op.BELIEF;


/**
 * primarily a collector for belief-generating time-changing (live) input scalar (1-d real value) signals
 *
 *
 * In vector analysis, a scalar quantity is considered to be a quantity that has magnitude or size, but no motion. An example is pressure; the pressure of a gas has a certain value of so many pounds per square inch, and we can measure it, but the notion of pressure does not involve the notion of movement of the gas through space. Therefore pressure is a scalar quantity, and it's a gross, external quantity since it's a scalar. Note, however, the dramatic difference here between the physics of the situation and mathematics of the situation. In mathematics, when you say something is a scalar, you're just speaking of a number, without having a direction attached to it. And mathematically, that's all there is to it; the number doesn't have an internal structure, it doesn't have internal motion, etc. It just has magnitude - and, of course, location, which may be attachment to an object.
 * http://www.cheniere.org/misc/interview1991.htm#Scalar%20Detector
 */
abstract public class Signal extends TaskConcept implements GameLoop, FloatFunction<Term>, FloatSupplier, PermanentConcept {


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

    private volatile float currentValue = Float.NaN;

//    public Signal(Term term, FloatSupplier signal, NAR n) {
//        this(term, n.newCause(term).id, signal, n);
//    }

    public Signal(Term term, NAR n) {
        this(term,
                beliefTable(term, n, true, true),
                beliefTable(term, n, false, false),
                n);
    }

    static BeliefTable beliefTable(Term term, NAR n, boolean beliefOrGoal, boolean sensor) {
        return sensor ? new SensorBeliefTables(term, beliefOrGoal) : n.conceptBuilder.newTable(term, beliefOrGoal);
    }

    public Signal(Term term, BeliefTable beliefTable, BeliefTable goalTable, NAR n) {
        super(term, beliefTable, goalTable, n.conceptBuilder);

        n.add(this);
    }


    @Override
    public Iterable<Termed> components() {
        return List.of(this);
    }


    @Override
    public float floatValueOf(Term anObject /* ? */) {
        return this.currentValue;
    }


    @Override
    public float asFloat() {
        return currentValue;
    }

    public void update(FloatFloatToObjectFunction<Truth> truther, FloatSupplier pri, short[] cause, Game g) {

        float prevValue = currentValue;

        float nextValue = (currentValue = nextValue());

        Truth nextTruth = nextValue == nextValue ? truther.value(prevValue, nextValue) : null;

        ((SensorBeliefTables) beliefs()).input(
                g.dither(nextTruth, this),
                g.now,
                pri, cause,
                g.durPhysical(),
                g.what(), autoTaskLink());
    }

    abstract public float nextValue();

    /** whether to tasklink on change; returns false in batch signal cases */
    protected boolean autoTaskLink() {
        return true;
    }

    @Override
    public void update(Game g) {
        NAR nar = g.nar();
        update((tp, tn) -> $.t(Util.unitize(tn), nar.confDefault(BELIEF)), this::pri, cause(), g);
    }

    abstract public short[] cause();

    abstract public float pri();

}
