package jcog.bag.impl;

import jcog.Paper;
import jcog.Skill;
import jcog.Util;
import jcog.bag.Bag;
import jcog.bag.util.SpinMutex;
import jcog.bag.util.Treadmill2;
import jcog.decide.Roulette;
import jcog.math.random.SplitMix64Random;
import jcog.pri.Prioritized;
import jcog.util.AtomicFloatFieldUpdater;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static jcog.bag.impl.HijackBag.Mode.*;

/**
 * unsorted priority queue with stochastic replacement policy
 * <p>
 * it uses a AtomicReferenceArray<> to hold the data but Unsafe CAS operations might perform better (i couldnt get them to work like NBHM does).  this is necessary when an index is chosen for replacement that it makes certain it was replacing the element it thought it was (that it hadnt been inter-hijacked by another thread etc).  on an insert i issue a ticket to the thread and store this in a small ConcurrentHashMap<X,Integer>.  this spins in a busy putIfAbsent loop until it can claim the ticket for the object being inserted. this is to prevent the case where two threads try to insert the same object and end-up puttnig two copies in adjacent hash indices.  this should be rare so the putIfAbsent should usually work on the first try.  when it exits the update critical section it removes the key,value ticket freeing it for another thread.  any onAdded and onRemoved subclass event handling happen outside of this critical section, and all cases seem to be covered.
 */
@Paper
@Skill("Concurrent_computing")
public abstract class HijackBag<K, V> implements Bag<K, V> {

    private static final AtomicIntegerFieldUpdater<HijackBag> sizeUpdater =
            AtomicIntegerFieldUpdater.newUpdater(HijackBag.class, "size");
    private static final AtomicIntegerFieldUpdater<HijackBag> capUpdater =
            AtomicIntegerFieldUpdater.newUpdater(HijackBag.class, "capacity");
    private static final AtomicFloatFieldUpdater<HijackBag> pressureUpdater =
            new AtomicFloatFieldUpdater(AtomicIntegerFieldUpdater.newUpdater(HijackBag.class, "pressure"));

    private static final AtomicReferenceFieldUpdater<HijackBag, AtomicReferenceArray> mapUpdater =
            AtomicReferenceFieldUpdater.newUpdater(HijackBag.class, AtomicReferenceArray.class, "map");

    private static final SpinMutex mutex = new Treadmill2();
    private static final AtomicInteger serial = new AtomicInteger();

    /** internal random number generator, used for deciding hijacks but not sampling. */
    final Random rng;

    /**
     * id unique to this bag instance, for use in treadmill
     */
    private final int id;

    volatile private int size, capacity, pressure;

    /**
     * TODO make non-public
     */
    volatile AtomicReferenceArray<V> map;




    /**
     * when size() reaches this proportion of space(), and space() < capacity(), grows
     */
    static final float loadFactor = 0.5f;

    /**
     * how quickly the current space grows towards the full capacity, using LERP
     */
    static final float growthLerpRate = 0.5f;



    static final AtomicReferenceArray EMPTY_ARRAY = new AtomicReferenceArray(0);

    public final int reprobes;

    public float mass;
    private float min;
    private float max;


