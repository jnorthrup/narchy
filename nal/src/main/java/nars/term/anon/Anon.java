package nars.term.anon;

import jcog.data.list.FasterList;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Variable;
import nars.term.atom.Atomic;
import nars.term.util.ByteAnonMap;
import nars.term.util.TermBuilder;
import nars.term.util.builder.InterningTermBuilder;
import nars.term.util.transform.TermTransform;
import nars.term.var.ImDep;
import nars.term.var.NormalizedVariable;
import nars.term.var.UnnormalizedVariable;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.jetbrains.annotations.Nullable;

/**
 * term anonymization context, for canonicalization and generification of compounds
 * //return new DirectTermTransform() {
 * //return new TermTransform.NegObliviousTermTransform() {
 */
public class Anon extends TermTransform.NegObliviousTermTransform {

    final static TermBuilder localBuilder =
            new InterningTermBuilder(Anon.class.getSimpleName(), 8*1024);
            //HeapTermBuilder.the;

    final static TermBuilder globalBuilder = Op.terms;

    final ByteAnonMap map;

    @Override
    public Term the(Op op, int dt, Term[] t) {
        return (putOrGet ? localBuilder : globalBuilder).compound(op, dt, t);
    }

    @Override
    public Term the(Op op, int dt, Subterms t) {
        return (putOrGet ? localBuilder : globalBuilder).theCompound(op, dt, t.arrayShared());
    }

    private boolean putOrGet = true;

    public Anon() {
        this(1);
    }

    public Anon(int estSize) {
        this.map = new ByteAnonMap(estSize);
    }

    public int uniques() {
        return map.size();
    }

    /**
     * returns true if anything changed
     */
    public boolean rollback(int toUniques) {
        if (toUniques == 0) {
            map.clear();
            return true;
        }

        int max;
        if (toUniques < (max = uniques())) {
            ObjectByteHashMap<Term> termToId = map.termToId;
            FasterList<Term> idToTerm = map.idToTerm;
            for (int i = toUniques; i < max; i++)
                termToId.removeKey(idToTerm.get(i));
            idToTerm.removeAbove(toUniques);
            return true;
        }
        return false;
    }

    @Override
    public final @Nullable Term transformAtomic(Atomic atomic) {
        return putOrGet ? put(atomic) : get(atomic);
    }

    public final Term put(Term x) {

        if (x instanceof Atomic) {

            if (x instanceof AnonID) {
                return putAnon(x);
            }

            if (x instanceof UnnormalizedVariable) {
                return x; //HACK
            }

            return Anom.the[map.intern(x)];

        } else {
            return putCompound((Compound) x);
        }
    }

    /** anon filter in which subclasses can implement variable shifting */
    protected Term putAnon(Term x) {
        return x;
    }




    public final Term get(Term x) {
        if (x instanceof Compound) {
            return getCompound((Compound) x);
        } else {
            if (x instanceof Anom) {
                return map.interned((byte) ((Anom) x).id);
            } else {
                return x;
            }
        }
    }

    protected final Term getCompound(Compound y) {
        putOrGet = false;
        Term x = transformCompound(y);
//        validate(y, x, false);
        return x;
    }

    protected final Term putCompound(Compound x) {
        putOrGet = true;

        Term y = transformCompound(x);

//        validate(x, y, true);
        return y;
    }

    public void clear() {
        map.clear();
    }

    public static class AnonWithVarShift extends Anon {
        int indepShift = 0, depShift = 0, queryShift = 0;

        public AnonWithVarShift(int cap) {
            super(cap);
        }

        @Override
        protected Term putAnon(Term x) {
            if (x instanceof Variable && !(x instanceof ImDep)) {
                NormalizedVariable v = ((NormalizedVariable) x);
                int shift;
                Op vv = v.op();
                switch (vv) {
                    case VAR_DEP: shift = depShift; break;
                    case VAR_INDEP: shift = indepShift; break;
                    case VAR_QUERY: shift = queryShift; break;
                    default:
                        throw new UnsupportedOperationException();
                }
                if (shift!=0) {
                    int newID = v.anonNum() + shift;
                    assert(newID < Byte.MAX_VALUE-3); //to be safe
                    x = v.normalize((byte) newID);
                }
            }
            return super.putAnon(x);
        }

        public AnonWithVarShift unshift() {
            indepShift = depShift = queryShift = 0;
            return this;
        }

        public AnonWithVarShift shift(Term base) {
            if (!base.hasVars())
                return this;
            else
                return shift(0 /*base.varIndep()*/, base.varDep(), base.varQuery());
        }

        public AnonWithVarShift shift(int indep, int dep, int query) {
            indepShift = indep;
            depShift = dep;
            queryShift = query;
            return this;
        }

        public Term putShift(Term x, Term base) {
            //TODO only shift if the variable bits overlap, but if disjoint not necessary
            return ((x.hasVars() && base.hasVars()) ? shift(base) : this).put(x);
        }
    }
}
//    private static final AtomicBoolean validateLock = new AtomicBoolean();
//
//    void validate(Term x, Term y, boolean putOrGet) {
//
//        if (Param.DEBUG) {
//            if (!validateLock.compareAndSet(false, true))
//                return;
//            try {
//
////            if (termToId.isEmpty() || idToTerm.isEmpty())
////                throw new WTF("termToId is empty: " + x + " -> " + y);
//
//                if (y.op() != x.op())
//                    throw new WTF("anon changed op: " + x + " -> " + y);
//                if (y.volume() != x.volume())
//                    throw new WTF("anon changed vol: " + x + " -> " + y + " <- " + get(y));
//
//
////            if (putOrGet) {
////                Term z = get(y);
////                if (!z.equals(x)) {
////                    /* temporary for debug: */ get(y);
////                    throw new WTF("invalid put:\n\t" + x + "\n\t" + y + "\n\t" + z);
////                }
////            } else {
////                Term z = put(y);
////                if (!z.equals(x))
////                    throw new WTF("invalid get:\n\t" + x + "\n\t" + y + "\n\t" + z);
////
////            }
//            } finally {
//                validateLock.set(false);
//            }
//        }
//    }
