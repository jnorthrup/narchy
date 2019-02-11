package nars.derive.premise;

import nars.derive.Derivation;
import nars.term.Term;
import nars.term.util.cache.Intermed;

public class PremiseKey extends Intermed {

    public static PremiseKey get(Derivation d) {
        return new PremiseKey(d.taskTerm, d.beliefTerm);
    }

    protected PremiseKey(Term taskTerm, Term beliefTerm) {
        super();

        write(taskTerm);
        write(beliefTerm);

        commit();
    }

}
