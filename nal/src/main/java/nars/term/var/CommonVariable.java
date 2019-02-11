package nars.term.var;

import nars.Op;
import nars.term.Term;
import nars.term.Variable;
import nars.unify.Unify;

public final class CommonVariable extends UnnormalizedVariable {



    CommonVariable(/*@NotNull*/ Op type, Variable x, Variable y) {
        super(type,
                commonVariableKey(type, x, y) /* include trailing so that if a common variable gets re-commonalized, it wont become confused with repeats in an adjacent variable */);
    }


    private static String commonVariableKey(Op type, Variable x, Variable y) {
        return String.valueOf(type.ch) + '\"' + filtercomponent(x) + filtercomponent(y) + '\"'; //HACK

        //TODO
//        byte[] xb = x.bytes();
//        byte[] yb = y.bytes();
//        byte[] b = new byte[xb.length + yb.length + 2];
//        System.arraycopy(xb, 0, b, 1, xb.length);
//        return b;
    }

    private static String filtercomponent(Variable x) {
        return x.toString().replace('\"','_'); //HACK
    }

    public static boolean unify(Variable X, Variable Y, Unify u) {
        //if (xOp == y.op()) {

        //same op: common variable
        //TODO may be possible to "insert" the common variable between these and whatever result already exists, if only one in either X or Y's slot
        Variable common = X.compareTo(Y) < 0 ? CommonVariable.common(X, Y) : CommonVariable.common(Y, X);
        if (u.putXY(X, common) && u.putXY(Y, common)) {
            //rewrite any appearances of X or Y in already-assigned variables
            if (u.xy.size() > 2) {
                return u.xy.tryReplaceAll((var, val) -> {
                    if (var.equals(X) || var.equals(Y) || !val.hasAny(X.op()))
                        return val; //unchanged
                    else
                        return val.replace(X, common).replace(Y, common);
                });
            } else {
                return true; //only the common variable components were asisgned
            }
        }
        return false;
        //}
    }

    @Override
    public int opX() {
        return Term.opX(op(), 1 /* different from normalized variables with a subOp of 0 */);
    }

    public static Variable common(Variable A, Variable B) {
        Op op = A.op();
        //assert(B.op()==op);

        return new CommonVariable(op, A,B);
    }


}
