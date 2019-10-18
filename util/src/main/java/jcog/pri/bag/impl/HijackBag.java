package jcog.pri.bag.impl;

import it.unimi.dsi.fastutil.HashCommon;
import jcog.Paper;
import jcog.Skill;
import jcog.Util;
import jcog.data.NumberX;
import jcog.data.atomic.MetalAtomicIntegerFieldUpdater;
import jcog.data.atomic.MetalAtomicReferenceArray;
import jcog.decide.Roulette;
import jcog.mutex.SpinMutex;
import jcog.mutex.SpinMutexArray;
import jcog.pri.ScalarValue;
import jcog.pri.bag.Bag;
import jcog.util.ArrayUtil;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.Float.NEGATIVE_INFINITY;
import static jcog.data.atomic.MetalAtomicReferenceArray.EMPTY_ARRAY;
import static jcog.pri.bag.impl.HijackBag.Mode.*;

/**
 * unsorted priority queue with stochastic replacement policy
 * <p>
 * it uses a MetalAtomicReferenceArray<> to hold the data but Unsafe CAS operations might perform better (i couldnt get them to work like NBHM does).  this is necessary when an index is chosen for replacement that it makes certain it was replacing the element it thought it was (that it hadnt been inter-hijacked by another thread etc).  on an insert i issue a ticket to the thread and store this in a small ConcurrentHashMap<X,Integer>.  this spins in a busy putIfAbsent loop until it can claim the ticket for the object being inserted. this is to prevent the case where two threads try to insert the same object and end-up puttnig two copies in adjacent hash indices.  this should be rare so the putIfAbsent should usually work on the first try.  when it exits the update critical section it removes the key,value ticket freeing it for another thread.  any onAdded and onRemoved subclass event handling happen outside of this critical section, and all cases seem to be covered.
 * <p>
 * <p>
 * although its called Bag (because of its use in other parts of this project) think of it like a sawed-off non-blocking hashmap with prioritized cells that compete with each other on insert. the way it works is like a probing hashtable but there is no guarantee something will be inserted.
 * <p>
 * first a spinlock is acquired to ensure that only one thread is inserting or removing per hash id at any given time.  i use a global atomic long array to combine each HijackBag's instance with the current item's hash to get a ticket that can ensure this condition.
 * <p>
 * an insertion can happen in a given cell range defined by the 'reprobes' parameter.  usually a number like 3 or 4 is ok but it can be arbitrarily long, but generally much less than the total bag capacity.  the hash computes the starting cell and then reprobe numer of cells forward, modulo capacity and these are the potential cells that a get/put/remove works with.   a get and remove only need to find the first cell with equivalent key.  put is the most complicated operation: first it must see if a cell exists and if so then an overridable merge operation is possible.  otherwise, while doing the initial pass, it chooses a victim based on the weakest priority of the cells it encounters, preferring a null cell to all others (NEGATIVE_INFINITY).  but if it chooses a non-null cell as a victim then a replacement test is applied to see if the new insertion can replace the existing. this can be decided by a random number generator in proportion to the relative priorities of the competing cells.
 */
@Paper
@Skill("Concurrent_computing")
public abstract class HijackBag<K, V> extends Bag<K, V> {

    private static final MetalAtomicIntegerFieldUpdater<Bag> SIZE =
            new MetalAtomicIntegerFieldUpdater(HijackBag.class, "size");

    private static final AtomicReferenceFieldUpdater<HijackBag, MetalAtomicReferenceArray> MAP =
            AtomicReferenceFieldUpdater.newUpdater(HijackBag.class, MetalAtomicReferenceArray.class, "map");

    private static final SpinMutex mutex = new SpinMutexArray();
    private static final AtomicInteger serial = new AtomicInteger();

    private static final int PUT_ATTEMPTS = 1;

    private static final float VICTIM_NOISE = 0.15f;


    /**
     * id unique to this bag instance, for use in treadmill
     */
    private final int id;

    private volatile int size;
    private volatile float min, max;

    /**
     * TODO make non-public
     */
    volatile MetalAtomicReferenceArray<V> map = EMPTY_ARRAY;


    /**
     * when size() reaches this proportion of space(), and space() < capacity(), grows
     */
    private static final float loadFactor = 0.75f;
    private static final float growthRate = 1.5f;





    public final int reprobes;

    protected HijackBag(int reprobes) {
        this(0, reprobes);
    }

    protected HijackBag(int initialCapacity, int reprobes) {
        this.id = serial.getAndIncrement();

        this.reprobes = reprobes; assert (reprobes < Byte.MAX_VALUE - 1);

        setCapacity(initialCapacity);
    }

