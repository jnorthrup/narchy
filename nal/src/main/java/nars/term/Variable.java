package nars.term;

import nars.Op;
import nars.term.atom.Atomic;
import nars.term.var.CommonVariable;
import nars.unify.Unify;
import org.jetbrains.annotations.Nullable;

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

    @Override
    default float voluplexity() {
        return 0.5f;
    }

    @Override
    default boolean unify(Term _y, Unify u) {

        if (equals(_y))
            return true;

        final Term x = u.resolve(this);

        if (x!=this && x.equals(_y))
            return true;

        final Term y = (_y instanceof Variable) ? u.resolve(_y) : _y;

        if (x.equals(y))
            return true;

//        if (x != this) {
////            try {
//                return x.unify(y, u);
////            } catch (StackOverflowError e) {
////                throw new RuntimeException("unification stack overflow: " + x + ' ' + y + " in " + u.xy);
////            }
//        } else {
        if (x instanceof Variable) {
            if (y instanceof Variable){
                return unifyVar((Variable) x, (Variable) y, u);
            } else {
                return u.matchType(x.op()) && u.putXY((Variable)x, y);
            }
        } else {
            return x != this && x.unify(y, u);
        }
//        }
    }



    static boolean unifyVar(Variable x, Variable y, Unify u) {

//        if (x instanceof CommonVariable)
//            return false;
//        if (y instanceof CommonVariable)
//            return false;

        Op xOp = x.op();

        Op yOp = y.op();
        if (xOp == yOp) {


             if (u.matchType(xOp)) {

                 Term common = x.compareTo(y) < 0 ? CommonVariable.common(x, y) : CommonVariable.common(y, x);

                 return u.putXY(x, common) && u.putXY(y, common);
             } else {

                 return false;
             }


        } else {
//            if (u.symmetric && u.matchType(yOp)) {
//                Term common = CommonVariable.common(y, x);
//                return u.putXY(x, common) && u.putXY(y, common);
//            }

            if (xOp.id < yOp.id) {
                if (u.symmetric && u.matchType(yOp)) {
                    /* reverse subsume */
                    return u.putXY(y, x);
                }

                return false;

            } else {

                /* subsume: x -> y */
                return u.matchType(xOp) && u.putXY(x, y);
            }
        }

    }

    @Override
    default boolean unifyReverse(Term x, Unify u) {

        return unify(x, u);

//        Term y = u.resolve(this);
//        if (y != this)
//            return y.unify(x, u);
//
//        return
//                u.matchType(op()) &&
//                        unifyVar(x, u, false);
    }
}
