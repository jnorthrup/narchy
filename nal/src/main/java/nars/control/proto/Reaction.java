package nars.control.proto;

import nars.NAR;
import nars.Task;
import nars.task.AbstractTask;
import nars.task.ITask;

/** emits the supplied task or tasks on the eventTask topic */
public final class Reaction extends AbstractTask {

    public final Task task;

    public Reaction(Task t) {
        this.task = t;
    }

    @Override
    public ITask next(NAR n) {
        n.eventTask.emit(task, task.punc());
        return null;
    }

}
