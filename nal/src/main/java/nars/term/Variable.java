package nars.term;

import jcog.Paper;
import jcog.Skill;
import nars.NAL;
import nars.Op;
import nars.term.atom.Atomic;
import nars.term.var.CommonVariable;
import nars.term.var.ellipsis.Ellipsis;
import nars.unify.Unify;

import static nars.Op.FRAG;
import static nars.term.atom.Bool.Null;

/**
 * similar to a plain atom, but applies altered operating semantics according to the specific
 * varible type, as well as serving as something like the "marker interfaces" of Atomic, Compound, ..
 * <p>
 * implemented by both raw variable terms and variable concepts
 **/
public interface Variable extends Atomic {

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

        Term x = u.resolve(this);
        if (x != this && x.equals(y0))
            return true;

        Term y = u.resolvePosNeg(y0);
        if (y != y0 && x.equals(y))
            return true;

        //unify variable negation mobius

        if (x instanceof Neg && y instanceof Neg) {
            x = x.unneg();
            y = y.unneg();
        } else {

            //mobius pos/neg unification resolution
            boolean done = false;
            if (x instanceof Neg) {
                Term xu = x.unneg();
                if (xu instanceof Variable && (!(y instanceof Variable) || u.assigns(xu.op(), y.op())) && neggable(y)) {
                    x = xu;
                    y = y.neg();
                    done = true;
                }
            }
            if (!done && y instanceof Neg) {
                Term yu = y.unneg();
                if (yu instanceof Variable && (!(x instanceof Variable) || u.assigns(yu.op(), x.op())) && neggable(x)) {
                    y = yu;
                    x = x.neg();
                    done = true;
                }
            }
        }


        if ((x instanceof Variable || y instanceof Variable)) {
            return unifyVar(u, x, y);
        } else {
            return unifyConst(u, x, y);
        }
    }

    private static boolean neggable(Term x) {
        return (x.op() != FRAG) || (x.subs()==1); //allow 1-element fragments, since they can be neg safely
    }

    private static boolean unifyConst(Unify u, Term x, Term y) {
        if (u.varDepth < NAL.unify.UNIFY_VAR_RECURSION_DEPTH_LIMIT) {
            u.varDepth++;
            boolean result = x.unify(y, u); //both constant-like
            u.varDepth--;
            return result;
        } else
            return false; //recursion limit exceeded
    }

    private static boolean unifyVar(Unify u, Term x, Term y) {
        Op xo = x.op(), yo = y.op();
        if (xo == yo) {
            if (u.commonVariables && !(x instanceof Ellipsis) && !(y instanceof Ellipsis))
                return CommonVariable.unify((Variable)x, (Variable) y, u);
            else {
                if (x.compareTo(y) > 0) {
                    //swap to natural ordering
                    Term z = x;
                    x = y;
                    y = z;
                    //continue below
                }
            }
        }

        if (x instanceof Variable && u.assigns(xo, yo)) {
            if (u.putXY((Variable) x, y))
                return true;
        }

        if (y instanceof Variable && u.assigns(yo, xo)) {
            return u.putXY((Variable) y, x);
        }

        return false;
    }


    @Override
    default Variable normalize(byte offset) {
        return this;
    }

    Variable normalizedVariable(byte vid);
}
