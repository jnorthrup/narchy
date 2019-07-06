package nars.derive.premise;

import nars.Task;
import nars.derive.model.Derivation;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import static nars.term.atom.Bool.Null;

public class MatchedPremise extends Premise {

    public final Task belief;

    public MatchedPremise(Task task, @Nullable Task belief, Term beliefTerm) {
        super(task, beliefTerm);
        this.belief = belief;
    }

    public boolean reset(Derivation d) {
        d.reset(this.task, belief, beliefTerm);
        return d.taskTerm != Null;
    }
}
