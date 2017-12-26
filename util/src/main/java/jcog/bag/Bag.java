package jcog.bag;

import jcog.Util;
import jcog.list.FasterList;
import jcog.list.table.Table;
import jcog.pri.PriReference;
import jcog.pri.Prioritized;
import jcog.pri.Priority;
import jcog.pri.op.PriForget;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static jcog.bag.Bag.BagSample.*;


/**
 * K=key, V = item/value of type Item
 */
public interface Bag<K, V> extends Table<K, V> {


    /**
     * action returned from bag sampling visitor indicating what to do with the current
     * item
     */
    enum BagSample {
        Next(false, false),
        Remove(true, false),
        Stop(false, true),
        RemoveAndStop(true, true);

        public final boolean remove;
        public final boolean stop;

        BagSample(boolean remove, boolean stop) {
            this.remove = remove;
            this.stop = stop;
        }
    }

    /**
     * used for sampling
     */
    @FunctionalInterface
    interface BagCursor<V> {
        /*@NotNull */BagSample next(/*@NotNull*/ V x); //@NotNull's removed for max speed, but kept here as reference
    }

    /**
     * returns the bag to an empty state
     */
    @Override
    void clear();

    /**
     * gets the next value without removing changing it or removing it from any index.  however
     * the bag is cycled so that subsequent elements are different.
     */
    @Nullable
    default V sample() {
        Object[] result = new Object[1];
        sample(((Predicate<? super V>)(x) -> {
            result[0] = x;
            return false;
        }));
        return (V) result[0];
    }


    /**
     * TODO rename 'remove'
     *
     * @param key
     * @return
     */
    @Override
    @Nullable
    V remove(/*@NotNull*/ K x);


    default V put(/*@NotNull */V x) {
        return put(x, null);
    }

    default void putAsync(/*@NotNull */V b) {
        put(b);
    }

    //        @Nullable
//        @Override public PLink<V> put(@NotNull V v) {
//            //TODO combine with CurveBag.put(v)
//            PLink<V> existing = get(v);
//            if (existing!=null) {
//                merge(existing, getDefaultBudget(v), 1f);
//                return existing;
//            } else {
//            }
//        }
//        @Nullable
//        @Override public PLink<V> put(@NotNull V v) {
//            PLink<V> existing = get(v);
//            return (existing != null) ?
//                    existing :
//                    put(v, getDefaultBudget(v));
//        }


    V put(/*@NotNull*/ V b, @Nullable MutableFloat overflowing);


    /* sample the bag, optionally removing each visited element as decided by the visitor's
     * returned value */
    /*@NotNull*/ Bag<K, V> sample(/*@NotNull */Bag.BagCursor<? super V> each);

    default Bag<K, V> sample(Predicate<? super V> each) {
        return sample((BagCursor<? super V>)((x) -> each.test(x) ? BagSample.Next : BagSample.Stop));
    }


    default List<V> copyToList(int n) {
        if (n == 0)
            return Collections.emptyList();

        List<V> l = new FasterList(n);
        sample(n, x -> {
            l.add(x);
        });
        return l;
    }


    /**
     * normalizes a priorty to within the present min/max range of this bag, and unitized to within 0..1.0 clipping if exceeded
     */
    default float normalizeMinMax(float pri) {
        return Util.unitize(Util.lerp(pri, priMin(), priMax()));
    }

    default void normalizeAll(float lerp) {

        int size = size();
        if (size == 0 || lerp < Prioritized.EPSILON)
            return;

        float min = priMin();
        float max = priMax();
        if (Util.equals(min, max, Prioritized.EPSILON)) {
            //flatten all to 0.5
            commit(x -> ((Priority) x).priLerp(0.5f, lerp));
        } else {
            float range = max - min;
            commit(x -> ((Priority) x).normalizePri(min, range, lerp));
        }
    }




    default Stream<V> stream() {
        return StreamSupport.stream(this::spliterator, 0, false);
    }

    default Bag<K, V> sample(int max, Consumer<? super V> each) {
        return sampleOrPop(false, max, each);
    }

