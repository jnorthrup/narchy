package nars.task.proxy;

import nars.Task;
import nars.task.ProxyTask;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

/** reported truth value is inverted */
public final class SpecialNegatedTermTask extends ProxyTask {

    public SpecialNegatedTermTask(Task task) {
        super(task);
        assert(!(task instanceof SpecialNegatedTermTask) && task.isBeliefOrGoal());
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
