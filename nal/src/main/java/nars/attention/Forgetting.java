package nars.attention;

import jcog.pri.bag.Bag;
import jcog.pri.op.PriForget;
import nars.NAR;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/** TODO abstract */
abstract public class Forgetting {

    public void update(NAR n) {

    }


    public final @Nullable Consumer forget(Bag b, float depressurizationRate, float temperature) {

        if (temperature > Float.MIN_NORMAL) {
            int size = b.size();
            if (size > 0) {
                int cap = b.capacity();
                if (cap > 0) {

                    float pressure = b.depressurizePct(depressurizationRate);
                    assert(pressure == pressure);

                    float mass = b.mass(); assert(mass == mass);

                    if (mass > Float.MIN_NORMAL) {
                        return forget(temperature, size, cap, pressure, mass);
                    }
                }
            }
        }

        return null;
    }

    abstract protected @Nullable Consumer forget(float temperature, int size, int cap, float pressure, float mass);

    /** temporally oblivious; uses only incoming pressure to determine forget amounts */
    public static class AsyncForgetting extends Forgetting {


//        public final FloatRange tasklinkForgetRate = new FloatRange(1f, 0f, 1f);
//
//
//
//        protected Consumer<TaskLink> forgetTasklinks(Concept c, Bag<Tasklike, TaskLink> tasklinks) {
//            return forget(tasklinks, 1f, tasklinkForgetRate.floatValue());
//        }
//
//
        @Override
        protected Consumer forget(float temperature, int size, int cap, float pressure, float mass) {
            //return PriForget.forgetPressure(temperature, 0, size, cap, pressure, mass);
            return PriForget.forgetIdeal(temperature, 0.5f, size, cap, pressure, mass);
        }
    }

//    /** experimental */
//    @Deprecated public static class TimedForgetting extends Forgetting {
//
//        /**
//         * number of clock durations composing a unit of short target memory decay (used by bag forgetting)
//         */
//        public final FloatRange memoryDuration = new FloatRange(1f, 0f, 64f);
//
//
//
//        @Override
//        protected Consumer forget(float temperature, int size, int cap, float pressure, float mass) {
//            return PriForget.forgetIdeal(temperature,
//                                        ScalarValue.EPSILON * cap,
//                                        //1f/size,
//                                        //1f/cap,
//                                        //0.1f,
//                                        //0.5f,
//                                        size, cap, pressure, mass);
//        }
//
//
////        @Override
////        public void updateConcepts(Bag<Term, Activate> active, long dt, NAR n) {
////            float temperature = 1f - (float) Math.exp(-(((double) dt) / n.dur()) / memoryDuration.floatValue());
////            active.commit(active.forget(temperature));
////        }
//
//        public void update(Concept c, NAR n) {
//
//
//            int dur = n.dur();
//
//            Consumer<TaskLink> tasklinkUpdate;
//            Bag<Tasklike, TaskLink> tasklinks = c.tasklinks();
//
//            long curTime = n.time();
//            Long prevCommit = c.meta("C", curTime);
//            if (prevCommit != null) {
//                if (curTime - prevCommit > 0) {
//
//                    double deltaDurs = ((double) (curTime - prevCommit)) / dur;
//
//                    //deltaDurs = Math.min(deltaDurs, 1);
//
//                    float forgetRate = (float) (1 - Math.exp(-deltaDurs / memoryDuration.doubleValue()));
//
//                    //System.out.println(deltaDurs + " " + forgetRate);
//                    tasklinkUpdate = tasklinks.forget(forgetRate);
//
//                } else {
//                    //dont need to commit, it already happened in this cycle
//                    return;
//                }
//            } else {
//                tasklinkUpdate = null;
//
//            }
//
//            tasklinks.commit(tasklinkUpdate);
//
//        }
//    }
}
