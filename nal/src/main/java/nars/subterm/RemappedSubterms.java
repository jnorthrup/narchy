package nars.subterm;

import jcog.Util;
import jcog.WTF;
import jcog.util.ArrayUtil;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Term;

import java.util.Arrays;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import static nars.Op.NEG;

/** assumes that each items in the base subterm are utilized exactly once in the structure, containment, etc.
 * a relaxed variation of this can be created without this assumption (vars() etc will need calculated, not ref.___ shortcut)
 * TODO separate into 2 abstract subclasses: Direct and Negating
 * ShuffledSubterms will inherit from Direct, otherwise generally use Negating
 * */
public abstract class RemappedSubterms<S extends Subterms> extends MappedSubterms<S> {


    public static <S extends Subterms> ArrayRemappedSubterms<S> the(Term[] x, S base) {

        var m = new byte[x.length];

        var hash = 1;
        for (int i = 0, xLength = x.length; i < xLength; i++) {
            var xx = x[i];
            hash = Util.hashCombine(hash, xx);

            var neg = (xx instanceof Neg);

            var xi = neg ? xx.unneg() : xx;

            var mi = base.indexOf(xi)+1;

            if (mi <= 0)
                throw new WTF
                    //TermException
                    (
                    xi + " not found in " + base + ", base.class=" + base.getClass() + " target.xi.class=" + xi.getClass());

            m[i] = (byte) (neg ? -mi : mi);
        }

        //int hash = Subterms.hash(target);

        return new ArrayRemappedSubterms<>(base, m, hash);
    }


    /** make sure to calculate hash code in implementation's constructor */
    RemappedSubterms(S base) {
        super(base);
    }

    public static Subterms reverse(Subterms x) {
        var n = x.subs();
        if (n <= 1)
            return x;

        if (x instanceof ReversedSubterms) {
            //un-reverse
            return ((ReversedSubterms)x).ref;
        }

        if (x instanceof RemappedSubterms.ArrayRemappedSubterms) {
            ArrayRemappedSubterms<?> mx = ((ArrayRemappedSubterms) x);
            //TODO test if the array is already perfectly reversed without cloning then just undo
            byte[] q = mx.map, r;

            //palindrome or repeats?
            if ((q.length==2 && q[0]==q[1]) || (q.length==3 && q[0]==q[2]) || (q.length==4 && q[0] == q[3] && q[1]==q[2]) /* ... */) {
                //obvious palindrome/repeats
                return x;
            } else {
                r = q.clone();
                ArrayUtil.reverse(r);
                if (Arrays.equals(q,r))
                    return x;
            }
            return new ArrayRemappedSubterms(mx.ref, r);
        }
        //else {
//            byte[] m = new byte[n];
//            for (byte k = 0, i = (byte) (m.length - 1); i >= 0; i--, k++)
//                m[k] = (byte) (i + 1);
//            return new ArrayMappedSubterms(x, m);
//        }
        return new ReversedSubterms(x);
    }

    public abstract static class HashCachedRemappedSubterms<S extends Subterms> extends RemappedSubterms<S> {

        /**
         * make sure to calculate hash code in implementation's constructor
         *
         * @param base
         */
        int hash;

        protected HashCachedRemappedSubterms(S base) {
            super(base);
        }

        @Override
        public final int hashCode() {
            return hash;
        }