    default Bag<K, V> pop(int max, Consumer<? super V> each) {
        return sampleOrPop(true, max, each);
    }

    default Bag<K, V> sampleOrPop(boolean pop, int max, Consumer<? super V> each) {
        if (max == 0)
            return this;

        final int[] count = {max};
        return sample((BagCursor<? super V>)(x -> {
            each.accept(x);
            return ((--count[0]) > 0) ? (pop ? Remove : Next) : (pop ? RemoveAndStop : Stop);
        }));
    }
    /**
     * convenience macro for using sample(BagCursor).
     * continues while either the predicate hasn't returned false and
     * < max true's have been returned
     */
    default Bag<K, V> sample(int max, Predicate<? super V> kontinue) {
        if (max == 0)
            return this;

        final int[] count = {max};
        return sample((BagCursor<? super V>)((x) ->
                (kontinue.test(x) && ((--count[0]) > 0)) ?
                    Next : Stop));
    }
    default Bag<K, V> sampleOrPop(boolean pop, int max, Predicate<? super V> each) {
        final int[] count = {max};
        return sample((BagCursor<? super V>)(x ->
                (each.test(x) && ((--count[0]) > 0)) ?
                        (pop ? Remove : Next)
                        :
                        (pop ? RemoveAndStop : Stop)));
    }

    @Nullable
    default V maxBy(FloatFunction<V> rank) {
        final float[] best = {Float.NEGATIVE_INFINITY};
        final Object[] which = new Object[1];
        //TODO if (y>=best[0]) collect several if they are of equal rank, then choose randomly. better yet make this a feature of Bag
        forEach(x -> {
            float y = rank.floatValueOf(x);
            if (y > best[0]) {
                best[0] = y;
                which[0] = x;
            }
        });
        return (V) which[0];
    }

    @FunctionalInterface
    interface EarlyFailRanker<X> {
        /**
         * return the computed value or Float.NaN if it
         * terminated early by deciding that the currently calculated
         * value would never exceed the provided current best,
         * which is initialized to NEGATIVE_INFINITY
         */
        float floatValueOf(X x, float mustExceed);
    }

    @Nullable
    default V maxBy(EarlyFailRanker<V> rank) {
        final float[] best = {Float.NEGATIVE_INFINITY};
        final Object[] which = new Object[1];
        forEach(x -> {
            float y = rank.floatValueOf(x, best[0]);
            if ((y == y) && (y > best[0])) {
                best[0] = y;
                which[0] = x;
            }
        });
        return (V) which[0];
    }

    /**
     * The number of items in the bag
     *
     * @return The number of items
     */
    @Override
    int size();


    /**
     * when adjusting the priority of links directly, to correctly absorb the pressure difference, call this
     */
    default void pressurize(float f) {

    }

    default float priAvg() {
        int s = size();
        return (s == 0) ? 0 : (priSum() / s);
    }


//    /** not used currently in Bag classes, but from CacheBag interface */
//    @Override public Consumer<V> getOnRemoval() {  return null;    }
//    /** not used currently in Bag classes, but from CacheBag interface */
//    @Override public void setOnRemoval(Consumer<V> onRemoval) { }

    /**
     * iterates all items in (approximately) descending priority
     * forEach may be used to avoid allocation of iterator instances
     */
    @NotNull
    @Override
    Iterator<V> iterator();

    /**
     * Check if an item is in the bag.  both its key and its value must match the parameter
     *
     * @param it An item
     * @return Whether the Item is in the Bag
     */
    default boolean contains(/*@NotNull*/ K it) {
        return get(it) != null;
    }

    default boolean isEmpty() {
        return size() == 0;
    }

    //    /**
//     * if the next item is true via the predicate, then it is TAKEn out of the bag; otherwise the item remains unaffected
//     */
//    public final V remove(final Predicate<V> iff) {
//        V v = peekNext();
//
//        if (v == null) return null;
//        if (iff.apply(v)) {
//            remove(v.name());
//            return v;
//        }
//        return null;
//    }

