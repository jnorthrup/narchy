package nars.term;

import jcog.Paper;
import jcog.Skill;
import nars.Op;
import nars.Param;
import nars.term.atom.Atomic;
import nars.term.var.CommonVariable;
import nars.unify.Unify;

import static nars.Op.NEG;

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
    default boolean unify(Term y, Unify u) {

        if (equals(y))
            return true;

        return unify(u.resolve(this), u.resolvePosNeg(y), u);
    }

    static boolean unify(Term x, Term y, Unify u) {
        if (x.equals(y))
            return true;

        Op xOp = x.op();
        if (x instanceof Variable && u.var(xOp)) {

            Op yOp = y.op();

            if (y instanceof Variable) {

                boolean xCommon = x instanceof CommonVariable;
                boolean yCommon = y instanceof CommonVariable;
                if (yCommon && !xCommon && ((CommonVariable) y).vars.contains(x))
                    return true; //already contained

                if (xCommon && !yCommon && ((CommonVariable) x).vars.contains(y))
                    return true; //already contained

                if (u.commonVariables && u.varCommon(xOp) && u.varCommon(yOp))
                    return CommonVariable.unify((Variable) x, (Variable) y, u);
                else if (yOp.id < xOp.id && u.varReverse(yOp))
                    return u.putXY((Variable) y, x);
            }

            return u.putXY((Variable) x, y);

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

            if (u.varDepth < Param.UNIFY_VAR_RECURSION_DEPTH_LIMIT) {
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
