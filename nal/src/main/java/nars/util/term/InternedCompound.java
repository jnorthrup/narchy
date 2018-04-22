package nars.util.term;

import com.google.common.io.ByteArrayDataOutput;
import jcog.data.byt.HashCachedBytes;
import jcog.memoize.HijackMemoize;
import jcog.pri.AbstractPLink;
import nars.Op;
import nars.term.Term;

import java.util.Arrays;

public final class InternedCompound extends AbstractPLink<Term> implements HijackMemoize.Computation<InternedCompound, Term> {
    //X
    public final Op op;
    public final int dt;
    private final int hash;

    //public Term[] subs;
    final byte[] subs;

    private transient Term[] rawSubs;

    //Y
    public Term y = null;

    public InternedCompound(Op o, int dt, Term... subs) {
        super();
        this.op = o;
        this.dt = dt;
        this.rawSubs = subs;

        HashCachedBytes key = new HashCachedBytes(4 * subs.length);
        key.writeByte(o.id);
        key.writeInt(dt);
        for (Term s : subs)
            s.append((ByteArrayDataOutput) key);

        this.subs = key.array();
        this.hash = key.hashCode();
    }

    @Override
    public Term get() {
        return y;
    }

    //        /**
//         * if accepted into the interner, it can call this with a resolver function to fully intern this
//         * by resolving the subterm values which are now present in the index
//         */
//        public void compact(Function<InternedCompound, Term> intern) {
////            for (int i = 0, subsLength = subs.length; i < subsLength; i++) {
////                Term x = subs[i];
////                if (x instanceof Compound) {
////                    Term y = intern.apply(key(x));
////                    if (y != null && y != x) {
////                        subs[i] = y;
////                    }
////                }
////            }
//        }

//        private InternedCompound key(Term x) {
//            return new InternedCompound(x.op(), x.subterms().arrayShared());
//        }


    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        //op == p.op && dt == p.dt &&
        InternedCompound p = (InternedCompound) obj;
        return hash == p.hash && Arrays.equals(subs, p.subs);
    }

    public float value() {
        return 0.5f;
//            float f;
//            switch (dt) {
//                case DTERNAL:
//                    f = 1f;
//                    break;
//                case XTERNAL:
//                case 0:
//                    f = 0.75f;
//                    break;
//                default:
//                    f = 0.25f;
//                    break;
//            }
//            return f;
        //return f / (1 + subs.length / 10f); //simple policy: prefer shorter
    }

    @Override
    public InternedCompound x() {
        return this;
    }

    public void set(Term y) {
        this.y = y;

//            //HACK extended interning
//            int n = subs.length;
//            if (y != null && y.subs() == n) {
//                if (n > 1) {
//                    Subterms ys = y.subterms();
//                    if (ys instanceof TermVector) {
//                        Term[] yy = ys.arrayShared();
//                        if (subs != yy && Arrays.equals(subs, yy)) {
//                            subs = yy;
//                        }
//                    }
//                } else if (n == 1) {
//                    Term y0 = y.sub(0);
//                    Term s0 = subs[0];
//                    if (s0 != y0 && s0.equals(y0))
//                        subs[0] = y0;
//                }
//            }
    }

    public Term compute() {
        Term[] rawSubs = this.rawSubs;
        this.rawSubs = null;
        return op.instance(dt, rawSubs);
    }

}
