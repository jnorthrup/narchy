package nars.term.container;

import jcog.list.ArrayIterator;
import nars.Op;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.function.Consumer;

import static nars.Op.Null;

/**
 * Size 2 TermVector
 */
public final class TermVector2 extends TermVector {

    public final Term x, y;

    /** uses an array argument so that the input array can be used directly without needing to create a new one when it calls the superclass constructor */
    public TermVector2(Term... xy) {
        super(xy);
        assert(xy.length == 2);
        this.x = xy[0];
        this.y = xy[1];
    }

    @NotNull
    @Override
    public Term[] arrayClone() {
        return new Term[] { x, y };
    }

    @Override
    public @NotNull Term sub(int i) {
        switch (i) {
            case 0: return x;
            case 1: return y;
            default:
                return Null; //throw new ArrayIndexOutOfBoundsException();
        }
    }

    @Override
    public boolean subIs(int i, Op o) {
        return sub(i).op()==o;
    }

    @Override
    public boolean subEquals(int i, Term maybeEquals) {
        return sub(i).equals(maybeEquals);
    }

    @Override
    public boolean equals(@NotNull Object obj) {
        if (this == obj) return true;
        if (obj instanceof Subterms) {
            if (hash == obj.hashCode()) {
                Subterms t = (Subterms) obj;
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
        int howMany = stop - start;

        if (!(howMany <= 2 && howMany >= 1 && start >= 0 && start < 2))
            throw new ArrayIndexOutOfBoundsException();

        if (howMany ==1) {
            action.accept( start == 0 ? x : y );
        } else {
            action.accept(x);
            action.accept(y);
        }
    }

    @Override
    public void forEach(Consumer<? super Term> action) {
        action.accept(x);
        action.accept(y);
    }


//    @Override
//    public boolean isDynamic() {
//        return x.isDynamic() || y.isDynamic();
//    }

}
