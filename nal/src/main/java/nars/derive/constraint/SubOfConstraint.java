package nars.derive.constraint;

import nars.subterm.util.Contains;
import nars.term.Term;
import nars.term.subst.Unify;

public class SubOfConstraint extends MatchConstraint {
    private final Term y;
    private final boolean forward;

    /** if the terms can be equal to be valid */
    private final boolean canEqual;
    private final Contains containment;

//    private final static int POSITIVE = 1;
//    private final static int NEGATIVE = -1;
//    private final static int ANY = 0;

    /** containment of the term positively (normal), negatively (negated), or either (must test both) */
    private final int polarityCompare;
    private final float cost;


    public SubOfConstraint(Term x, Term y, boolean reverse, boolean canEqual, Contains contains) {
        this(x, y, reverse,canEqual, contains, +1);
    }

    public SubOfConstraint(Term x, Term y, /* HACK change to forward semantics */ boolean reverse, boolean canEqual, Contains contains, int polarityCompare) {
        super(x,
            contains.name() +
            ((!reverse) ? "->" : "<-") +
            (canEqual ? "|=" : "") +
            (polarityCompare!=1 ? (polarityCompare==-1 ? "--" : "+-") : ""),
                y);
        this.y = y;

        //TODO compile these as separate subclasses and assign each a different cost
        this.forward = !reverse;
        this.containment = contains;
        this.canEqual = canEqual;
        this.polarityCompare = polarityCompare;
        this.cost = containment.cost();
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

        /** x polarized */
        Term contentP = (forward ? yy : xx).negIf(polarityCompare < 0);
        Term container = forward ? xx : yy;

        if (!canEqual && (container.impossibleSubTerm(contentP)))
            return true;

        if (canEqual) {
            if (polarityCompare != 0) {
                if (container.equalsRoot(contentP))
                    return false;
            } else {
                if (container.unneg().equalsRoot(contentP.unneg()))
                    return false;
            }
        }

        if (polarityCompare==0) {
            //if posOrNeg, discover if the negative case is valid.  positive (normal) case is tested after
            if (containment.test( container,  contentP.neg()))
                return false;
        }

        return !containment.test( container, contentP);
            //only use xxp when testing for containment, keep the outer raw
    }
}
