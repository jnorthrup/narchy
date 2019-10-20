package nars.term;

import jcog.Paper;
import jcog.Skill;
import nars.Op;
import nars.term.var.CommonVariable;
import nars.term.var.ellipsis.Ellipsis;
import nars.unify.Unify;
import nars.unify.UnifyFirst;

import static nars.Op.VAR_PATTERN;
import static nars.term.atom.Bool.Null;

/**
 * similar to a plain atom, but applies altered operating semantics according to the specific
 * varible type, as well as serving as something like the "marker interfaces" of Atomic, Compound, ..
 * <p>
 * implemented by both raw variable terms and variable concepts
 **/
public interface Variable extends /* Atomic - but all implementations are subclasses of Atomic through other impl */ Term, UnifyFirst {

//    private static boolean neggable(Term t) {
//        return !(t instanceof Ellipsislike) && (t.op() != FRAG);
//    }

//    private static boolean unifyConst(Unify u, Term x, Term y) {
//        if (u.varDepth < NAL.unify.UNIFY_VAR_RECURSION_DEPTH_LIMIT) {
//            u.varDepth++;
//            boolean result = x.unify(y, u);
//            u.varDepth--;
//            return result;
//        } else
//            return false; //recursion limit exceeded
//    }

    private static boolean unifyVar(Unify u, Term x, Term y) {
        if (x instanceof Variable) {
            var xop = x.op();
            if (y instanceof Variable)  {
                var yop = y.op();

                if (xop.id > yop.id) {
                    //swap for variable rank
                    var z = x;
                    var zop = xop;
                    x = y; xop = yop;
                    y = z; yop = zop;
                }
                if (xop == yop && u.commonVariables && xop != VAR_PATTERN) {
                    if (!(x instanceof Ellipsis) && !(y instanceof Ellipsis)) {
                        return CommonVariable.unify((Variable) x, (Variable) y, u);
                    }
                }
            }
            if (Unify.canPut(xop, y)) {
                if (u.putXY((Variable) x, y))
                    return true;
            }
        }

        if (y instanceof Variable) {
            var yop = y.op();
            return u.var(yop) && Unify.canPut(yop, x) && !x.containsRecursively(y) && u.putXY((Variable) y, x);
        }

        return false;
    }

    @Override
    default Term concept() {
        return Null;
    }

    /**
     * The syntactic complexity of a variable is 0, because it does not refer to
     * any concept.
     *
     * @return The complexity of the target, an integer
     */
    @Override
    default int complexity() {
        return 0;
    }

    @Override
    default boolean hasVars() {
        return true;
    }

    /**
     * average of complexity(=0) and volume(=1)
     */
    @Override
    default float voluplexity() {
        return 0.5f;
    }

    @Override
    @Paper
    @Skill({"Prolog", "Unification_(computer_science)", "Negation", "Möbius_strip", "Total_order", "Recursion"})
    default boolean unify(Term _y, Unify u) {

        if (equals(_y))
            return true;

        var x = u.resolveTerm(this, true);
        var y = u.resolveTerm(_y, true);

        //unify variable negation mobius
        var xn = x instanceof Neg;
        var yn = y instanceof Neg;
        if (xn && yn) {
            //unnegate both
            x = x.unneg();
            y = y.unneg();
        } else {
            //swap neg for variable order
            if (xn && !yn && y instanceof Variable) {
                var xu = x.unneg();
                if (xu instanceof Variable && xu.opID() < y.opID()) {
                    x = xu;
                    y = y.neg();
                } else if (y.equals(xu))
                    return false; //x != --x
            } else if (yn && !xn && x instanceof Variable) {
                var yu = y.unneg();
                if (yu instanceof Variable && yu.opID() < x.opID()) {
                    y = yu;
                    x = x.neg();
                } else if (x.equals(yu))
                    return false; //x != --x
            }
        }
        if (xn == yn && (x != this || y != _y) && x.equals(y))
            return true;

        return x instanceof Variable || y instanceof Variable ?
                unifyVar(u, x, y)
                :
                x.unify(y, u);
    }

    @Override
    default Term normalize(byte offset) {
        return this;
    }

    Variable normalizedVariable(byte vid);
}
