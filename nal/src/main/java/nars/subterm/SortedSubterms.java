package nars.subterm;

import jcog.util.ArrayUtil;
import nars.term.Neg;
import nars.term.Term;
import nars.term.Terms;

import java.util.Arrays;
import java.util.function.Function;

/** canonical subterm sorting and permutation wrapping for advanced interning */
public final class SortedSubterms{

    public static Subterms the(Term[] x, Function<Term[],Subterms> b) {
        return the(x, false, b);
    }

    /** @noinspection ArrayEquality*/
    public static Subterms the(Term[] x, boolean dedup, Function<Term[], Subterms> b) {

        switch (x.length) {
            case 1:
                return b.apply(x);

            case 2:
                if (!(x[0] instanceof Neg) && (!(x[1] instanceof Neg))) {
                    int i = x[0].compareTo(x[1]);
                    if (dedup && i == 0)
                        return b.apply(new Term[]{x[0]});
                    else if (i <= 0)
                        return b.apply(x);
                    else
                        return b.apply(new Term[]{x[1], x[0]}).reversed();
                }
                break;
        }

        Term[] y = x;
        boolean hadNegs = false;
        for (int j = 0; j < y.length; j++) {
            if (y[j] instanceof Neg) {
                if (y==x)
                    y = x.clone();
                y[j] = y[j].unneg();
                hadNegs = true;
            }
        }

        if (dedup)
            y = Terms.commute(y);
        else {
            if (y==x)
                y = Terms.sort(y);
            else
                Arrays.sort(y);
        }

        //already sorted and has no negatives
        //TODO if (xx.length == 1) return RepeatedSubterms.the(xx[0],x.length);
        return !hadNegs && ArrayUtil.equalsIdentity(x, y) ? b.apply(x) : RemappedSubterms.the(x, b.apply(y));
    }

}
