package nars.term.constraint;

import nars.term.Term;
import nars.term.transform.FindSubst;
import org.jetbrains.annotations.NotNull;


public final class NotEqualsConstraint implements MatchConstraint {

    private final Term b;

    public NotEqualsConstraint(Term b) {
        this.b = b;
    }

    @Override
    public boolean invalid(Term x, @NotNull Term y, @NotNull FindSubst f) {
        Term canNotEqual = f.getXY(b);
        if (canNotEqual != null) {
            return y.equals(canNotEqual);
        }
        return false;
    }

    @NotNull
    @Override
    public String toString() {
        return "notEqual:" + b;
    }
}
