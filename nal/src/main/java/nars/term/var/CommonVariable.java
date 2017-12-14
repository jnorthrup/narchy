package nars.term.var;

import nars.$;
import nars.Op;
import nars.term.Term;
import org.eclipse.collections.api.set.primitive.ImmutableByteSet;
import org.eclipse.collections.impl.factory.primitive.ByteSets;

public final class CommonVariable extends UnnormalizedVariable {

    public final ImmutableByteSet vars;

    CommonVariable(/*@NotNull*/ Op type, byte x, byte y) {
        super(type, type.ch + name(x, y));
        if (x == y)
            throw new RuntimeException();
        assert(x!=0 && y!=0);
        this.vars = ByteSets.immutable.of(x, y);
    }

    CommonVariable(/*@NotNull*/ Op type, ImmutableByteSet vars) {
        super(type, type.ch + name(vars));
        this.vars = vars;
    }

    /** simple name generator */
    private static String name(byte x, byte y) {
        return x < y ? x + "_" + y : y + "_" + x;
    }

    private static String name(ImmutableByteSet vars) {
        StringBuilder s = new StringBuilder(vars.size() * 2);
        byte[] bb = vars.toSortedArray();
        for (int i = 0, bbLength = bb.length; i < bbLength; i++) {
            byte b = bb[i];
            s.append(b);
            if (i!=bbLength-1)
                s.append('_');
        }
        return s.toString();
    }


    @Override
    public int opX() {
        return Term.opX(op(), 1 /* different from normalized variables with a subOp of 0 */);
    }

    public static Variable common(Variable A, Variable B) {

        //1. sort
        if (A.compareTo(B) < 0) {
            Variable c = B;
            B = A;
            A = c;
        }

        Op Aop = A.op();
        assert(B.op()==Aop);

        boolean aa = A instanceof AbstractVariable;
        boolean bb = B instanceof AbstractVariable;
        if (aa && bb) {
            byte ai = ((AbstractVariable)A).anonNum();
            byte bi = ((AbstractVariable)B).anonNum();
            return new CommonVariable(Aop, ai, bi);
        }

        if (!aa && bb) {
            ImmutableByteSet ai = ((CommonVariable)A).vars;
            byte bi = ((AbstractVariable)B).anonNum();
            if (ai.contains(bi))
                return A;
            return new CommonVariable(Aop, ai.newWith(bi));
        }

        if (aa && !bb) {
            byte ai = ((AbstractVariable)A).anonNum();
            ImmutableByteSet bi = ((CommonVariable)B).vars;
            if (bi.contains(ai))
                return B;
            return new CommonVariable(Aop, bi.newWith(ai));
        }

        /*if (!aa && !bb)*/ {
            ImmutableByteSet ai = ((CommonVariable)A).vars;
            ImmutableByteSet bi = ((CommonVariable)B).vars;
            ImmutableByteSet combined = ai.newWithAll(bi);
            if (combined.equals(ai))
                return A;
            return new CommonVariable(Aop, combined);
        }

    }


    //    public boolean common(@NotNull AbstractVariable y) {
//        int yid = y.id;
//
//        int v1 = (hash & 0xff) - 1; //unhash
//        if (v1 == yid)
//            return true;
//
//        int v2 = ((hash >> 8) & 0xff) - 1; //unhash
//        return v2 == yid;
//
//    }


//    //TODO use a 2d array not an enum map, just flatten the 4 op types to 0,1,2,3
//    /** variables x 10 (digits) x (1..10) (digits) cache;
//     *  triangular matrix because the pairs is sorted */
//    static final EnumMap<Op,CommonVariable[][]> common = new EnumMap(Op.class);
//    static {
//        for (Op o : new Op[] { Op.VAR_PATTERN, Op.VAR_QUERY, Op.VAR_INDEP, Op.VAR_DEP}) {
//            CommonVariable[][] cm = new CommonVariable[10][];
//            for (int i = 0; i < 10; i++) {
//                cm[i] = new CommonVariable[i+2];
//            }
//            common.put(o, cm);
//        }
//    }


}
