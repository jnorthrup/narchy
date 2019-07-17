package jcog.pri.bag;

import jcog.Util;
import jcog.data.NumberX;
import jcog.data.atomic.AtomicFloatFieldUpdater;
import jcog.data.atomic.MetalAtomicIntegerFieldUpdater;
import jcog.data.list.table.Table;
import jcog.pri.Forgetting;
import jcog.pri.Prioritizable;
import jcog.pri.ScalarValue;
import jcog.pri.op.PriMerge;
import jcog.util.ArrayUtil;
import jcog.util.FloatFloatToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static jcog.Util.assertUnitized;


/**
 * X=key, Y = item/value of type Item
 * TODO make an abstract class by replacing ArrayBag's superclass inheritance with delegation
 */
public abstract class Bag<X, Y> implements Table<X, Y>, Sampler<Y>, jcog.pri.Pressurizable {

    private static final AtomicFloatFieldUpdater<Sampler> MASS =
            new AtomicFloatFieldUpdater(Bag.class, "mass");
    private static final AtomicFloatFieldUpdater<Sampler> PRESSURE =
            new AtomicFloatFieldUpdater(Bag.class, "pressure");
    private static final MetalAtomicIntegerFieldUpdater<Sampler> CAPACITY =
            new MetalAtomicIntegerFieldUpdater(Bag.class, "capacity");

    private PriMerge merge;

    private volatile int mass, pressure, capacity;

    protected static <X, Y> void forEachActive(Bag<X, Y> bag, AtomicReferenceArray<Y> map, Consumer<? super Y> e) {
        forEach(map, bag::active, e);
    }

    private static <Y> void forEach(AtomicReferenceArray<Y> map, Predicate<Y> accept, Consumer<? super Y> e) {
        for (int c = map.length(), j = 0; j < c; j++) {
            Y v = map.getOpaque(j);
            if (v != null && accept.test(v)) {
                e.accept(v);
            }
        }
    }
    protected static <Y> void forEach(AtomicReferenceArray<Y> map, int max, Consumer<? super Y> e) {
        for (int c = map.length(), j = 0; j < c; j++) {
            Y v = map.getOpaque(j);
            if (v != null) {
                e.accept(v);
                if (--max <= 0)
                    return;
            }
        }
    }

    /**TODO make version of this which optionally tests for deletion */
    protected static <Y> void forEach(IntToObjectFunction<Y> accessor, int capacity, int max, Consumer<? super Y> e) {
        for (int j = 0; j < capacity; j++) {
            Y v = accessor.apply(j);
            if (v != null) {
                e.accept(v);
                if (--max <= 0)
                    return;
            }
        }
    }

    public static <Y> void forEach(AtomicReferenceArray<Y> map, Consumer<? super Y> e) {
        for (int c = map.length(), j = -1; ++j < c; ) {
            Y v = map
                    .getOpaque(j);

            if (v != null) {
                e.accept(v);
            }
        }
    }

//    @Nullable Bag EMPTY = new Bag() {
//
//        @Nullable
//        @Override
//        public Consumer forget(float temperature) {
//            return null;
//        }
//
//        @Override
//        public void sample(Random rng, Function each) {
//            //nothing
//        }
//
//        @Override
//        public void clear() {
//        }
//
//        @Override
//        public Object key(Object value) {
//            throw new UnsupportedOperationException();
//        }
//
//        @Override
//        public float pri(Object key) {
//            return 0;
//        }
//
//
//        @Nullable
//        @Override
//        public Object remove(Object x) {
//            return null;
//        }
//
//        @Override
//        public Object put(Object b, NumberX overflowing) {
//            return null;
//        }
//
//        @Override
//        public int size() {
//            return 0;
//        }
//
//
//        @Override
//        public Iterator iterator() {
//            return Collections.emptyIterator();
//        }
//
//        @Override
//        public boolean contains(Object it) {
//            return false;
//        }
//
//        @Override
//        public boolean isEmpty() {
//            return true;
//        }
//
//
//        @Override
//        public void setCapacity(int c) {
//
//        }
//
//        @Override
//        public Iterable commit() {
//            return this;
//        }
//
//
//        @Override
//        public Bag commit(Consumer update) {
//            return this;
//        }
//
//        @Nullable
//        @Override
//        public Object get(Object key) {
//            return null;
//        }
//
//        @Override
//        public void forEachKey(Consumer each) {
//
//        }
//
//        @Override
//        public int capacity() {
//            return 0;
//        }
//
//    };


