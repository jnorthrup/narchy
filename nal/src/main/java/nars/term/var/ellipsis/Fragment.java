package nars.term.var.ellipsis;

import jcog.util.ArrayUtil;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.compound.CachedCompound;
import nars.term.compound.LightCompound;
import nars.term.util.transform.Retemporalize;
import nars.unify.Unify;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.SortedSet;

import static nars.Op.EmptySubterms;
import static nars.Op.FRAG;
import static nars.time.Tense.DTERNAL;

/**
 * Holds results of an ellipsis match and
 */
public final class Fragment extends LightCompound {

    public final static Compound empty = CachedCompound.newCompound(Op.FRAG, DTERNAL, Op.EmptyProduct);

    private Fragment() {
        super(FRAG.id, EmptySubterms);
    }

    public Fragment(Subterms x) {
        super(FRAG.id, x);
        assert(x.subs() > 1 || (x.subs() == 0 && empty == null /* HACK */));
    }

    private Fragment(Term[] x) {
        super(FRAG, x);
        assert(x.length > 1 || (x.length == 0 && empty == null /* HACK */));
    }

    private Fragment(Collection<Term> x) {
        this(x.toArray(Op.EmptyTermArray));
    }

    /** the ellipsis itself contributes no op */
    @Override public final int structure() {
        return subStructure();
    }

    public static Term fragment(Term... x) {

        switch (x.length) {
            case 0:
                return empty;
            case 1:
                return x[0];
            default:
                return new Fragment(x);
        }

    }



    public static Term fragment(SortedSet<Term> x) {
        int num = x.size();
        switch (num) {
            case 0:
                return empty;
            case 1:
                return x.first();
            default:
                return new Fragment(x);
        }
    }

    public static Term matchedExcept(Subterms matched, byte... except) {

        int ll = matched.subs();
        if (except.length == ll-1) {
            //choose only the unmatched subterm
            for (byte i = 0; i < ll; i++) {
                if (ArrayUtil.indexOf(except, i)==-1)
                    return matched.sub(i);
            }
            throw new NullPointerException();

        } else {
            Term[] t = new Term[ll - except.length];
            int j = 0;
            for (byte i = 0; i < ll; i++) {
                if (ArrayUtil.indexOf(except, i)==-1)
                    t[j++] = matched.sub(i);
            }
            //assert(j == t.length);
            return fragment(t);
        }
    }

    @Nullable public static Term matchedExcept(Term[] matched, byte... except) {
        int ll = matched.length;
        int ee = except.length;
        int ml = ll - ee;

        Term[] t = new Term[ml];


        int j = 0;
        main:
        for (int i = 0; i < ll; i++) {
            for (byte anExcept : except)
                if (i == anExcept)
                    continue main;

            t[j++] = matched[i];
        }
        return fragment(t);
    }

    public static Term fragment(/*@NotNull*/ Subterms y, int from, int to) {

        int len = to-from;
        switch (len) {
            case 0:
                return Fragment.empty;
            case 1:
                return y.sub(from);
            default:
                return fragment(y.subRangeArray(from, to));
        }
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
