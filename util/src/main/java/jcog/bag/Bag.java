package jcog.bag;

import jcog.Util;
import jcog.list.table.Table;
import jcog.pri.Prioritized;
import jcog.pri.op.PriForget;
import jcog.util.FloatFloatToFloatFunction;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static jcog.bag.Bag.BagSample.*;


/**
 * K=key, V = item/value of type Item
 */
public interface Bag<K, V> extends Table<K, V> {

   
    @Override
    @Nullable
    V remove(K x);

    default V put(V x) {
        return put(x, null);
    }

    default void putAsync(V b) {
        put(b);
    }

    V put(V b, @Nullable MutableFloat overflowing);

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
    default V sample(Random rng) {
        Object[] result = new Object[1];
        sample(rng, ((Predicate<? super V>) (x) -> {
            result[0] = x;
            return false;
        }));
        return (V) result[0];
    }



    /* sample the bag, optionally removing each visited element as decided by the visitor's
     * returned value */
    Iterable<V> sample(Random rng, BagCursor<? super V> each);

    default Iterable<V> sample(Random rng, Predicate<? super V> each) {
        return sample(rng, (BagCursor<? super V>) ((x) -> each.test(x) ? BagSample.Next : BagSample.Stop));
    }

    default Stream<V> stream() {
        return StreamSupport.stream(this::spliterator, 0, false);
    }

    default Iterable<V> sample(Random rng, int max, Consumer<? super V> each) {
        return sampleOrPop(rng, false, max, each);
    }

    default Iterable<V> pop(Random rng, int max, Consumer<? super V> each) {
        return sampleOrPop(rng, true, max, each);
    }

    default Iterable<V> sampleOrPop(Random rng, boolean pop, int max, Consumer<? super V> each) {
        if (max == 0)
            return this;

        final int[] count = {max};
        return sample(rng, (BagCursor<? super V>) (x -> {
            each.accept(x);
            return ((--count[0]) > 0) ? (pop ? Remove : Next) : (pop ? RemoveAndStop : Stop);
        }));
    }

    /**
     * convenience macro for using sample(BagCursor).
     * continues while either the predicate hasn't returned false and
     * < max true's have been returned
     */
    default Iterable<V> sample(Random rng, int max, Predicate<? super V> kontinue) {
        if (max == 0)
            return this;

        final int[] count = {max};
        return sample(rng, (BagCursor<? super V>) ((x) ->
                (kontinue.test(x) && ((--count[0]) > 0)) ?
                        Next : Stop));
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

    /**
     * iterates all items in (approximately) descending priority
     * forEach may be used to avoid allocation of iterator instances
     */
    @Override
    Iterator<V> iterator();


    /**
     * Check if an item is in the bag.  both its key and its value must match the parameter
     *
     * @param it An item
     * @return Whether the Item is in the Bag
     */
    default boolean contains(K it) {
        return get(it) != null;
    }







    default boolean isEmpty() {
        return size() == 0;
    }

    /**
     * @return null if this is an event which was rejected on input, non-null if it was a re
     */
    default void onRemove(V value) {

    }

    /**
     * called if an item which was attempted to be inserted was not
     */
    default void onReject(V value) {

    }

    default void onAdd(V v) {

    }

    /**
     * returns the priority of a value, or NaN if such entry is not present
     */
    float pri(V key);

    /**
     * allows a Prioritized.priUpdate call in impl
     */
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

    /** true if an item is not deleted */
    default boolean active(V key) {
        float x = pri(key);
        return (x == x);
        
    }

    default float priElse(V key, float valueIfMissing) {
        float p = pri(key);
        return (p == p) ? p : valueIfMissing;
    }

    /**
     * resolves the key associated with a particular value
     */
    K key(V value);

    default void print() {
        print(System.out);
    }

    default void print(PrintStream p) {
        forEach(p::println);
    }

    /** scalar pri reducer arg 0 = accumulated value, arg 1 = priority WARNING may be NaN if an item is deleted */
    default float priIfy(float initial, FloatFloatToFloatFunction reduce) {
        float[] x = new float[] { initial };
        forEach(v -> {
           x[0] = reduce.apply(x[0], pri(v));
        });
        return x[0];
    }

    /** priIfy only non-deleted items */
    default float priIfyNonDeleted(float initial, FloatFloatToFloatFunction reduce) {
        float[] x = new float[] { initial };
        forEach(v -> {
            float p = pri(v);
            if (p==p)
                x[0] = reduce.apply(x[0], p);
        });
        return x[0];
    }
    /**
     * should visit items highest priority first, if possible.
     * for some bags this may not be possible.
     */
    
    
    default float priSum() {
        return priIfyNonDeleted(0, (sum, x) -> (sum + x));



    }

    /**
     * default slow implementation.
     * returns a value between 0..1.0. if empty, returns 0
     */
    default float priMin() {
        float m = priIfyNonDeleted(Float.POSITIVE_INFINITY, Math::min);
        return Float.isFinite(m) ? m : 0;
    }

    /**
     * default slow implementation.
     * returns a value between 0..1.0. if empty, returns 0
     */
    default float priMax() {
        float m = priIfyNonDeleted(Float.NEGATIVE_INFINITY, Math::max);
        return Float.isFinite(m) ? m : 0;
    }

    @Override
    void setCapacity(int c);

    @NotNull
    default float[] histogram(float[] x) {
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

    default float depressurize() {
        return 0; 
    }



    default Iterable<V> commit() {
        return commit(forget(PriForget.FORGET_TEMPERATURE_DEFAULT));
    }

    /**
     * creates a forget procedure for the current bag's
     * state, which can be applied as a parameter to the commit(Consumer<V>) method
     * temperature is a value between 0..1.0 controlling
     * how fast the bag should allow new items. 0.5 is a default value
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
    Bag<K, V> commit(Consumer<V> update);

    @Override
    default void forEachKey(Consumer<? super K> each) {
        forEach(b -> each.accept(key(b)));
    }

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
        BagSample next(V x); 
    }


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
        public Iterable sample(Random rng, BagCursor each) {
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
        public Iterable commit() {
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



}




































































































































































































































