    public static boolean hijackGreedy(float newPri, float weakestPri) {
        return weakestPri <= newPri;
    }

    private static void updateEach(AtomicReferenceArray map, Function update) {
        for (int c = map.length(), j = 0; j < c; j++) {
            //map.updateAndGet(j, (vv) -> vv != null ? update.apply(vv) : null);
            map.accumulateAndGet(j, update, (vv, u) -> vv!=null ? ((Function)u).apply(vv) : null);
        }
    }

    @Override
    protected void onCapacityChange(int oldCap, int newCap) {

        int s = Math.max(reprobes, size());

        newCap = Math.min(s, newCap);

        if (((oldCap < newCap && (s >= oldCap   /* should grow */)
                ||
                (newCap < space() /* must shrink */)))) {
            resize(newCap);
        }

    }

    /**
     * minimum allocation
     */
    protected int spaceMin() {
        return reprobes;
    }

    protected void resize(int newSpace) {
        final MetalAtomicReferenceArray<V>[] prev = new MetalAtomicReferenceArray[1];

        MetalAtomicReferenceArray<V> next = newSpace != 0 ? new MetalAtomicReferenceArray<>(newSpace) : EMPTY_ARRAY;
        if (next == MAP.updateAndGet(this, (x) -> {
            if (x.length() != newSpace) {
                prev[0] = x;
                return next;
            } else return x;
        })) {


            forEachActive(this, prev[0], (b) -> {
                if (put(b) == null)
                    onRemove(b);
            });

            commit(null);
        }
    }


    @Override
    public void clear() {

        MetalAtomicReferenceArray current = MAP.get(this);
        MetalAtomicReferenceArray<V> x = (current == null || reshrink(space())) ? reset(spaceMin()) : current;

        for (int i = 0; i < x.length(); i++) {
            V xi = x.getAndSet(i, null);
            if (xi!=null) {
                onRemove(xi);
            }
        }
        pressureZero();
        massZero();
        SIZE.set(this, 0);
        min = max = 0;


    }

    protected boolean reshrink(int length) {
        return true;
    }

    private @Nullable MetalAtomicReferenceArray<V> reset(int space) {

        if (SIZE.getAndSet(this, 0) != 0) {
            MetalAtomicReferenceArray<V> newMap = new MetalAtomicReferenceArray<>(space);

            MetalAtomicReferenceArray<V> prevMap = MAP.getAndSet(this, newMap);

            if (prevMap == newMap)

            commit();

            return prevMap;
        } else {
            return EMPTY_ARRAY; //remains empty
        }
    }

    /**
     * the current allocation, which is less than or equal to the value returned by capacity()
     */
    public int space() {
        return map.length();
    }

