package alice.util;

import org.jetbrains.annotations.Nullable;

import java.util.Deque;
import java.util.Iterator;

public final class OneWayList<E> {

    public final E head;
    public final OneWayList<E> tail;

    public OneWayList(E head, @Nullable OneWayList<E> tail) {
        this.head = head;
        this.tail = tail;
    }

    public static <T> OneWayList<T> the(T d) {
        return new OneWayList<>(d, null);
    }

    public static <T> OneWayList<T> add(@Nullable OneWayList<T> list, T x) {
        return list == null ? the(x) : new OneWayList<>(x, list);
    }

    public static <T> OneWayList<T> get(Deque<T> d) {
        var s = d.size();
        switch (s) {
            case 0:
                return null;
            case 1:
                return the(d.getFirst());
            case 2:
                return new OneWayList<>(d.getFirst(), new OneWayList<>(d.getLast(), null));
            default:
                var i = d.descendingIterator();
                i.hasNext();
                var o = new OneWayList<T>(i.next(), null);
                while (i.hasNext()) {
                    o = new OneWayList<>(i.next(), o);
                }
                return o;
        }
    }


    public String toString() {
        var head = this.head;
        var elem = (head == null) ? "null" : head.toString();
        var tail = this.tail;
        return '[' + (tail == null ? elem : tail.toString(elem)) + ']';
    }

    private String toString(String elems) {
        var elem = head == null ? "null" : head.toString();
        if (tail == null) return elems + ',' + elem;
        return elems + ',' + tail.toString(elem);
    }

}