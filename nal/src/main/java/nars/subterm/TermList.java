package nars.subterm;

import jcog.data.list.FasterList;
import nars.Op;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/** mutable subterms, used in intermediate operations */
public class TermList extends FasterList<Term> implements Subterms {


    public TermList(int initialCapacity) {
        super(0, new Term[initialCapacity]);
    }

    public TermList(Term... direct) {
        super(direct);
    }
    public TermList(int startingSize, Term[] direct) {
        super(startingSize, direct);
    }

    public TermList(Subterms copied) {
        super(0, new Term[copied.subs()]);
        copied.forEach(this::addFast);
    }
    public TermList(Subterms copied, int from, int to) {
        super(0, new Term[to-from]);
        copied.forEach(this::addFast, from, to);
    }

    public final void addAllNegated(Iterable<Term> x) {
        x.forEach(this::addNegated);
    }

    public final void addNegated(Term x) {
        add(x.neg());
    }

    @Override
    public int hashCode() {
        return Subterms.hash(items, size);
    }

    /** dont intern */
    @Override public final boolean these() {
        return false;
    }

    void ensureExtraCapacityExact(int num) {
        int oldCap = this.items.length;
        int minCapacity = oldCap + num;
        if (minCapacity > oldCap)
            this.items = Arrays.copyOf(items, minCapacity);
    }

    @Override
    public TermList toList() {
        return new TermList(arrayClone());
    }

    @Override
    public final Term sub(int i) {
        return items[i];
    }

    @Override
    public Term[] arrayClone() {
        return toArray();
    }


    @Override
    public final int subs() {
        return size;
    }

    @Override
    public String toString() {
        return Subterms.toString(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        return (obj instanceof TermList) ? nonNullEquals(((TermList) obj)) : ((Subterms) obj).equalTerms(this);
    }

//
//    public void addAll(Subterms x, int xStart, int xEnd) {
//        ensureCapacity(xEnd-xStart);
//        for (int i = xStart; i < xEnd; i++) {
//            addWithoutResizeCheck(x.sub(i));
//        }
//    }

    /** use this only if a (disposable) TermList is done being modified */
    public Term[] arrayKeep() {
        Term[] x = items;
        int s = size;
        return x.length == s ? x : Arrays.copyOf(x, s);
    }

//    @Override
//    public final boolean containsInstance(Term term) {
//        return super.containsInstance(term);
//    }

    /** finalization step on constructing a Subterm */
    @Nullable public Subterms commit(Subterms src) {
        int ys = size;


//        if (volume() > NAL.term.COMPOUND_VOLUME_MAX) {
//            throw new TermException("complexity overflow", this);
//        }

        if (ys == 0)
            return Op.EmptySubterms;
        else {
            //replace prefix nulls with the input's values
            //any other modifications specific to the superOp type

            Term[] ii = items;

            for (int i = 0; i < ys; i++) {
                if (ii[i] == null)
                    ii[i] = src.sub(i);
                else
                    break; //finished at first non-null subterm
            }
//            //test for identical
//            int s = this.size;
//            if (s == src.subs()) {
//                boolean identical = true;
//                for (int i = 0; i < s; i++) {
//                    if (src.sub(i) != ii[i]) {
//                        identical = false;
//                        break;
//                    }
//                }
//                if (identical)
//                    return src;
//            }
            return this;
        }
    }

    public Term[] sort() {
        int s = size;
        if (s > 1)
            Arrays.sort(this.items, 0, s);

        return arrayKeep();
    }

    public Term[] sortAndDedup() {


        //TODO if size > threshld: use original insertion sort method:
//        SortedList<Term> sl = new SortedList<>(t, new Term[t.length]);
//        return sl.orderChangedOrDeduplicated ?
//                sl.toArrayRecycled(Term[]::new) : t;


        Term[] ii = this.items;

        int s = size;
        if (s > 1) {
            //TODO pre-sort by volume since this is the first condition for comparison.  if there are N unique volumes then it will be sorted without needing compareTo

            Arrays.sort(ii, 0, s);

            Term prev = ii[0];
            boolean dedup = false;
            for (int i = 1; i < s; i++) {
                Term next = ii[i];
                if (prev.equals(next)) {
                    ii[i] = null;
                    dedup = true;
                } else {
                    prev = next;
                }
            }
            if (dedup)
                removeNulls();
        }




        return arrayKeep();
    }
}
