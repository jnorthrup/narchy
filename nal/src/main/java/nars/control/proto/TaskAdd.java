package nars.control.proto;

import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.task.ITask;
import nars.task.NativeTask;
import org.jetbrains.annotations.Nullable;

/**
 * conceptualize and insert/merge a task to belief table
 */
public class TaskAdd extends NativeTask {

    private final Task task;

    public TaskAdd(Task task) {
        this.task = task;
    }

    @Override
    public String toString() {
        return "TaskAdd(" + task + ")";
    }

    @Override
    public ITask run(NAR n) {
        @Nullable Concept c = task.concept(n, true);
        if (!(c instanceof TaskConcept)) {
            if (task.isBeliefOrGoal() || Param.DEBUG_EXTRA)
                throw new RuntimeException(task + " does not resolve a TaskConcept");
            else
                return null;
        }

        ((TaskConcept) c).add(task, n);

        return null;
    }
}
