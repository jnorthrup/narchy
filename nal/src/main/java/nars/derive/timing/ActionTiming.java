package nars.derive.timing;


import jcog.math.FloatRange;
import nars.NAR;
import nars.Task;
import nars.term.Term;
import nars.time.Tense;

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


        long start, end;
        long now = nar.time();
        int dur = nar.dur();
        long then = now + Math.round(((nar.random().nextFloat()-0.5f)*2f * horizonDurs.floatValue()) * dur);
//        if (task.endsBefore(now)) {
//        if (!task.isEternal()) {
        start = Tense.dither(then - dur, nar); // + Math.round( ( nar.random().nextDouble() * horizonDurs.floatValue() ) * nar.dur() );

        end = Tense.dither(then + dur, nar);
                    //Math.round(widthDurs.doubleValue()*nar.dur());

//        } else {
//            start = task.start();
//            end = task.end();
//        }
        return new long[] { start, end };
    }
}
