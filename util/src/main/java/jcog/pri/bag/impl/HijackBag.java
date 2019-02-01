package jcog.pri.bag.impl;

import jcog.Paper;
import jcog.Skill;
import jcog.Util;
import jcog.data.NumberX;
import jcog.data.atomic.AtomicFloatFieldUpdater;
import jcog.data.atomic.MetalAtomicIntegerFieldUpdater;
import jcog.decide.Roulette;
import jcog.mutex.SpinMutex;
import jcog.mutex.SpinMutexArray;
import jcog.pri.ScalarValue;
import jcog.pri.bag.Bag;
import jcog.random.SplitMix64Random;
import jcog.util.ArrayUtils;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static jcog.pri.bag.impl.HijackBag.Mode.*;

/**
 * unsorted priority queue with stochastic replacement policy
 * <p>
 * it uses a AtomicReferenceArray<> to hold the data but Unsafe CAS operations might perform better (i couldnt get them to work like NBHM does).  this is necessary when an index is chosen for replacement that it makes certain it was replacing the element it thought it was (that it hadnt been inter-hijacked by another thread etc).  on an insert i issue a ticket to the thread and store this in a small ConcurrentHashMap<X,Integer>.  this spins in a busy putIfAbsent loop until it can claim the ticket for the object being inserted. this is to prevent the case where two threads try to insert the same object and end-up puttnig two copies in adjacent hash indices.  this should be rare so the putIfAbsent should usually work on the first try.  when it exits the update critical section it removes the key,value ticket freeing it for another thread.  any onAdded and onRemoved subclass event handling happen outside of this critical section, and all cases seem to be covered.
 */
@Paper
@Skill("Concurrent_computing")
public abstract class HijackBag<K, V> implements Bag<K, V> {

    private static final MetalAtomicIntegerFieldUpdater<HijackBag> SIZE =
            new MetalAtomicIntegerFieldUpdater(HijackBag.class, "size");
    private static final MetalAtomicIntegerFieldUpdater<HijackBag> CAPACITY =
            new MetalAtomicIntegerFieldUpdater(HijackBag.class, "capacity");
    private static final AtomicFloatFieldUpdater<HijackBag> PRESSURE =
            new AtomicFloatFieldUpdater(HijackBag.class, "pressure");

    private static final AtomicReferenceFieldUpdater<HijackBag, AtomicReferenceArray> MAP =
            AtomicReferenceFieldUpdater.newUpdater(HijackBag.class, AtomicReferenceArray.class, "map");

    private static final SpinMutex mutex = new SpinMutexArray();
    private static final AtomicInteger serial = new AtomicInteger();

    /**
     * internal random NumberX generator, used for deciding hijacks but not sampling.
     */
    private final SplitMix64Random rng;

    /**
     * id unique to this bag instance, for use in treadmill
     */
    private final int id;

    private volatile int size, capacity, pressure;
    private volatile float min, max, mass;

    /**
     * TODO make non-public
     */
    volatile AtomicReferenceArray<V> map;


    /**
     * when size() reaches this proportion of space(), and space() < capacity(), grows
     */
    private static final float loadFactor = 0.5f;

    /**
     * how quickly the current space grows towards the full capacity, using LERP
     */
    private static final float growthLerpRate = 0.5f;


    private static final AtomicReferenceArray EMPTY_ARRAY = new AtomicReferenceArray(0);

    public final int reprobes;


    protected HijackBag(int initialCapacity, int reprobes) {
        this.id = serial.getAndIncrement();
        this.rng = new SplitMix64Random(id);

        assert (reprobes < Byte.MAX_VALUE - 1);
        this.reprobes = reprobes;
        this.map = EMPTY_ARRAY;

        if (initialCapacity > 0)
            setCapacity(initialCapacity);
    }

    protected HijackBag(int reprobes) {
        this(0, reprobes);
    }

    public static boolean hijackGreedy(float newPri, float weakestPri) {
        return weakestPri <= newPri;
    }

    private static <X, Y> void forEachActive(HijackBag<X, Y> bag, AtomicReferenceArray<Y> map, Consumer<? super Y> e) {
        forEach(map, bag::active, e);
    }

    @Override
    public float pressure() {
        return PRESSURE.getOpaque(this);
    }

    @Override
    public void pressurize(float f) {
        PRESSURE.add(this, f);
    }