    abstract protected void onCapacityChange(int before, int after);

    @Override
    @Nullable
    abstract public Y remove(X x);

    public Y put(Y x) {
        return put(x, null);
    }

    public void putAsync(Y b) {
        put(b);
    }

    public void putAll(Consumer<Consumer<Y>> c) {
        c.accept(this::put);
    }

    abstract public Y put(Y b, NumberX overflowing);

    /**
     * returns the bag to an empty state.
     * should also depressurize to zero
     */
    @Override
    abstract public void clear();

    public Stream<Y> stream() {
        return StreamSupport.stream(this::spliterator, 0, false);
    }

    public final Y[] toArray(@Nullable Y[] _target) {
        return toArray(_target, y->y);
    }

    /** subclasses may have more efficient ways of doing this */
    public <Z> Z[] toArray(@Nullable Z[] _target, Function<Y,Z> apply) {
        int s = size();
        if (s == 0) return (Z[]) ArrayUtil.EMPTY_OBJECT_ARRAY;

        Z[] target = _target == null || _target.length < s ? Arrays.copyOf(_target, s) : _target;

        final int[] i = {0}; //HACK this is not good. use a AND predicate iteration or just plain iterator?

        forEach(s, (y) -> target[i[0]++] = apply.apply(y));

        //either trim the array. size could have decreased while iterating, or its perfect sized
        return i[0] < target.length ? Arrays.copyOf(target, i[0]) : target;
    }

    @Nullable public <X> X reduce(BiFunction<Y,X,X> each, X init) {
        X x = init;
        for (Y y : this) {
            x = each.apply(y, x);
        }
        return x;
    }

    /**
     * iterates all items in (approximately) descending priority
     * forEach may be used to avoid allocation of iterator instances
     */
    @Override
    abstract public Iterator<Y> iterator();

    /**
     * Check if an item is in the bag.  both its key and its value must match the parameter
     *
     * @param it An item
     * @return Whether the Item is in the Bag
     */
    public boolean contains(X it) {
        return get(it) != null;
    }

    public boolean isEmpty() {
        return size() == 0;
    }


    public void onRemove(Y value) {

    }

    public void onEvict(Y incoming) {
        if (incoming instanceof Prioritizable)
            ((Prioritizable)incoming).delete();
    }

    /**
     * called if an item which was attempted to be inserted was not
     */
    public void onReject(Y value) {

    }

    public void onAdd(Y y) {

    }

    /**
     * returns the priority of a value, or NaN if such entry is not present
     */
    abstract public float pri(Y key);




    /**
     * true if an item is not deleted
     */
    public final boolean active(Y key) {
        float x = pri(key);
        return (x == x);

    }

    public float priElse(Y key, float valueIfMissing) {
        float p = pri(key);
        return (p == p) ? p : valueIfMissing;
    }

    /**
     * resolves the key associated with a particular value
     */
    abstract public X key(Y value);

    public void print() {
        print(System.out);
    }

    public void print(PrintStream p) {
        forEach(p::println);
    }

    abstract public void forEach(int max, Consumer<? super Y> action);

    /**
     * priIfy only non-deleted items
     */
    public float priIfyNonDeleted(float initial, FloatFloatToFloatFunction reduce) {
        float[] x = new float[]{initial};
        forEach(y -> {
            float p = pri(y);
            if (p == p)
                x[0] = reduce.apply(x[0], p);
        });
        return x[0];
    }

    /**
     * should visit items highest priority first, if possible.
     * for some bags this may not be possible.
     */


    public float priSum() {
        return priIfyNonDeleted(0, Float::sum);
    }

    /**
     * public slow implementation.
     * returns a value between 0..1.0. if empty, returns 0
     */
    @Deprecated public float priMin() {
        float m = priIfyNonDeleted(Float.POSITIVE_INFINITY, Math::min);
        return Float.isFinite(m) ? m : 0;
    }

    /**
     * public slow implementation.
     * returns a value between 0..1.0. if empty, returns 0
     */
    @Deprecated public float priMax() {
        float m = priIfyNonDeleted(Float.NEGATIVE_INFINITY, Math::max);
        return Float.isFinite(m) ? m : 0;
    }



