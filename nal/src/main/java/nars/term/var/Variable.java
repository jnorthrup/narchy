package nars.term.var;

import nars.Op;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.subst.Unify;
import org.jetbrains.annotations.Nullable;

/**
 * similar to a plain atom, but applies altered operating semantics according to the specific
 * varible type, as well as serving as something like the "marker interfaces" of Atomic, Compound, ..
 * <p>
 * implemented by both raw variable terms and variable concepts
 **/
public interface Variable extends Atomic {


    @Override
    @Nullable
    default Term normalize() {
        return this; //override: only normalize if given explicit offset with normalize(int offset) as is done during normalization
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
    default float voluplexity() {
        return 0.5f;
    }

    @Override
    default Term conceptual() {
        throw new UnsupportedOperationException();
    }

    //    @Override
//    @Nullable
//    default Set<Variable> varsUnique(@Nullable Op type) {
//        if ((type == null || op() == type))
//            return Set.of(this);
//        else
//            return null;
//    }

    static boolean commonalizableVariable(Op x) {
        return x.in(Op.VAR_DEP.bit | Op.VAR_INDEP.bit);
    }

    @Override
    default boolean unify(Term y, Unify u) {

        if (this.equals(y))
            return true;

        //var pattern will unify anything (below)
        //see: https://github.com/opennars/opennars/blob/4515f1d8e191a1f097859decc65153287d5979c5/nars_core/nars/language/Variables.java#L18
        Op xOp = op();

        if (y instanceof Variable) {
            Op yOp = y.op();
            if (xOp == yOp) {

                if (u.varCommonalize && commonalizableVariable(xOp) && commonalizableVariable(yOp)) {
                    //TODO check if this is already a common variable containing y
                    Term common = CommonVariable.common(this, (Variable) y);

                    if (common == this || common == y)
                        return true; //no change

                    return u.putXY(this, common) && u.putXY(y, common);
                }

            }

            if (xOp.id < yOp.id) {  //only allows indep to subsume dep but not vice versa
                if (u.varSymmetric)
                    return y.unify(this, u);
                else
                    return false;
            }
        }

        return u.matchType(xOp) && u.putXY(this, y);


//        if (y instanceof Variable) {
//            return subst.putXY(this, y);
//        }
//
//        if (subst.matchType(this)
//                //&& !subst.matchType(y) //note: the !subst.matchType(y) subcondition is an attempt at preventing infinite cycles of variable references
//                ) {
//            return subst.putXY(this, y);
//        }
//
//        return false;
    }
}
