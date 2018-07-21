package nars.control.proto;

import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.task.ITask;

public class TaskLinkTaskAndEmit extends TaskLinkTask {

    public TaskLinkTaskAndEmit(Task result) {
        super(result);
    }

    public TaskLinkTaskAndEmit(Task result, float pri, Concept concept) {
        super(result, pri, concept);
    }

    @Override
    public ITask next(NAR n) {
         super.next(n);
         n.eventTask.emit(task);
         return null;
    }
}
