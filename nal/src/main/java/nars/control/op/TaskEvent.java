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
    public ITask next(NAR n) {
        n.eventTask.emit(task, task.punc());
        return null;
    }

}
