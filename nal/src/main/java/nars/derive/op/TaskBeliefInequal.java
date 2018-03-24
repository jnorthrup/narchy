package nars.derive.op;

import nars.$;
import nars.derive.PreDerivation;
import nars.term.pred.AbstractPred;

public class TaskBeliefInequal extends AbstractPred<PreDerivation> {

    public static TaskBeliefInequal the = new TaskBeliefInequal();

    private TaskBeliefInequal() {
        super($.the("TaskBeliefInequal"));
    }

    @Override
    public boolean test(PreDerivation d) {
        return !d.taskTerm.equalsRoot(d.beliefTerm);
    }

    @Override
    public float cost() {
        return 0.1f;
    }
}
