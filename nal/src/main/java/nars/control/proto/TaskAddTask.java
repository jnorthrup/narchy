package nars.control.proto;

import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.task.ITask;
import nars.task.NativeTask;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

/**
 * conceptualize and insert/merge a task to belief table
 */
public class TaskAddTask extends NativeTask {

    private final Task task;

    public TaskAddTask(Task task) {
        this.task = task;
    }

    @Override
    public String toString() {
        return "TaskAdd(" + task + ")";
    }

    @Override
    public final ITask next(NAR n) {


        //verify dithering
        if (Param.DEBUG) {
            Truth t = task.truth();
            if (t != null)
                t.ensureDithered(n);
        }


        /* the tasks pri may change after starting insertion, so cache here */
        float pri = task.priElseZero();


        n.emotion.onInput(task, n);

        @Nullable Concept c = task.concept(n, true);
        if (c == null) {
            return null; 
        } else if (!(c instanceof TaskConcept)) {
            if (task.isBeliefOrGoal() || Param.DEBUG_EXTRA) {
                task.delete();
                throw new RuntimeException(task + " does not resolve a TaskConcept");
            } else
                return null;
        }



        if (add(n, (TaskConcept) c)) {
            n.activate(c, pri);
            return new TaskLinkTask(task, pri, c);
        } else {
            return null;
        }
    }

    protected boolean add(NAR n, TaskConcept c) {
        return c.add(task, n);
    }
}