    public float density() {
        MetalAtomicReferenceArray<V> m = map;
        int mm = m.length();
        int filled = (int) IntStream.range(0, mm).filter(i -> m.getFast(i) != null).count();
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

        MetalAtomicReferenceArray<V> map = MAP.get(this);
        int c = map.length();
        if (c == 0)
            return null;

        int kHash = hash(k);
        final int start = index(kHash, c);

        float incomingPri;
        if (mode == PUT) {
            if ((incomingPri = pri(incoming)) != incomingPri)
                return null;
            pressurize(incomingPri);
        } else {
            incomingPri = Float.POSITIVE_INFINITY; /* shouldnt be used */
        }

        V toAdd = null, toRemove = null, toReturn = null;
        int mutexTicket = -1;

        boolean locking = (mode != GET) && !unsafe();
        if (locking) {
            if (mode == PUT && optimisticPut()) {
                Object exist = _get(k, kHash, start, c, map);
                if (exist!=null && exist.equals(incoming) /*exist==incoming*/)
                    return incoming; //no change
            }

            if (mode == REMOVE && optimisticRemove()) {
                Object exist = _get(k, kHash, start, c, map);
                if (exist==null)
                    return null; //no change
            }

            locking = true;
            mutexTicket = mutex.start(id, start);
        }

        try {

            switch (mode) {
                case GET:
                    return _get(k, kHash, start, c, map);
                case REMOVE:
                    for (int i = start, j = reprobes; j > 0; j--) {
                        V v = map.getFast(i);
                        if (v != null && keyEquals(k, kHash, v)) {
                            if (map.compareAndSetFast(i, v, null)) {
                                toReturn = toRemove = v;
                                break;
                            }
                            //else: this actually shouldnt happen if access to the hashes has been correctly restricted by the mutex
                        }
                        if (++i == c)
                            i = 0;
                    }
                    break;
                case PUT:

                    int retriesRemain = PUT_ATTEMPTS;
                    do {
                        int victimIndex = -1;
                        float victimPri = Float.POSITIVE_INFINITY;
                        V victimValue = null;

                        for (int i = start, j = reprobes; j > 0; j--) {

                            V v = map.getFast(i);
                            if (v == null) {
                                if (victimPri > NEGATIVE_INFINITY) {
                                    victimIndex = i;
                                    victimPri = NEGATIVE_INFINITY;
                                    victimValue = null;
                                }
                            } else if (v == incoming) {
                                toReturn = v; //identical match
                                break;
                            } else if (keyEquals(k, kHash, v)) {

                                float before = pri(v);
                                V next = merge(v, incoming, overflowing);
                                float after = pri(v);
                                depressurize(Math.max(0, incomingPri - (after - before)));

                                assert (next != null);
                                if (next != v) {
                                    if (!map.compareAndSetFast(i, v, next)) {
//                                        throw new WTF("merge mismatch: " + v);
//                                        //the previous value likely got hijacked by another thread
//                                        //restart?
//                                        //continue insert;
                                        next = null; //retry
                                    }
                                }

                                toReturn = next;
                                break;

                            } else {
                                if (victimPri > NEGATIVE_INFINITY) {
                                    float pp;
                                    if (chooseVictim(pp = priElseNegInfinity(v), victimPri)) {
                                        victimPri = pp;
                                        victimValue = v;
                                        victimIndex = i;
                                    }
                                }
                            }

                            if (++i == c) i = 0;
                        }


                        if (toReturn == null) {

                            //ATTEMPT HIJACK

                            //assert(victimWeakest!=-1);

                            if ((victimIndex >= 0 && victimValue == null) || (victimValue != null && replace(incomingPri, victimValue, victimPri))) {
                                if (map.compareAndSetFast(victimIndex, victimValue, incoming)) {
                                    toRemove = victimValue;
                                    toReturn = toAdd = incoming;
                                    break; //done
                                }
                            }
                        }

//                        if (retriesRemain > 0)
//                            victimPri *= 1 - (1f / reprobes); //weaken

                    } while (--retriesRemain > 0);

                    break;

            }


        } catch (Throwable t) {

            throw new RuntimeException(t);

        } finally {
            if (locking)
                mutex.end(mutexTicket);
        }

        if (toAdd!=null || toRemove!=null) {
            int dSize = (toAdd != null ? +1 : 0) + (toRemove != null ? -1 : 0);
            //only used if toAdd!=null
            int size = SIZE.addAndGet(this, dSize);

            if (toAdd != null) {
                _onAdded(toAdd);

                if (regrowForSize(toRemove != null ? (size + 1) /* hypothetical size if we can also include the displaced */ :
                        size /* size which has been increased by the insertion */, space())) {

                    if (toRemove != null) {
                        update(key(toRemove), toRemove, PUT, overflowing);
                        toRemove = null;
                    }
                }
            }

            if (toRemove != null) {
                if (map == this.map) {
                    depressurize(pri(toRemove));
                    onRemove(toRemove);
                }
            }
        }


        return toReturn;
    }

    private V _get(Object k, int kHash, int start, int c, MetalAtomicReferenceArray<V> map) {
        for (int i = start, j = reprobes; j > 0; j--) {
            V v = map.getFast(i);
            if (v != null && keyEquals(k, kHash, v))
                return v;

            if (++i == c)
                i = 0;
        }
        return null; //not found
    }

    protected static boolean optimisticPut() {
        return false;
    }
    protected static boolean optimisticRemove() {
        return false;
    }

    /** allow duplicates and other unexpected phenomena resulting from disabling locking write access */
    protected boolean unsafe() {
        return false;
    }

    protected boolean chooseVictim(float potentialVictimPri, float currentVictimPri) {

        //OPTIONAL: victim noise
        //      causes insertion to underestimate the priority allowing disproportionate replacement
        //      in some low probability of cases, allowing more item churn
        if (VICTIM_NOISE> 0) {
            float victimNoise = VICTIM_NOISE / (reprobes * reprobes);
            currentVictimPri *= victimNoise > 0 ? (1 - (random().nextFloat() * victimNoise)) : 1;
        }


        return (potentialVictimPri < currentVictimPri);
    }

    private float priElseNegInfinity(V x) {
        float p = pri(x);
        if (p == p)
            return p;
        else
            return NEGATIVE_INFINITY;
    }

    protected boolean keyEquals(Object k, int kHash, V p) {
        return k == p || k.equals(key(p));
    }

    /** default impl = x.hashCode() */
    protected static int hash(Object x) {
        return x.hashCode();
    }

