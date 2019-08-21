package nars.derive.time;


import jcog.math.FloatRange;
import nars.Task;
import nars.attention.What;
import nars.derive.util.TimeFocus;
import nars.term.Term;

import java.util.Random;

import static java.lang.Math.round;

public class ActionTiming implements TimeFocus {

    /** TODO mutable histogram model for temporal focus duration  */
    public final FloatRange focusDurs = new FloatRange(1, 0, 32);

    /** TODO mutable histogram model for temporal focus position  */
    public final FloatRange horizonDurs = new FloatRange(4, 0, 32);

    public ActionTiming() {

    }

//    float past = 1;
//    float future = 2;
//    @Override
//    public When<NAR> task(What what) {
//        float dur = what.dur() * horizonDurs.floatValue()/2;
//        NAR nar = what.nar;
//        return WhenTimeIs.now(nar, 0, nar.time(), dur*past, dur*future, nar.dtDither());
//    }

    @Override
    public long[] premise(What what, Task task, Term term) {

        float dur = what.dur() * focusDurs.floatValue();

        long now = what.time();
        long tStart = task.start();

        long target = Math.max(now, tStart);
        Random rng = what.random();

        //gaussian
        long then = round(target + rng.nextGaussian() * horizonDurs.floatValue() * dur);
        //uniform
        //long then = Math.round(now + (-.5f + rng.nextFloat()) * 2 * horizonDurs.floatValue() * dur); //uniform

        return new long[] { round(then - dur / 2), round(then + dur / 2)};
    }
}
