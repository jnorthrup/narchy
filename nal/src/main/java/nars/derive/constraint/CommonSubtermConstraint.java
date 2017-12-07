package nars.derive.constraint;

import nars.term.Term;
import nars.term.container.Subterms;
import org.jetbrains.annotations.NotNull;

/**
 * invalid if any of the following:
 *      terms are equal
 *      a term contains the other
 *      the terms have no non-variable subterms in common
 */
public final class CommonSubtermConstraint extends CommonalityConstraint {

    public CommonSubtermConstraint(Term target, @NotNull Term x) {
        super("neqAndCom", target, x);
    }

    @Override
    public float cost() {
        return 2;
    }

    @Override
    protected boolean invalid(Term x, Term y) {

        return x.equals(y) || x.containsRecursively(y) || y.containsRecursively(x) || !Subterms.hasCommonSubtermsRecursive(x, y, true);
    }

}
