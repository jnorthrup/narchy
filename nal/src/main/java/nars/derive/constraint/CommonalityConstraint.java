package nars.derive.constraint;

import nars.term.Term;
import nars.term.subst.Unify;

/**
 * note: if the two terms are equal, it is automatically invalid ("neq")
 */
public abstract class CommonalityConstraint extends MatchConstraint {

    private final Term other;

    protected CommonalityConstraint(String func, Term target, Term other) {
        super(target, func, other);
        this.other = other;
    }

    @Override
    public final boolean invalid(Term y, Unify f) {

        Term x = f.xy(other);
        if (x == null) {
            return false; //not invalid until both are present to be compared
        } else if (x.equals(y)) {
            return true;
        } else {
            return invalidCommonality(x, y);
        }
    }


    /**
     * equality will have already been tested prior to calling this
     */
    protected abstract boolean invalidCommonality(Term x, Term y);


}
