package nars.subterm;

import nars.Op;
import nars.term.Term;
import nars.term.Terms;

import java.util.Arrays;
import java.util.function.Function;

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

}
