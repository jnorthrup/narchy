package nars.derive.premise;

import nars.Task;
import nars.term.Term;

public class MatchedPremise extends Premise {

    public final Task belief;

    public MatchedPremise(Task task, Task belief, Term beliefTerm) {
        super(task, beliefTerm);
        this.belief = belief;
        assert(!task.equals(belief));
    }

    @Override
    public final Task belief() {
        return belief;
    }

    @Override
    public String toString() {
        return "Premise(" + task +
                " * " + beliefTerm +
                " : " + belief +
                ')';
    }
}
