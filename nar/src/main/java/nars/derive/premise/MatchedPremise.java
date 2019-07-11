package nars.derive.premise;

import nars.Task;
import nars.derive.model.Derivation;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

public class MatchedPremise extends Premise {

    public final Task belief;

    public MatchedPremise(Task task, @Nullable Task belief, Term beliefTerm) {
        super(task, beliefTerm);
        this.belief = belief;
    }

    public void apply(Derivation d) {
        d.reset(this.task, belief, beliefTerm);
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
