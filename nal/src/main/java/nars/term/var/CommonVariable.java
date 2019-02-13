package nars.term.var;

import jcog.WTF;
import jcog.data.byt.DynBytes;
import jcog.data.byt.RecycledDynBytes;
import jcog.data.set.ArrayUnenforcedSortedSet;
import jcog.util.ArrayUtils;
import nars.Op;
import nars.term.Term;
import nars.term.Variable;
import nars.unify.Unify;

import java.util.SortedSet;
import java.util.TreeSet;

public final class CommonVariable extends UnnormalizedVariable {


    private final SortedSet<Variable> vars;

    public static Variable common(Variable A, Variable B) {
        int cmp = A.compareTo(B);
        assert(cmp!=0);
        if (cmp > 0) {
            Variable x = A;
            A = B;
            B = x;
        }

        Op op = A.op();

        if (!(A instanceof CommonVariable) && !(B instanceof CommonVariable)) {
            byte[] a = A.bytes();
            byte[] b = B.bytes();
            byte[] key = ArrayUtils.addAll(a, b);
            return new CommonVariable(op, ArrayUnenforcedSortedSet.the(A, B), key);
        } else {
            SortedSet<Variable> v = new TreeSet();
            if (A instanceof CommonVariable)
                v.addAll(((CommonVariable)A).vars);
            else
                v.add(A);
            if (B instanceof CommonVariable)
                v.addAll(((CommonVariable)B).vars);
            else
                v.add(B);
            if (v.size() <= 2)
                throw new WTF();
            try (DynBytes k = RecycledDynBytes.tmpKey()) {
                assert (k.len == 0);
                for (Variable vv : v)
                    k.write(vv.bytes());
                byte[] key = k.arrayCopy();
                return new CommonVariable(op, v, key);
            }
        }
    }

    CommonVariable(/*@NotNull*/ Op type, SortedSet<Variable> vars, byte[] key) {
        super(type, key);
        this.vars = vars;
                //commonVariableKey(type, x, y) /* include trailing so that if a common variable gets re-commonalized, it wont become confused with repeats in an adjacent variable */);
    }

    @Override
    public boolean the() {
        for (Variable v : vars)
            if (!v.the())
                return false;
        return true;
    }

    @Override
    public String toString() {
        return op() + vars.toString();
    }

    @Override
    public boolean unify(Term y, Unify u) {
        if (y instanceof Variable) {
            if (vars.contains(y))
                return true; //already present
        }
        return super.unify(y,  u);
    }

    public static boolean unify(Variable X, Variable Y, Unify u) {
        //if (xOp == y.op()) {

        if (Y instanceof CommonVariable) {
            if (((CommonVariable)Y).vars.contains(X))
                return true; //already contained
        } else if (X instanceof CommonVariable) {
            if (((CommonVariable)X).vars.contains(Y))
                return true; //already contained
        }

        //same op: common variable
        //TODO may be possible to "insert" the common variable between these and whatever result already exists, if only one in either X or Y's slot
        Variable common = CommonVariable.common(X,Y);
        if (u.putXY(X, common) && u.putXY(Y, common)) {
            //rewrite any appearances of X or Y in already-assigned variables
//            if (u.xy.size() > 2) {
//                return u.xy.tryReplaceAll((var, val) -> {
//                    if (var.equals(X) || var.equals(Y) || !val.hasAny(X.op()))
//                        return val; //unchanged
//                    else
//                        return val.replace(X, common).replace(Y, common);
//                });
//            } else {
//                return true; //only the common variable components were asisgned
//            }
            return true;
        }
        return false;
        //}
    }

    @Override
    public int opX() {
        return Term.opX(op(), 1 /* different from normalized variables with a subOp of 0 */);
    }



}
