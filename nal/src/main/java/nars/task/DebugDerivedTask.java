package nars.task;

import nars.Task;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DebugDerivedTask extends DerivedTask {


    private final Task parentBelief;
    private final Task parentTask;

    public DebugDerivedTask(Term tc, byte punct, @Nullable Truth truth, long now, long start, long end, long[] evi, Task parentTask, Task parentBelief) {
        super(tc, punct, truth, now, start, end, evi);
        this.parentTask = parentTask;
        this.parentBelief = parentBelief;
    }

    @Override
    @Nullable
    public final Task getParentTask() {
        return parentTask;
    }

    @Override
    @Nullable
    public final Task getParentBelief() {
        return parentBelief;
    }

}
