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
    public final FloatRange horizonDurs = new FloatRange(2, 0, 32);

//    /** tense focus (past <> future balance), 0=present, -1 = full past, +1 full future */
//    public final FloatRange balance = new FloatRange(0f, -1, +1);


//    float past = 1;
//    float future = 1;
//    @Override
//    public When<NAR> task(What what) {
//        NAR nar = what.nar;
//        float dur = (float) ((what.dur() * horizonDurs.floatValue()/2) * Math.pow( 2, nar.random().nextFloat() * 4));
//        return WhenTimeIs.now(nar, 0, nar.time(), dur*past, dur*future, nar.dtDither());
//    }

    @Override
    public long[] premise(What what, Task task, Term beliefTerm) {

        var dur = what.dur();


        var focusRadius = dur * focusDurs.floatValue() / 2;
        var range = horizonDurs.floatValue() * dur;

        var now = what.time();

        var rng = what.random();

        //gaussian
        var then = (now + range * rng.nextGaussian());
        //uniform
        //double then = (now + range * (-.5f + rng.nextFloat())); //uniform

        return new long[] { round(then - focusRadius), round(then + focusRadius)};
    }

}
