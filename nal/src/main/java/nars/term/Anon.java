package nars.term;

import jcog.Util;
import jcog.list.FasterList;
import jcog.memoize.LinkedMRUMemoize;
import nars.Op;
import nars.Task;
import nars.The;
import nars.term.atom.Atomic;
import nars.term.atom.AtomicConst;
import nars.term.atom.Bool;
import nars.term.atom.Int;
import nars.term.compound.CachedCompound;
import nars.term.container.Subterms;
import nars.term.container.TermVector;
import nars.term.transform.CompoundTransform;
import nars.term.var.UnnormalizedVariable;
import nars.term.var.Variable;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.function.primitive.ByteFunction;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static nars.Op.ATOM;
import static nars.Op.NEG;
import static nars.term.Anon.Anom.MAX_ANOM;
import static nars.time.Tense.DTERNAL;

/**
 * term anonymization context, for canonicalization and generification of compounds
 */
public class Anon {


    /* indexed anonymous term */
    static final class Anom extends Int {

        final static int MAX_ANOM = 127;
        final static int ANOM = Term.opX(ATOM, 0);

        Anom(byte i) {
            super(i, AtomicConst.bytes(ATOM, i));
        }

        @Override
        public int opX() {
            return ANOM;
        }

        @Override
        public /**/ Op op() {
            return ATOM;
        }

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public final boolean equals(Object obj) {
            return this == obj;
        }

        //optimized compareTo for frequent Anom->Anom cases
        @Override
        public int compareTo(Termed yy) {
            if (this == yy) return 0;

            Term y = yy.term();
            if (y instanceof Anom) {
                return Integer.compare(id, ((Anom) y).id);
            } else {
                int vc = Integer.compare(y.volume(), this.volume());
                if (vc != 0)
                    return vc;

                int oc = Integer.compare(this.opX(), y.opX());
                assert (oc != 0);
                return oc;
            }
            //return super.compareTo(yy);
        }

        static Anom[] the = Util.map(0, MAX_ANOM, (i) -> new Anom((byte) i), Anom[]::new);

        public static Anom the(int i) {
            return the[i];
        }
    }

    final ObjectByteHashMap<Term> fwd = new ObjectByteHashMap();
    final List<Term> rev = new FasterList<>();

    final ByteFunction<Term> nextUniqueAtom = (Term next) -> {
        int s = rev.size();
        assert (s < MAX_ANOM);
        assert (!(next instanceof Bool));
        rev.add(next);
        return (byte) s;
    };

    abstract static class DirectCompoundTransform implements CompoundTransform {


        @Override
        public @Nullable Term transform(Compound x, Op op, int dt) {
            Op o = x.op();
            if (o == NEG) {
                Term y = x.unneg().transform(this);
                return y == null ? null : y.neg();
            } else if (dt == DTERNAL && !o.commute(DTERNAL, x.subs())) {

                //replace in same positions, avoiding the more detailed term building processes

                return transformDirect(o, x);

            } else {
                return CompoundTransform.super.transform(x, op, dt);
            }
        }

        protected Term transformDirect(Op o, Compound x) {

            Subterms xx = x.subterms();
            Term[] yy = Util.map(this::applyTermOrNull, Term[]::new, xx.arrayShared());

            if (Util.or((Term y) -> y instanceof Bool, yy))
                return null;

            Term z = transformDirect(o, yy);


            Subterms zz = z.subterms();
            if (!zz.isNormalized() && xx.isNormalized()) //propagate normalization, it should still hold
                ((TermVector) zz).setNormalized();


            return z;
        }

        @Nullable
        protected Term transformDirect(Op o, Term[] yy) {
            return The.compound(o, yy);
        }

        @Override
        public final @Nullable Termed apply(Term t) {
            return applyTermOrNull(t);
        }
    }

    ;

    final CompoundTransform PUT = new DirectCompoundTransform() {

//        @Override
//        protected @Nullable Term transformDirect(Op o, Term[] yy) {
//            if (AnomVector.valid(yy)) {
//                return new CachedCompound(o, new AnomVector(yy));
//            } else {
//                return super.transformDirect(o, yy);
//            }
//        }

        @Override
        public @Nullable Term applyTermOrNull(Term t) {
            return put(t); //may be called more directly
        }
    };

