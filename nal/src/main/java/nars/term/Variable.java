package nars.term;

import nars.Op;
import nars.term.atom.Atomic;
import nars.term.var.CommonVariable;
import nars.unify.Unify;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

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
        if (x != this && x.equals(_y))
            return true;

        final Term y = (_y instanceof Variable) ? u.resolve(_y) : _y;
        if (y != _y && x.equals(y))
            return true;

        if (x instanceof Variable) {

            Op xOp = x.op();
            Op yOp = y.op();
            boolean xMatches = u.matchType(xOp);
            boolean yMatches = y instanceof Variable && ((xMatches && xOp == yOp) || u.matchType(yOp));

            Variable X = (Variable) x;

            if (xMatches && yMatches) {
                Variable Y = (Variable) y;

                //same op: common variable
                if (yOp == xOp) {


                    Supplier<Term> common = () -> X.compareTo(Y) < 0 ? CommonVariable.common(X, Y) : CommonVariable.common(Y, X);

                    if (!u.xy.tryPut(X, Y, common)) {
                        //TODO may be possible to "insert" the common variable between these and whatever result already exists, if only one in either X or Y's slot
                        return false;
                    } else {
                        return true;
                    }

                }
                //choose by id
                if (xOp.id > yOp.id) {
                    return u.putXY(X, Y);
                } else {
                    return u.putXY(Y, X);
                }
            } else if (xMatches) {
                return u.putXY(X, y);
            } else if (yMatches) {
                //not subsume-able; the reverse should be called on its own if symmetric u.symmetric;
                return u.putXY((Variable)y, x); //require symmetric?
            } else {
                return false;
            }

        } else {
            return x.unify(y, u);
        }
    }

}
