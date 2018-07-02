package nars.concept.action;

import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Sensor;
import nars.concept.dynamic.SignalBeliefTable;
import nars.control.MetaGoal;
import nars.control.proto.Remember;
import nars.link.TermlinkTemplates;
import nars.task.ITask;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.stream.Stream;

import static nars.Op.BELIEF;


public abstract class ActionConcept extends Sensor {

    protected ActionConcept(Term term, NAR n) {
        super(term,
                new SignalBeliefTable(term, true, n.conceptBuilder),
                n.conceptBuilder.newTable(term, false),
                n.conceptBuilder);
        ((SignalBeliefTable)beliefs()).pri(() -> n.priDefault(BELIEF));
        ((SignalBeliefTable)beliefs()).res(resolution);
    }

    @Override
    protected TermlinkTemplates buildTemplates(Term term) {
        return super.buildTemplates(term);
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

    abstract public Stream<ITask> update(long start, long end, int dur, NAR nar);

    @Override
    public void value(Task t, float activation, NAR n) {

        super.value(t, activation, n);

        if (t.isGoal()) {
            long now = n.time();
            if (!t.isBefore(now - n.dur()/2)) { 
                MetaGoal.Action.learn(t.cause(), Param.beliefValue(t) * activation, n.causes);
            }
        }
    }



    /**
     * determines the feedback belief when desire or belief has changed in a MotorConcept
     * implementations may be used to trigger procedures based on these changes.
     * normally the result of the feedback will be equal to the input desired value
     * although this may be reduced to indicate that the motion has hit a limit or
     * experienced resistence

     * @param desired  current desire - null if no desire Truth can be determined
     * @param believed current belief - null if no belief Truth can be determined
     * @return truth of a new feedback belief, or null to disable the creation of any feedback this iteration
     */
    @FunctionalInterface
    public interface MotorFunction extends BiFunction<Truth,Truth,Truth> {


        @Nullable Truth apply(@Nullable Truth believed, @Nullable Truth desired);










    }

}



























