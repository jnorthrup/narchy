package jcog.data.list;

import jcog.TODO;
import jcog.data.iterator.ArrayIterator;
import jcog.util.ArrayUtil;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.stream.Stream;

/** be careful about synchronizing to instances of this class
 * because the class synchronizes on itself and not a separate lock object
 * (for efficiency purposes)
 *
 * TODO size inherited from FasterList is not volatile
 */
public class FastCoWList<X> /*extends AbstractList<X>*/ /*implements List<X>*/ implements Iterable<X>, UnaryOperator<X[]> {

    public final FasterList<X> list;

    private final IntFunction<X[]> arrayBuilder;

    
    public final AtomicReference<X[]> copy = new AtomicReference(ArrayUtil.EMPTY_OBJECT_ARRAY);


    public FastCoWList(Class<X> x) {
        this((i)->(X[])Array.newInstance(x, i));
    }

    public FastCoWList(IntFunction<X[]> arrayBuilder) {
        this(0, arrayBuilder);
    }

    public FastCoWList(int capacity, IntFunction<X[]> arrayBuilder) {
        list = new FasterList<>(capacity);
        this.copy.set( (this.arrayBuilder = arrayBuilder).apply(0) );
    }
    
    protected Object[] newArray(int newCapacity) {
        return arrayBuilder.apply(newCapacity);
    }

    public void synch(Consumer<FastCoWList<X>> with) {
        synchronized(list) {
            with.accept(this);
        }
    }

    public void synchDirect(Predicate<FasterList<X>> with) {
        synchronized(list) {
            if (with.test(list))
                commit();
        }
    }

    public void sort() {
        synchronized (list) {
            if (list.size() > 1) {
                list.sortThis();
                commit();
            }
        }
    }

    public void commit() {
        //this.copy = //toArrayCopy(copy, arrayBuilder);
        copy.set(null);
    }

    //@Override
    public void clear() {
//        X[] empty = arrayBuilder.apply(0);
//        copy.updateAndGet((p)->{
//            if (p.length > 0) {
//                synchronized (list) {
//                    list.clear();
//                }
//                return empty;
//            } else
//                return p;
//        });
        synchronized (list) {
            if (list.clearIfChanged())
                commit();
        }
    }
    //@Override
    public Iterator<X> iterator() {
        return ArrayIterator.iterator(array());
    }



    //@Override
    public final int size() {
        X[] x = this.array();
        //return (this.size = (x != null ? x.length : 0));
        return x.length;
    }


    //@Override
    public X set(int index, X element) {

        synchronized (list) {
            if (list.size() <= index) {
                list.ensureCapacity(index + 1);
                if (element != null) {
                    list.setFast(index, element);
                    list.setSize(index + 1);
                    commit();
                }
                return null;
            } else {
                X[] ii = list.array();
                X old = ii[index];
                if (old!=element) {
                    ii[index] = element;
                    commit();
                }
                return old;
            }
        }


    }

    public void set(Collection<? extends X> newContent) {
        if (newContent.isEmpty())
            clear();
        else {
            synchronized (list) {
//            if (size() < minIdentityCompareThresh) {
//                if(list.equalsIdentically(newContent))
//            }
                list.clear();
                if (newContent instanceof FasterList)
                    list.addAllFaster((FasterList<X>) newContent);
                else
                    list.addAll(newContent);
                commit();
            }
        }
    }


    //@Override
    public void forEach(Consumer<? super X> c) {
        for (X x : array())
            c.accept(x);
    }

    public void forEachNonNull(Consumer<? super X> c) {
        for (X x : array()) {
            if (x!=null)
                c.accept(x);
        }
    }

    public final <Y> void forEachWith(Procedure2<? super X, ? super Y> c, Y y) {
        for (X x : array())
            c.accept(x, y);
    }


    //@Override
    public Stream<X> stream() {
        return ArrayIterator.stream(array());
    }


    public void reverseForEach(Procedure<X> c) {
        X[] copy = array();
//        if (copy != null) {
            for (int i = copy.length-1; i >= 0; i--) {
                c.accept(copy[i]);
            }
//        }
    }

    public final X[] array() {
        //modified updateAndGet: //return copy.updateAndGet(this);
        X[] prev = copy.getOpaque();
        return prev != null ? prev : commit(prev);
    }

    private X[] commit(X[] prev) {
        X[] next = null;
        boolean haveNext = false;
        while(true) {
            if (!haveNext)
                next = apply(prev);

            if (copy.compareAndSet(prev, next))
                return next;

            haveNext = prev == (prev = copy.get());
        }
    }

    @Override
    public final X[] apply(X[] current) {
        if (current != null) {
            return current;
        } else {
            return list.fillArray(arrayBuilder.apply(list.size()), false);
        }
    }

