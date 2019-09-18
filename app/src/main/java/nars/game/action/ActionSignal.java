package nars.game.action;

import nars.NAR;
import nars.concept.PermanentConcept;
import nars.game.sensor.GameLoop;
import nars.game.sensor.UniSignal;
import nars.table.BeliefTable;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;


public abstract class ActionSignal extends UniSignal implements GameLoop, PermanentConcept {


    protected ActionSignal(Term term, BeliefTable beliefs, BeliefTable goals, NAR n) {
        super(term, null, beliefs, goals, n);
    }


//    /** estimates the organic (derived, excluding curiosity) goal confidence for the given time interval
//     * TODO exclude input tasks from the calculation */
//    abstract public float dexterity(long start, long end, NAR n);
    abstract public double dexterity();
    abstract public double coherency();

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



























