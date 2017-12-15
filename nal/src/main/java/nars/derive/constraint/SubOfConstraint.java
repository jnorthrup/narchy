package nars.derive.constraint;

import nars.term.Term;
import nars.term.subst.Unify;

public class SubOfConstraint extends MatchConstraint {
    private final Term y;
    private final boolean reverse;

    /** if the terms can be equal to be valid */
    private final boolean canEqual;
    private final boolean recursive;

    public SubOfConstraint(Term x, Term y, boolean reverse, boolean canEqual, boolean recursive) {
        super(x,
                (reverse ? "compoundOf" :"subOf") +
                (recursive ? "Rec" : "") +
                (canEqual ? "orEq" : ""), y);
        this.y = y;
        this.reverse = reverse;
        this.recursive = recursive;
        this.canEqual = canEqual;
    }

    @Override
    public float cost() {
        return 0.4f;
    }

    @Override
    public boolean invalid(Term xx, Unify f) {
        Term yy = f.xy(y);
        if (yy == null)
            return false; //unknown yet

        if (canEqual && xx.equals(yy))
            return false;

        return !(
                recursive ?
                    (reverse ? xx.containsRecursively(yy) : yy.containsRecursively(xx)) :
                    (reverse ? xx.contains(yy) : yy.contains(xx))
        );
    }
}
