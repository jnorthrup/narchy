package nars.derive.timing;


import jcog.math.FloatRange;
import nars.NAR;
import nars.Task;
import nars.term.Term;

import java.util.Random;
import java.util.function.BiFunction;

public class ActionTiming implements BiFunction<Task, Term, long[]> {

    private final NAR nar;
    public final FloatRange horizonDurs = new FloatRange(4, 0, 32);
    //public final FloatRange widthDurs = new FloatRange(2, 0, 8);

    public ActionTiming(NAR n) {
        this.nar = n;
    }

    @Override
    public long[] apply(Task task, Term term) {

        long now = nar.time();
        int dur = nar.dur();
        Random rng = nar.random();

        //long then = Math.round(now + rng.nextGaussian() * horizonDurs.floatValue() * dur);
        long then = now + Math.round(((rng.nextFloat()-0.5f)*2f * horizonDurs.floatValue()) * dur);

        long start, end;
        start = (then - dur);
        //start = Tense.dither(start, nar);
        end = (then + dur);
        //end = Tense.dither(end, nar);

        return new long[] { start, end };
    }
}
