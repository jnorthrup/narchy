package nars.control.proto;

import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.link.TaskLink;
import nars.link.Tasklinks;
import nars.task.AbstractTask;
import nars.task.ITask;
import org.jetbrains.annotations.Nullable;

public class TaskLinkTask extends AbstractTask {

    protected final Task task;
    @Nullable private final Concept concept;

    public TaskLinkTask(Task task) {
        this(task, null);
    }

    public TaskLinkTask(Task task, @Nullable Concept c) {
        this.task = task;
        this.concept = c;
    }

    @Override
    public ITask next(NAR n) {

        float pri = task.pri();
        if (pri!=pri)
            return null;

        pri = Math.max(EPSILON, pri);

        Concept c = n.activate(this.concept == null ? task.term() : this.concept, pri);
        if (c == null)
            return null;

        //2. tasklink
        Tasklinks.linkTask(new TaskLink.GeneralTaskLink(task, n, pri), c.tasklinks(), null);

        //3. feel
        ((TaskConcept) c).value(task, n);

        n.emotion.perceive(task);

        //finished
        return null;
    }

    @Override
    public String toString() {
        return "TaskLink(" + task + ')';
    }

}
