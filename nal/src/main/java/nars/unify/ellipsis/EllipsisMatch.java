package nars.unify.ellipsis;

import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.compound.LightCompound;
import nars.term.util.transform.Retemporalize;
import nars.unify.Unify;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.SortedSet;

import static nars.Op.PROD;

/**
 * Holds results of an ellipsis match and
 */
public final class EllipsisMatch extends LightCompound {

    public static final Op EllipsisOp = PROD;

    public final static EllipsisMatch empty = new EllipsisMatch(Op.EmptyTermArray);


    private EllipsisMatch(Term[] t) {
        super(EllipsisOp.id, t /*new DisposableTermList(t)*/);
        assert(t.length > 1 || (t.length == 0 && empty == null /* HACK */));
    }

    private EllipsisMatch(Collection<Term> term) {
        this(term.toArray(Op.EmptyTermArray));
    }

//    @Override
//    public Term[] arrayShared() {
//        Subterms s = subterms();
//        return s instanceof TermList ? ((TermList)s).arrayKeep() : s.arrayShared();
//    }

//    public static Term[] flatten(Term[] xy, int expectedEllipsisAdds, int expectedEllipsisRemoves) {
//        int n = xy.length;
//        Term[] z = new Term[n + expectedEllipsisAdds - expectedEllipsisRemoves];
//        int k = 0;
//        for (Term x : xy) {
//            if (x instanceof EllipsisMatch) {
//                Term[] xx = x.arrayShared();
//                for (Term xxx : xx)
//                    z[k++] = xxx;
//            } else {
//                z[k++] = x;
//            }
//        }
//        assert (k == z.length);
//        return z;
//    }


    /** the ellipsis itself contributes no op */
    @Override public int structure() {
        return subterms().structure();
    }


    public static Term matched(Term... matched) {
        switch (matched.length) {
            case 0:
                return empty;
            case 1:
                return matched[0];
            default:
                return new EllipsisMatch(matched);
        }

    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof EllipsisMatch && subterms().equals(((Compound)obj).subterms()));
    }

    /** behave like a constant that only matches other EllipsisMatch */ @Override
    public boolean unifyForward(Term y, Unify u) {
        return y instanceof EllipsisMatch && (
            equals(y) || (subs() == y.subs() && unifyLinear(y.subterms(), u))
        );
    }

    public static Term matched(SortedSet<Term> term) {
        int num = term.size();
        switch (num) {
            case 0:
                return empty;
            case 1:
                return term.first();
            default:
                return new EllipsisMatch(term);
        }
    }

    public static Term matchedExcept(Subterms matched, byte... except) {
        int ll = matched.subs();
        int ee = except.length;
        Term[] t = new Term[ll - ee];
        int j = 0;
        main:
        for (int i = 0; i < ll; i++) {
            for (byte anExcept : except)
                if (i == anExcept)
                    continue main;


            t[j++] = matched.sub(i);
        }
        return matched(t);
    }

    @Nullable public static Term matchedExcept(int minArity,Term[] matched, byte... except) {
        int ll = matched.length;
        int ee = except.length;
        int ml = ll - ee;
        Term[] t = new Term[ml];
        if (ml < minArity)
            return null;

        int j = 0;
        main:
        for (int i = 0; i < ll; i++) {
            for (byte anExcept : except)
                if (i == anExcept)
                    continue main;

            t[j++] = matched[i];
        }
        return matched(t);
    }

    public static Term matched(/*@NotNull*/ Subterms y, int from, int to) {

        int len = to-from;
        switch (len) {
            case 0:
                return EllipsisMatch.empty;
            case 1:
                return y.sub(from);
            default:
                return matched(y.subRangeArray(from, to));
        }
    }

    @Override
    public Term neg() {
        throw new UnsupportedOperationException();
    }

    @Override
    public /*@NotNull*/ Term concept() {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable Term temporalize(Retemporalize r) {
        throw new UnsupportedOperationException();
    }

    public boolean linearMatch(Subterms y, int from, /*@NotNull*/ Unify subst) {
        int s = subs();

        if (s + from > y.subs())
            return false;

        for (int i = 0; i < s; i++) {
            if (!y.sub(i + from).unify(sub(i), subst))
                return false;
        }
        return true;
    }


    @Override
    public boolean isCommutative() {
        return false;
    }



}