    /**
     * @return null if this is an event which was rejected on input, non-null if it was a re
     */
    default void onRemove(/*@NotNull*/ V value) {

    }

    /**
     * called if an item which was attempted to be inserted was not
     */
    default void onReject(/*@NotNull*/ V value) {

    }

    default void onAdd(/*@NotNull*/ V v) {

    }

    /**
     * returns the priority of a value, or NaN if such entry is not present
     */
    float pri(V key);

    /** allows a Prioritized.priUpdate call in impl */
    default float priUpdate(V key) {
        return pri(key);
    }

    default float pri(Object key, float ifMissing) {
        V x = get(key);
        if (x == null)
            return ifMissing;
        else
            return priElse(x, ifMissing);
    }

    default boolean active(V key) {
        float x = pri(key);
        return (x == x);
        //return priSafe(key, -1) >= 0;
    }


    default float priElse(V key, float valueIfMissing) {
        float p = pri(key);
        return (p == p) ? p : valueIfMissing;
    }


    /**
     * resolves the key associated with a particular value
     */
    K key(/*@NotNull*/ V value);


    default void print() {
        print(System.out);
    }

    default void print(@NotNull PrintStream p) {
        forEach(p::println);
    }

    /**
     * should visit items highest priority first, if possible.
     * for some bags this may not be possible.
     */
    //by default this will use iterator().forEach() but this can be used to make sure each implementation offers its best
    //@Override abstract public void forEach(final Consumer<? super V> action);
    default float priSum() {
        float[] total = {0};
        forEach(v -> total[0] = total[0] + priElse(v, 0));
        return total[0];
    }


//    final public int forgetNext(float forgetCycles, final V[] batch, final long now) {
//        return forgetNext(forgetCycles, batch, 0, batch.length, now, batch.length/2 /* default to max 1.5x */);
//    }

//    /** warning: slow */
//    public double getStdDev(StandardDeviation target) {
//        target.clear();
//        forEachEntry(x -> target.increment(x.getPriority()));
//        return target.getResult();
//    }


    /**
     * default slow implementation.
     * returns a value between 0..1.0. if empty, returns 0
     */
    default float priMin() {
        float[] min = {Float.POSITIVE_INFINITY};
        forEach(b -> {
            float p = priElse(b, Float.POSITIVE_INFINITY);
            if (p < min[0]) min[0] = p;
        });
        float m = min[0];
        if (Float.isFinite(m)) return m;
        else return 0;
    }

    /**
     * default slow implementation.
     * returns a value between 0..1.0. if empty, returns 0
     */
    default float priMax() {
        float[] max = {Float.NEGATIVE_INFINITY};
        forEach(b -> {
            float p = priElse(b, Float.NEGATIVE_INFINITY);
            if (p > max[0]) max[0] = p;
        });
        float m = max[0];
        if (Float.isFinite(m)) return m;
        else return 0;
    }


