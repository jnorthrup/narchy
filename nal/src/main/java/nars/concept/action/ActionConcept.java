package nars.concept.action;

import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Sensor;
import nars.control.MetaGoal;
import nars.task.ITask;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.stream.Stream;


public abstract class ActionConcept extends Sensor {

    protected ActionConcept(@NotNull Term term, @NotNull NAR n) {
        super(term, n);
    }

    @Nullable
    abstract public Stream<ITask> update(long now, int dur, NAR nar);

    @Override
    public void value(Task t, float activation, NAR n) {

        super.value(t, activation, n);

        if (t.isGoal()) {
            long now = n.time();
            if (!t.isBefore(now - n.dur()/2)) { //present or future
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

//        /**
//         * all desire passes through to affect belief
//         */
//        MotorFunction Direct = (believed, desired) -> desired;
//
//        /**
//         * absorbs all desire and doesnt affect belief
//         */
//        @Nullable MotorFunction Null = (believed, desired) -> null;
    }

}



//    @Deprecated public static class CuriosityTask extends GeneratedTask {
//
//        public CuriosityTask(Compound term, byte punc, Truth truth, long creation, long start, long end, long[] stamp) {
//            super(term, punc, truth, creation, start, end, stamp);
//        }
//    }

//    public static CuriosityTask curiosity(Compound term, byte punc, float conf, long next, NAR nar) {
//        long now = nar.time();
//        CuriosityTask t = new CuriosityTask(term, punc,
//                $.t(nar.random().nextFloat(), conf),
//                now,
//                next,
//            next + nar.dur(),
//                new long[] { nar.time.nextStamp() }
//        );
//        t.budget( nar );
//        return t;
//
//    }


//    /** produces a curiosity exploratoin task */
//    @Nullable public abstract Task curiosity(float conf, long next, NAR nar);
