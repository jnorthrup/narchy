package nars.unify.match;

import nars.Op;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.compound.LightCompound;
import nars.unify.Unify;
import nars.util.term.transform.Retemporalize;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.SortedSet;

/**
 * Holds results of an ellipsis match and
 */
public final class EllipsisMatch extends LightCompound {


    public final static EllipsisMatch empty = new EllipsisMatch(Op.EmptyTermArray);


    private EllipsisMatch(Term[] t) {
        super(((byte)0), t);
    }

    private EllipsisMatch(Collection<Term> term) {
        this(term.toArray(Op.EmptyTermArray));
    }

    public static Term[] flatten(Term[] xy, int expectedEllipsisAdds, int expectedEllipsisRemoves) {
        int n = xy.length;
        Term[] z = new Term[n + expectedEllipsisAdds - expectedEllipsisRemoves];
        int k = 0;
        for (Term x : xy) {
            if (x instanceof EllipsisMatch) {
                Term[] xx = x.arrayShared();
                for (Term xxx : xx)
                    z[k++] = xxx;
            } else {
                z[k++] = x;
            }
        }
        assert (k == z.length);
        return z;
    }

    @Override
    public Term the() {
        return null;
    }

    /** the ellipsis itself contributes no op */
    @Override public int structure() {
        return subterms().structure();
    }


    public static Term match(Term... matched) {
        switch (matched.length) {
            case 0:
                return empty;
            case 1:
                return matched[0];
            default:
                return new EllipsisMatch(matched);
        }
    }

    public static Term match(SortedSet<Term> term) {
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

    public static Term matchExcept(Subterms matched, byte... except) {
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
        return new EllipsisMatch(t);
    }

    public static Term matchExcept(Term[] matched, byte... except) {
        int ll = matched.length;
        int ee = except.length;
        Term[] t = new Term[ll - ee];
        int j = 0;
        main:
        for (int i = 0; i < ll; i++) {
            for (byte anExcept : except)
                if (i == anExcept)
                    continue main;

            t[j++] = matched[i];
        }
        return new EllipsisMatch(t);
    }

    public static Term match(/*@NotNull*/ Subterms y, int from, int to) {


        if (from == to) {
            return EllipsisMatch.empty;
        }

        return match(y.toArraySubRange(from, to));


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


    public boolean rematch(/*@NotNull*/ Subterms y, /*@NotNull*/ Collection<Term> yFree) {
        /*@NotNull*/
        Subterms x = subterms();
        int xs = x.subs();
        for (int i = 0; i < xs; i++) {
            Term e = x.sub(i);


            if (!y.contains(e) || !yFree.remove(e))
                return false;
        }
        return true;
    }


}