    private static <Y> void forEach(AtomicReferenceArray<Y> map, Predicate<Y> accept, Consumer<? super Y> e) {
        for (int c = map.length(), j = 0; j < c; j++) {
            Y v = map
                    .getOpaque(j);

            if (v != null && accept.test(v)) {
                e.accept(v);
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

    @Override
    public void setCapacity(int _newCapacity) {

        int newCapacity = _newCapacity > 0 ? Math.max(_newCapacity, reprobes) : 0;

        if (CAPACITY.getAndSet(this, newCapacity) != newCapacity) {

            int s = space();
            if (s == 0 || newCapacity < s /* must shrink */) {
                resize(newCapacity);
            }


        }


    }

    protected void resize(int newSpace) {
        final AtomicReferenceArray<V>[] prev = new AtomicReferenceArray[1];


        AtomicReferenceArray<V> next = newSpace != 0 ? new AtomicReferenceArray<>(newSpace) : EMPTY_ARRAY;
        if (next == MAP.updateAndGet(this, (x) -> {
            if (x.length() != newSpace) {
                prev[0] = x;
                return next;
            } else return x;
        })) {


            forEachActive(this, prev[0], (b) -> {
                if (put(b) == null)
                    _onRemoved(b);
            });

            commit(null);
        }
    }


    @Override
    public void clear() {
        AtomicReferenceArray<V> x = reset(reprobes);
        PRESSURE.zero(this);
        SIZE.set(this, 0);
        mass = 0;
        if (x != null) {
            forEachActive(this, x, this::_onRemoved);
        }

    }

    @Nullable
    private AtomicReferenceArray<V> reset(int space) {

        if (!SIZE.compareAndSet(this, 0, 0)) {
            AtomicReferenceArray<V> newMap = new AtomicReferenceArray<>(space);

            AtomicReferenceArray<V> prevMap = MAP.getAndSet(this, newMap);

            commit();

            return prevMap;
        }

        return null;
    }

    /**
     * the current capacity, which is less than or equal to the value returned by capacity()
     */
    public int space() {
        return map.length();
    }

    public float density() {
        AtomicReferenceArray<V> m = map;
        int mm = m.length();
        int filled = 0;
        for (int i = 0; i < mm; i++) {
            if (m.getOpaque(i) != null)
                filled++;
        }
        return ((float) filled) / mm;
    }

    enum Mode {
        GET, PUT, REMOVE
    }

    /**
     * core update function
     * TODO add compacting procedure which, if nulls are detected while scanning and a
     * a result is found after it, to move the result to the earlier null position.
     */
    private V update(/*@NotNull*/ Object k, @Nullable V incoming /* null to remove */, Mode mode, @Nullable NumberX overflowing) {

        final AtomicReferenceArray<V> map = this.map;
        int c = map.length();
        if (c == 0)
            return null;


        final int kHash = hash(k);

        float incomingPri;
        if (mode == Mode.PUT) {
            if ((incomingPri = pri(incoming)) != incomingPri)
                return null;
            pressurize(incomingPri);
        } else {
            incomingPri = Float.POSITIVE_INFINITY; /* shouldnt be used */
        }


        int mutexTicket = -1;
        V toAdd = null, toRemove = null, toReturn = null;

        int start = (kHash % c);
        if (start < 0)
            start += c;

        if (mode != GET) {
            mutexTicket = mutex.start(id, kHash);
        }

        try {


            probing:
            for (int i = start, probe = reprobes; probe > 0; probe--) {

                V p = map.get(i);
                      //  map.get(i);

                if (p != null && keyEquals(k, kHash, p)) {
                    switch (mode) {

                        case GET:
                            return p;


                        case PUT:
                            if (p == incoming) {
                                toReturn = p;
                            } else {
                                float priBefore = pri(p);
                                V next = merge(p, incoming, overflowing);
                                if (next != null && (next == p || map.compareAndSet(i, p, next))) {
                                    if (next != p) {
                                        toRemove = p;
                                        toAdd = next;
                                    }
                                    toReturn = next;
                                }
                            }
                            break;

                        case REMOVE:
                            if (map.compareAndSet(i, p, null)) {
                                toReturn = toRemove = p;
                            }
                            break;
                    }

                    break probing;
                }

                if (++i == c) i = 0;
            }

            if (mode == PUT && toReturn == null) {

                int victim = -1, j = 0;
                float victimPri = Float.POSITIVE_INFINITY;
                for (int i = start; j < reprobes; j++) {
                    V mi = map.get(i);

                    float mp;
                    if (mi == null || ((mp = pri(mi)) != mp)) {
                        if (map.compareAndSet(i, mi, incoming)) {
                            toReturn = toAdd = incoming; /** became empty or deleted, take the slot */
                            break;
                        } else {
                            continue; //retry the slot since it had changed
                        }
                    }
                    if (mp < victimPri) {
                        victim = i;
                        victimPri = mp;
                    }
                    if (++i == c) i = 0;
                }


                if (toReturn == null) {
                    assert (victim != -1);

                    V existing = map.get(victim); //map.get(victim);
                    if (existing == null) {
                        //acquired new empty cell
                        toReturn = toAdd = incoming;

                    } else {
                        if (replace(incomingPri, existing) && map.compareAndSet(victim, existing, incoming)) {
                            //acquired
                            toRemove = existing;
                            toReturn = toAdd = incoming;
                        }
                    }

                }

            }

        } catch (Throwable t) {

            throw new RuntimeException(t);

        } finally {
            if (mode != GET)
                mutex.end(mutexTicket);
        }


        int size = SIZE.addAndGet(this, (toAdd != null ? +1 : 0) + (toRemove != null ? -1 : 0));


        if (toAdd != null) {
            _onAdded(toAdd);

            if (attemptRegrowForSize(toRemove != null ? (size + 1) /* hypothetical size if we can also include the displaced */ : size /* size which has been increased by the insertion */)) {

                if (toRemove != null) {
                    update(key(toRemove), toRemove, Mode.PUT, null);
                    toRemove = null;
                }
            }
        }

        if (toRemove != null) {
            if (map == this.map) {
                _onRemoved(toRemove);
            }
        }


        return toReturn;
    }

    protected boolean keyEquals(Object k, int kHash, V p) {
        return k.equals(key(p));
    }


    private int hash(Object x) {
        return x.hashCode();
    }


    /**
     * if no existing value then existing will be null.
     * this should modify the existing value if it exists,
     * or the incoming value if none.
     * <p>
     * if adding content, pressurize() appropriately
     * <p>
     * <p>
     * NOTE:
     * this should usually equal the amount of priority increased by the
     * insertion (considering the scale's influence too) as if there were
     * no existing budget to merge with, even if there is.
     * <p>
     * this supports fairness so that existing items will not have a
     * second-order budgeting advantage of not contributing as much
     * to the presssure as new insertions.
     * <p>
     * if returns null, the merge is considered failed and will try inserting/merging
     * at a different probe location
     */
    protected abstract V merge(V existing, V incoming, NumberX overflowing);

    /**
     * true if the incoming priority is sufficient to replace the existing value
     * can override in subclasses for custom replacement policy.
     * <p>
     * a potential eviction can be intercepted here
     * <p>
     * returns incomingPower, possibly reduced by the fight with the existing.
     * or NaN if the incoming wins
     */
    protected boolean replace(float incomingPri, V existing) {
        float existingPri = pri(existing);
        if (existingPri != existingPri) {
            return true; //became deleted
        } else {
            return replace(incomingPri, existingPri);
//                priAdd(existing, -incomingPri / reprobes);
//                return Float.NaN;
//            } else {
//                return Math.max((float) 0, incomingPri - existingPri / reprobes);
//            }
        }
    }

    abstract public void priAdd(V entry, float amount);

    private boolean replace(float incoming, float existing) {
        return hijackFair(incoming, existing);
    }

    @Nullable
    @Override
    public V remove(K k) {
        return update(k, null, REMOVE, null);
    }

    /**
     * roulette fair
     */
    private boolean hijackFair(float newPri, float oldPri) {
        return rng.nextFloat() < newPri / (newPri + oldPri);

//        float priEpsilon = ScalarValue.EPSILON;
//
//        if (oldPri > priEpsilon) {
//
//
//            float newPriSlice = temperature * newPri / reprobes;
//            float thresh = newPriSlice / (newPriSlice + oldPri);
//            return rng.nextFloat() < thresh;
//        } else {
//            return (newPri >= priEpsilon) || (rng.nextFloat() < (1.0f / reprobes));
//        }
    }


    protected boolean hijackSoftmax2(float newPri, float oldPri, Random random) {


        newPri = newPri * newPri * reprobes;
        oldPri = oldPri * oldPri * reprobes;
        if (oldPri > 2 * Float.MIN_NORMAL) {
            float thresh = 1.0f - (1.0f - (oldPri / (newPri + oldPri)));
            return random.nextFloat() > thresh;
        } else {
            return (newPri >= Float.MIN_NORMAL) || random.nextBoolean();

        }
    }

    /**
     *
     */
    @Override
    public final V put(/*@NotNull*/ V v,  /* TODO */ NumberX overflowing) {

        K k = key(v);
        if (k == null)
            return null;

        //HACK this should depressurize even if overflowing is null..
//        float oBefore;
//        if (overflowing == null) {
//            overflowing = new MutableFloat(0);
//            oBefore = 0;
//        } else {
//            oBefore = overflowing.floatValue();
//        }

        V x = update(k, v, PUT, overflowing);


//        float oAfter = overflowing.floatValue();
//        float oDelta = oAfter - oBefore;
//        if (oDelta >= ScalarValue.EPSILON)
//            depressurize(oDelta);

        if (x == null)
            onReject(v);

        return x;
    }


    @Override
    public final @Nullable V get(/*@NotNull*/ Object key) {
        return update(key, null, GET, null);
    }

    @Override
    public int capacity() {
        return CAPACITY.getOpaque(this);
    }

    @Override
    public void sample(/*@NotNull*/ Random random, Function<? super V, SampleReaction> each) {
        int s;

        //System.out.println(); System.out.println();

        restart:
        while ((s = size()) > 0) {
            final AtomicReferenceArray<V> map = this.map;
            int c = map.length();
            if (c == 0)
                break;

            int i = random.nextInt(c);


            boolean direction = random.nextBoolean();

            final int windowCap = Math.min(s,

                    //(1 + reprobes)
                    Math.min(s, 2 * reprobes)
            );

            final float[] wPri = new float[windowCap];
            final Object[] wVal = new Object[windowCap];

            /** emergency null counter, in case map becomes totally null avoids infinite loop*/
            int mapNullSeen = 0;

            IntToFloatFunction weight = (k) -> wPri[k];
            //MutableRoulette roulette = new MutableRoulette(windowCap, weight, random);

            int windowSize = 0;

            while ((mapNullSeen + windowSize) < c /*&& size > 0*/) {
                V v = map.getOpaque(i);

                if (v != null) {
                    float p = priElse(v, 0);
                    if (p != p) {
                        evict(map, i, v);
                        mapNullSeen++;
                    } else {
                        wVal[windowSize] = v;
                        wPri[windowSize] = p;
                        if (++windowSize >= windowCap) {
                            break;
                        }
                    }
                } else {
                    mapNullSeen++;
                }

                i = Util.next(i, direction, c);

            }

            if (windowSize == 0)
                return; //emptied

            mapNullSeen = 0;

            while (mapNullSeen < c) {

                //System.out.println(n2(wPri) + "\t" + Arrays.toString(wVal));

                //int which = roulette.reweigh(weight).next();
                int which = Roulette.selectRoulette(windowSize, weight, random);

                V v = (V) wVal[which];
                if (v == null)
                    continue; //assert(v!=null);

                SampleReaction next = each.apply(v);

                if (next.stop) {
                    break restart;
                } else if (next.remove) {

                    if (windowSize == windowCap) {

                        wVal[which] = null;
                        wPri[which] = 0;
                    } else {
                        //compact the array by swapping the empty cell with the entry cell's (TODO or any other non-null)
                        ArrayUtils.swap(wVal, windowSize - 1, which);
                        ArrayUtils.swap(wPri, windowSize - 1, which);

                    }
                    windowSize--;
                    remove(key(v));
                }


                if (map != this.map)
                    continue restart;


                //shift window

                V v0;
                float p = Float.NaN;
                mapNullSeen = 0;
                do {
                    v0 = map.getOpaque(i = Util.next(i, direction, c));
                    if (v0 == null) {
                        if (mapNullSeen++ >= c)
                            break restart;
                    } else if ((p = pri(v0)) != p) {
                        evict(map, i, v0);
                        if (mapNullSeen++ >= c)
                            break restart;
                    }
                } while (v0 == null);

                if (windowSize >= windowCap) {
                    //TODO if there are any holes in the window maybe fill those rather than sliding
                    System.arraycopy(wVal, 1, wVal, 0, windowSize - 1);
                    System.arraycopy(wPri, 1, wPri, 0, windowSize - 1);
                }
                wVal[windowSize - 1] = v0;
                wPri[windowSize - 1] = Math.max(p, ScalarValue.EPSILON);

            }

        }
    }

    private void evict(AtomicReferenceArray<V> map, int i, V v) {
        evict(map, i, v, true);
    }

    private void evict(AtomicReferenceArray<V> map, int i, V v, boolean updateSize) {
        if (map.compareAndSet(i, v, null)) {

            //if the map is still active
            if (this.map == map) {
                if (updateSize)
                    SIZE.getAndDecrement(this);
                _onRemoved(v);
            }
        }
    }


    @Override
    public int size() {
        return Math.max(0, SIZE.getOpaque(this));
    }

    @Override
    public void forEach(Consumer<? super V> e) {
        forEachActive(this, map, e);
    }


    @Override
    public Spliterator<V> spliterator() {
        return stream().spliterator();
    }

    @Override
    public Iterator<V> iterator() {
        return stream().iterator();
    }


    @Override
    public Stream<V> stream() {
        final AtomicReferenceArray<V> map = this.map;
        return IntStream.range(0, map.length())
                .mapToObj(map::get).filter(Objects::nonNull);
    }

    /**
     * linear scan through the map with kontinue callback. returns the last value
     * encountered or null if totally empty
     */
    public V next(int offset, Predicate<V> each) {
        final AtomicReferenceArray<V> map = this.map;
        int n = map.length();
        V xx = null;
        for (int i = offset; i < n; i++) {
            V x = map.getOpaque(i);
            if (x != null) {
                if (!each.test(xx = x)) {
                    break;
                }
            }
        }
        return xx;
    }


//    /**
//     * always >= 0
//     */
//    @Override
//    public float depressurize() {
//        return Math.max(0, PRESSURE.getAndZero(this));
//    }


    @Override
    public void depressurize(float priAmount) {
        PRESSURE.update(this, (p, a) -> Math.max(0, p - a), priAmount);
    }

    @Override
    public float depressurizePct(float percentage) {
        return PRESSURE.getAndUpdate(this, (p, factor) -> Math.min(mass() /* limit */, p * factor), 1 - percentage) * percentage;
    }

    @Override
    public float mass() {
        return mass;
    }

    @Override
    public float priMin() {
        return min;
    }

    @Override
    public float priMax() {
        return max;
    }


    @Override
    public HijackBag<K, V> commit(@Nullable Consumer<V> update) {


        float mass = 0;
        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;

        int count = 0;

        AtomicReferenceArray<V> map = this.map;

        int len = map.length();
        for (int i = 0; i < len; i++) {
            V f = map.getOpaque(i);

            if (f == null)
                continue;

            float p = pri(f);

            if (update != null && p == p) {
                update.accept(f);
                p = pri(f);
            }

            if (p == p) {
                mass += p;
                if (p > max) max = p;
                if (p < min) min = p;
                count++;
            } else {
                evict(map, i, f, false);
            }
        }


        SIZE.set(this, count);

        if (count > 0) {
            this.mass = mass;
            this.min = min;
            this.max = max;
        } else {
            this.mass = 0;
            this.min = this.max = 0;
        }


        return this;
    }

    private void _onAdded(V x) {

        onAdd(x);

    }

    protected boolean attemptRegrowForSize(int s) {

        int sp = space();
        int cp = capacity();
        if (sp < cp && s >= (loadFactor * sp)) {

            int ns = Util.lerp(growthLerpRate, sp, cp);
            if ((cp - ns) / ((float) cp) >= loadFactor)
                ns = cp;

            if (ns != sp) {
                resize(ns);
                return true;
            }
        }
        return false;
    }

    private void _onRemoved(V x) {
        onRemove(x);
    }


    /**
     * SUSPECT
     */
    public static <X> Stream<X> stream(AtomicReferenceArray<X> a) {
        return IntStream.range(0, a.length()).mapToObj(a::get);
    }

    public static <X> List<X> list(AtomicReferenceArray<X> a) {
        return IntStream.range(0, a.length()).mapToObj(a::get).filter(Objects::nonNull).collect(Collectors.toList());
    }


































    /*private static int i(int c, int hash, int r) {
        return (int) ((Integer.toUnsignedLong(hash) + r) % c);
    }*/


}
