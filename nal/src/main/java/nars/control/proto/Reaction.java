package nars.control.proto;

import jcog.Util;
import nars.NAR;
import nars.Task;
import nars.task.AbstractTask;
import nars.task.ITask;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/** emits the supplied task or tasks on the eventTask topic */
public class Reaction extends AbstractTask {

    private final Object taskOrTasks;

    public Reaction(Task t) {
        this.taskOrTasks = t;
    }
    protected Reaction(Iterable<Task> tt) {
        this.taskOrTasks = tt;
    }

    @Override
    public ITask next(NAR n) {

        if (taskOrTasks instanceof Iterable) {
            ((Iterable)taskOrTasks).forEach(t -> emit((Task)t, n));
        } else {
            emit((Task)taskOrTasks, n);
        }
        return null;
    }

    protected final void emit(Task x, NAR n) {
        n.eventTask.emit(x, x.punc());
    }

    @Nullable
    public static ITask emit(Collection<Task> c) {
        switch (c.size()) {
            case 0: return null;
            case 1: return new Reaction(Util.only(c));
            default: return new Reaction(c);
        }
    }
}
