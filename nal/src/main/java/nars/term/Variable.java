package nars.term;

import jcog.Paper;
import jcog.Skill;
import nars.Op;
import nars.term.atom.Atomic;
import nars.term.var.CommonVariable;
import nars.term.var.ellipsis.Ellipsis;
import nars.term.var.ellipsis.Ellipsislike;
import nars.unify.Unify;
import nars.unify.UnifyFirst;

import static nars.Op.FRAG;
import static nars.Op.VAR_PATTERN;
import static nars.term.atom.Bool.Null;

/**
 * similar to a plain atom, but applies altered operating semantics according to the specific
 * varible type, as well as serving as something like the "marker interfaces" of Atomic, Compound, ..
 * <p>
 * implemented by both raw variable terms and variable concepts
 **/
public interface Variable extends Atomic, UnifyFirst {

    private static boolean neggable(Term t) {
        return !(t instanceof Ellipsislike) && (t.op() != FRAG);
    }

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
            Op xop = x.op();
            if (u.var(xop)) {
                if (xop != VAR_PATTERN && y instanceof Variable) {
                    if (u.commonVariables) {
                        if (!(x instanceof Ellipsis) && !(y instanceof Ellipsis)) {
                            if (xop == y.op())
                                return CommonVariable.unify((Variable) x, (Variable) y, u);
                        }
                    }
                }

                if (u.canPut(xop, y)) {
                    if (u.putXY((Variable) x, y))
                        return true;
                }
            }
        }

        if (u.canPut(y, x)) {
            if (u.putXY((Variable) y, x))
                return true;
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
    default boolean unify(Term y0, Unify u) {

        if (equals(y0))
            return true;

        //Term x = u.resolveVar(this);
        //Term x = u.resolveTerm(this, false);
        Term x = u.resolveTermRecurse(this);
        if (x != this && x.equals(y0))
            return true;

        Term y = u.resolveTermRecurse(y0);
        //Term y = u.resolveTerm(y0);
        if (y != y0 && x.equals(y))
            return true;

        //unify variable negation mobius

        if (x instanceof Neg && y instanceof Neg) {
            x = x.unneg();
            y = y.unneg();
        } else {

            //mobius pos/neg unification resolution
            boolean done = false;
            if (x instanceof Neg && neggable(y)) {
                Term xu = x.unneg();
                if (u.canPut(xu, y)) {
                    x = xu;
                    y = y.neg();
                    done = true;
                }
            }
            if (!done && y instanceof Neg && neggable(x)) {
                Term yu = y.unneg();
                if (u.canPut(yu, x)) {
                    y = yu;
                    x = x.neg();
                }
            }
        }

        return x instanceof Variable || y instanceof Variable ?
                unifyVar(u, x, y)
                :
                x.unify(y, u);
                //unifyConstSafe(u, x, y);
    }

    @Override
    default Variable normalize(byte offset) {
        return this;
    }

    Variable normalizedVariable(byte vid);
}
