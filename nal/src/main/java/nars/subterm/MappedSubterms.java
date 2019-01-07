package nars.subterm;

import com.google.common.io.ByteArrayDataOutput;
import jcog.WTF;
import jcog.util.ArrayUtils;
import nars.Op;
import nars.subterm.util.TermMetadata;
import nars.term.Compound;
import nars.term.Term;

import java.util.Arrays;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static nars.Op.NEG;

/** assumes that each items in the base subterm are utilized exactly once in the structure, containment, etc.
 * a relaxed variation of this can be created without this assumption
 * TODO separate into 2 abstract subclasses: Direct and Negating
 * ShuffledSubterms will inherit from Direct, otherwise generally use Negating
 * */
abstract public class MappedSubterms extends ProxySubterms {

    private boolean normalizedKnown = false, normalized = false;

    public static MappedSubterms the(Term[] target, Subterms base) {
        byte[] m = new byte[target.length];
        for (int i = 0, xLength = target.length; i < xLength; i++) {
            Term xi = target[i];
            boolean neg = (xi.op()==NEG);
            if (neg) xi = xi.unneg();

            int mi = base.indexOf(xi)+1;
            if (mi <= 0)
                throw new WTF(xi + "not found in " + base);

            m[i] = (byte) (neg ? -mi : mi);
        }
        return new ArrayMappedSubterms(base, m, Subterms.hash(target));
    }

    /** make sure to calculate hash code in implementation's constructor */
    protected MappedSubterms(Subterms base) {
        super(base);
    }

    public static Subterms reverse(Subterms x) {
        int n = x.subs();
        if (n <= 1)
            return x;

        if (x instanceof ArrayMappedSubterms) {
            ArrayMappedSubterms mx = ((ArrayMappedSubterms) x);
            //TODO test if the array is already perfectly reversed without cloning then just undo
            byte[] r = mx.map.clone();
            ArrayUtils.reverse(r);
            if (Arrays.equals(mx.map, r))
                return x; //palindrome or repeats

            return new ArrayMappedSubterms(mx.ref, r);
        } else {
            byte[] m = new byte[n];
            for (byte k = 0, i = (byte) (m.length - 1); i >= 0; i--, k++)
                m[k] = (byte) (i + 1);
            return new ArrayMappedSubterms(x, m);
        }
    }

    private static final class ArrayMappedSubterms extends MappedSubterms {
        /** TODO even more compact 2-bit, 3-bit etc representations */
        final byte[] map;

        final int hash;
        boolean hasNegs;

        private ArrayMappedSubterms(Subterms base, byte[] map) {
            super(base); assert(base.subs()==map.length);
            this.map = map;
            this.hasNegs = super.hasNegs();
            this.hash = Subterms.hash(this);
        }

        private ArrayMappedSubterms(Subterms base, byte[] map, int hash) {
            super(base); assert(base.subs()==map.length);
            this.map = map;
            this.hash = hash;
        }

        @Override
        protected boolean hasNegs() {
            return hasNegs;
        }

        /** @see AnonVector.appendTo */
        @Override
        public void appendTo(ByteArrayDataOutput out) {
            byte[] ss = map;
            out.writeByte(ss.length);
            for (byte s : ss) {
                if (s < 0) {
                    out.writeByte(Op.NEG.id);
                    s = (byte) -s;
                }
                ref.sub(s-1).appendTo(out);
            }
        }

        @Override
        protected int subMap(int i) {
            return map[i];
        }

        @Override
        public int hashCodeSubterms() {
            return hash;
        }

        @Override
        public int hashCode() {
            return hash;
        }


        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj instanceof ArrayMappedSubterms) {
                ArrayMappedSubterms m = (ArrayMappedSubterms) obj;
                return hash == m.hash && ref.equals(m.ref) && Arrays.equals(map, m.map);
            } else {
                return obj instanceof Subterms && hash == obj.hashCode() && ((Subterms) obj).equalTerms(this);
            }
        }

        @Override
        public int subs() {
            return map.length;
        }

    }

    @Override
    public boolean recurseTerms(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, Compound parent) {
        return !hasNegs() ?
                ref.recurseTerms(inSuperCompound, whileTrue, parent)
                :
                super.recurseTerms(inSuperCompound, whileTrue, parent);
    }

    @Override
    public boolean recurseTerms(Predicate<Compound> aSuperCompoundMust, BiPredicate<Term, Compound> whileTrue, Compound parent) {
        return !hasNegs() ?
                ref.recurseTerms(aSuperCompoundMust, whileTrue, parent)
                :
                super.recurseTerms(aSuperCompoundMust, whileTrue, parent);
    }

    @Override
    public boolean contains(Term t) {
        return !hasNegs() ? ref.contains(t) : super.contains(t);
    }

    @Override
    public boolean containsRecursively(Term x, boolean root, Predicate<Term> subTermOf) {
        return (!hasNegs() || x.op()!=NEG) ? ref.containsRecursively(x, root, subTermOf) :
                super.containsRecursively(x, root, subTermOf); //exhaustive
    }

    @Override
    public boolean has(int structuralVector, boolean anyOrAll) {
        return ((structuralVector & Op.NEG.bit) == 0 || !hasNegs()) ? ref.has(structuralVector,anyOrAll) :
                super.has(structuralVector, anyOrAll);//exhaustive
    }


    @Override
    public int subs() {
        return ref.subs();
    }

    @Override
    public boolean hasXternal() {
        return ref.hasXternal();
    }

    @Override
    public int structure() {
        int s = ref.structure();
        return ((s & Op.NEG.bit) != 0) ? s :
                s | (hasNegs() ? NEG.bit : 0);
    }

    @Override
    public int structureSurface() {
        return !hasNegs() ? ref.structureSurface() : super.structureSurface();
    }

    @Override
    public boolean these() {
        return ref.these();
    }

    @Override
    public boolean isNormalized() {
        if (!normalizedKnown && !normalized) {
            normalized = TermMetadata.normalized(this);
            normalizedKnown = true;
        }
        return normalized;
    }

    @Override
    public void setNormalized() {
        this.normalizedKnown = this.normalized = true;
    }

    protected boolean hasNegs() {
        int s = subs();
        for (int i = 0; i < s; i++)
            if (subMap(i) < 0)
                return true;
        return false;
    }

    protected int negs() {
        int s = subs();
        int n = 0;
        for (int i = 0; i < s; i++)
            if (subMap(i) < 0)
                n++;
        return n;
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
    public String toString() {
        return Subterms.toString(this);
    }


    @Override
    public Term sub(int i) {
        int xy = subMap(i);
        boolean neg = (xy < 0);
        if (neg) xy = -xy;
        Term y  = ref.sub(xy-1);
        return neg ? y.neg() : y;
    }

    protected abstract int subMap(int i);


//TODO these are only valid if the map contains no repeats and uses all the subterms of ref
//    @Override
//    public int vars() {
//        return ref.vars();
//    }
//
//    @Override
//    public int varDep() {
//        return ref.varDep();
//    }
//
//    @Override
//    public int varIndep() {
//        return ref.varIndep();
//    }
//
//    @Override
//    public int varPattern() {
//        return ref.varPattern();
//    }
//
//    @Override
//    public int varQuery() {
//        return ref.varQuery();
//    }

}
