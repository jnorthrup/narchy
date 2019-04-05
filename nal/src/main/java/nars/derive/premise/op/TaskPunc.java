package nars.derive.premise.op;

import nars.$;
import nars.derive.PreDerivation;
import nars.term.control.AbstractPred;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;

public final class TaskPunc extends AbstractPred<PreDerivation> {
    private final BytePredicate taskPunc;

    public TaskPunc(BytePredicate taskPunc) {
        super($.funcFast(TaskPunc.class.getSimpleName(), $.quote(taskPunc)));
        this.taskPunc = taskPunc;
    }

    @Override
    public float cost() {
        return 0.02f;
    }

    @Override
    public boolean test(PreDerivation preDerivation) {
        return taskPunc.accept(preDerivation.taskPunc);
    }
}
