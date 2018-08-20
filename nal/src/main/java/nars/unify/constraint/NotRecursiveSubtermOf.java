package nars.unify.constraint;

import nars.Op;
import nars.term.Term;
import nars.term.Variable;

import java.util.function.Predicate;

/**
 * containment test of x to y's subterms and y to x's subterms
 */
public final class NotRecursiveSubtermOf extends RelationConstraint {

    public NotRecursiveSubtermOf(Term target, Term x) {
        super(target, x, "neqRCom");
    }

    @Override
    public float cost() {
        return 1.5f;
    }

    @Override
    public boolean invalid(Term x, Term y) {
        return isSubtermOfTheOther(x, y, true, true);
    }

    final static Predicate<Term> limit =
            Op.recursiveCommonalityDelimeterWeak;
    //Op.recursiveCommonalityDelimeterStrong;


    static boolean isSubtermOfTheOther(Term a, Term b, boolean recurse, boolean excludeVariables) {

        if ((excludeVariables) && (a instanceof Variable || b instanceof Variable))
            return false;

        int av = a.volume(), bv = b.volume();
        if (av == bv) {
            return false;
        } else {


            if (av < bv) {

                Term c = a;
                a = b;
                b = c;
            }

            return recurse ?
                    a.containsRecursively(b, false, limit) :
                    a.contains(b);
        }
    }


}
