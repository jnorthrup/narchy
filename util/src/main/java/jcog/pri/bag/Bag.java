package jcog.pri.bag;

import jcog.Util;
import jcog.data.NumberX;
import jcog.data.list.table.Table;
import jcog.pri.op.PriForget;
import jcog.util.FloatFloatToFloatFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


/**
 * K=key, V = item/value of type Item
 */
public interface Bag<K, V> extends Table<K, V>, Sampler<V>, jcog.pri.Pressurizable {


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
        public void sample(Random rng, Function each) {
            //nothing
        }

        @Override
        public float pressure() {
            return 0;
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
        public Object put(Object b, NumberX overflowing) {
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

        @Override
        public float depressurizePct(float rate) {
            return 0;
        }

        @Override
        public void depressurize(float pri) {

        }
    };

    @Override
    @Nullable
    V remove(K x);

    default V put(V x) {
        return put(x, null);
    }

    default void putAsync(V b) {
        put(b);
    }

    V put(V b, NumberX overflowing);

    /**
     * returns the bag to an empty state
     */
    @Override
    void clear();

    default Stream<V> stream() {
        return StreamSupport.stream(this::spliterator, 0, false);
    }

    /**
     * The NumberX of items in the bag
     *
     * @return The NumberX of items
     */
    @Override
    int size();

    /**
     * when adjusting the priority of links directly, to correctly absorb the pressure difference, call this
     */
    @Override
    default void pressurize(float f) {

    }

    @Nullable default <X> X reduce(BiFunction<V,X,X> each, X init) {
        X x = init;
        for (V v : this) {
            x = each.apply(v, x);
        }
        return x;
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



    default float pri(Object key, float ifMissing) {
        V x = get(key);
        if (x == null)
            return ifMissing;
        else {
            return priElse(x, ifMissing);
        }
    }

    /**
     * true if an item is not deleted
     */
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


    /**
     * priIfy only non-deleted items
     */
    default float priIfyNonDeleted(float initial, FloatFloatToFloatFunction reduce) {
        float[] x = new float[]{initial};
        forEach(v -> {
            float p = pri(v);
            if (p == p)
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


    default Iterable<V> commit() {
        return commit(forget(PriForget.FORGET_TEMPERATURE_DEFAULT));
    }

    /**
     * creates a forget procedure for the current bag's
     * state, which can be applied as a parameter to the commit(Consumer<V>) method
     * temperature is a value between 0..1.0 controlling
     * how fast the bag should allow new items. 0.5 is a default value
     */
    @Deprecated default @Nullable Consumer<V> forget(float temperature) {

        if (temperature > Float.MIN_NORMAL) {
            int size = size();
            if (size > 0) {
                int cap = capacity();
                if (cap > 0) {

                    float pressure = depressurizePct(1);

                    float mass = mass();
                    if (mass > Float.MIN_NORMAL) {

                        Consumer eachMustForgetPct =


                            //PriForget.forgetPressure(temperature, 0, size, cap, pressure, mass);
                            PriForget.forgetIdeal(temperature, 0.5f, size, cap, pressure, mass);

                                //OVERDRIVE (attenuated by size/capacity ratio)
                                //PriForget.forgetPressure(temperature * (((float)size)/cap), cap, pressure, mass);

//                                //..?
//                                PriForget.forgetIdeal(temperature,
//                                        ScalarValue.EPSILON * cap,
//                                        //1f/size,
//                                        //1f/cap,
//                                        //0.1f,
//                                        //0.5f,
//                                        size, cap, pressure, mass);

                        if (eachMustForgetPct != null)
                            return eachMustForgetPct;

                    }
                }
            }
        }

        return null;

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


//    /**
//     * TODO super-bag acting as a router for N sub-bags
//     */
//    abstract class CompoundBag<K, V> implements Bag<K, V> {
//        abstract public Bag<K, V> bag(int selector);
//
//        /**
//         * which bag to associate with a keys etc
//         */
//        abstract protected int insertToWhich(K key);
//    }
}




































































































































































































































































