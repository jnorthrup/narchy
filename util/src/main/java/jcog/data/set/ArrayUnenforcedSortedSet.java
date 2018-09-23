package jcog.data.set;

import jcog.TODO;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.SortedSet;

/** use with caution */
abstract public class ArrayUnenforcedSortedSet<X> extends ArrayUnenforcedSet<X> implements SortedSet<X> {

    public static final SortedSet empty = new ArrayUnenforcedSortedSet<>() {

        @Override
        public Object first() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object last() {
            throw new UnsupportedOperationException();
        }
    };

    public ArrayUnenforcedSortedSet(X... xx) {
        super((X[])xx);
    }

    @Override
    public boolean add(X newItem) {
        throw new TODO();
    }


    public static <X> SortedSet<X> the(X x) {
        return new One(x);
    }

    public static <X extends Comparable> SortedSet<X> the(X x, X y) {
        int c = x.compareTo(y);
        switch (c) {
            case 0:
                return new One(x);
            case 1:
                return new Two(y, x);
            default:
                return new Two(x, y);
        }
    }

    @Nullable
    @Override
    public Comparator<? super X> comparator() {
        return null;
    }

    @NotNull
    @Override
    public SortedSet<X> subSet(X x, X e1) {
        throw new TODO();
    }

    @NotNull
    @Override
    public SortedSet<X> headSet(X x) {
        throw new TODO();
    }

    @NotNull
    @Override
    public SortedSet<X> tailSet(X x) {
        throw new TODO();
    }

    private static class One<X> extends jcog.data.set.ArrayUnenforcedSortedSet<X> {

        private One(X x) {
            super(x);
        }

        @Override
        public X first() {
            return getFirst();
        }

        @Override
        public X last() {
            return getLast();
        }
    }

    private static class Two<X> extends jcog.data.set.ArrayUnenforcedSortedSet<X> {

        private Two(X x, X y) {
            super(x, y);
        }

        @Override
        public X first() {
            return getFirst();
        }

        @Override
        public X last() {
            return getLast();
        }

    }


}
