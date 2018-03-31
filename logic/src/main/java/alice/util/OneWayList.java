package alice.util;

import java.util.Deque;
import java.util.Iterator;

public final class OneWayList<E> {

    public final E head;
    public final OneWayList<E> tail;

    public OneWayList(E head, OneWayList<E> tail) {
        this.head = head;
        this.tail = tail;
    }

//    public static <T> OneWayList<T> transform(List<T> list) {
//        return list.isEmpty() ?
//                null :
//                new OneWayList<>(
//                        list.remove(0),
//                        transform(list)
//                );
//    }

//    public static <T> OneWayList<T> transform2(List<T> list) {
//        int s = list.size();
//        return s == 0 ?
//                null :
//                new OneWayList<>(
//                        list.get(0),
//                        s > 1 ? transform2(list.subList(1, list.size())) : null
//                );
//    }

    public static <T> OneWayList<T> get(Deque<T> d) {
        int s = d.size();
        switch (s) {
            case 0:
                return null;
            case 1:
                return new OneWayList<>(d.getFirst(), null);
            case 2:
                return new OneWayList<>(d.getFirst(), new OneWayList<>(d.getLast(), null));
            default:
                Iterator<T> i = d.descendingIterator();
                i.hasNext(); //trigger just in case
                OneWayList<T> o = new OneWayList<>(i.next(), null); //inner seed
                while (i.hasNext()) {
                    o = new OneWayList<>(i.next(), o); //wrap in prevous
                }
                return o;
        }
    }
//    /**
//     * Transforms given list into a OneWayList without any modification
//     * to it
//     *
//     * Method introduced during revision by Paolo Contessi
//     *
//     * @param list  Input list to be transformed
//     * @return      An equivalent OneWayList
//     */
//    public static <T> OneWayList<T> transform2(List<T> list){
//        if (list.isEmpty())
//            return null;
//
//        //OneWayList<T> result = null;
//        OneWayList<T> p = null;
//
//        for(T obj : list){
//            p = new OneWayList<T>(obj, p);
//            //result = l;
//            //p = ((result == null ? result : p.tail = l));
//
//        }
//
//        return p;
//    }

    //    public void setHead(E head) {
//        this.head = head;
//    }


    //    public void setTail(OneWayList<E> tail) {
//        this.tail = tail;
//    }

//    public void addLast(OneWayList<E> newTail) {
//        OneWayList<E> tail = this.tail;
//        if (tail == null) {
//            this.tail = newTail;
//            return;
//        }
//        tail.addLast(newTail);
//    }
//
//    public OneWayList<E> get(int index) {
//        OneWayList<E> tail = this.tail;
//        if (tail == null) throw new NoSuchElementException();
//        return index <= 0 ? this : tail.get(index - 1);
//    }

    public String toString() {
        E head = this.head;
        String elem = (head == null) ? "null" : head.toString();
        OneWayList<E> tail = this.tail;
        return '[' + (tail == null ? elem : tail.toString(elem)) + ']';
    }

    private String toString(String elems) {
        String elem;
        elem = head == null ? "null" : head.toString();
        if (tail == null) return elems + ',' + elem;
        return elems + ',' + tail.toString(elem);
    }

}