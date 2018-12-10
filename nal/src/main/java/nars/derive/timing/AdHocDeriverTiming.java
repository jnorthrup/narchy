package nars.derive.timing;

import nars.NAR;
import nars.Task;
import nars.term.Term;

import java.util.Random;
import java.util.function.BiFunction;

import static nars.time.Tense.ETERNAL;

/** naively applies a variety of methods for calculating time focus targets */
public class AdHocDeriverTiming implements BiFunction<Task, Term, long[]> {

    private final Random rng;
    private final NAR nar;

    public AdHocDeriverTiming(NAR n) {
        this.nar = n;
        this.rng = n.random();
    }

    @Override
    public long[] apply(Task task, Term term) {
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
                tt = pastFutureRadius(task);
//                break;
//            }
//            default:
//                throw new UnsupportedOperationException();
//        }

        return tt;
    }



    private long[] presentDuration(float factor) {
        long now = nar.time.now();
        int dur = Math.round(factor * nar.dur());
        return new long[] { now - dur/2, now + dur/2 };
    }

    private long[] taskTime(Task t) {
        return new long[] {t.start(), t.end() };
    }

    private long[] pastFutureRadius(Task t) {
        long now = nar.time.now();
        int dur = nar.dur();

        long[] tt = taskTime(t);
        if (tt[0]==ETERNAL) {
            tt[0] = now - dur;
            tt[1] = now + dur;
            return tt;
        } else {
//            return presentDuration(4);
//        } else {


            double focus = 0.5f;
            long range =
                    //Math.max(Math.abs(tt[0] - now), Math.abs(tt[1] - now));
                    Math.max(dur, Math.round(Math.min(Math.abs(tt[0] - now), Math.abs(tt[1] - now)) * focus));

            //float factor = rng.nextFloat();
            long center;
            switch (rng.nextInt(3)) {
                case 0:
                    center = (now + ((tt[0] + tt[1]) / 2L)) / 2L; //midpoint between now and the task
                    break;
                case 1:
                    center = now;
                    break;
                case 2:
                    center = ((tt[0] + tt[1]) / 2L); //task midpoint
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
            return new long[]{center - range, center + range};
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

    }

}
