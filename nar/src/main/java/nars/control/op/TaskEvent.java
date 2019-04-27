package nars.control.op;

import nars.NAR;
import nars.Task;
import nars.task.AbstractTask;
import nars.task.ITask;

/** emits the supplied task or tasks on the eventTask topic */
public final class TaskEvent extends AbstractTask {

    public final Task task;

    public TaskEvent(Task t) {
        this.task = t;
    }

    @Override
    public ITask next(Object n) {
        emit(task, (NAR)n);
        return null;
    }

    public static void emit(Task task, NAR n) {
        n.eventTask.emit(task);
    }

}
