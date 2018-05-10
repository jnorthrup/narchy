package nars.term;

import nars.Op;
import nars.term.atom.Atomic;
import nars.term.var.CommonVariable;
import nars.unify.Unify;
import org.jetbrains.annotations.Nullable;

import static nars.Op.Null;

/**
 * similar to a plain atom, but applies altered operating semantics according to the specific
 * varible type, as well as serving as something like the "marker interfaces" of Atomic, Compound, ..
 * <p>
 * implemented by both raw variable terms and variable concepts
 **/
public interface Variable extends Atomic {


    static boolean commonalizableVariable(Op x) {
        return x.in(Op.VAR_QUERY.bit | Op.VAR_DEP.bit | Op.VAR_INDEP.bit);
    }

    @Override
    @Nullable
    default Term normalize() {
        return this; //override: only normalize if given explicit offset with normalize(int offset) as is done during normalization
    }

    @Override
    Variable normalize(byte offset);

    @Override
    default Term conceptualizableOrNull() {
        return Null;
    }

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

//    @Override
//    default Term concept() {
//        //throw new UnsupportedOperationException();
//        return Null;
//    }

    //    @Override
//    @Nullable
//    default Set<Variable> varsUnique(@Nullable Op type) {
//        if ((type == null || op() == type))
//            return Set.of(this);
//        else
//            return null;
//    }

    @Override
    default float voluplexity() {
        return 0.5f;
    }

    @Override
    default boolean unify(Term y, Unify u) {
        return unifyVar(y, u, true);
    }

    /** the direction parameter is to maintain correct provenance of variables when creating common vars.
     *  since
     *    #1 from x  is a different instance than  #1 from y
     */
    default boolean unifyVar(Term y, Unify u, boolean forward) {

        final Variable x = this;

        if (u.constant(x) && x.equals(y))
            return true;

        if (y instanceof Variable) {
            return unifyVar(x, ((Variable)y), forward, u);
        } else {
            return u.putXY(x, y);
        }
    }

    static boolean unifyVar(Variable x, Variable y, boolean forward, Unify u) {
        //HACK exclude Image terms from unification
        if (x == Op.imInt || x == Op.imExt || y == Op.imInt || y == Op.imExt)
            return x==y;

        //var pattern will unify anything (below)
        //see: https://github.com/opennars/opennars/blob/4515f1d8e191a1f097859decc65153287d5979c5/nars_core/nars/language/Variables.java#L18
        Op xOp = x.op();
        Op yOp = y.op();
        if (xOp == yOp) {

            if (u.varCommonalize && commonalizableVariable(xOp) && commonalizableVariable(yOp)) {
                //TODO check if this is already a common variable containing y


                Term xBound = u.xy(x);
                Term yBound = u.xy(y);
//                    Termed _yBound = u.apply(y); //full resolve via apply
//                    Term yBound  = _yBound == null ? null : _yBound.term(); //HACK

                if (yBound != null) {
                    if (yBound.equals(x))
                        return true;
                    if (xBound != null) {
                        if (xBound.equals(y)||xBound.equals(yBound))
                            return true;

                        return xBound.unify(yBound, u);
                    }
                }

                Term common = forward ? CommonVariable.common(x, y) : CommonVariable.common(y, x);

                Term target;
                if (xBound != null) target = xBound;
                else if (yBound != null) target = yBound;
                else target = null;

                if (u.replaceXY(x, common) && u.replaceXY(y, common)) {
                    if (target != null)
                        return u.putXY(common, target);
                    else
                        return true;
                }
            }

        } else {

            //variable subsumption order
            if (xOp.id < yOp.id) {
                if (u.varSymmetric)
                    return y.unifyVar(x, u, false);
                else
                    return false;
            }
        }

        return u.putXY(x, y);
    }
}
