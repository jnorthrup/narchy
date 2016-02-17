package nars.term.container;


import nars.term.Term;
import nars.term.Terms;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.TreeSet;

public class TermSet<X extends Term> extends TermVector<X> {

    @NotNull
    public static TermSet the(Term... x) {
        return new TermSet(Terms.toSortedSetArray(x));
    }

    @NotNull
    public static TermSet the(Collection<? extends Term> x) {
        return new TermSet(toSortedSetArray(x));
    }

    @NotNull
    public static TermSet union(@NotNull TermContainer a, @NotNull TermContainer b) {
        TreeSet<Term> t = new TreeSet<Term>();
        a.addAllTo(t);
        b.addAllTo(t);
        return TermSet.the(t);
    }

//    public static TermSet newTermSetPresorted(Term... presorted) {
//        return new TermSet(presorted);
//    }

    private TermSet(X[] x) {
        super(x);
    }

    public static Term[] toSortedSetArray(Collection<? extends Term> c) {
        TreeSet<Term> t = c instanceof TreeSet ? (TreeSet<Term>) c : new TreeSet<Term>(c);
        return t.toArray(new Term[t.size()]);
    }

    @Override public final boolean isSorted() {
        return true;
    }

    @NotNull
    @Override
    public TermVector replacing(int subterm, Term replacement) {
        throw new RuntimeException("n/a for set");
    }
}