        @Override
        public final int hashCodeSubterms() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            return (this==obj) || (obj instanceof Subterms && hash == ((Subterms)obj).hashCodeSubterms() && ((Subterms) obj).equalTerms(this));
        }
    }


    /** TODO */
    abstract static class NegatedSubterms extends HashCachedRemappedSubterms {

        protected NegatedSubterms(Subterms base) {
            super(base);
        }
    }

    public static final class RepeatedSubterms<T extends Term> extends MappedSubterms<T> {

        final int n;
        private final int hash;

        public RepeatedSubterms(T base, int n) {
            super(base);
            this.n = n;
            this.hash = Subterms.hash(this);
        }

        @Override
        public final int hashCode() {
            return hash;
        }

        @Override
        public final int hashCodeSubterms() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            return (this == obj) || (
                    obj instanceof Subterms && hash == ((Subterms)obj).hashCodeSubterms() && ((Subterms) obj).equalTerms(this)
            );
        }

        @Override
        public int volume() {
            return 1 + ref.volume() * n;
        }

        //TODO others

        @Override
        public int indexOf(Term t) {
            return ref.equals(t) ? 0 : -1;
        }

        @Override
        public int indexOf(Term t, int after) {
            return ref.equals(t) ? ((after < n-1) ? after+1 : -1) : -1;
        }

        @Override
        public boolean contains(Term t) {
            return ref.equals(t);
        }

        @Override
        public int vars() {
            return ref.vars() * n;
        }

        @Override
        public int varDep() {
            return ref.varDep() * n;
        }

        @Override
        public int varIndep() {
            return ref.varIndep() * n;
        }

        @Override
        public int varPattern() {
            return ref.varPattern() * n;
        }

        @Override
        public int varQuery() {
            return ref.varQuery() * n;
        }


        @Override
        public int subs() {
            return n;
        }

        @Override
        public Term sub(int i) {
            return ref;
        }

    }

    private static final class ReversedSubterms extends HashCachedRemappedSubterms<Subterms> {

        /** cached */
        private final byte size;

        /**
         * make sure to calculate hash code in implementation's constructor
         *
         * @param base
         */
        protected ReversedSubterms(Subterms base) {
            super(base);
            size = (byte) base.subs();
            assert(size > 1);
            this.hash = Subterms.hash(this);
        }


        @Override
        public Subterms reversed() {
            return ref;
        }

        @Override
        public int subs() {
            return size;
        }

        @Override
        public int subMap(int i) {
            return (size) - i;
        }
    }


    public static final class ArrayRemappedSubterms<S extends Subterms> extends HashCachedRemappedSubterms<S> {
        /** TODO even more compact 2-bit, 3-bit etc representations */
        public final byte[] map;

        final byte negs;

        private ArrayRemappedSubterms(S base, byte[] map) {
            super(base); assert(base.subs()==map.length);
            this.map = ArrayUtil.intern(map);
            this.negs = (byte) super.negs();
            this.hash = Subterms.hash(this);
        }

        private ArrayRemappedSubterms(S base, byte[] map, int hash) {
            super(base); assert(base.subs()==map.length);
            this.map = ArrayUtil.intern(map);
            this.negs = (byte) super.negs();
            this.hash = hash;
        }


        @Override
        protected final boolean wrapsNeg() {
            return negs>0;
        }

        @Override
        protected final int negs() {
            return negs;
        }


        @Override
        public final int subs() {
            return map.length;
        }

        @Override
        public final int subMap(int i) {
            return map[i];
        }


        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj instanceof RemappedSubterms.ArrayRemappedSubterms) {
                var m = (ArrayRemappedSubterms) obj;
                return hash == m.hash && ref.equals(m.ref) && Arrays.equals(map, m.map);
            } else {
                return obj instanceof Subterms && /*hash == obj.hashCodeSubterms() && */
                        ((Subterms) obj).equalTerms(this);
            }
        }


    }

    @Override
    public boolean recurseTerms(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, Compound parent) {
        return !wrapsNeg() ?
                ref.recurseTerms(inSuperCompound, whileTrue, parent)
                :
                super.recurseTerms(inSuperCompound, whileTrue, parent);
    }

    @Override
    public boolean recurseTerms(Predicate<Compound> aSuperCompoundMust, BiPredicate<Term, Compound> whileTrue, Compound parent) {
        return !wrapsNeg() ?
                ref.recurseTerms(aSuperCompoundMust, whileTrue, parent)
                :
                super.recurseTerms(aSuperCompoundMust, whileTrue, parent);
    }

    @Override
    public boolean contains(Term t) {
        return (!wrapsNeg() && t.op()!=NEG) ? ref.contains(t) : super.contains(t);
    }

    @Override
    public int height() {
        return (wrapsNeg() ? 1 : 0) + ref.height();
    }

    /** optimized to avoid wrapping in Neg temporarily */
    @Override public final int subEventRange(int i) {
        var xy = subMap(i);
        if (xy < 0)
            return 0;
        return mapTerm(xy).eventRange();
    }

    @Override
    public abstract int subs();

    @Override
    public final int structure() {
        var s = ref.structure();
        if (wrapsNeg())
            s |= NEG.bit;
        return s;
    }

    @Override
    public final int structureSurface() {
        var s = ref.structureSurface();
        if (wrapsNeg())
            s |= NEG.bit;
        return s;
    }

    protected boolean wrapsNeg() {
        var s = subs();
        return IntStream.range(0, s).anyMatch(i -> subMap(i) < 0);
    }

    protected int negs() {
        return (int) IntStream.range(0, subs()).filter(i -> subMap(i) < 0).count();
    }

    @Override
    public int volume() {
        return ref.volume() + negs();
    }

    @Override
    public int complexity() {
        return ref.complexity() + negs();
    }

    @Override
    public int vars() {
        return ref.vars();
    }

    @Override
    public int varPattern() {
        return ref.varPattern();
    }

    @Override
    public int varQuery() {
        return ref.varQuery();
    }

    @Override
    public int varDep() {
        return ref.varDep();
    }

    @Override
    public int varIndep() {
        return ref.varIndep();
    }


    @Override
    public Term sub(int i) {
        return mapTerm(subMap(i));
    }

    public Term mapTerm(int xy) {
        var neg = (xy < 0);
        if (neg) xy = -xy;
        return mapSub(xy).negIf(neg);
    }

    private Term mapSub(int xy) {
        return ref.sub(xy - 1);
    }

    public abstract int subMap(int i);



}
