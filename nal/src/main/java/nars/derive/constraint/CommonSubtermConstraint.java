package nars.derive.constraint;

import nars.Op;
import nars.subterm.Subterms;
import nars.term.Term;

/**
 * invalid if any of the following:
 *      terms are equal
 *      a term contains the other
 *      the terms have no non-variable subterms in common
 */
public final class CommonSubtermConstraint extends MatchConstraint.RelationConstraint {

    public CommonSubtermConstraint(Term target, Term x) {
        super(target, x, "common_subterms");
    }

    @Override
    public float cost() {
        return 2;
    }

    @Override
    public boolean invalid(Term x, Term y) {

        int vx = x.volume();
        int vy = y.volume();
        if (vx == vy) {
            if (x.containsRecursively(y, true, Op.recursiveCommonalityDelimeterWeak) || y.containsRecursively(x, true, Op.recursiveCommonalityDelimeterWeak))
                return true;
        } else {
            if (vy > vx) {
                //swap so that v is larger
                Term c = x;
                x = y;
                y = c;
            }
            if (x.containsRecursively(y, true, Op.recursiveCommonalityDelimeterWeak))
                return true;
        }

        return !Subterms.hasCommonSubtermsRecursive(x, y, true);
    }

}
