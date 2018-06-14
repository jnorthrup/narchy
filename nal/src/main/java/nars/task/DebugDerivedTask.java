package nars.task;

import nars.Task;
import nars.derive.Derivation;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

public class DebugDerivedTask extends DerivedTask {

    Object log = null;

    private final Task parentBelief;
    private final Task parentTask;

    public DebugDerivedTask(Term tc, byte punct, @Nullable Truth truth, long start, long end, Derivation d) {
        super(tc, punct, truth, start, end, d);
        this.parentTask = d._task;
        this.parentBelief = !d.concSingle ? d._belief : null;
    }

    @Override
    public Task log(Object entry) {
        this.log = entry;
        return this;
    }

    @Override
    public @Nullable Object lastLogged() {
        return log;
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
