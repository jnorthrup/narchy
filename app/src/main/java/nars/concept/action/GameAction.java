package nars.concept.action;

import jcog.math.FloatRange;
import nars.NAR;
import nars.attention.AttnBranch;
import nars.concept.PermanentConcept;
import nars.concept.TaskConcept;
import nars.concept.sensor.GameLoop;
import nars.table.BeliefTable;
import nars.table.dynamic.SensorBeliefTables;
import nars.table.temporal.RTreeBeliefTable;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiFunction;


public abstract class GameAction extends TaskConcept implements GameLoop, PermanentConcept {

    public final AttnBranch attn;

    protected GameAction(Term term, NAR n) {
        this(term,
                new SensorBeliefTables(term, true),
                new RTreeBeliefTable(),
                n);
    }

    protected GameAction(Term term, BeliefTable beliefs, BeliefTable goals, NAR n) {
        super(term, beliefs, goals, n.conceptBuilder);

        this.attn = new AttnBranch(term, List.of(term));
        ((SensorBeliefTables) beliefs()).resolution(FloatRange.unit(n.freqResolution));
    }


    @Override
    public Iterable<Termed> components() {
        return List.of(this);
    }

//    /** estimates the organic (derived, excluding curiosity) goal confidence for the given time interval
//     * TODO exclude input tasks from the calculation */
//    abstract public float dexterity(long start, long end, NAR n);
    abstract public double dexterity();

    @Override
    public FloatRange resolution() {
        return ((SensorBeliefTables) beliefs()).resolution();
    }


    public GameAction resolution(float v) {
        resolution().set(v);
        return this;
    }



    /**
     * determines the feedback belief when desire or belief has changed in a MotorConcept
     * implementations may be used to trigger procedures based on these changes.
     * normally the result of the feedback will be equal to the input desired value
     * although this may be reduced to indicate that the motion has hit a limit or
     * experienced resistence
     *
     * @param desired  current desire - null if no desire Truth can be determined
     * @param believed current belief - null if no belief Truth can be determined
     * @return truth of a new feedback belief, or null to disable the creation of any feedback this iteration
     */
    @FunctionalInterface
    public interface MotorFunction extends BiFunction<Truth, Truth, Truth> {

        @Nullable Truth apply(@Nullable Truth believed, @Nullable Truth desired);

    }

}


























