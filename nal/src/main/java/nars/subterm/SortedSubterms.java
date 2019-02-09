package nars.subterm;

import nars.Op;
import nars.term.Neg;
import nars.term.Term;
import nars.term.Terms;

import java.util.Arrays;
import java.util.function.Function;

import static nars.Op.NEG;

/** canonical subterm sorting and permutation wrapping for advanced interning */
public class SortedSubterms {

    public static Subterms the(Term[] x) {
        return the(x, Op.terms::subterms);
    }

    public static Subterms the(Term[] x, Function<Term[],Subterms> b) {
        return the(x, b, false);
    }

    public static Subterms the(final Term[] x, Function<Term[],Subterms> b, boolean dedup) {

        switch (x.length) {
            case 1:
                return b.apply(x);

            case 2:
                //if (x[0].op()!=NEG && x[1].op()!=NEG) {
                if (!(x[0] instanceof Neg) && !(x[1] instanceof Neg)) {
                    int i = x[0].compareTo(x[1]);
                    if (i == 0 && dedup)
                        return b.apply(new Term[]{x[0]});
                    if (i <= 0)
                        return b.apply(x);
                    else
                        return b.apply(new Term[]{x[1], x[0]}).reversed();
                }
                break;
        }

        Term[] xx = x.clone();
        boolean hasNeg = false;
        for (int j = 0; j < xx.length; j++) {
            if (xx[j].op()==NEG) {
                hasNeg = true;
                xx[j] = xx[j].unneg();
            }
        }

        if (dedup)
            xx = Terms.sorted(xx);
        else {
            Arrays.sort(xx);
        }

        if (Arrays.equals(xx, x)) {
            //already sorted and has no negatives
            return b.apply(x);
        } else {
            //TODO if (xx.length == 1) return RepeatedSubterms.the(xx[0],x.length);
            return MappedSubterms.the(x, b.apply(xx), xx);
        }
    }

}
