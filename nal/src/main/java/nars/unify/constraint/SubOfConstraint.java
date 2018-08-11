package nars.unify.constraint;

import nars.subterm.util.SubtermCondition;
import nars.term.Term;

public class SubOfConstraint extends RelationConstraint {
    private final boolean forward;

    /**
     * if the terms can be equal to be valid
     */
    private final boolean canEqual;
    private final SubtermCondition containment;


    /**
     * containment of the term positively (normal), negatively (negated), or either (must test both)
     */
    private final int polarityCompare;

    final static float EQUALITY_TEST_COST = 0.25f;

    public SubOfConstraint(Term x, Term y, boolean reverse, boolean canEqual, SubtermCondition contains) {
        this(x, y, reverse, canEqual, contains, +1);
    }

    public SubOfConstraint(Term x, Term y, /* HACK change to forward semantics */ boolean reverse, boolean canEqual, SubtermCondition contains, int polarityCompare) {
        super(x, y,
                contains.name() +
                        (!reverse ? "->" : "<-") +
                        (canEqual ? "|=" : "") +
                        (polarityCompare != 0 ? (polarityCompare == -1 ? "(-)" : "(+)") : "(+|-)"));


        this.forward = !reverse;
        this.containment = contains;
        this.canEqual = canEqual;
        this.polarityCompare = polarityCompare;
    }


    @Override
    public float cost() {
        return containment.cost() + (canEqual ? EQUALITY_TEST_COST : 0);
    }

    public final boolean invalid(Term xx, Term yy) {

        /** x polarized */
        Term contentP = (forward ? yy : xx).negIf(polarityCompare < 0);
        Term container = forward ? xx : yy;


        boolean posAndNeg = polarityCompare==0;
        if (canEqual) {
            if (posAndNeg) {
                if (container.unneg().equals(contentP.unneg()))
                    return false;
            } else {
                if (container.equals(contentP))
                    return false;
            }
        }

        if (posAndNeg) {
            if (container.impossibleSubTerm(contentP) && container.impossibleSubTerm(contentP.neg()))
                return true;
        } else {
            if (container.impossibleSubTerm(contentP))
                return true;
        }

        return !containment.test(container, contentP, posAndNeg);
    }
}
