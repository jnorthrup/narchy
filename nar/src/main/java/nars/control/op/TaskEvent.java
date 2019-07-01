package nars.control.op;

import nars.NAR;
import nars.Task;
import nars.attention.What;
import nars.task.AbstractTask;

/** emits the supplied task or tasks on the eventTask topic */
public final class TaskEvent extends AbstractTask {

    public final Task task;

    TaskEvent(Task t) {
        this.task = t;
    }

    @Override
    public Task next(Object n) {
        emit(task, ((What)n).nar);
        return null;
    }

    public static void emit(Task task, NAR n) {
        n.eventTask.emit(task, task.punc());
    }

}
