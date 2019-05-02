package nars.term;

import jcog.Paper;
import jcog.Skill;
import nars.NAL;
import nars.Op;
import nars.term.atom.Atomic;
import nars.term.var.CommonVariable;
import nars.term.var.ellipsis.Ellipsis;
import nars.unify.Unify;

import static nars.Op.NEG;
import static nars.Op.VAR_PATTERN;

/**
 * similar to a plain atom, but applies altered operating semantics according to the specific
 * varible type, as well as serving as something like the "marker interfaces" of Atomic, Compound, ..
 * <p>
 * implemented by both raw variable terms and variable concepts
 **/
public interface Variable extends Atomic {

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
        if (x!=this && x.equals(y0))
            return true;
        Term y = u.resolvePosNeg(y0);
        if (y!=y0 && x.equals(y))
            return true;

        if (x!=this || y!=y0) {
            if (x instanceof Variable)
                return unifyVar(u, (Variable)x, y);
            else if (y instanceof Variable && u.varReverse(y.op()))
                return unifyVar(u, (Variable)y, x);
            else
                return x.unify(y, u); //both constant-like
        } else {
            return unifyVar(u, this, y0);
        }
    }

    default boolean unifyVar(Unify u, Variable x, Term y) {
        Op xOp = x.op();
        if (x instanceof Variable && u.var(xOp)) {

            if (!(x instanceof Ellipsis)) {

                if (y instanceof Variable && !(y instanceof Ellipsis)) {
                    Op yOp = y.op();

//                    if (u.commonVariables && (yOp!=VAR_PATTERN && (u.varCommon(xOp) || u.varCommon(yOp))))
//                        return CommonVariable.unify((Variable) x, (Variable) y, u);
//                    else if (yOp.id < xOp.id && u.varReverse(yOp))
//                        return u.putXY((Variable) y, x);
//                    if (yOp == xOp)
                    if (xOp!=VAR_PATTERN && yOp!=VAR_PATTERN && u.commonVariables)
                        return CommonVariable.unify(x, (Variable) y, u);
                    else {
                        if (yOp.id < xOp.id && u.varReverse(yOp))
                            return u.putXY((Variable) y, x);
                    }

                }
            }

            return u.putXY(x, y);

        } else if (y instanceof Variable && u.varReverse(y.op())) {
            return u.putXY((Variable) y, x);
        } else {
            if (x instanceof Variable)
                return false; //a variable; but not unifiable
            if (y.op()==NEG && y.unneg().equals(x))
                return false;
            if (x.op()==NEG && x.unneg().equals(y))
                return false;
//            if (x instanceof Variable)
//                return u.putXY((Variable)x, y);
//            else
//                return x.unify(y, u);

            if (u.varDepth < NAL.unify.UNIFY_VAR_RECURSION_DEPTH_LIMIT) {
                u.varDepth++;
                boolean result = x.unify(y, u);
                u.varDepth--;
                return result;
            } else
                //recursion limit exceeded
                return false;
        }
    }


    @Override
    default Variable normalize(byte offset) {
        return this;
    }

    Variable normalizedVariable(byte vid);
}
