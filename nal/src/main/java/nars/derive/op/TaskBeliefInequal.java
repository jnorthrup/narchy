package nars.derive.op;

import nars.$;
import nars.control.ProtoDerivation;
import nars.term.pred.AbstractPred;

public class TaskBeliefInequal extends AbstractPred<ProtoDerivation> {

    public static TaskBeliefInequal the = new TaskBeliefInequal();

    private TaskBeliefInequal() {
        super($.the("TaskBeliefInequal"));
    }

    @Override
    public boolean test(ProtoDerivation d) {
        return !d.taskTerm.equalsRoot(d.beliefTerm);
    }

    @Override
    public float cost() {
        return 0.1f;
    }
}
