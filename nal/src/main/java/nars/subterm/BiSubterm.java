package nars.subterm;

import jcog.Util;
import jcog.data.iterator.ArrayIterator;
import nars.term.Term;
import nars.term.Terms;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * Size 2 TermVector
 */
public class BiSubterm extends TermVector {

    protected final Term x,y;

    public static class ReversibleBiSubterm extends BiSubterm {

        final int hashReversed;
        boolean normalizedReverse;

//        public ReversibleBiSubterm(Term both) {
//            super(both, both);
//            this.hashReversed = hash;
//            this.normalizedReverse = isNormalized();
//        }

        public ReversibleBiSubterm(Term x, Term y) {
            super(x, y);
            assert(x.compareTo(y) < 0): x + "," + y + " should be ordered as: " +
                    Arrays.toString(Terms.sorted(x, y));
            this.hashReversed = Util.hashCombine(Util.hashCombine1(y), x);
            this.normalizedReverse = !hasVars(); //TODO other tests
        }

        public BiSubtermReverse reverse() {
            return new BiSubtermReverse();
        }

        public Subterms reverseIf(boolean reverse) {
            return reverse ? reverse() : this;
        }

        private final class BiSubtermReverse implements Subterms {

            @Override
            public int hashCode() {
                return hashReversed;
            }


            @Override
            public boolean equals(Object obj) {
                if (this == obj) return true;
                if (obj instanceof Subterms) {
                    Subterms t;
                    if (hashReversed == (t = ((Subterms) obj)).hashCodeSubterms()) {
                        return (t.subs() == 2 && t.sub(0).equals(y) && t.sub(1).equals(x));
                    }
                }
                return false;
            }

            @Override
            public int structure() {
                return ReversibleBiSubterm.this.structure();
            }
            @Override
            public int volume() {
                return ReversibleBiSubterm.this.volume();
            }

            @Override
            public int complexity() {
                return ReversibleBiSubterm.this.complexity();
            }

            @Override
            public int varIndep() {
                return ReversibleBiSubterm.this.varIndep();
            }
            @Override
            public int varQuery() {
                return ReversibleBiSubterm.this.varQuery();
            }

            @Override
            public int varDep() {
                return ReversibleBiSubterm.this.varDep();
            }

            @Override
            public boolean isNormalized() {
                return normalizedReverse;
            }

            @Override
            public void setNormalized() {
                normalizedReverse = true;
            }

            @Override
            public int varPattern() {
                return ReversibleBiSubterm.this.varPattern();
            }

            @Override
            public int vars() {
                return ReversibleBiSubterm.this.vars();
            }

            @Override
            public boolean OR(Predicate<Term> p) {
                return ReversibleBiSubterm.this.OR(p);
            }
            @Override
            public boolean AND(Predicate<Term> p) {
                return ReversibleBiSubterm.this.AND(p);
            }

            @Override
            public int intifyShallow(IntObjectToIntFunction<Term> reduce, int v) {
                return ReversibleBiSubterm.this.intifyShallow(reduce, v);
            }

            @Override
            public int intifyRecurse(IntObjectToIntFunction<Term> reduce, int v) {
                return ReversibleBiSubterm.this.intifyRecurse(reduce, v);
            }


            @Override
            public Term[] arrayClone() {
                return new Term[] { y, x};
            }

            @Override
            public Term sub(int i) {
                switch (i) {
                    case 0: return y;
                    case 1: return x;
                    default: throw new NullPointerException();
                }
            }

            @Override
            public final int subs() {
                return 2;
            }

            @Override public Iterator<Term> iterator() {
                return ArrayIterator.get(y, x);
            }

            @Override
            public void forEach(Consumer<? super Term> action) {
                action.accept(y);
                action.accept(x);
            }
        }


    }

    /**
     * uses an array argument so that the input array can be used directly without needing to create a new one when it calls the superclass constructor
     */
    public BiSubterm(Term x, Term y) {
        super(x, y);
        this.x = x;
        this.y = y;
        testIfAutoNormalized();
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
        return p.test(x) || (x==y || p.test(y));
    }
    @Override
    public boolean AND(Predicate<Term> p) {
        return p.test(x) && (x==y || p.test(y));
    }

    @Override
    public boolean ORrecurse(Predicate<Term> p) {
        return x.ORrecurse(p) || (x==y || y.ORrecurse(p));
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
            if (obj instanceof TermVector) {
                if (hash != ((TermVector)obj).hash)
                    return false;
            }

            Subterms t = ((Subterms) obj);
            //Subterms t;
            //if (hash == (t = ((Subterms) obj)).hashCodeSubterms()) {
                if (t.subs() == 2 && t.sub(0).equals(x) && t.sub(1).equals(y)) {
                    if (t instanceof TermVector)
                        equivalentTo((TermVector) t);
                    return true;
                }
            //}
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
