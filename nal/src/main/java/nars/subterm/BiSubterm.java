package nars.subterm;

import com.google.common.io.ByteArrayDataOutput;
import jcog.data.iterator.ArrayIterator;
import nars.term.Term;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;

import java.util.Iterator;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * Size 2 TermVector
 */
public class BiSubterm extends TermVector {

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
    public void appendTo(ByteArrayDataOutput out) {
        out.writeByte(2);
        x.appendTo(out);
        y.appendTo(out);
    }

    @Override
    public int sum(ToIntFunction<Term> value) {
        int vx = value.applyAsInt(x);
        return x == y ? (vx*2) : vx + value.applyAsInt(y);
    }

    @Override
    public final int intifyRecurse(IntObjectToIntFunction<Term> reduce, int v) {
        return y.intifyRecurse(reduce, x.intifyRecurse(reduce, v));
    }

    @Override
    public final int intifyShallow(IntObjectToIntFunction<Term> reduce, int v) {
        return reduce.intValueOf(reduce.intValueOf(v, x), y);
    }

    @Override
    public boolean OR(Predicate<Term> p) {
        return p.test(x) || (x!=y && p.test(y));
    }

    @Override
    public <X> boolean ORwith(BiPredicate<Term, X> p, X param) {
        return p.test(x, param) || (x!=y && p.test(y, param));
    }

    @Override
    public <X> boolean ANDwith(BiPredicate<Term, X> p, X param) {
        return p.test(x, param) && (x==y || p.test(y, param));
    }

    @Override
    public boolean ORrecurse(Predicate<Term> p) {
        return x.ORrecurse(p) || (x!=y && y.ORrecurse(p));
    }

    @Override
    public boolean AND(Predicate<Term> p) {
        return p.test(x) && (x==y || p.test(y));
    }

    @Override
    public boolean ANDrecurse(Predicate<Term> p) {
        return x.ANDrecurse(p) && (x==y || y.ANDrecurse(p));
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
                if (t.subs() == 2 && t.sub(0).equals(x) && t.sub(1).equals(y)) {
                    if (t instanceof TermVector)
                        equivalentTo((TermVector) t);
                    return true;
                }
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




    @Override
    public boolean containsRecursively(Term t, boolean root, Predicate<Term> subTermOf) {
        return ((root ? x.equalsRoot(t) : x.equals(t)) || ((x!=y) && (root ? y.equalsRoot(t) : y.equals(t))))
                ||
               (x.containsRecursively(t, root, subTermOf) || ((x!=y) && y.containsRecursively(t,root,subTermOf)));
    }


    final public static class BiRepeat extends BiSubterm {
        public BiRepeat(Term x) {
            super(x,x);
        }

        @Override
        public Subterms reversed() {
            return this;
        }

        @Override
        public int sum(ToIntFunction<Term> value) {
            return value.applyAsInt(x)*2;
        }

        @Override
        public boolean AND(Predicate<Term> p) {
            return p.test(x);
        }

        @Override
        public boolean OR(Predicate<Term> p) {
            return p.test(x);
        }


    }
}
