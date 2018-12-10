package nars.op;

import nars.NAR;
import nars.Task;
import nars.bag.leak.TaskLeakTransform;
import nars.task.UnevaluatedTask;
import nars.term.Term;
import nars.term.atom.Bool;
import org.jetbrains.annotations.Nullable;

public abstract class Introduction extends TaskLeakTransform {

    protected Introduction(NAR nar) {
        super(nar);
    }

    protected Introduction(NAR nar, int capacity) {
        super(capacity, nar);
    }

    /** returns the new, transformed term or null if the task should not be cloned */
    @Nullable
    protected abstract Term newTerm(Task x);

    @Override
    protected float leak(Task xx) {

        Term y = newTerm(xx);

        if (y != null && !(y instanceof Bool) && (y.volume() < volMax)) {
            Term x = xx.term();
            if (!y.equals(x) && y.op().conceptualizable) {

                Task yy = Task.clone(xx, y, xx.truth(), xx.punc(), (c, t) -> new UnevaluatedTask(c, xx, t));

                if (yy != null) {
                    //discount pri by increase in term complexity

                    float xc = x.voluplexity(), yc = y.voluplexity();
                    float priSharePct =
                            1f - (yc / (xc + yc));
                    yy.pri(0);
                    yy.take(xx, priSharePct, false, true);

                    input(yy);
                    return 1;
                }
            }
        }

        return 0;
    }
}
