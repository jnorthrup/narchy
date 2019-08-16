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
    protected abstract Term newTerm(Term x);

    @Override
    protected float leak(Task xx, What what) {

        Term x = xx.term();
        Term y = newTerm(x);

        if(y!=null && !x.equals(y) && !(y instanceof Bool)) {
            Term yu = y.unneg();
            if (yu.volume() <= volMax && yu.op().conceptualizable) {
                if (!yu.equals(x)) {

                    Task yy = Task.clone(xx, y, xx.truth(), xx.punc(),
                            (c, t) -> {
                                if (c.equals(x)) //HACK normalization might cause this to become true although it is seemingly checked before Task.clone()
                                    return null;

                                long now = what.time();
                                return tasksUnevaluated() ?
                                        new TemporalTask.Unevaluated(c, xx, t, now) :
                                        new TemporalTask(c, xx, t, now);
                            });

                    if (yy != null) {
                        yy.pri(0); //HACK
                        Task.fund(yy, xx, priFactor.floatValue(), true);
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
