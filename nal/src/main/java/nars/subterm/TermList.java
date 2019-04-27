package nars.subterm;

import jcog.data.list.FasterList;
import nars.Op;
import nars.NAL;
import nars.term.Term;
import nars.term.util.TermException;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

import static nars.Op.CONJ;
import static nars.term.atom.Bool.True;

/** mutable subterms, used in intermediate operations */
public class TermList extends FasterList<Term> implements Subterms {


    public TermList(int initialCapacity) {
        super(0, new Term[initialCapacity]);
    }

    public TermList(Term... direct) {
        super(direct);
    }

    public TermList(Subterms copied) {
        super(0, new Term[copied.subs()]);
        copied.forEach(this::addFast);
    }

    @Override
    public int hashCode() {
        int s = size;
        return s > 0 ? Subterms.hash(items, size) : 1;
    }



    @Override
    public final boolean these() {
        return false;
    }

    public void ensureExtraCapacityExact(int num) {
        int l = this.items.length;
        int oldCapacity = l, minCapacity = l + num;
        if (minCapacity > oldCapacity)
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
        return toArray(Op.EmptyTermArray);
    }


//    @Override
//    public Term[] arrayShared() {
//        throw new TODO("did you mean to use .arrayKeep");
//    }

    @Override
    public int subs() {
        return size;
    }

    @Override
    public String toString() {
        return Subterms.toString(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        
        if ((obj instanceof TermList)) {
            return nonNullEquals(((TermList)obj));
        } else {
            Subterms ss = ((Subterms)obj);
            if (hashCodeSubterms()!=ss.hashCodeSubterms())
                return false;
            else
                return ss.equalTerms(this);
        }
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
        Term[] x = array();
        int s = size();
        if (x.length == s)
            return x;
        else
            return Arrays.copyOf(x, s);
    }


    /** finalization step on constructing a Subterm */
    @Nullable
    public Subterms commit(Subterms src, Op superOp) {
        int ys = size();

        //replace prefix nulls with the input's values
        //any other modifications specific to the superOp type

        Term[] ii = items;

        if (superOp == CONJ) {
            for (int i = 0; i < ys;) {
                Term j;
                if ((j = ii[i]) == null)
                    ii[i] = j = src.sub(i);

                if (j == True) {
                    removeFast(i);
                    ys--;
                } else
                    i++;
            }
        } else {
            for (int i = 0; i < ys; i++) {
                if (ii[i] == null)
                    ii[i] = src.sub(i);
                else
                    break; //finished at first non-null subterm
            }
        }

        if (volume() > NAL.term.COMPOUND_VOLUME_MAX) {
            throw new TermException("complexity overflow", this);
        }

        if (ys == 0)
            return Op.EmptySubterms;
        else {
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

    public Term[] sortAndDedup() {


        //TODO if size > threshld: use original insertion sort method:
//        SortedList<Term> sl = new SortedList<>(t, new Term[t.length]);
//        return sl.orderChangedOrDeduplicated ?
//                sl.toArrayRecycled(Term[]::new) : t;


        int s = size();
        Term[] ii = this.items;
        Arrays.sort(ii, 0, s);

        Term prev = ii[0];
        boolean dedup = false;
        for (int i = 1; i < s; i++) {
            Term next = ii[i];
            if (prev.equals(next)) {
                setFast(i, null);
                dedup = true;
            } else {
                prev = next;
            }
        }
        if (dedup)
            removeNulls();

        return arrayKeep();
    }
}
