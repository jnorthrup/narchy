package nars.derive.timing;


import jcog.func.TriFunction;
import jcog.math.FloatRange;
import nars.Task;
import nars.attention.What;
import nars.term.Term;

import java.util.Random;

public class ActionTiming implements TriFunction<What, Task, Term, long[]> {

    public final FloatRange horizonDurs = new FloatRange(4, 0, 32);
    //public final FloatRange widthDurs = new FloatRange(2, 0, 8);

    public ActionTiming() {

    }

    @Override
    public long[] apply(What what, Task task, Term term) {

        int dur = what.dur();

        long now = what.time();
        Random rng = what.random();
        long start, end;
//        long taskStart = task.start();
//        if (taskStart <= now - dur) {

            //gaussian
            long then = Math.round(now + rng.nextGaussian() * horizonDurs.floatValue() * dur);

            //uniform
            //long then = Math.round(now + (-.5f + rng.nextFloat()) * 2 * horizonDurs.floatValue() * dur); //uniform



            start = (then - dur / 2);
            //start = Tense.dither(start, nar);
            end = (then + dur / 2);
            //end = Tense.dither(end, nar);


//        } else {
//            //non-eternal present or future
//            start = taskStart; end = task.end();
//        }
        return new long[]{start, end};
    }
}
