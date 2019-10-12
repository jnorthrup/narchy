package nars.task.proxy;

import nars.Task;
import nars.task.ProxyTask;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

/** reported truth value is inverted */
public final class SpecialNegatedTask extends ProxyTask {

    public SpecialNegatedTask(Task task) {
        super(task);
        assert(!(task instanceof SpecialNegatedTask) && task.isBeliefOrGoal());
    }

    @Override
    public Term term() {
        return super.term().neg();
    }

    @Override
    public @Nullable Truth truth() {
        return super.truth().neg();
    }

}
