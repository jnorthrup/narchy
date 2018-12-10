package nars.subterm;

import nars.term.Term;
import nars.term.Terms;

import java.util.Arrays;
import java.util.function.Function;

public class SortedSubterms {


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
                    return b.apply(x).reversed();

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

    public static class MappedSubterms extends ProxySubterms {

        /** TODO even more compact 2-bit, 3-bit etc representations */
        final byte[] map;
        final int hash;

        protected MappedSubterms(Subterms base, byte[] map) {
            super(base);
            this.map = map;

            this.hash = super.hashCode();
        }

        public static MappedSubterms the(Term[] target, Subterms base) {
            byte[] m = new byte[target.length];
            for (int i = 0, xLength = target.length; i < xLength; i++)
                m[i] = (byte) base.indexOf(target[i]);
            return new MappedSubterms(base, m);
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
