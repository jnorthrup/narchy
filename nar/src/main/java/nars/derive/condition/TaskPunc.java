package nars.derive.condition;

import nars.$;
import nars.derive.model.PreDerivation;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;

public final class TaskPunc extends AbstractPred<PreDerivation> {
    private final BytePredicate taskPunc;

    private static final Atom TASKPUNC = (Atom) Atomic.the(TaskPunc.class.getSimpleName());

    public TaskPunc(BytePredicate taskPunc) {
        super($.func(TASKPUNC, $.quote(taskPunc)));
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
