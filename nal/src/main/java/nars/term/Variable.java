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
        }

        //mobius neg-unification resolution
        boolean done = false;
        if (x instanceof Neg) {
            Term xu = x.unneg();
            if (xu instanceof Variable && (!(y instanceof Variable) || u.assigns(xu.op(), y.op())) && !(y.op() == FRAG)) {
                x = xu;
                y = y.neg();
                done = true;
            }
        }
        if (!done && y instanceof Neg) {
            Term yu = y.unneg();
            if (yu instanceof Variable && (!(x instanceof Variable) || u.assigns(yu.op(), x.op())) && !(x.op() == FRAG)) {
                y = yu;
                x = x.neg();
                done = true;
            }
        }


        if (x instanceof Variable && u.assigns(x.op(), y.op()))
            return unifyVar(u, (Variable) x, y);
        else if (y instanceof Variable && u.assigns(y.op(), x.op()))
            return unifyVar(u, (Variable) y, x);
        else
            return (!(x instanceof Variable)) && unifyConst(u, x, y);
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

    private static boolean unifyVar(Unify u, Variable x, Term y) {
        Op xOp = x.op(), yOp = y.op();
        if (xOp == yOp && u.commonVariables && !(x instanceof Ellipsis) && !(y instanceof Ellipsis))
            return CommonVariable.unify(x, (Variable) y, u);
        else
            return u.putXY(x, y);
    }


    @Override
    default Variable normalize(byte offset) {
        return this;
    }

    Variable normalizedVariable(byte vid);
}
