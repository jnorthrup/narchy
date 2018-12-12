package nars.subterm;

import jcog.util.ArrayUtils;
import nars.subterm.util.TermMetadata;
import nars.term.Term;

import java.util.Arrays;
import java.util.function.Predicate;

/** assumes that each items in the base subterm are utilized exactly once in the structure, containment, etc.
 * a relaxed variation of this can be created without this assumption */
abstract public class MappedSubterms extends ProxySubterms {

    private boolean normalizedKnown = false, normalized = false;

    public static MappedSubterms the(Term[] target, Subterms base) {
        byte[] m = new byte[target.length];
        for (int i = 0, xLength = target.length; i < xLength; i++)
            m[i] = (byte) base.indexOf(target[i]);
        return new ArrayMappedSubterms(base, m);
    }

    /** make sure to calculate hash code in implementation's constructor */
    protected MappedSubterms(Subterms base) {
        super(base);
    }

    public static MappedSubterms reverse(Subterms x) {
        if (x instanceof ArrayMappedSubterms) {
            ArrayMappedSubterms mx = ((ArrayMappedSubterms) x);
            //TODO test if the array is already perfectly reversed without cloning then just undo
            byte[] r = mx.map.clone();
            ArrayUtils.reverse(r);
            return new ArrayMappedSubterms(mx.ref, r);
        }

        byte[] m = new byte[x.subs()];
        for (byte k = 0, i = (byte) (m.length - 1); i >= 0; i--, k++)
            m[k] = i;
        return new ArrayMappedSubterms(x, m);
    }

    private static final class ArrayMappedSubterms extends MappedSubterms {
        /** TODO even more compact 2-bit, 3-bit etc representations */
        final byte[] map;

        final int hash;

        public ArrayMappedSubterms(Subterms base, byte[] map) {
            super(base);
            assert(base.subs()==map.length);
            this.map = map;
            this.hash = Subterms.hash(this);
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
        public int subs() {
            return map.length;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj instanceof ArrayMappedSubterms) {
                ArrayMappedSubterms m = (ArrayMappedSubterms) obj;
                return hash == m.hash && ref.equals(m.ref) && Arrays.equals(map, m.map);
            }
            return ((Subterms)obj).equalTerms(this);
        }

        @Override
        public Subterms reversed() {
            byte[] r = map.clone();
            ArrayUtils.reverse(r);
            if (Arrays.equals(r, map)) //palindrome?
                return this;
            return new ArrayMappedSubterms(ref, r);
        }
    }

    @Override
    public boolean contains(Term t) {
        return ref.contains(t);
    }

    @Override
    public boolean containsRecursively(Term x, boolean root, Predicate<Term> subTermOf) {
        return ref.containsRecursively(x, root, subTermOf);
    }

    @Override
    public boolean has(int structuralVector, boolean anyOrAll) {
        return ref.has(structuralVector,anyOrAll);
    }

    @Override
    public int vars() {
        return ref.vars();
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
    public int varPattern() {
        return ref.varPattern();
    }

    @Override
    public int varQuery() {
        return ref.varQuery();
    }


    @Override
    public boolean hasXternal() {
        return ref.hasXternal();
    }

    @Override
    public int structure() {
        return ref.structure();
    }

    @Override
    public boolean these() {
        return ref.these();
    }

    @Override
    public boolean isNormalized() {
        if (!normalizedKnown && !normalized) {
            normalized = TermMetadata.preNormalize(this);
            normalizedKnown = true;
        }
        return normalized;
    }

    @Override
    public void setNormalized() {
        this.normalizedKnown = this.normalized = true;
    }

    @Override
    public int structureSurface() {
        return ref.structureSurface();
    }

    @Override
    public int volume() {
        return ref.volume();
    }

    @Override
    public int complexity() {
        return ref.complexity();
    }


    @Override
    public String toString() {
        return Subterms.toString(this);
    }

    @Override
    public int subs() {
        return ref.subs();
    }
    @Override
    public final Term sub(int i) {
        return ref.sub(subMap(i));
    }

    protected abstract int subMap(int i);
}
