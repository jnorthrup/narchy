package nars.term;

import jcog.Paper;
import jcog.Skill;
import nars.Op;
import nars.Param;
import nars.term.atom.Atomic;
import nars.term.var.CommonVariable;
import nars.unify.Unify;

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
    default boolean unify(Term _y, Unify u) {

        if (equals(_y))
            return true;

        Op xOp = op();
        if (!u.var(xOp))
            return false;

        Term x = u.resolve(this);
        if (!x.equals(this)) {
            xOp = x.op();
            if (!(x instanceof Variable) || !u.var(xOp)) {
                if (u.varDepth < Param.UNIFY_VAR_RECURSION_DEPTH_LIMIT) {
                    u.varDepth++;
                    boolean result = x.unify(_y, u);
                    u.varDepth--;
                    return result;
                } else {
                    //recursion limit exceeded
                    return false;
                    //                } catch (StackOverflowError e) {
                    //                    System.err.println("unify stack overflow: " + x + "->" + y + " in " + u.xy); //TEMPORARY
                    //                    return false;
                    //                }
                }
//              else: continue below
            }
        }

        Term y = u.resolvePosNeg(_y);

        if (x instanceof Variable && u.varCommon(xOp)) {
            if (y instanceof Variable && u.commonVariables && u.varCommon(y.op())) {
                return CommonVariable.unify((Variable) x, (Variable) y, u);
            } else {
                return u.putXY((Variable) x, y);
            }
        } else if (y instanceof Variable && u.varReverse(y.op())) {
            return u.putXY((Variable) y, x);
        } else {
            if (x instanceof Variable)
                return u.putXY((Variable)x, y);
            else
                return x.unify(y, u);
        }


    }


    @Override
    default Variable normalize(byte offset) {
        return this;
    }

    Variable normalizedVariable(byte vid);
}
