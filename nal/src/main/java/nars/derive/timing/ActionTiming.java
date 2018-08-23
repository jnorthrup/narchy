package nars.derive.timing;


import jcog.math.FloatRange;
import nars.NAR;
import nars.Task;
import nars.term.Term;

import java.util.function.BiFunction;

public class ActionTiming implements BiFunction<Task, Term, long[]> {

    private final NAR nar;
    public final FloatRange horizonDurs = new FloatRange(4, 0, 32);
    public final FloatRange widthDurs = new FloatRange(2, 0, 8);

    public ActionTiming(NAR n) {
        this.nar = n;
    }

    @Override
    public long[] apply(Task task, Term term) {
        long start = nar.time() + Math.round( ( nar.random().nextDouble() * horizonDurs.floatValue() ) * nar.dur() );
        long end = start + Math.round(widthDurs.doubleValue()*nar.dur());
        return new long[] { start, end };
    }
}