    final CompoundTransform GET = new DirectCompoundTransform() {

//        @Override
//        protected Term transformDirect(Op o, Compound x) {
//            Subterms ss = x.subterms();
//            if (ss instanceof AnomVector) {
//                return ((AnomVector)ss).reverse(o,Anon.this);
//            } else {
//                return super.transformDirect(o, x);
//            }
//        }

        @Override
        public @Nullable Term applyTermOrNull(Term t) {
            return _get(t); //may be called more directly
        }
    };

    public void clear() {
        fwd.clear();
        rev.clear();
    }

    public Term put(Term x) {
        if (x instanceof Variable) {
            //assert (!(x instanceof UnnormalizedVariable));
            if (x instanceof UnnormalizedVariable)
                throw new RuntimeException("unnormalized variable in Anon");
            return x; //ignore normalized variables
        } else if (x instanceof Atomic) {
            return Anom.the[fwd.getIfAbsentPutWithKey(x, nextUniqueAtom)];
        } else {
            return x.transform(PUT);
        }
    }

    public Term get(Term x) {
        return _get(x);
    }

    public Term _get(Term x) {
        if (x instanceof Anom) {
            return rev.get(((Anom) x).id); //assume it is an int
        } else if (x instanceof Atomic) {
            return x; //ignore variables, ints
        } else {
            return x.transform(GET);
        }
    }

    public Task put(Task t) {
        Term x = t.term();
        assert (x.isNormalized());
        Term y = put(x);
        if (y == null || y instanceof Bool) {
            put(x); //temporary
            throw new RuntimeException("Anon fail for term: " + t);
        }
        if (y instanceof Compound)
            ((TermVector) y.subterms()).setNormalized();

        return Task.clone(t, y);
    }


    public static class CachingAnon extends Anon {

        final LinkedMRUMemoize<Term, Term> cache;

        public CachingAnon() {
            this(16);
        }

        public CachingAnon(int capacity) {
            cache = new LinkedMRUMemoize.LinkedMRUMemoizeRecurseable<>(super::_get, capacity);
        }

        @Override
        public void clear() {
            super.clear();
            cache.clear();
        }

        @Override
        public Term _get(Term x) {
            if (x instanceof Anom)
                return super._get(x);
            else
                return cache.apply(x);
        }
    }

    /**
     * a vector which consists purely of Anom terms
     */
    public static class AnomVector extends TermVector {

        final byte[] subterms;

        public AnomVector(Term[] s) {
            super(s); //TODO optimize this for certain Anom invariants (ie. no variables etc)
            byte[] t = subterms = new byte[s.length];
            for (int i = 0, sLength = s.length; i < sLength; i++) {
                t[i] = (byte) ((Anom) s[i]).id;
            }

            normalized = true;

        }

        @Override
        public Term sub(int i) {
            return Anom.the[i];
        }

        @Override
        public int subs() {
            return subterms.length;
        }

        @Override
        public boolean contains(Term t) {
            if (t instanceof Anom)
                return ArrayUtils.indexOf(subterms, (byte) (((Anom) t).id)) != -1;
            else
                return super.contains(t);
        }

        @Override
        public boolean containsRecursively(Term t) {
            return contains(t); //since it will be flat
        }

        @Override
        public Iterator<Term> iterator() {
            return new AnomArrayIterator(subterms);
        }

        public static boolean valid(Term[] xx) {
            return Util.and(Anom.class::isInstance, xx);
        }


        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;

            if (this.hash != obj.hashCode()) return false;

            if (obj instanceof Subterms) {

                if (obj instanceof AnomVector) {
                    return Arrays.equals(subterms, ((AnomVector) obj).subterms);
                } else {
                    Subterms ss = (Subterms)obj;
                    int s = subterms.length;
                    if (ss.subs()!=s)
                        return false;
                    for (int i = 0; i < s; i++) {
                        Term y = ss.sub(i);
                        if (!(y instanceof Anom) || !sub(i).equals(y))
                            return false;
                    }
                    return true;
                }
            }
            return false;
        }

        public Term reverse(Op o, Anon anon) {
            int n = subterms.length;
            Term[] yy = new Term[n];
            for (int i = 0; i < n; i++)
                yy[i] = anon.rev.get(subterms[i]);
            return new CachedCompound(o, TermVector.the(yy));
        }
    }

    private static class AnomArrayIterator implements Iterator<Term> {

        private int current;
        private final byte[] values;

        public AnomArrayIterator(byte[] subterms) {
            this.values = subterms;
        }

        @Override
        public boolean hasNext() {
            return this.current < values.length;
        }

        @Override
        public Term next() {
            return Anom.the[values[this.current++]];
        }

    }

}
