package nars.term.var;

import jcog.WTF;
import jcog.data.set.MetalTreeSet;
import jcog.util.ArrayUtil;
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
import java.util.StringJoiner;
import java.util.stream.Collectors;

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
            z = new IntrinSubterms(Terms.commute(A, B));
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

        MetalTreeSet<Variable> s = Arrays.stream(cv).map(o -> (Variable) o).collect(Collectors.toCollection(MetalTreeSet::new));

        int ss = s.size();
        if (ss < 2 || ss > NAL.unify.UNIFY_COMMON_VAR_MAX)
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

    static String key(Op o, IntrinSubterms vars) {
        StringJoiner joiner = new StringJoiner("", String.valueOf(o.ch), "");
        int n = vars.subs();
        for (int i = 0; i < n; i++)
            joiner.add(String.valueOf(vars.sub(i)));

        return joiner.toString();
    }

    public static boolean unify(Variable X, Variable Y, Unify u) {
        //same op: common variable

        if (X instanceof CommonVariable) {
            if (((CommonVariable)X).includes(Y))
                return true;
        }
        if (Y instanceof CommonVariable) {
            if (((CommonVariable)Y).includes(X))
                return true;
        }

        Variable common = CommonVariable.common(X,Y);
        return (common.equals(X) || u.putXY(X, common)) && (common.equals(Y) || u.putXY(Y, common));
    }

    @Override
    public boolean unify(Term y, Unify u) {
        return includes(y) || super.unify(y, u);
    }

    /** includes but doesnt equal */
    public boolean includes(Term v) {
        return v instanceof Variable && v.opID()==opID() && !this.equals(v) && _includes(v);
    }

    private boolean _includes(Term v) {
        return ArrayUtil.contains(this.vars, Intrin.id(v));
    }

}
