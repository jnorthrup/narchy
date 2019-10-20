package nars.op;

import nars.Task;
import nars.attention.TaskLinkWhat;
import nars.attention.What;
import nars.derive.Derivation;
import nars.derive.action.TaskTransformAction;
import nars.task.TemporalTask;
import nars.term.Term;
import nars.term.atom.IdempotentBool;
import org.jetbrains.annotations.Nullable;

public abstract class Introduction extends TaskTransformAction {

    //public final FloatRange priFactor = new FloatRange(0.5f, 0, 1);

    protected Introduction() {
        super();
    }


    protected boolean filter(Term t) {
        return true;
    }

    /** returns the new, transformed target or null if the task should not be cloned */
    protected abstract @Nullable Term apply(Term x, What what);

    @Override
    protected final Task transform(Task t, Derivation d) {

        var x = t.term();
        if (!filter(x))
            return null;

        var w = d.x;

        var y = apply(x, w);

        return (y != null && !(y instanceof IdempotentBool) && y.unneg().op().conceptualizable && !x.equals(y)) ?
            taskify(t, x, y, w) : null;
    }

    private static Task taskify(Task xt, Term x, Term y1, What w) {
        Task yy = Task.tryTask(y1, xt.punc(), xt.truth(),
                (c, t) -> {
                    if (c.equals(x)) //HACK normalization might cause this to become true although it is seemingly checked before Task.clone()
                        return null;

                    var now = w.time();
                    return tasksUnevaluated() ?
                            new TemporalTask.Unevaluated(c, xt, t, now) :
                            new TemporalTask(c, xt, t, now);
                });

        if (yy != null) {
            yy.pri(0); //HACK
            Task.fund(yy, xt, ((TaskLinkWhat)w).links.grow.floatValue() /* priFactor.floatValue() */, true);
        }
        return yy;
    }

    /** return true to produce Unevaluated tasks, which can prevent circular processing */
    protected static boolean tasksUnevaluated() {
        return true;
    }

}
