package nars.term;

import jcog.Paper;
import jcog.Skill;
import nars.Op;
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

        if (this==_y || (_y instanceof Variable && equals(_y)))
            return true;

        Op xOp = op();

        if (!u.matchType(xOp))
            return false;

        Term y = u.resolvePosNeg(_y);
        if (!y.equals(_y)) {
            if (equals(y))
                return true;
//            if (y.containsRecursively(this))
//                return false; //cycle caught
        }

        Term x = u.resolve(this);
        if (!x.equals(this)) {
            if (x.equals(y))
                return true;

            if (!(x instanceof Variable) || !u.matchType(x.op())) {
//                if (u.varDepth < Param.UNIFY_VAR_DEPTH_LIMIT) {
//                    u.varDepth++;
                    boolean result = x.unify(y, u);
//                    u.varDepth--;
                    return result;
//                } else {
//                    return false;
                    //                } catch (StackOverflowError e) {
                    //                    System.err.println("unify stack overflow: " + x + "->" + y + " in " + u.xy); //TEMPORARY
                    //                    return false;
                    //                }
                } else {
                    //continue below
                }
        }

        if (y instanceof Variable && x instanceof Variable && u.commonVariables && u.matchType(y.op())) {
            return CommonVariable.unify((Variable)x, (Variable)y, u);
        }

//        if (y instanceof EllipsisMatch && xOp != VAR_PATTERN)
//            return false;


//        if (y instanceof Variable) {
//            Op yOp = y.op();
//
//            boolean yMatches = /*((xOp == yOp) || */u.matchType(yOp);
//
//            if (yMatches) {
//                Variable X = (Variable) x, Y = (Variable) y;
//
//                //choose by id, establishing a deterministic chain of variable command
//                //return (xOp.id > yOp.id) ? u.putXY(X, Y) : u.putXY(Y, X);
//
//                //int before = u.size();
//                boolean ok = (xOp.id > yOp.id) ? u.putXY(X, Y) : u.putXY(Y, X);
//                if (ok) {
//                    return true;
//                } else {
//                    //u.revert(before);
//                    return //(xOp.id < yOp.id) ? u.putXY(X, Y) : u.putXY(Y, X);
//                            false;
//                }
//
//            }
//        }


//        //negation mobius strip
//        //  check if negation is the only thing wrapping either's possible matching variable.
//        //  and apply negation to both
//        if (!yMatches) {
//            if ((xOp != VAR_PATTERN)) {
//                if (y.op() == NEG) {
//                    Term yy = y.unneg();
//                    Op yyo = yy.op();
//                    if (yyo.id > xOp.id && u.matchType(yyo)) {
//                        y = yy;
//                        x = x.neg();
//                        yMatches = true;
//                    }
//                }
//            }
//        }

        return u.putXY((Variable)x, y);


    }


    @Override
    default Variable normalize(byte offset) {
        return this;
    }

    Variable normalizedVariable(byte vid);
}
