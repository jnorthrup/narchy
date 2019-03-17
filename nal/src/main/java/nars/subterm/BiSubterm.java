package nars.subterm;

import jcog.data.iterator.ArrayIterator;
import nars.term.Term;

import java.util.Iterator;
import java.util.function.Consumer;

/**
 * Size 2 TermVector
 */
public final class BiSubterm extends TermVector {

    protected final Term x,y;

    /**
     * uses an array argument so that the input array can be used directly without needing to create a new one when it calls the superclass constructor
     */
    public BiSubterm(Term x, Term y) {
        super(x, y);
        this.x = x;
        this.y = y;
        normalized = normalized(this);
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
            boolean avoidDynamicHashing = obj instanceof TermList; //TODO marker interface
            Subterms t = ((Subterms) obj);
            if (avoidDynamicHashing || hash == t.hashCodeSubterms()) {
                //                    if (t instanceof TermVector)
                //                        equivalentTo((TermVector) t);
                return t.subs() == 2 && t.sub(0).equals(x) && t.sub(1).equals(y);
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
        return ArrayIterator.iterator(x, y);
    }

    @Override
    public void forEach(Consumer<? super Term> a, int start, int stop) {
        switch (start) {
            case 0:
                switch (stop) {
                    case 0: return;
                    case 1: a.accept(x); return;
                    case 2: a.accept(x); a.accept(y); return;
                }
                break;
            case 1:
                switch (stop) {
                    case 1: return;
                    case 2: a.accept(y); return;
                }
                break;
        }
        throw new ArrayIndexOutOfBoundsException();
    }

    @Override
    public final void forEach(Consumer<? super Term> action) {
        action.accept(x);
        action.accept(y);
    }

}
