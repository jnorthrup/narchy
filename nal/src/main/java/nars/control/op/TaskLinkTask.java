package nars.control.op;

import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.link.TaskLink;
import nars.task.AbstractTask;
import nars.task.ITask;
import nars.term.Termed;
import org.jetbrains.annotations.Nullable;

/** creates a seed tasklink for a processed Task that can subdivide recursively on propagation */
public class TaskLinkTask extends AbstractTask {

    public final Task task;
    @Nullable private final Concept concept;

//    public TaskLinkTask(Task task) {
//        this(task, null);
//    }

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
        Termed cc = concept == null ? task : concept;
        Concept c = //n.activate(cc, pri, true);
                n.conceptualize(cc);
        if (c == null)
            return null;

        //2. tasklink
        TaskLink.link(
                TaskLink.the(c.term(), task, generify(), eternalize(), pri, n),
                n);


        //3. feel
        ((TaskConcept) c).value(task, n);

        //finished
        return null;
    }

    protected boolean generify() {
        return true;
    }

    protected boolean eternalize() {
        return true;
    }


    @Override
    public String toString() {
        return "TaskLink(" + task + ')';
    }

}