    @Override
    void setCapacity(int c);


//    /**
//     * for bags which maintain a separate name index from the items, more fine-granied access methods to avoid redundancy when possible
//     */
//    @Deprecated
//    abstract public static class IndexedBag<E extends Item<K>, K> extends Bag<K, E> {
//
//        public E TAKE(final K key) {
//            return take(key, true);
//        }
//
//
//        /**
//         * registers the item
//         */
//        abstract protected void index(E value);
//
//        /**
//         * unregisters it
//         */
//        abstract protected E unindex(K key);
//
//        protected E unindex(E e) {
//            return unindex(e.name());
//        }
//
//        abstract public E take(final K key, boolean unindex);
//
//        public E TAKE(E value) {
//            return TAKE(value.name());
//        }
//
//
//        /**
//         * Insert an item into the itemTable, and return the overflow
//         *
//         * @param newItem The Item to put in
//         * @return null if nothing was displaced and if the item itself replaced itself,
//         * or the The overflow Item if a different item had to be removed
//         */
//        abstract protected E addItem(final E newItem, boolean index);
//
//        public E PUT(final E newItem) {
//            return addItem(newItem, true);
//        }
//
//
//        /**
//         * Add a new Item into the Bag via a BagSelector interface for lazy or cached instantiation of Bag items
//         *
//         * @return the item which was removed, which may be the input item if it could not be inserted; or null if nothing needed removed
//         * <p/>
//         * WARNING This indexing-avoiding version not completely working yet, so it is not used as of this commit
//         */
//
//        public E putInFast(BagSelector<K, E> selector) {
//
//            E item = take(selector.name(), false);
//
//            if (item != null) {
//                item = (E) item.merge(selector);
//                final E overflow = addItem(item, false);
//                if (overflow == item) {
//                    unindex(item.name());
//                }
//                return overflow;
//            } else {
//                item = selector.newItem();
//
//                //compare by reference, sanity check
//                if (item.name() != selector.name())
//                    throw new RuntimeException("Unrecognized selector and resulting new instance have different name()'s: item=" + item.name() + " selector=" + selector.name());
//
//                // put the (new or merged) item into itemTable
//                return PUT(item);
//            }
//
//
//        }
//    }


    @NotNull
    default float[] histogram(/*@NotNull*/ float[] x) {
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


    default float depressurize(float frac) {
        frac = Util.unitize(frac);
        float p = depressurize();
        float pF = frac * p;
        if (pF >= Prioritized.EPSILON) {
            pressurize(p - pF);
            return pF;
        }
        return 0;
    }

    default float depressurize() {
        return 0f; //TODO
    }


    default Bag<K, V> commit() {
        return commit(forget(PriForget.FORGET_TEMPERATURE_DEFAULT));
    }

    /** creates a forget procedure for the current bag's
     *  state, which can be applied as a parameter to the commit(Consumer<V>) method
     *  temperature is a value between 0..1.0 controlling
     *  how fast the bag should allow new items. 0.5 is a default value
     */
    default @Nullable Consumer<V> forget(float temperature) {
        return PriForget.forget(this, temperature, Prioritized.EPSILON, PriForget::new);
    }


    float mass();

    /**
     * commits the next set of changes and updates budgeting
     *
     * @return this bag
     */
    @NotNull Bag<K, V> commit(Consumer<V> update);


    @Nullable Bag EMPTY = new Bag() {


        @Override
        public float mass() {
            return 0;
        }

        @Nullable
        @Override
        public Consumer forget(float temperature) {
            return null;
        }

        @Override
        public Bag sample(Bag.BagCursor each) {
            return this;
        }

        @Override
        public void clear() {
        }

        @Override
        public Object key(Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public float pri(Object key) {
            return 0;
        }


        @Nullable
        @Override
        public Object remove(Object x) {
            return null;
        }

        @Override
        public Object put(Object b, @Nullable MutableFloat overflowing) {
            return null;
        }

        @Override
        public int size() {
            return 0;
        }


        @Override
        public Iterator iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public boolean contains(Object it) {
            return false;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }


        @Override
        public void setCapacity(int c) {

        }

        @Override
        public Bag commit() {
            return this;
        }


        @Override
        public Bag commit(Consumer update) {
            return this;
        }


        @Nullable
        @Override
        public Object get(Object key) {
            return null;
        }

        @Override
        public void forEachKey(Consumer each) {

        }

        @Override
        public int capacity() {
            return 0;
        }

    };


//    @NotNull
//    default <Y> PLink<Y> newLink(@NotNull Y i) {
//
//        if (i instanceof Budgeted)
//            return new DependentBLink((Budgeted) i);
//        else
//            return new DefaultBLink(i);
//    }

    @Override
    default void forEachKey(Consumer<? super K> each) {
        forEach(b -> each.accept(key(b)));
    }


//    default boolean putIfAbsent(V b) {
//        K x = b.get();
//        if (contains(x))
//            return false;
//        if (put(b, 1f, null)==null)
//            return false;
//        return true;
//    }


    //TODO default @NotNull Bag<V> move(int limit, @NotNull Bag target) {

}
