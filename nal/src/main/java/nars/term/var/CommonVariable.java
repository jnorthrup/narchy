package nars.term.var;

import com.google.common.base.Joiner;
import jcog.WTF;
import nars.Op;
import nars.Param;
import nars.subterm.AnonVector;
import nars.term.Term;
import nars.term.Terms;
import nars.term.Variable;
import nars.unify.Unify;

import java.util.SortedSet;
import java.util.TreeSet;

import static nars.term.atom.Bool.Null;

public final class CommonVariable extends UnnormalizedVariable {

    /** stored sorted */
    public final AnonVector vars;

    public static Variable common(Variable A, Variable B) {
        int cmp = A.compareTo(B);
        assert(cmp!=0);
        if (cmp > 0) {
            Variable x = A;
            A = B;
            B = x;
        }

        Op op = A.op();

        AnonVector z;
        boolean ac = A instanceof CommonVariable;
        boolean bc = B instanceof CommonVariable;
        if (!ac && !bc) {
            z = new AnonVector(Terms.sorted(A, B));
        } else {
            TreeSet<Term> t = new TreeSet<Term>();

            if (ac && bc) {
                ((CommonVariable) A).vars.forEach(t::add);
                ((CommonVariable) B).vars.forEach(t::add);
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
                C.vars.forEach(t::add);
                if (!t.add(V))
                    return C; //subsumed
            }

            if (t.size() <= 2)
                throw new WTF();

            z = new AnonVector(t.toArray(Op.EmptyTermArray));

            if (ac && ((CommonVariable)A).vars.equals(z))
                return A; //subsumed
            if (bc && ((CommonVariable)B).vars.equals(z))
                return B; //subsumed

        }
        return new CommonVariable(op, z);
    }

    /** vars must be sorted */
    private CommonVariable(/*@NotNull*/ Op type, AnonVector vars) {
        super(type, key(type, vars));
        if (Param.DEBUG) {
            for (Term t : vars)
                if (!t.the()) throw new WTF();
        }
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

        return new CommonVariable(s.first().op(), new AnonVector(s.toArray(Op.EmptyTermArray)));
    }

    @Override
    public boolean the() {
        for (Term v : vars)
            if (!v.the())
                return false;
        return true;
    }

    @Override
    public String toString() {
        return key(op(), vars);
    }


    static String key(Op o, Iterable<Term> vars) {
        return o + Joiner.on("").join(vars);
    }
    static String key(Op o, Term[] vars) {
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
