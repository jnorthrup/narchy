package nars.task;

import nars.Task;
import nars.derive.Derivation;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

public class DebugDerivedTask extends DerivedTask {

    private final Task parentTask;
    private final @Nullable Task parentBelief;

    public DebugDerivedTask(Term tc, byte punct, @Nullable Truth truth, long start, long end, Derivation d) {
        super(tc, punct, truth, d.time, start, end, d.evidence());
        this.parentTask = d._task;
        this.parentBelief = !d.single ? d._belief : null;
    }

    @Override
    public final Task parentTask() {
        return parentTask;
    }

    @Override
    public final @Nullable Task parentBelief() {
        return parentBelief;
    }

}
