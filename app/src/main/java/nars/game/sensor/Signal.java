package nars.game.sensor;

import jcog.math.FloatSupplier;
import nars.$;
import nars.NAR;
import nars.attention.What;
import nars.concept.PermanentConcept;
import nars.concept.TaskConcept;
import nars.game.Game;
import nars.table.BeliefTable;
import nars.table.dynamic.SensorBeliefTables;
import nars.term.Term;
import nars.term.Termed;
import nars.time.When;
import nars.truth.DiscreteTruth;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;


/**
 * primarily a collector for belief-generating time-changing (live) input scalar (1-d real value) signals
 *
 *
 * In vector analysis, a scalar quantity is considered to be a quantity that has magnitude or size, but no motion. An example is pressure; the pressure of a gas has a certain value of so many pounds per square inch, and we can measure it, but the notion of pressure does not involve the notion of movement of the gas through space. Therefore pressure is a scalar quantity, and it's a gross, external quantity since it's a scalar. Note, however, the dramatic difference here between the physics of the situation and mathematics of the situation. In mathematics, when you say something is a scalar, you're just speaking of a number, without having a direction attached to it. And mathematically, that's all there is to it; the number doesn't have an internal structure, it doesn't have internal motion, etc. It just has magnitude - and, of course, location, which may be attachment to an object.
 * http://www.cheniere.org/misc/interview1991.htm#Scalar%20Detector
 */
public abstract class Signal extends TaskConcept implements GameLoop, PermanentConcept {


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

    private volatile Truth currentValue = null;
    boolean inputting;

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
    public final Iterable<? extends Termed> components() {
        return List.of(this);
    }

//    @Override
//    public float floatValueOf(Term anObject /* ? */) {
//        return this.currentValue;
//    }

    public final Truth value() {
        return currentValue;
    }

    @Deprecated public static FloatToObjectFunction<Truth> truther(float freqRes, float conf, Game g) {
        float c = g.ditherConf(conf);
        return (float nextValue) ->
            nextValue==nextValue ?
                DiscreteTruth.the(g.ditherFreq(nextValue, freqRes),c) : null;
    }

    public static Truth truthDithered(float nextValue, float freqRes, Game g) {
        return DiscreteTruth.the(
            g.ditherFreq(nextValue, freqRes),
            g.ditherConf(g.confDefaultBelief)
        );
    }

    /** pre-commit phase */
    public final boolean input(@Nullable Truth next, Term why, When<What> wLoop) {
        //Truth prevValue = currentValue;
        Truth nextValue = (currentValue = next);

        return this.inputting = ((SensorBeliefTables) beliefs()).input(nextValue, wLoop, why);
    }

    /** combined phases */
    public final void input(Truth next, float pri, Term why, When<What> wLoop) {
//        assert(pri > ScalarValue.EPSILON); //HACK
        input(next, why, wLoop);
        commit(pri, wLoop);
    }

    /** post-commit phase */
    public final void commit(float pri, When<What> w) {
        SensorBeliefTables b = (SensorBeliefTables) beliefs();

        float dur = w.dur;

        if (inputting)
            b.remember(w.x, pri, autoTaskLink(), dur);

        b.series.clean(b, SensorBeliefTables.cleanMarginCycles(dur));

    }


    /** whether to tasklink on change; returns false in batch signal cases */
    protected boolean autoTaskLink() {
        return true;
    }

}
