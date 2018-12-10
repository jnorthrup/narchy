package nars.subterm;

import jcog.util.ArrayUtils;
import nars.Op;
import nars.subterm.util.TermMetadata;
import nars.term.Term;
import nars.term.Terms;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Predicate;

public class SortedSubterms {

    public static Subterms the(Term[] x) {
        return the(x, Op.terms::subterms);
    }

    public static Subterms the(Term[] x, Function<Term[],Subterms> b) {
        return the(x, b, false);
    }

    public static Subterms the(Term[] x, Function<Term[],Subterms> b, boolean dedup) {

        switch (x.length) {
            case 1:
                return b.apply(x);

            case 2:
                int i = x[0].compareTo(x[1]);
                if (i == 0 && dedup)
                    return b.apply(new Term[] { x[0] });
                if (i <= 0)
                    return b.apply(x);
                else
                    return b.apply(new Term[]{ x[1], x[0] }).reversed();

            default: {
                Term[] xx;
                if (dedup)
                    xx = Terms.sorted(x);
                else {
                    xx = x.clone();
                    Arrays.sort(xx);
                }
                if (Arrays.equals(xx, x)) {
                    //already sorted
                    return b.apply(x);
                } else {
                    //TODO if (xx.length == 1) return RepeatedSubterms.the(xx[0],x.length);
                    return MappedSubterms.the(x, b.apply(xx));
                }
            }
        }
    }

    /** assumes that each items in the base subterm are utilized exactly once in the structure, containment, etc.
     * a relaxed variation of this can be created without this assumption */
    public static class MappedSubterms extends ProxySubterms {

        /** TODO even more compact 2-bit, 3-bit etc representations */
        final byte[] map;
        final int hash;
        private boolean normalized;

        protected MappedSubterms(Subterms base, byte[] map) {
            super(base);
            assert(base.subs()==map.length);
            this.map = map;
            this.hash = super.hashCode();
            this.normalized = TermMetadata.preNormalize(this);
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
        public Subterms reversed() {
            byte[] r = map.clone();
            ArrayUtils.reverse(r);
            if (Arrays.equals(r, map)) //palindrome?
                return this;
            return new MappedSubterms(ref, r);
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
            return normalized;
        }

        @Override
        public void setNormalized() {
            this.normalized = true;
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


        public static MappedSubterms the(Term[] target, Subterms base) {
            byte[] m = new byte[target.length];
            for (int i = 0, xLength = target.length; i < xLength; i++)
                m[i] = (byte) base.indexOf(target[i]);
            return new MappedSubterms(base, m);
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
            if (obj instanceof MappedSubterms) {
                MappedSubterms m = (MappedSubterms) obj;
                return hash == m.hash && ref.equals(m.ref) && Arrays.equals(map, m.map);
            }
            return ((nars.subterm.Subterms)obj).equalTerms(this);
        }

        @Override
        public String toString() {
            return nars.subterm.Subterms.toString(this);
        }

        @Override
        public Term sub(int i) {
            return ref.sub(map[i]);
        }

        @Override
        public int subs() {
            return map.length;
        }

    }

}
