package nars.derive.constraint;

import nars.term.Term;
import nars.term.subst.Unify;

/** X in Y:      X is recursive subterm of Y
 *  X inNeg Y: --X is recursive subterm of Y
 */
public class SubOfConstraint extends MatchConstraint {
    private final Term y;
    private final boolean reverse;

    /** if the terms can be equal to be valid */
    private final boolean canEqual;
    private final boolean recursive;
    private final boolean negatedAsSubterm;
    private float cost;

    public SubOfConstraint(Term x, Term y, boolean reverse, boolean canEqual, boolean recursive) {
        this(x, y, reverse,canEqual, recursive, false);
    }

    public SubOfConstraint(Term x, Term y, boolean reverse, boolean canEqual, boolean recursive, boolean negatedAsSubterm) {
        super(x,
                (reverse ? "compoundOf" :"subOf") +
                (recursive ? "Rec" : "") +
                (canEqual ? "orEq" : ""), y);
        this.y = y;

        //TODO compile these as separate subclasses and assign each a different cost
        this.reverse = reverse;
        this.recursive = recursive;
        this.canEqual = canEqual;
        this.negatedAsSubterm = negatedAsSubterm;
        this.cost = recursive ? 0.8f : 0.4f;
    }

    @Override
    public float cost() {
        return cost;
    }

    @Override
    public boolean invalid(Term xx, Unify f) {
        Term yy = f.xy(y);
        if (yy == null)
            return false; //unknown yet

        if (negatedAsSubterm)
            xx = xx.neg();

        if (canEqual && xx.equalsRoot(yy))
            return false;

        return !(
                recursive ?
                    (reverse ? xx.containsRecursively(yy, true, (x)->true) : yy.containsRecursively(xx, true, (x)->true)) :
                    (reverse ? xx.containsRoot(yy) : yy.containsRoot(xx))
        );
    }
}
