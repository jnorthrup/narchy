package nars.concept.action;

import jcog.math.FloatRange;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.PermanentConcept;
import nars.concept.TaskConcept;
import nars.concept.sensor.Sensor;
import nars.control.MetaGoal;
import nars.control.proto.Remember;
import nars.table.BeliefTable;
import nars.table.dynamic.SignalBeliefTable;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiFunction;


public abstract class ActionConcept extends TaskConcept implements Sensor, PermanentConcept {

    protected ActionConcept(Term term, NAR n) {
        this(term,
                new SignalBeliefTable(term, true, n.conceptBuilder),
                n.conceptBuilder.newTable(term, false),
                n);
    }

    protected ActionConcept(Term term, BeliefTable beliefs, BeliefTable goals, NAR n) {
        super(term, beliefs, goals, n.conceptBuilder);

        ((SignalBeliefTable) beliefs()).setPri(
                FloatRange.unit(
                        //Util.or(n.priDefault(BELIEF), n.priDefault(GOAL))
                        n.goalPriDefault //even though the tasks are beliefs
                )
        );
        ((SignalBeliefTable) beliefs()).resolution(FloatRange.unit(n.freqResolution));
    }

    @Override
    public Iterable<Termed> components() {
        return List.of(this);
    }

    /** estimates the organic (derived, excluding curiosity) goal confidence for the given time interval
     * TODO exclude input tasks from the calculation */
    abstract public float dexterity(long start, long end, NAR n);

    @Override
    public FloatRange resolution() {
        return ((SignalBeliefTable) beliefs()).resolution();
    }
    @Override
    public FloatRange pri() {
        return ((SignalBeliefTable) beliefs()).pri();
    }

    @Override
    public void add(Remember r, NAR n) {
        Task t = r.input;
        if (t.isBeliefOrGoal() && t.isEternal()) {
            /** reject eternal beliefs and goals of any type
             * TODO avoid creation of Eternal tables
             *  */
            r.reject();

        } else {
            super.add(r, n);
        }
    }



    @Override
    public void value(Task t, float activation, NAR n) {

        super.value(t, activation, n);

        if (t.isGoal()) {
            long now = n.time();
            if (!t.isBefore(now - n.dur() / 2)) {
                MetaGoal.Action.learn(t.cause(), Param.beliefValue(t) * activation, n.causes);
            }
        }
    }

    public ActionConcept resolution(float v) {
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



























