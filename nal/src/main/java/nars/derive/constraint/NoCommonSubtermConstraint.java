package nars.derive.constraint;

import nars.Op;
import nars.term.Term;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/**
 * containment test of x to y's subterms and y to x's subterms
 */
public final class NoCommonSubtermConstraint extends CommonalityConstraint {

    public final boolean recurse;

    /**
     * @param recurse true: recursive
     *                false: only cross-compares the first layer of subterms.
     */
    public NoCommonSubtermConstraint(@NotNull Term target, @NotNull Term x, boolean recurse) {
        super(recurse ? "neqRCom" : "neqCom",
                target, x);
        this.recurse = recurse;
    }


    @Override
    public float cost() {
        return recurse ? 1.5f : 1f;
    }

    @Override
    protected boolean invalidCommonality(Term x, Term y) {
        return isSubtermOfTheOther(x, y, recurse, true);
    }


    final static Predicate<Term> limit =
            Op.recursiveCommonalityDelimeterWeak;


    static boolean isSubtermOfTheOther(Term a, Term b, boolean recurse, boolean excludeVariables) {

        if ((excludeVariables) && (a instanceof Variable || b instanceof Variable))
            return false;

        int av = a.volume();
        int bv = b.volume();
        if (av == bv) {
            return recurse ?
                    a.containsRecursively(b, true, limit) ||
                            b.containsRecursively(a, true, limit) :

                    a.containsRoot(b) || b.containsRoot(a);
        } else {
            //if one volume is smaller than the other we only need to test containment unidirectionally

            if (av < bv) {
                //swap
                Term c = a;
                a = b;
                b = c;
            }

            return recurse ?
                    a.containsRecursively(b, true, limit) :
                    a.containsRoot(b);
        }
    }
    //commonSubtermsRecurse((Compound) B, C, true, new HashSet())
    //commonSubterms((Compound) B, C, true, scratch.get())


}
