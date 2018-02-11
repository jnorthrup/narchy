package nars.derive.match;

import nars.$;
import nars.Op;
import nars.index.term.TermContext;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.term.CompoundLight;
import nars.term.Term;
import nars.term.subst.Unify;
import nars.term.transform.Retemporalize;
import nars.term.transform.TermTransform;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.SortedSet;

import static nars.Op.PROD;

/**
 * Holds results of an ellipsis match and
*/
public class EllipsisMatch extends CompoundLight {

    //    public static ArrayEllipsisMatch matchedSubterms(Compound Y, IntObjectPredicate<Term> filter) {
//        Function<IntObjectPredicate,Term[]> arrayGen =
//                !(Y instanceof Sequence) ?
//                        Y::terms :
//                        ((Sequence)Y)::toArrayWithIntervals;
//
//        return new ArrayEllipsisMatch(arrayGen.apply( filter ));
//    }


    public final static EllipsisMatch empty = new EllipsisMatch(Term.EmptyArray);


    protected EllipsisMatch(Term[] t) {
        super(PROD, $.vFast(t));
    }

    public EllipsisMatch(Collection<Term> term) {
        super(PROD, new TermList(term));
    }

    @NotNull public static Term[] flatten(Term[] xy, int expectedEllipsisAdds, int expectedEllipsisRemoves) {
        int n = xy.length;
        Term[] z = new Term[n + expectedEllipsisAdds - expectedEllipsisRemoves];
        int k = 0;
        for (int i = 0; i < n; i++) {
            Term x = xy[i];
            if (x instanceof EllipsisMatch) {
                Term[] xx = ((EllipsisMatch) x).arrayShared();
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
    public Term evalSafe(TermContext context, Op supertermOp, int subterm, int remain) {
        //dont eval until it's unwrapped
        return this;
    }

    @Override
    public @Nullable Term transform(TermTransform t) {
        //dont eval until it's unwrapped
        return this;
    }

    @Override
    public Term neg() {
        throw new UnsupportedOperationException();
    }


    @Override
    public /*@NotNull*/ Term conceptual() {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable Term temporalize(Retemporalize r) {
        //throw new UnsupportedOperationException();
        return this; //HACK
    }


    public static Term match(Term... matched) {
        switch (matched.length) {
            case 0: return empty;
            case 1: return matched[0]; //if length==1 it should not be an ellipsismatch, just the raw term
            default: return new EllipsisMatch(matched);
        }
    }

    public static Term match(SortedSet<Term> term) {
        int num = term.size();
        switch (num) {
            case 0: return empty;
            case 1: return term.first();
            default: return new EllipsisMatch(term);
        }
    }

    public static Term matchExcept(Subterms matched, byte... except) {
        int ll = matched.subs();
        int ee = except.length;
        Term[] t = new Term[ll - ee];
        int j = 0;
        main: for (int i = 0; i < ll; i++) {
            for (int k = 0; k < ee; k++)
                if (i == except[k])
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
        main: for (int i = 0; i < ll; i++) {
            for (int k = 0; k < ee; k++)
                if (i == except[k])
                    continue main;

            t[j++] = matched[i];
        }
        return new EllipsisMatch(t);
    }



    public static Term match(/*@NotNull*/ Subterms y, int from, int to) {


        if (from == to) {
            return EllipsisMatch.empty;
        }

        return match( y.toArraySubRange(from, to));

//        } else {
//            assert(to == y.size());
//            return ImageMatch.getRemaining(y, from);
//        }

    }


//    public static Term matchExcept(Term[] x, int index) {
//        int num = x.length - 1;
//        switch (num) {
//            case 0: return empty;
//            case 1: return x[0].equals(without) ? x[1] : x[0];
//            default: return new EllipsisMatch(ArrayUtils.removeElement(x, without));
//        }
//    }

//    public final boolean forEachWhile(Predicate<? super Term> c) {
//        int s = subs();
//        for (int i = 0; i < s; i++) {
//            if (!c.test(sub(i)))
//                return false;
//        }
//        return true;
//    }

    public boolean linearMatch(Subterms y, int from, /*@NotNull*/ Unify subst) {
        int s = subs();

        if (s + from > y.subs())
            return false; //size mismatch: would extend beyond y's size

        for (int i = 0; i < s; i++) {
            if (!y.sub(i + from).unify(sub(i), subst)) //term mismatch
                return false;
        }
        return true;
    }

//    /** HACK */
//    /*@NotNull*/
//    static Term[] expand(Term raw) {
//        return raw instanceof EllipsisMatch ?
//                ((EllipsisMatch)raw).terms :
//                new Term[] { raw };
//    }

//    public EllipsisMatch(/*@NotNull*/ Collection<Term> term, Term except) {
//        this(term.stream().filter(t -> ((t!=except) )).collect(toList()));
//    }

//    @Deprecated public EllipsisMatch(/*@NotNull*/ Collection<Term> term, Term except, Term except2) {
//        this(term.stream().filter(t -> ((t!=except) && (t!=except2) )).collect(toList()));
//    }



    //abstract public boolean addContained(Compound Y, Set<Term> target);

    @Override
    public boolean isCommutative() {
        //throw new UnsupportedOperationException("it depends");
        return false; //to be careful
    }


    public boolean rematch(/*@NotNull*/ Subterms y, /*@NotNull*/ Collection<Term> yFree) {
        /*@NotNull*/ Subterms x = subterms();
        int xs = x.subs();
        for (int i = 0; i < xs; i++) {
            Term e = x.sub(i);
            //if something in this ellipsis was not present in the matchable subterms
            //or if something else has matched it
            if (!y.contains(e) || !yFree.remove(e))
                return false;
        }
        return true;
    }



}
