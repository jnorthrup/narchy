package nars.term.var;

import com.google.common.base.Joiner;
import jcog.WTF;
import jcog.data.set.ArrayUnenforcedSortedSet;
import nars.Op;
import nars.Param;
import nars.term.Term;
import nars.term.Variable;
import nars.unify.Unify;

import java.util.SortedSet;
import java.util.TreeSet;

import static nars.term.atom.Bool.Null;

public final class CommonVariable extends UnnormalizedVariable {

    public final SortedSet<Variable> vars;

    public static CommonVariable common(Variable A, Variable B) {
        int cmp = A.compareTo(B);
        assert(cmp!=0);
        if (cmp > 0) {
            Variable x = A;
            A = B;
            B = x;
        }

        Op op = A.op();

        if (!(A instanceof CommonVariable) && !(B instanceof CommonVariable)) {
            return new CommonVariable(op, ArrayUnenforcedSortedSet.the(A, B));
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
//            try (DynBytes k = RecycledDynBytes.tmpKey()) {
//                assert (k.len == 0);
//                for (Variable vv : v)
//                    k.write(vv.bytes());
//                byte[] key = k.arrayCopy();
//                return new CommonVariable(op, v, key);
//            }
             return new CommonVariable(op, v);
        }
    }

    CommonVariable(/*@NotNull*/ Op type, SortedSet<Variable> vars) {
        super(type, key(type, vars));
        this.vars = vars;
                //commonVariableKey(type, x, y) /* include trailing so that if a common variable gets re-commonalized, it wont become confused with repeats in an adjacent variable */);
    }

    public static Term parse(Object... cv) {
        if (cv.length < 2 || cv.length > Param.COMMON_VAR_MAX)
            return Null;

        SortedSet<Variable> s = new TreeSet();
        for (int i = 0; i < cv.length; i++)
            s.add((Variable)cv[i]);
        if (s.size() < 2 || s.size() > Param.COMMON_VAR_MAX)
            return Null;

        Op o = s.first().op();
        return new CommonVariable(o, s);
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
        return key(op(), vars);
    }


    static String key(Op o, SortedSet<Variable> vars) {
        return o + Joiner.on("").join(vars);
    }

    public static boolean unify(Variable X, Variable Y, Unify u) {

        //same op: common variable
        //TODO may be possible to "insert" the common variable between these and whatever result already exists, if only one in either X or Y's slot
        Variable common = CommonVariable.common(X,Y);
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
        return u.putXY(X, common) && u.putXY(Y, common);
        //}
    }

    @Override
    public int opX() {
        return Term.opX(op(), 1 /* different from normalized variables with a subOp of 0 */);
    }



}
