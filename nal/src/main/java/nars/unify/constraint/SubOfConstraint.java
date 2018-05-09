package nars.unify.constraint;

import nars.subterm.util.Contains;
import nars.term.Term;

public class SubOfConstraint extends RelationConstraint {
    private final boolean forward;

    /** if the terms can be equal to be valid */
    private final boolean canEqual;
    private final Contains containment;


    /** containment of the term positively (normal), negatively (negated), or either (must test both) */
    private final int polarityCompare;

    public SubOfConstraint(Term x, Term y, boolean reverse, boolean canEqual, Contains contains) {
        this(x, y, reverse,canEqual, contains, +1);
    }

    public SubOfConstraint(Term x, Term y, /* HACK change to forward semantics */ boolean reverse, boolean canEqual, Contains contains, int polarityCompare) {
        super(x, y,
            contains.name() +
            (!reverse ? "->" : "<-") +
            (canEqual ? "|=" : "") +
            (polarityCompare!=0 ? (polarityCompare==-1 ? "--" : "++") : "+-"));

        //TODO compile these as separate subclasses and assign each a different cost
        this.forward = !reverse;
        this.containment = contains;
        this.canEqual = canEqual;
        this.polarityCompare = polarityCompare;
    }

    @Override
    public float cost() {
        return containment.cost();
    }

    public final boolean invalid(Term xx, Term yy) {
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

        //if posOrNeg, discover if the negative case is valid.  positive (normal) case is tested after
        return !containment.test( container, contentP) && (polarityCompare!=0 || !containment.test( container,  contentP.neg()));
    }
}
