package nars.control.proto;

import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.link.Tasklinks;
import nars.task.ITask;
import nars.task.NativeTask;
import org.jetbrains.annotations.Nullable;

public class TaskLinkTask extends NativeTask {

    private final Task task;
    private final Concept concept;
    private final float pri;

//    public TaskLinkTask(Task task, float pri) {
//        this(task, pri, null);
//    }

    public TaskLinkTask(Task task, float pri, @Nullable Concept c) {
        this.task = task;
        this.concept = c;
        this.pri = pri;
    }

    @Override
    public ITask next(NAR n) {
        Concept c = this.concept;
//        if (c == null) {
//            c = task.concept(n, true);
//            if (c == null)
//                return null;
//        }

        Tasklinks.linkTask(task, pri, c, n);
        return null;
    }

    @Override
    public String toString() {
        return "TaskLink(" + task + ",$" + pri + ")";
    }

}