    public final void setCapacity(int capNext) {
        assert(capNext >= 0);
        int capBefore = CAPACITY.getAndSet(this, capNext);
        if (capBefore!=capNext)
            onCapacityChange(capBefore, capNext);
    }

    public final <B extends Bag<X,Y>> B capacity(int c) {
        setCapacity(c);
        return (B) this;
    }

    public float[] histogram(float[] x) {
        int bins = x.length;
        forEach(budget -> {
            float p = priElse(budget, 0);
            int b = Util.bin(p, bins - 1);
            x[b]++;
        });
        double total = 0;
        for (float e : x) {
            total += e;
        }
        if (total > 0) {
            for (int i = 0; i < bins; i++)
                x[i] /= total;
        }
        return x;
    }


    public Iterable<Y> commit() {
        return commit(forget(1));
    }

    /**
     * creates a forget procedure for the current bag's
     * state, which can be applied as a parameter to the commit(Consumer<Y>) method
     * temperature is a value between 0..1.0 controlling
     * how fast the bag should allow new items. 0.5 is a public value
     */
    @Deprecated public @Nullable Consumer<Y> forget(float temperature) {
        return Forgetting.forget(this, temperature);
    }

    /**
     * commits the next set of changes and updates budgeting
     *
     * @return this bag
     */
    abstract public Bag<X, Y> commit(Consumer<Y> update);

    @Override
    public void forEachKey(Consumer<? super X> each) {
        forEach(b -> each.accept(key(b)));
    }

    /** sets the bag's merge strategy */
    public void merge(PriMerge merge) {
        this.merge = merge;
    }

    /** gets the bag's merge strategy */
    public final PriMerge merge() {
        return merge;
    }
    protected final void massAdd(float m) {
        MASS.add(this, m);
    }
    protected final void massSet(float m) {
        MASS.set(this, m);
    }
    protected final void massZero() {
        MASS.zero(this);
    }
    protected final void pressureZero() {
        MASS.zero(this);
    }

    /**
     * The NumberX of items in the bag
     *
     * @return The NumberX of items
     */
    @Override
    abstract public int size();

    @Override
    public int capacity() {
        /** TODO move implementation to an AbstractBag instance that has such a field and implements an abstract method from this class */
        //return CAPACITY.getOpaque(this);
        return capacity;
    }

    public float mass() {
        /** TODO move implementation to an AbstractBag instance that has such a field and implements an abstract method from this class */
        return MASS.getOpaque(this);
    }
    @Override
    public float pressure() {
        /** TODO move implementation to an AbstractBag instance that has such a field and implements an abstract method from this class */
        return PRESSURE.getOpaque(this);
    }

    protected final void pressureSet(float m) {
        PRESSURE.set(this, m);
    }


    /**
     * WARNING this is a duplicate of code in hijackbag, they ought to share this through a common Pressure class extending AtomicDouble or something
     */
    @Override
    public void pressurize(float f) {
        if (f == f && Math.abs(f) > Float.MIN_NORMAL)
            PRESSURE.add(this, f);
    }

    /**
     * WARNING this is a duplicate of code in hijackbag, they ought to share this through a common Pressure class extending AtomicDouble or something
     */
    @Override
    public void depressurize(float toRemove) {
        if (toRemove == toRemove && toRemove > Float.MIN_NORMAL)
            PRESSURE.update(this, (p, a) -> Math.max(0, p - a), toRemove);
    }

    @Override
    public float depressurizePct(float percentToRemove) {
        assertUnitized(percentToRemove);
        if (percentToRemove < ScalarValue.EPSILON) {
            return 0; //remove nothing
        }

        float percentToRemain = 1-percentToRemove;

        float[] delta = new float[1];
        PRESSURE.update(this, (priBefore, f) -> {
            float priAfter = priBefore * f;
            delta[0] = priBefore - priAfter;
            return priAfter;
        }, percentToRemain);

        return Math.max(0, delta[0]);
    }


//    /**
//     * TODO super-bag acting as a router for N sub-bags
//     */
//    abstract class CompoundBag<K, Y> implements Bag<K, Y> {
//        abstract public Bag<K, Y> bag(int selector);
//
//        /**
//         * which bag to associate with a keys etc
//         */
//        abstract protected int insertToWhich(K key);
//    }
}




































































































































































































































































