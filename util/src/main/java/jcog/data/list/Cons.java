package jcog.data.list;

import com.google.common.collect.Iterators;
import jcog.TODO;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Consumer;

/** immutable cons list useful for growing paths */
public class Cons<T> extends AbstractList<T> {

    private final List<T> head;
    public final T tail;

    public static <T> List<T> the(List<T> f, T r) {
		//new FasterList(1).with(r);
		return f.isEmpty() ? List.of(r) : new Cons(f, r);
    }

    private Cons(List<T> f, T r) {
        head = f;
        tail = r;
    }

    @Override
    public int hashCode() {
        throw new TODO();
    }

    @Override
    public boolean equals(Object o) {
        throw new TODO();
    }

    @Override
    public boolean add(T t) {
        throw new TODO();
    }

    @Override
    public boolean remove(Object o) {
        throw new TODO();
    }

    @Override
    public T get(int index) {
		return index < head.size() ? head.get(index) : tail;
//        else if (index == head.size())
//            return tail;
//        else
//            throw new TODO();
    }

    public boolean isEmpty() { return false; }

    @Override
    public int size() {
        return head.size()+1;
    }

    @Override
    public Iterator<T> iterator() {
        return Iterators.concat(head.iterator(), Iterators.singletonIterator(tail));
    }

    @Override
    public ListIterator<T> listIterator() {
        throw new TODO();
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        for (T t : head) {
            action.accept(t);
        }
        action.accept(tail);
    }


}