    protected HijackBag(int initialCapacity, int reprobes) {
        this.id = serial.getAndIncrement();
        this.rng = new SplitMix64Random(id);

        assert(reprobes < Byte.MAX_VALUE-1); 
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

    static <X, Y> void forEachActive(HijackBag<X, Y> bag, AtomicReferenceArray<Y> map, Consumer<? super Y> e) {
        forEach(map, bag::active, e);
    }

    @Override
    public void pressurize(float f) {
        pressureUpdater.add(this, f);
    }

    public static <Y> void forEach(AtomicReferenceArray<Y> map,  Predicate<Y> accept,   Consumer<? super Y> e) {
        for (int c = map.length(), j = 0; j < c; j++) {
            Y v = map
                    .get(j);
                    
            if (v != null && accept.test(v)) {
                e.accept(v);
            }
        }
    }

    public static <Y> void forEach(AtomicReferenceArray<Y> map, Consumer<? super Y> e) {
        for (int c = map.length(), j = -1; ++j < c; ) {
            Y v = map
                    .get(j);
                    
            if (v != null) {
                e.accept(v);
            }
        }
    }

    @Override
    public void setCapacity(int _newCapacity) {

        int newCapacity = _newCapacity > 0 ? Math.max(_newCapacity, reprobes) : 0;

        if (capUpdater.getAndSet(this, newCapacity) != newCapacity) {

            int s = space();
            if (s == 0 || newCapacity < s /* must shrink */) {
                resize(newCapacity);
            }


            
        }

        
    }

    protected void resize(int newSpace) {
        final AtomicReferenceArray<V>[] prev = new AtomicReferenceArray[1];

        
        
        AtomicReferenceArray<V> next = newSpace != 0 ? new AtomicReferenceArray<>(newSpace) : EMPTY_ARRAY;
        if (next == mapUpdater.updateAndGet(this, (x) -> {
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
        pressureUpdater.zero(this);
        if (x != null) {
            forEachActive(this, x, this::_onRemoved);
        }

    }

    @Nullable
    private AtomicReferenceArray<V> reset(int space) {

        if (!sizeUpdater.compareAndSet(this, 0, 0)) {
            AtomicReferenceArray<V> newMap = new AtomicReferenceArray<>(space);

            AtomicReferenceArray<V> prevMap = mapUpdater.getAndSet(this, newMap);

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
            if (m.get(i) != null)
                filled++;
        }
        return ((float) filled) / mm;
    }

    enum Mode {
        GET, PUT, REMOVE
    }

    /** core update function */
    private V update(/*@NotNull*/ Object k, @Nullable V incoming /* null to remove */, Mode mode, @Nullable MutableFloat overflowing) {

        final AtomicReferenceArray<V> map = this.map;
        int c = map.length();
        if (c == 0)
            return null;


        final int hash = hash(k);

        byte[] rank;
        float[] rpri;
        float incomingPri;
        if (mode == Mode.PUT) {
            if ((incomingPri = pri(incoming)) != incomingPri)
                return null;
            pressurize(incomingPri);
            rank = new byte[reprobes];
            rpri = new float[reprobes];
        } else {
            incomingPri = Float.POSITIVE_INFINITY; /* shouldnt be used */
            rpri = null;
            rank = null;
        }




        int mutexTicket = -1;
        V toAdd = null, toRemove = null, toReturn = null;
        try {

            int start = (hash % c); 
            if (start < 0)
                start += c; 

            if (mode != GET) {
                mutexTicket = mutex.start(id, hash);
            }

            probing:
            for (int i = start, probe = reprobes; probe > 0; probe--) {

                V p = map.get(i); 

                if (p != null && keyEquals(k, p)) { 
                    switch (mode) {

                        case GET:
                            toReturn = p;
                            break;

                        case PUT:
                            if (p == incoming) {
                                toReturn = p; 
                            } else {
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
                

                


                byte j=0;
                for (int i = start; j < reprobes; ) {
                    V mi = map.get(i);
                    rank[j] = j;
                    float mp;
                    if (mi == null || ((mp = pri(mi))!=mp)) {
                        if (map.compareAndSet(i, mi, incoming)) {
                            
                            toReturn = toAdd = incoming;
                            break;
                        } else {
                            mp = 0; 
                        }
                    }
                    rpri[j++] = mp;
                    if (++i == c) i = 0; 
                }

                if (toReturn == null) { 

                    ArrayUtils.sort(rank, r -> -rpri[r]);

                    float power = incomingPri;

                    
                    combative_insert: for (int f = 0; f < reprobes; f++) {
                        int i = rank[f]+start; 
                        if (i >= c) i-=c;

                        V existing = map.compareAndExchange(i, null, incoming);
                        if (existing == null) {

                            toReturn = toAdd = incoming;
                            break combative_insert; 

                        } else {
                            

                            float resultPower = replace(power, existing);
                            if (resultPower!=resultPower) {
                                if (map.compareAndSet(i, existing, incoming)) { 
                                    toRemove = existing;
                                    toReturn = toAdd = incoming;
                                    break combative_insert; 
                                }
                            } else {
                                power = resultPower; 
                            }
                        }

                    }

                }

            }

            ready: {
                int delta = (toAdd != null ? +1 : 0) + (toRemove != null ? -1 : 0);
                if (delta != 0)
                    sizeUpdater.addAndGet(this, delta);
            }


        } catch (Throwable t) {
            t.printStackTrace(); 
        } finally {
            if (mode != GET) {
                mutex.end(mutexTicket);
            }
        }

        if (toAdd != null) {
            _onAdded(toAdd);

            int size = size();
            if (attemptRegrowForSize(toRemove != null ? (size + 1) /* hypothetical size if we can also include the displaced */ : size /* size which has been increased by the insertion */)) {
                
                if (toRemove != null) {
                    update(key(toRemove), toRemove, Mode.PUT, null); 
                    toRemove = null;
                }
            }
        }

        if (toRemove != null) {
            _onRemoved(toRemove);
        }

        if (mode == PUT && toAdd == null) {

            if (attemptRegrowForSize(size() + 1)) {
                return update(k, incoming, PUT, overflowing); 
            }

        }

        return toReturn;
    }

    protected boolean keyEquals(Object k, V p) {
        return k.equals(key(p)); 
    }


    protected int hash(Object x) {

        

        

        
        

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
    protected abstract V merge(V existing, V incoming, @Nullable MutableFloat overflowing);

    /**
     * true if the incoming priority is sufficient to replace the existing value
     * can override in subclasses for custom replacement policy.
     *
     * a potential eviction can be intercepted here
     *
     * returns incomingPower, possibly reduced by the fight with the existing.
     * or NaN if the incoming wins
     */
    protected float replace(float incomingPower, V existing) {
        float e = pri(existing);
        if (e != e) {
            return Float.NaN; 
        } else {
            if (replace(incomingPower, e)) {
                priAdd(existing, -incomingPower/reprobes);
                return Float.NaN; 
            } else {
                return Util.max(0, incomingPower - e/reprobes);
            }
        }
    }

    abstract public void priAdd(V entry, float amount);

    protected boolean replace(float incoming, float existing) {
        return hijackFair(incoming, existing);
    }

    @Nullable
    @Override
    public V remove(K k) {
        return update(k, null, REMOVE, null);
    }

    protected boolean hijackFair(float newPri, float oldPri) {
        return hijackFair(newPri, oldPri, 1.0f);
    }
    /** roulette fair */
    protected boolean hijackFair(float newPri, float oldPri, float temperature) {


        float priEpsilon = Prioritized.EPSILON;

        if (oldPri > priEpsilon) {
            

            float newPriSlice = temperature * newPri / reprobes;
            float thresh = newPriSlice / (newPriSlice + oldPri);
            return rng.nextFloat() < thresh;
        } else {
            return (newPri >= priEpsilon) || (rng.nextFloat() < (1.0f /reprobes));
        }
    }


    protected boolean hijackSoftmax2(float newPri, float oldPri, Random random) {
        
        
        newPri = newPri * newPri * reprobes;
        oldPri = oldPri * oldPri * reprobes;
        if (oldPri > 2 * Float.MIN_VALUE) {
            float thresh = 1.0f - (1.0f - (oldPri / (newPri + oldPri)));
            return random.nextFloat() > thresh;
        } else {
            return (newPri >= Float.MIN_VALUE) || random.nextBoolean();
            
        }
    }

    /**
     */
    @Override
    public final V put(/*@NotNull*/ V v,  /* TODO */ @Nullable MutableFloat overflowing) {






        K k = key(v);
        if (k == null)
            return null;

        
        
        


        V x = update(k, v, PUT, overflowing);
        if (x == null) {
            onReject(v);
        }

        return x;
    }


    @Override
    public @Nullable V get(/*@NotNull*/ Object key) {
        return update(key, null, GET, null);
    }

    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public HijackBag<K, V> sample(/*@NotNull*/ Random random, BagCursor<? super V> each) {
        final int s = size();
        if (s <= 0)
            return this;

        restart:
        while (true) {
            final AtomicReferenceArray<V> map = this.map;
            int c = map.length();
            if (c == 0)
                return this;

            int i = random.nextInt(c);


            boolean direction = random.nextBoolean();

            int windowCap = Math.min(s,
                    
                    (1 + reprobes) 
            );
            float[] wPri = new float[windowCap];
            Object[] wVal = new Object[windowCap];

            /** emergency null counter, in case map becomes totally null avoids infinite loop*/
            int nulls = 0;

            
            int prefilled = 0;
            while ((nulls+prefilled) < c /*&& size > 0*/) {
                V v = map
                        .get(i);
                        

                
                if (direction) {
                    if (++i == c) i = 0;
                } else {
                    if (--i == -1) i = c - 1;
                }

                if (v != null) {
                    wVal[windowCap - 1 - prefilled] = v;
                    wPri[windowCap - 1 - prefilled] = pri(v);
                    if (++prefilled >= windowCap)
                        break;
                } else {
                    nulls++;
                }

            }


            

            nulls = 0;
            while (nulls < c/* && size > 0*/) {
                V v0 = map
                        .get(i);
                        
                float p;
                if (v0 == null) {
                    nulls++;
                } else  if ((p = pri(v0)) == p /* not deleted*/) {
                    nulls=0; 

                    
                    System.arraycopy(wVal, 1, wVal, 0, windowCap - 1);
                    wVal[windowCap - 1] = v0;
                    System.arraycopy(wPri, 1, wPri, 0, windowCap - 1);
                    wPri[windowCap - 1] = Util.max(p, Prioritized.EPSILON); 

                    int which = Roulette.selectRoulette(windowCap, r -> wPri[r], random);
                    V v = (V) wVal[which];
                    if (v == null)
                        continue; 

                    BagSample next = each.next(v);
                    if (next.remove) {
                        if (map.compareAndSet(i, v, null)) {
                            
                            

                            sizeUpdater.decrementAndGet(this);
                            _onRemoved(v);
                        }
                    }

                    if (next.stop) {
                        break;
                    } else if (next.remove) {
                        
                        if (which==windowCap-1) {
                            
                            wVal[which] = null;
                            wPri[which] = 0;
                        } else if (wVal[0] != null) {
                            
                            ArrayUtils.swap(wVal, 0, which);
                            ArrayUtils.swap(wPri, 0, which);
                        }
                    }
                }


                if (map != this.map)
                    continue restart;

                if (direction) {
                    if (++i == c) i = 0;
                } else {
                    if (--i == -1) i = c - 1;
                }
            }

            return this;
        }
    }


    @Override
    public int size() {
        return Math.max(0, sizeUpdater.get(this));
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

    /** linear scan through the map with kontinue callback. returns the last value
     *  encountered or null if totally empty
     * */
    public V next(int offset, Predicate<V> each) {
        final AtomicReferenceArray<V> map = this.map;
        int n = map.length();
        V xx = null;
        for (int i = offset; i < n; i++) {
            V x = map.get(i);
            if (x != null) {
                if (!each.test(xx = x)) {
                    break;
                }
            }
        }
        return xx;
    }


    /**
     * always >= 0
     */
    @Override
    public float depressurize() {
        return Math.max(0, pressureUpdater.getAndZero(this));
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

        AtomicReferenceArray<V> a = map;

        int len = a.length();
        for (int i = 0; i < len; i++) {
            V f = a
                    .get(i);
                    
            if (f == null)
                continue;

            float p = priUpdate(f);

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
                if (a.compareAndSet(i, f, null)) {
                    _onRemoved(f); 
                }
            }
        }

        
        sizeUpdater.set(this, count);

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
        int cp = capacity;
        if (sp < cp && s >= (int) (loadFactor * sp)) {

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
