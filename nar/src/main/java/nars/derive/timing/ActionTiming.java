package nars.derive.timing;


import jcog.math.FloatRange;
import nars.NAR;
import nars.Task;
import nars.attention.What;
import nars.derive.TimeFocus;
import nars.term.Term;
import nars.time.When;
import nars.time.event.WhenTimeIs;

import java.util.Random;

public class ActionTiming implements TimeFocus {

    //TODO parametr for shifting focus balance toward past, present or future

    public final FloatRange focusDurs = new FloatRange(2, 0, 32);

    public final FloatRange horizonDurs = new FloatRange(4, 0, 32);
    //public final FloatRange widthDurs = new FloatRange(2, 0, 8);

    float past = 1;
    float future = 2;

    public ActionTiming() {

    }

    @Override
    public When<NAR> task(What what) {
        float dur = what.dur() * focusDurs.floatValue();
        NAR nar = what.nar;
        return WhenTimeIs.now(nar, 0, nar.time(), dur*past, dur*future, nar.dtDither());
    }

    @Override
    public long[] premise(What what, Task task, Term term) {

        int dur = Math.round(what.dur() * focusDurs.floatValue());

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
