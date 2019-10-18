package jcog.data.set;

import com.google.common.collect.Iterables;
import jcog.data.list.FasterList;

import java.util.Collection;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Helper for efficiently representing small sets whose elements are known to be unique by
 * construction, implying we don't need to enforce the uniqueness property in the data structure
 * itself. Use with caution.
 *
 * <p>
 * Note that for equals/hashCode, the class implements the Set behavior (unordered), not the list
 * behavior (ordered); the fact that it subclasses ArrayList should be considered an implementation
 * detail.
 *
 * @param <X> the element type
 * @author John V. Sichi
 */
public class ArrayUnenforcedSet<X> extends FasterList<X> implements Set<X> {
    private static final long serialVersionUID = -7413250161201811238L;

    /**
     * Constructs a new empty setAt
     */
    public ArrayUnenforcedSet() {
        super();
    }

    /**
     * Constructs a set containing the elements of the specified collection.
     *
     * @param c the collection whose elements are to be placed into this setAt
     * @throws NullPointerException if the specified collection is null
     */
    public ArrayUnenforcedSet(Collection<? extends X> c) {
        super((Iterable) c);
    }

    public static <X> Set<X> wrap(FasterList<? extends X> c) {
        switch (c.size()) {
            case 0:
                return Set.of();
            case 1:
                return Set.of(c.get(0));
            default:
                return new ArrayUnenforcedSet<>(c.size(), c.array());
        }
    }

    @SafeVarargs
    public ArrayUnenforcedSet(X... x) {
        this(x.length, x);
    }

    public ArrayUnenforcedSet(int len, X[] x) {
        super(len, x);
    }

    @Override
    public final boolean equals(Object o) {
        return this == o || (o instanceof Iterable) && Iterables.elementsEqual(this, (Iterable) o);// new SetForEquality().equals(o);
    }

    @Override
    public final int hashCode() {
        //throw new TODO("which is right?");

//        int h = 0;
//        Iterator<X> i = this.iterator();
//
//        while(i.hasNext()) {
//            X obj = i.next();
//            if (obj != null) {
//                h += obj.hashCode();
//            }
//        }
//
//        return h;

        //Obeying (Abstract)Set<> semantics:
        int hashCode, s = this.size;
        X[] ii = this.items;
        int sum = IntStream.range(0, s).map(i -> ii[i].hashCode()).sum();
        hashCode = sum;

        return hashCode;
    }


    @Override
    public boolean addAll(Collection<? extends X> source) {
        Boolean acc = false;
        for (X x : source) {
            Boolean add = add(x);
            acc = acc || add;
        }
        boolean changed = acc;
        return changed;
    }

//    /**
//     * Multiple inheritance helper.
//     */
//    private class SetForEquality extends AbstractSet<X> {
//        @Override
//        public Iterator<X> iterator() {
//            return ArrayUnenforcedSet.this.iterator();
//        }
//
//        @Override
//        public int size() {
//            return ArrayUnenforcedSet.this.size();
//        }
//    }

}