package nars.op;

import jcog.math.FloatRange;
import nars.NAR;
import nars.Task;
import nars.attention.What;
import nars.task.UnevaluatedTask;
import nars.term.Term;
import nars.term.atom.Bool;
import org.jetbrains.annotations.Nullable;

public abstract class Introduction extends TaskLeakTransform {

    public final FloatRange priFactor = new FloatRange(0.5f, 0, 1);

    protected Introduction(NAR nar) {
        super(nar);
    }

    protected Introduction(NAR nar, int capacity) {
        super(capacity, nar);
    }

    /** returns the new, transformed target or null if the task should not be cloned */
    @Nullable
    protected abstract Term newTerm(Task x);

    @Override
    protected float leak(Task xx, What what) {

        Term y = newTerm(xx);

        if (y != null && !(y instanceof Bool) && (y.volume() <= volMax)) {
            Term x = xx.term();
            if (!y.equals(x) && y.op().conceptualizable) {

                Task yy = Task.clone(xx, y, xx.truth(), xx.punc(), (c, t) -> new UnevaluatedTask(c, xx, t));

                if (yy != null) {
                    Task.deductComplexification(xx, yy, priFactor.floatValue(), true);

                    in.accept(yy, what);
                    return 1;
                }
            }
        }

        return 0;
    }

}