    private static int index(int h, int cap) {


        //mix for additional mix
//        h ^= h >>> 20 ^ h >>> 12;
//        h ^= h >>> 7 ^ h >>> 4;
        h = HashCommon.mix(h);


        //modulo
        h %= cap;
        if (h < 0)
            h += cap;

        return h;
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
     * to the pressure as new insertions.
     * <p>
     * if returns null, the merge is considered failed and will try inserting/merging
     * at a different probe location
     */
    protected abstract V merge(V existing, V incoming, NumberX overflowing);

    /**
     * HIJACK test
     * true if the incoming priority is sufficient to replace the existing value
     * can override in subclasses for custom replacement policy.
     * <p>
     * a potential eviction can be intercepted here
     * <p>
     * returns incomingPower, possibly reduced by the fight with the existing.
     * or NaN if the incoming wins
     */
    protected boolean replace(float incoming, V existingValue, float existing) {
        return hijackFair(incoming, existing);
    }


//    /** loss of priority if survives */
//    protected void cut(V p) {
//        p.priSub(CACHE_SURVIVE_COST);
//    }

//    abstract public void priAdd(V entry, float amount);

    @Override
    public @Nullable V remove(K k) {
        return update(k, null, REMOVE, null);
    }

    public static Random random() {
        return ThreadLocalRandom.current();
    }

    /**
     * roulette fair
     */
    private static boolean hijackFair(float newPri, float oldPri) {
        return random().nextFloat() < newPri / (newPri + oldPri);

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
    public final V put(/*@NotNull*/ V potentialVictimPri,  /* TODO */ NumberX overflowing) {

        K k = key(potentialVictimPri);
        if (k == null)
            return null;

        V x = update(k, potentialVictimPri, PUT, overflowing);

        if (x == null)
            onReject(potentialVictimPri);

        return x;
    }


    @Override
    public final @Nullable V get(/*@NotNull*/ Object key) {
        return update(key, null, GET, null);
    }


    @Override
    public void sample(/*@NotNull*/ Random random, Function<? super V, SampleReaction> each) {
        int s;

        //System.out.println(); System.out.println();

        restart:
        while ((s = size()) > 0) {
            final MetalAtomicReferenceArray<V> map = this.map;
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
                V v = map.getFast(i);

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
						ArrayUtil.swapObj(wVal, windowSize - 1, which);
						ArrayUtil.swapFloat(wPri, windowSize - 1, which);
                    }
                    remove(key(v));
                    if (windowSize > size()) windowSize--;
                }

                if (map != this.map)
                    continue restart;

                //shift window

                V v0;
                float p = Float.NaN;
                mapNullSeen = 0;
                do {
                    v0 = map.getFast(i = Util.next(i, direction, c));
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

    private void evict(MetalAtomicReferenceArray<V> map, int i, V potentialVictimPri) {
        evict(map, i, potentialVictimPri, true);
    }

    private void evict(MetalAtomicReferenceArray<V> map, int i, V victim, boolean updateSize) {
        if (map.compareAndSet(i, victim, null)) {

            //if the map is still active
            if (this.map == map) {
                if (updateSize)
                    SIZE.getAndDecrement(this);
                onRemove(victim);
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
    public void forEach(int max, Consumer<? super V> e) {
        Bag.forEach(map, max, e);
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
        final MetalAtomicReferenceArray<V> map = this.map;
        return IntStream.range(0, map.length())
                .mapToObj(map::getFast).filter(Objects::nonNull);
    }

    /**
     * linear scan through the map with kontinue callback. returns the last value
     * encountered or null if totally empty
     */
    public V next(int offset, Predicate<V> each) {
        final MetalAtomicReferenceArray<V> map = this.map;
        int n = map.length();
        V xx = null;
        for (int i = offset; i < n; i++) {
            V x = map.getFast(i);
            if (x != null) {
                if (!each.test(xx = x)) {
                    break;
                }
            }
        }
        return xx;
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
    public Bag<K,V> commit(@Nullable Consumer<V> update) {


        float mass = 0;
        float min = Float.POSITIVE_INFINITY;
        float max = NEGATIVE_INFINITY;

        int count = 0;

        MetalAtomicReferenceArray<V> map = this.map;

        int len = map.length();
        for (int i = 0; i < len; i++) {
            V f = map.getFast(i);

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
            this.massSet(mass);
            this.min = min;
            this.max = max;
        } else {
            this.massZero();
            this.min = this.max = 0;
        }


        return this;
    }

    private void _onAdded(V x) {

        onAdd(x);

    }

    protected boolean regrowForSize(int s, int sp) {


        if (s >= (loadFactor * sp)) {
            int cp = capacity();
            if (sp < cp) {
                int ns =
                    Math.min(cp, Math.round(sp * growthRate));
                if (ns != sp) {
                    resize(ns);
                    return true;
                }
            }
        }


        return false;
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
