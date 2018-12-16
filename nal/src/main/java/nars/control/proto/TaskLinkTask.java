package nars.control.proto;

import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.link.TaskLink;
import nars.task.AbstractTask;
import nars.task.ITask;
import nars.task.Tasklike;
import org.jetbrains.annotations.Nullable;

/** creates a seed tasklink for a processed Task that can subdivide recursively on propagation */
public class TaskLinkTask extends AbstractTask {

    public final Task task;
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

        //full task pri to concept
        Concept c = n.activate(concept == null ? task : concept, pri, true);
        if (c == null)
            return null;

        //2. tasklink
        TaskLink.link(
                tasklink(pri, n),
                //TaskLink.tasklink(task, generify(), eternalize(),pri * n.taskLinkActivation.floatValue(), n),
                c);


        //3. feel
        ((TaskConcept) c).value(task, n);

        n.emotion.perceive(task);

        //finished
        return null;
    }

    protected TaskLink.GeneralTaskLink tasklink(float pri, NAR n) {
        return new TaskLink.GeneralTaskLink(Tasklike.seed(task, generify(), eternalize(), n), pri);
    }

    protected boolean generify() {
        return true;
    }

    protected boolean eternalize() {
        return false;
    }


    @Override
    public String toString() {
        return "TaskLink(" + task + ')';
    }

}
