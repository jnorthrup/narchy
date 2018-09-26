package jcog.data.set;

import com.google.common.collect.Iterables;
import jcog.data.list.FasterList;

import java.util.Collection;
import java.util.Set;

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
     * Constructs a new empty set
     */
    public ArrayUnenforcedSet() {
        super();
    }

    /**
     * Constructs a set containing the elements of the specified collection.
     *
     * @param c the collection whose elements are to be placed into this set
     * @throws NullPointerException if the specified collection is null
     */
    public ArrayUnenforcedSet(Collection<? extends X> c) {
        super((Iterable) c);
    }


    public ArrayUnenforcedSet(X... x) {
        super(x.length, x);
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
        int hashCode = 0, s = this.size;
        X[] ii = this.items;
        for (int i = 0; i < s; i++)
            hashCode += ii[i].hashCode();

        return hashCode;
    }


    @Override
    public boolean addAll(Collection<? extends X> source) {
        boolean changed = false;
        for (X x : source) {
            changed |= add(x);
        }
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