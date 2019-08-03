package nars.derive.time;

import jcog.func.TriFunction;
import nars.Task;
import nars.attention.What;
import nars.term.Term;

import static nars.derive.time.TaskOrPresentTiming.taskTime;
import static nars.time.Tense.ETERNAL;

/** naively applies a variety of methods for calculating time focus targets */
public class AdHocDeriverTiming implements TriFunction<What, Task, Term, long[]> {


    public AdHocDeriverTiming() {

    }

    @Override
    public long[] apply(What what, Task task, Term term) {
//        if (task.isEternal())
//            return new long[] { ETERNAL, ETERNAL };

        long[] tt;
//        switch (rng.nextInt(2)) {
//
//            case 0:
//                tt = taskTime(task); break;
//            case 1: {
//                boolean past =
//                        //(task.mid() < nar.time());
//                        nar.random().nextBoolean();
                //tt = pastFutureRadius(task, past, !past);
        long[] result;
        long now = what.time();
        float dur = what.dur();

        long[] tt1 = taskTime(task);
        if (tt1[0]==ETERNAL) {
            tt1[0] = Math.round(now - dur/2);
            tt1[1] = Math.round(now + dur/2);
            result = tt1;
        } else {
//            return presentDuration(4);
//        } else {


            double focus = 0.5f;
            long range =
                    //Math.max(Math.abs(tt[0] - now), Math.abs(tt[1] - now));
                    Math.round(Math.max(dur, Math.min(Math.abs(tt1[0] - now), Math.abs(tt1[1] - now)) * focus));

            //float factor = rng.nextFloat();
            long center;
            switch (what.random().nextInt(3)) {
                case 0:
                    center = (now + ((tt1[0] + tt1[1]) / 2L)) / 2L; //midpoint between now and the task
                    break;
                case 1:
                    center = now;
                    break;
                case 2:
                    center = ((tt1[0] + tt1[1]) / 2L); //task midpoint
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
            result = new long[]{center - range, center + range};
        }

//            //factor *= factor * factor; //^3
//            range = Math.round(factor * range * 2);
//
//            if (past && future) {
//            } else if (future) {
//                return new long[]{now, now + range}; //future span
//            } else {
//                return new long[]{now - range, now}; //past span
//            }
////        }

        tt = result;
//                break;
//            }
//            default:
//                throw new UnsupportedOperationException();
//        }

        return tt;
    }


//
//    private long[] presentDuration(float factor) {
//        long now = nar.time.now();
//        int dur = Math.round(factor * nar.dur());
//        return new long[] { now - dur/2, now + dur/2 };
//    }


}
