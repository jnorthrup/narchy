package nars.op;

import nars.Task;
import nars.attention.TaskLinkWhat;
import nars.attention.What;
import nars.derive.Derivation;
import nars.derive.action.TaskTransformAction;
import nars.task.TemporalTask;
import nars.term.Term;
import nars.term.atom.IdempotentBool;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

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

        Term x = t.term();
        if (!filter(x))
            return null;

        What w = d.x;

        Term y = apply(x, w);

        return (y != null && !(y instanceof IdempotentBool) && y.unneg().op().conceptualizable && !x.equals(y)) ?
            taskify(t, x, y, w) : null;
    }

    private static Task taskify(Task xt, Term x, Term y1, What w) {
        Task yy = Task.tryTask(y1, xt.punc(), xt.truth(),
                new BiFunction<Term, Truth, TemporalTask>() {
                    @Override
                    public TemporalTask apply(Term c, Truth t) {
                        if (c.equals(x)) //HACK normalization might cause this to become true although it is seemingly checked before Task.clone()
                            return null;

                        long now = w.time();
                        return tasksUnevaluated() ?
                                new TemporalTask.Unevaluated(c, xt, t, now) :
                                new TemporalTask(c, xt, t, now);
                    }
                });

        if (yy != null) {
            yy.pri((float) 0); //HACK
            Task.fund(yy, xt, ((TaskLinkWhat)w).links.grow.floatValue() /* priFactor.floatValue() */, true);
        }
        return yy;
    }

    /** return true to produce Unevaluated tasks, which can prevent circular processing */
    protected static boolean tasksUnevaluated() {
        return true;
    }

}
