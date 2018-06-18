package nars.subterm;

import jcog.util.ArrayIterator;
import nars.term.Term;

import java.util.Iterator;
import java.util.function.Consumer;

/**
 * Size 2 TermVector
 */
public final class TermVector2 extends TermVector {

    public final Term x, y;

    /**
     * uses an array argument so that the input array can be used directly without needing to create a new one when it calls the superclass constructor
     */
    public TermVector2(Term... xy) {
        super(xy);
        assert (xy.length == 2);
        this.x = xy[0];
        this.y = xy[1];
    }


    @Override
    public Term[] arrayClone() {
        return new Term[]{x, y};
    }

    @Override
    public Term sub(int i) {
        switch (i) {
            case 0:
                return x;
            case 1:
                return y;
            default:
                throw new ArrayIndexOutOfBoundsException();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof Subterms) {
            Subterms t;
            if (hash == (t = ((Subterms) obj)).hashCodeSubterms()) {
                return (t.subs() == 2 && t.sub(0).equals(x) && t.sub(1).equals(y));
            }
        }
        return false;
    }

    @Override
    public final int subs() {
        return 2;
    }

    @Override
    public Iterator<Term> iterator() {
        return ArrayIterator.get(x, y);
    }

    @Override
    public void forEach(Consumer<? super Term> action, int start, int stop) {
        int n = stop - start;
        if (n == 1 && start == 0 || start == 1) {
            action.accept(start == 0 ? x : y);
        } else if (start == 0) {
            forEach(action);
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    @Override
    public void forEach(Consumer<? super Term> action) {
        action.accept(x);
        action.accept(y);
    }


}
