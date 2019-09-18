package nars.op;

import jcog.math.FloatRange;
import nars.NAR;
import nars.Task;
import nars.attention.What;
import nars.derive.Derivation;
import nars.task.TemporalTask;
import nars.term.Term;
import nars.term.atom.Bool;
import org.jetbrains.annotations.Nullable;

public abstract class Introduction extends TaskLeakTransform {

    public final FloatRange priFactor = new FloatRange(0.5f, 0, 1);

    protected Introduction(NAR nar) {
        super(nar);
    }


    /** returns the new, transformed target or null if the task should not be cloned */
    @Nullable
    protected abstract Term apply(Term x, What what);

    @Override
    protected final void accept(Task t, Derivation d) {

        Term x = t.term();
        if (!filter(x))
            return;

        What w = d.what;

        Term y1 = apply(x, w);

        if(y1 !=null && !x.equals(y1) && !(y1 instanceof Bool)) {
            Term yu = y1.unneg();
            if (/*yu.volume() <= volMax &&*/ yu.op().conceptualizable) {
                if (!yu.equals(x)) {

                    taskify(t, x, y1, w);
                }
            }
        }

    }

    private void taskify(Task xt, Term x, Term y1, What w) {
        Task yy = Task.clone(xt, y1, xt.truth(), xt.punc(),
                (c, t) -> {
                    if (c.equals(x)) //HACK normalization might cause this to become true although it is seemingly checked before Task.clone()
                        return null;

                    long now = w.time();
                    return tasksUnevaluated() ?
                            new TemporalTask.Unevaluated(c, xt, t, now) :
                            new TemporalTask(c, xt, t, now);
                });

        if (yy != null) {
            yy.pri(0); //HACK
            Task.fund(yy, xt, priFactor.floatValue(), true);
            in.accept(yy, w);
            return;
        }
    }

    /** return true to produce Unevaluated tasks, which can prevent circular processing */
    protected boolean tasksUnevaluated() {
        return true;
    }

}
