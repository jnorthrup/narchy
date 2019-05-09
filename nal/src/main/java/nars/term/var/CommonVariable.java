package nars.term.var;

import com.google.common.base.Joiner;
import jcog.WTF;
import nars.NAL;
import nars.Op;
import nars.subterm.IntrinSubterms;
import nars.term.Term;
import nars.term.Terms;
import nars.term.Variable;
import nars.term.anon.Intrin;
import nars.unify.Unify;
import org.eclipse.collections.impl.set.mutable.primitive.ShortHashSet;

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

import static nars.term.atom.Bool.Null;

public final class CommonVariable extends UnnormalizedVariable {

    /** provided by a sorted AnonVector */
    @Deprecated private final short[] vars; //TODO compute dynamically from bytes()

    public static Variable common(Variable A, Variable B) {
        int cmp = A.compareTo(B);
        assert(cmp!=0);
        if (cmp > 0) {
            Variable x = A;
            A = B;
            B = x;
        }

        Op op = A.op();

        IntrinSubterms z;
        boolean ac = A instanceof CommonVariable;
        boolean bc = B instanceof CommonVariable;
        if (!ac && !bc) {
            z = new IntrinSubterms(Terms.sorted(A, B));
        } else {
            ShortHashSet t = new ShortHashSet();

            if (ac && bc) {
                t.addAll(((CommonVariable) A).vars);
                t.addAll(((CommonVariable) B).vars);
            } else {

                CommonVariable C;
                Variable V;
                if (ac) {
                    C = ((CommonVariable) A);
                    V = B;
                } else {
                    C = ((CommonVariable) B);
                    V = A;
                }

                t.addAll(C.vars);

                if (!t.add(Intrin.id(V)))
                    return C; //subsumed
            }

            if (t.size() <= 2)
                throw new WTF();

            short[] tt = t.toSortedArray();

            if (ac && Arrays.equals(tt, ((CommonVariable)A).vars))
                return A; //subsumed
            if (bc && Arrays.equals(tt, ((CommonVariable)B).vars))
                return B; //subsumed

            z = new IntrinSubterms(tt);

        }
        return new CommonVariable(op, z);
    }

    /** vars must be sorted */
    private CommonVariable(/*@NotNull*/ Op type, IntrinSubterms vars) {
        super(type, key(type, vars));
//        if (NAL.DEBUG) {
//            for (Term t : vars)
//                if (!t.the()) throw new WTF();
//        }
        this.vars = vars.subterms;
                //commonVariableKey(type, x, y) /* include trailing so that if a common variable gets re-commonalized, it wont become confused with repeats in an adjacent variable */);
    }

    public static Term parse(Object... cv) {
        if (cv.length < 2 || cv.length > NAL.unify.UNIFY_COMMON_VAR_MAX)
            return Null;

        SortedSet<Variable> s = new TreeSet();
        for (Object o : cv) s.add((Variable) o);
        if (s.size() < 2 || s.size() > NAL.unify.UNIFY_COMMON_VAR_MAX)
            return Null;

        return new CommonVariable(s.first().op(), new IntrinSubterms(s.toArray(Op.EmptyTermArray)));
    }

    @Override
    public boolean the() {
//        for (Term v : vars)
//            if (!v.the())
//                return false;
        return true;
    }

    @Override
    public String toString() {
        return key(op(), common());
    }

    public IntrinSubterms common() {
        return new IntrinSubterms(vars);
    }

    static String key(Op o, Iterable<Term> vars) {
        return o + Joiner.on("").join(vars);
    }
//    static String key(Op o, Term[] vars) {
//        return o + Joiner.on("").join(vars);
//    }

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

}
