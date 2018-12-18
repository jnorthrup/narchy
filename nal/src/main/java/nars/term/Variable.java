package nars.term;

import jcog.Paper;
import jcog.Skill;
import nars.Op;
import nars.term.atom.Atomic;
import nars.term.var.CommonVariable;
import nars.unify.Unify;
import nars.unify.ellipsis.EllipsisMatch;
import org.jetbrains.annotations.Nullable;

import static nars.Op.VAR_PATTERN;

/**
 * similar to a plain atom, but applies altered operating semantics according to the specific
 * varible type, as well as serving as something like the "marker interfaces" of Atomic, Compound, ..
 * <p>
 * implemented by both raw variable terms and variable concepts
 **/
public interface Variable extends Atomic {


//    static boolean commonalizableVariable(Op x) {
//        return x.in(Op.VAR_QUERY.bit | Op.VAR_DEP.bit | Op.VAR_INDEP.bit);
//    }


    @Override
    @Nullable
    default Term normalize() {
        return this;
    }

    @Override
    Variable normalize(byte offset);


    /**
     * The syntactic complexity of a variable is 0, because it does not refer to
     * any concept.
     *
     * @return The complexity of the term, an integer
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

        if (!u.matchType(xOp))
            return false;

        Term x = u.resolve(this);
        Term y = u.resolvePosNeg(_y);
        if (y!=_y) {
            if (x.equals(y))
                return true;
            if (x.equalsNeg(y))
                return false;
        }
        if (x != this) {
//            try {
                return x.unify(y, u);
//            } catch (StackOverflowError e) {
//                throw new WTF("stack overflow unifying variable " + x + " -> " + y + " resolved from " + _y); //TEMPORARY
//            }
        }

        if (y instanceof Variable) {
            if (!(y instanceof EllipsisMatch)) {
                if (xOp == y.op()) {
                    Variable Y = (Variable) y;
                    Variable X = this;

                    //same op: common variable
                    //TODO may be possible to "insert" the common variable between these and whatever result already exists, if only one in either X or Y's slot
                    Variable common = X.compareTo(Y) < 0 ? CommonVariable.common(X, Y) : CommonVariable.common(Y, X);
                    return u.putXY(X, common) && u.putXY(Y, common);
                }
            }
        }



        if (y instanceof EllipsisMatch && xOp != VAR_PATTERN)
            return false;

        boolean yMatches;


        if (y instanceof Variable) {
            Op yOp = y.op();

            yMatches = ((xOp == yOp) || u.matchType(yOp));

            if (yMatches) {
                Variable X = (Variable) x;
                Variable Y = (Variable) y;

                //choose by id, establishing a deterministic chain of variable command
                //return (xOp.id > yOp.id) ? u.putXY(X, Y) : u.putXY(Y, X);

                //int before = u.size();
                boolean ok = (xOp.id > yOp.id) ? u.putXY(X, Y) : u.putXY(Y, X);
                if (ok) {
                    return true;
                } else {
                    //u.revert(before);
                    return (xOp.id < yOp.id) ? u.putXY(X, Y) : u.putXY(Y, X);
                }

            }
        }


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


        Variable a;
        Term b;
//            if (xMatches) {
//                if (x.containsRecursively(y))
//                    return false; //cycle
        a = (nars.term.Variable) x;
        b = y;
//            } else if (yMatches) {
////                if (y.containsRecursively(x))
////                    return false; //cycle
//                a = (Variable) y;
//                b = x;
//            } else {
//                return false;
//            }

        //            Op ao = a.op();
        //if (ao !=VAR_PATTERN) {
        //TODO total ordering to prevent something like #1 = x(%1)
        //                int mask;
        //                switch (ao) {
        //                    case VAR_DEP: mask = Op.or(VAR_PATTERN, VAR_QUERY, VAR_INDEP); break;
        //                    case VAR_INDEP: mask = Op.or(VAR_PATTERN, VAR_QUERY); break;
        //                    case VAR_QUERY: mask = Op.or(VAR_PATTERN); break;
        //                    default:
        //                        throw new UnsupportedOperationException();
        //                }
//        if (b instanceof Compound) {
//            int mask = VAR_PATTERN.bit;
//            if (b.hasAny(mask))
//                return false;
//        }
        //}

        return u.putXY(a, b);
//        } else {
//            try {
//                return x.unify(y, u);
//            } catch (StackOverflowError e) {
//                System.err.println("unify stack overflow: " + x + " -> " + y);
//                return false;
//            }
//        }

    }

}
