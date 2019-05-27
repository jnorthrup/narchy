package nars.task;

import nars.Task;
import nars.derive.model.Derivation;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

public class DebugDerivedTask extends DerivedTask {

    private final Task parentTask;
    @Nullable private final Task parentBelief;

    public DebugDerivedTask(Term tc, byte punct, @Nullable Truth truth, long start, long end, Derivation d) {
        super(tc, punct, truth, d.time(), start, end, d.evidence());
        this.parentTask = d._task;
        this.parentBelief = !d.concSingle ? d._belief : null;
    }

    @Override
    public final Task getParentTask() {
        return parentTask;
    }

    @Override
    @Nullable
    public final Task getParentBelief() {
        return parentBelief;
    }

}
