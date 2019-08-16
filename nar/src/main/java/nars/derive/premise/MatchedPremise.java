package nars.derive.premise;

import nars.Task;
import nars.derive.Derivation;
import nars.term.Term;

public class MatchedPremise extends Premise {

    public final Task belief;

    public MatchedPremise(Task task, Task belief, Term beliefTerm) {
        super(task, beliefTerm);
        this.belief = belief;
    }

    @Override public void apply(Derivation d) {
        d.reset(this.task, belief, beliefTerm);

//        if (!belief.equals(task))
//            emit(belief, d);
    }


    @Override
    public String toString() {
        return "Premise(" +
                task +
                " * " + beliefTerm +
                " : " + belief +
                ')';

    }
}
