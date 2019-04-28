package nars.op;

import jcog.math.FloatRange;
import nars.NAR;
import nars.Task;
import nars.attention.What;
import nars.task.TemporalTask;
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

        if(y!=null && !(y instanceof Bool)) {
            Term yu = y.unneg();
            if (yu.volume() <= volMax && yu.op().conceptualizable) {
                Term x = xx.term();
                if (!yu.equals(x)) {

                    Task yy = Task.clone(xx, y, xx.truth(), xx.punc(),
                            (c, t) -> {
                                if (c.equals(x)) //HACK normalization might cause this to become true although it is seemingly checked before Task.clone()
                                    return null;

                                return tasksUnevaluated() ?
                                        new TemporalTask.Unevaluated(c, xx, t) :
                                        new TemporalTask(c, xx, t);
                            });

                    if (yy != null) {
                        Task.deductComplexification(xx, yy, priFactor.floatValue(), true);

                        in.accept(yy, what);
                        return 1;
                    }
                }
            }
        }

        return 0;
    }

    /** return true to produce Unevaluated tasks, which can prevent circular processing */
    protected boolean tasksUnevaluated() {
        return true;
    }

}
