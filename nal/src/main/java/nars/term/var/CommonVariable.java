package nars.term.var;

import jcog.WTF;
import nars.Op;
import nars.Param;
import nars.The;
import nars.subterm.AnonVector;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.Terms;
import nars.term.Variable;
import nars.term.anon.AnonID;
import nars.unify.Unify;
import org.eclipse.collections.impl.set.mutable.primitive.ShortHashSet;

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

import static nars.term.atom.Bool.Null;

public final class CommonVariable extends UnnormalizedVariable implements The {

    ///** provided by a sorted AnonVector */
    //@Deprecated private final short[] vars; //TODO compute dynamically from bytes()

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
            ShortHashSet t = new ShortHashSet();

            if (ac && bc) {
                t.addAll(vars((CommonVariable) A));
                t.addAll(vars((CommonVariable) B));
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

                t.addAll(vars(C));

                if (!t.add(AnonID.id(V)))
                    return C; //subsumed
            }

            if (t.size() <= 2)
                throw new WTF();

            short[] tt = t.toSortedArray();

            if (ac && Arrays.equals(tt, vars((CommonVariable)A)))
                return A; //subsumed
            if (bc && Arrays.equals(tt, vars((CommonVariable)B)))
                return B; //subsumed

            z = new AnonVector(tt);

        }
        return new CommonVariable(op, z);
    }

    /** decode common variable short[] from the Atomic.bytes() */
    private static short[] vars(CommonVariable a) {
        int o = 3; //skip: special byte, op, string length
        byte[] x = a.bytes();
        short[] y = new short[(x.length-o)/2];
        int i = 0;
        for (; i < y.length; ) {
            y[i++] = (short) ((x[o++] << 8) | (x[o++]));
        }
        return y;
    }

    /** vars must be sorted */
    private CommonVariable(/*@NotNull*/ Op type, AnonVector vars) {
        super(type, key(type, vars));

        if (Param.DEBUG_EXTRA) {
            for (Term t : vars)
                if (!t.the()) throw new WTF();
            assert(Arrays.equals(vars.subterms, vars(this))); //TEMPORARY
        }
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
//        for (Term v : vars)
//            if (!v.the())
//                return false;
        return true;
    }

    @Override
    public String toString() {
        return key(op(), this.variables());
    }

    Subterms variables() {
        return new AnonVector(vars(this));
    }

    static String key(Op o, Subterms vars) {
        byte[] k = new byte[1 + vars.subs()*2];
        k[0] = o.id;
        int i = 0;
        for (Term v : vars) {
            short a = ((AnonID)v).anonID;
            k[i++] = (byte) (a >> 8);
            k[i++] = (byte) (a & 0xff);
        }
        return new String(k);
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

    @Override
    public int opX() {
        return Term.opX(op(), 1 /* different from normalized variables with a subOp of 0 */);
    }



}