    //@Override
    public boolean remove(Object o) {
        synchronized (list) {
            if (removeDirect(o)) {
                commit();
                return true;
            }
            return false;
        }
    }
    public X remove(int index) {
        synchronized (list) {
            X removed = list.remove(index);
            if (removed!=null) {
                commit();
            }
            return removed;
        }
    }

    private boolean addDirect(X o) {
        return list.add(o);
    }
    private boolean removeDirect(Object o) {
        return list.remove(o);
    }

    //@Override
    public boolean add(X o) {
        synchronized (list) {
            if (addDirect(o)) {
                commit();
                return true;
            }
            return false;
        }
    }

    //@Override
    public boolean contains(Object object) {
        return ArrayUtil.indexOf(array(), object)!=-1;
    }
    public boolean containsInstance(Object object) {
        return ArrayUtil.indexOfIdentity(array(), object)!=-1;
    }

    //@Override
    public boolean addAll(Collection<? extends X> source) {
        if (source.isEmpty())
            return false;
        synchronized (list) {
            list.addAll(source);
            commit();
            return true;
        }
    }

    //@Override
    public boolean removeAll(Collection<?> source) {
        throw new TODO();
    }

    //@Override
    public void add(int index, X element) {
        synchronized (list) {
            list.add(index, element);
            commit();
        }
    }

    public boolean removeIf(Predicate<? super X> predicate) {
        synchronized (list) {
            if (list.removeIf(predicate)) {
                commit();
                return true;
            }
            return false;
        }
    }

    public boolean removeFirstInstance(X x) {
        synchronized (list) {
            if (list.removeFirstInstance(x)) {
                commit();
                return true;
            }
            return false;
        }
    }

    //@Override
    public final X get(int index) {
        X[] c = array();
        return c.length > index ? c[index] : null;
    }

    public float[] map(FloatFunction<X> f, float[] target) {
        X[] c = this.array();
        if (c == null)
            return ArrayUtil.EMPTY_FLOAT_ARRAY;
        int n = c.length;
        if (n != target.length) {
            target = new float[n];
        }
        for (int i = 0; i < n; i++) {
            target[i] = f.floatValueOf(c[i]);
        }
        return target;
    }

    /**
     * directly setAt
     */
    public void set(X[] newValues) {
        if (newValues.length == 0)
             clear();
        else {
            copy.accumulateAndGet(newValues, (p, v) -> {
                synchronized(list) {
                    list.setArray(v);
                }
                return v;
            });
        }
//        synchronized (list) {
//            if (newValues.length == 0) {
//                clear();
//            } else {
//                list.clear();
//                list.addingAll(newValues);
//                commit();
//            }
//        }
    }

    public boolean isEmpty() { return size() == 0; }

    public boolean AND(Predicate<X> o) {
        for (X x : array()) {
            if (!o.test(x))
                return false;
        }
        return true;
    }
    public boolean OR(Predicate<X> o) {
        for (X x : array()) {
            if (o.test(x))
                return true;
        }
        return false;
    }
    public boolean whileEach(Predicate<X> o) {
        for (X x : array()) {
            if (x!=null && !o.test(x))
                return false;
        }
        return true;
    }
    public boolean whileEachReverse(Predicate<X> o) {
        @Nullable X[] xx = this.array();
        for (int i = xx.length - 1; i >= 0; i--) {
            X x = xx[i];
            if (x!=null && !o.test(x))
                return false;
        }
        return true;
    }

    public double sumBy(FloatFunction<X> each) {
        double s =  0;
        for (X x : array())
            s += each.floatValueOf(x);
        return s;
    }
    public double sumBy(ToDoubleFunction<X> each) {
        double s =  0;
        for (X x : array())
            s += each.applyAsDouble(x);
        return s;
    }
    public double meanBy(FloatFunction<X> each) {
        double s =  0;
        int i = 0;
        for (X x : array()) {
            s += each.floatValueOf(x);
            i++;
        }
        return i > 0 ? s/i : 0;
    }

    public <Y> Y[] toArray(Y[] _target, Function<X, Y> f) {
        int s = size();
        if (s == 0) return _target.length == 0 ? _target : null;

        Y[] target = _target == null || _target.length < s ? Arrays.copyOf(_target, s) : _target;

        int i = 0; //HACK this is not good. use a AND predicate iteration or just plain iterator?

        for (X x : array()) {
            if (x!=null) {
                target[i++] = f.apply(x);
                if (i >= s)
                    break;
            }
        }

        //either trim the array. size could have decreased while iterating, or its perfect sized
        return i < target.length ? Arrays.copyOf(target, i) : target;

    }

    public void clear(Consumer<? super X> each) {
        removeIf((x)->{
            each.accept(x);
            return true;
        });
    }
}
