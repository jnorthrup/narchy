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
import jcog.util.AtomicFloat;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.eclipse.collections.api.block.procedure.primitive.ObjectFloatProcedure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * unsorted priority queue with stochastic replacement policy
 * <p>
 * it uses a AtomicReferenceArray<> to hold the data but Unsafe CAS operations might perform better (i couldnt get them to work like NBHM does).  this is necessary when an index is chosen for replacement that it makes certain it was replacing the element it thought it was (that it hadnt been inter-hijacked by another thread etc).  on an insert i issue a ticket to the thread and store this in a small ConcurrentHashMap<X,Integer>.  this spins in a busy putIfAbsent loop until it can claim the ticket for the object being inserted. this is to prevent the case where two threads try to insert the same object and end-up puttnig two copies in adjacent hash indices.  this should be rare so the putIfAbsent should usually work on the first try.  when it exits the update critical section it removes the key,value ticket freeing it for another thread.  any onAdded and onRemoved subclass event handling happen outside of this critical section, and all cases seem to be covered.
 * <p>
 * revision using separate integer array to hold priorities and hashes like the original NBHM
 *
 * not complete, untested
 */
@Paper
@Skill("https://en.wikipedia.org/wiki/Concurrent_computing")
public abstract class HijackBag2<K, V> implements Bag<K, V> {

    /**
     * when size() reaches this proportion of space(), and space() < capacity(), grows
     */
    static final float loadFactor = 0.5f;
    /**
     * how quickly the current space grows towards the full capacity, using LERP
     */
    static final float growthLerpRate = 0.5f;
    static final AtomicReferenceArray EMPTY_ARRAY = new AtomicReferenceArray(0);
    static final AtomicIntegerArray EMPTY_INT_ARRAY = new AtomicIntegerArray(0);
    final static float GET = Float.NaN;
    final static float REMOVE = Float.NEGATIVE_INFINITY;
    private static final AtomicIntegerFieldUpdater<HijackBag2> sizeUpdater =
            AtomicIntegerFieldUpdater.newUpdater(HijackBag2.class, "size");
    private static final AtomicIntegerFieldUpdater<HijackBag2> capUpdater =
            AtomicIntegerFieldUpdater.newUpdater(HijackBag2.class, "capacity");
    private static final AtomicReferenceFieldUpdater<HijackBag2, AtomicReferenceArray> mapUpdater =
            AtomicReferenceFieldUpdater.newUpdater(HijackBag2.class, AtomicReferenceArray.class, "map");
    private static final SpinMutex mutex = new Treadmill2();
    private static final AtomicInteger serial = new AtomicInteger();
    public final int reprobes;

//    final static float PRESSURE_THRESHOLD = 0.05f;
    //TODO use atomic field updater
    public final AtomicFloat pressure = new AtomicFloat();
    /**
     * internal random number generator, used for deciding hijacks but not sampling.
     */
    final Random rng;
    /**
     * id unique to this bag instance, for use in treadmill
     */
    private final int id;
    public float mass;
    volatile AtomicReferenceArray<V> map;
    volatile AtomicIntegerArray maf;
    volatile private int size, capacity;
    private float min;
    private float max;

    protected HijackBag2(int initialCapacity, int reprobes) {
        this.id = serial.getAndIncrement();

        this.rng =
                new SplitMix64Random(id); //supposedly even faster
        //new XorShift128PlusRandom(id); //lighter-weight, non-atomic
        //new XoRoShiRo128PlusRandom(id);

        assert (reprobes < Byte.MAX_VALUE - 1); //for sizing of byte[] rank in update(..)
        this.reprobes = reprobes;
        this.map = EMPTY_ARRAY;
        this.maf = EMPTY_INT_ARRAY;

        setCapacity(initialCapacity);

        int initialSpace = Math.min(reprobes, capacity);
        resize(initialSpace, false);
    }

    protected HijackBag2(int reprobes) {
        this(0, reprobes);
    }

    public static boolean hijackGreedy(float newPri, float weakestPri) {
        return weakestPri <= newPri;
    }

    static <X, Y> void forEachActive(HijackBag2<X, Y> bag, AtomicReferenceArray<Y> map, Consumer<? super Y> e) {
        forEach(map, bag::active, e);
    }

    public static <Y> void forEach(AtomicReferenceArray<Y> map, Predicate<Y> accept, Consumer<? super Y> e) {
        for (int c = map.length(), j = 0; j < c; j++) {
            Y v = map.get(j);
            if (v != null && accept.test(v)) {
                e.accept(v);
            }
        }
    }
    public static <Y> void forEach(AtomicReferenceArray<Y> map, AtomicIntegerArray maf, ObjectFloatProcedure<? super Y> e) {
        for (int c = map.length(), j = 0; j < c; j++) {
            Y v = map.get(j);
            if (v != null) {
                e.value(v, Float.intBitsToFloat(maf.get(j*2+1)));
            }
        }
    }

    public static <Y> void forEach(AtomicReferenceArray<Y> map, Consumer<? super Y> e) {
        for (int c = map.length(), j = -1; ++j < c; ) {
            Y v = map.get(j);
            if (v != null) {
                e.accept(v);
            }
        }
    }

    /**
     * SUSPECT
     */
    public static <X> Stream<X> stream(AtomicReferenceArray<X> a) {
        return IntStream.range(0, a.length()).mapToObj(a::get);//.filter(Objects::nonNull);
    }

    public static <X> List<X> list(AtomicReferenceArray<X> a) {
        return IntStream.range(0, a.length()).mapToObj(a::get).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public void pressurize(float f) {
        pressure.add(f);
    }

    @Override
    public void setCapacity(int _newCapacity) {

        int newCapacity = Math.max(_newCapacity, reprobes);

        if (capUpdater.getAndSet(this, newCapacity) != newCapacity) {

            int s = space();
            if (newCapacity < s /* must shrink */) {
                s = newCapacity;
                resize(s, true);
            }


            //return true;
        }

        //return false;
    }

    protected void resize(int newSpace, boolean saveExisting) {
        final AtomicReferenceArray<V>[] prev = new AtomicReferenceArray[1];
        final AtomicIntegerArray[] pref = new AtomicIntegerArray[1];

        //ensures sure only the thread successful in changing the map instance is the one responsible for repopulating it,
        //in the case of 2 simultaneous threads deciding to allocate a replacement:
        AtomicReferenceArray<V> next = newSpace != 0 ? new AtomicReferenceArray<>(newSpace) : EMPTY_ARRAY;
        if (next == mapUpdater.updateAndGet(this, (x) -> {
            if (x.length() != newSpace) {
                prev[0] = x;
                pref[0] = maf;
                AtomicIntegerArray nef = newSpace != 0 ? new AtomicIntegerArray(newSpace*2) : EMPTY_INT_ARRAY;
                maf = nef;
                return next;
            } else return x;
        })) {

            if (saveExisting) {
                //copy items from the previous map into the new map. they will be briefly invisibile while they get transferred.  TODO verify
                forEach(prev[0], pref[0], (V b, float bPri) -> {
                    if (bPri==bPri && put(b, bPri) == null)
                        _onRemoved(b);
                });
            } else {
                forEachActive(this, prev[0], (b) -> {
                    _onRemoved(b);
                });
            }

            commit(null);
        }

    }

    @Override
    public void clear() {
        reset(reprobes);
    }

    @Nullable
    private void reset(int space) {
        if (!sizeUpdater.compareAndSet(this, 0, 0)) {
            pressure.set(0);
            resize(space, false);
        }
    }
    //final static float PUT = (Finite, non-negative value)

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

    /**
     * core update function
     */
    private V update(/*@NotNull*/ Object k, @Nullable V incoming /* null to remove */, float mode, @Nullable MutableFloat overflowing) {

        AtomicReferenceArray<V> map;
        AtomicIntegerArray maf;
        int c;
        do {
            maf = this.maf;
            map = this.map;
        } while (maf.length() != 2 * (c = map.length())); //wait for any resizing operation to end

        if (c == 0)
            return null;


        final int hash = hash(k);

        byte[] rank;
        float[] rpri;
        float incomingPri;

        if (Float.isFinite(mode) /* PUT */) {
            pressurize(incomingPri = mode);
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

            int start = (hash % c); //Math.min(Math.abs(hash), Integer.MAX_VALUE - reprobes - 1); //dir ? iStart : (iStart + reprobes) - 1;
            if (start < 0)
                start += c; //Fair wraparound: ex, -1 -> (c-1)

            if (mode == mode /* PUT or REMOVE */) {
                mutexTicket = mutex.start(id, hash);
            }

            probing:
            for (int i = start, probe = reprobes; probe > 0; probe--) {
                int ph = maf.get(i*2);
                if (ph==hash || ph == 0) {

                    V p = map.get(i); //probed value 'p'

                    if (p != null && k.equals(key(p))) { //existing, should only occurr at most ONCE in this loop
                        if (mode != mode /* GET */) {
                            toReturn = p;
                        } else if (mode == REMOVE) {
                            if (map.compareAndSet(i, p, null)) {
                                maf.set(i*2, 0); //clear hash
                                maf.set(i*2+1, Float.floatToIntBits(Float.NEGATIVE_INFINITY)); //clear pri
                                toReturn = toRemove = p;
                            }
                        } else {
                            //PUT
                            if (p == incoming) {
                                toReturn = p; //identical match found, keep original
                            } else {

                                merge(maf, i, incomingPri, overflowing);
                                if (incoming != null && (incoming == p || map.compareAndSet(i, p, incoming))) {
                                    if (incoming != p) {
                                        toRemove = p; //replaced
                                        toAdd = incoming;
                                    }
                                    toReturn = incoming;
                                }
                            }

                        }

                        break probing; //successful if y!=null
                    }
                }

                if (++i == c) i = 0; //continue to next probed location
            }

            if (rank != null /* PUT */ && toReturn == null) {
                //attempt insert

                //sort - this is only an approximation since values may change while this occurrs

                byte j = 0;
                for (int i = start, probe = reprobes; probe > 0; probe--, j++) {
                    V mi = map.get(i);
                    float mp;
                    if (mi == null) {
                        if (map.compareAndSet(i, null, incoming)) {
                            //take empty slot
                            toReturn = toAdd = incoming;
                            break;
                        }
                        mp = 0; //value has changed, assume 0
                    } else {
                        mp = Float.intBitsToFloat(maf.get(i*2+1));
                    }

                    rank[j] = j;
                    rpri[j] = mp;
                    if (++i == c) i = 0; //continue to next probed location
                }

                if (toReturn == null) {

                    ArrayUtils.sort(rank, r -> -rpri[r]);

                    float power = incomingPri;

                    //FIGHT! starting with weakest
                    combative_insert:
                    for (int f = 0; f < reprobes; f++) {
                        int i = rank[f] + start; //try against next weakest opponent
                        if (i >= c) i -= c;

                        V existing = map.compareAndExchange(i, null, incoming);
                        if (existing == null) {
                            maf.set(i*2+1, Float.floatToIntBits(power));
                            toReturn = toAdd = incoming;
                            break combative_insert; //took empty slot, done

                        } else {
                            //attempt HIJACK (tm)

                            if (replace(power, i, existing, map, maf)) {
                                if (map.compareAndSet(i, existing, incoming)) { //inserted
                                    maf.set(i*2+1, Float.floatToIntBits(power));
                                    toRemove = existing;
                                    toReturn = toAdd = incoming;
                                    break combative_insert; //hijacked replaceable slot, done
                                }
                            }
                        }

                    }

                }

            }

            ready:
            {
                int delta = (toAdd != null ? +1 : 0) + (toRemove != null ? -1 : 0);
                if (delta != 0)
                    sizeUpdater.addAndGet(this, delta);
            }


        } catch (Throwable t) {
            t.printStackTrace(); //should not happen
        } finally {
            if (mode==mode /* PUT or REMOVE */) {
                mutex.end(mutexTicket);
            }
        }

        if (toAdd != null) {
            _onAdded(toAdd);

            int size = size();
            if (attemptRegrowForSize(toRemove != null ? (size + 1) /* hypothetical size if we can also include the displaced */ : size /* size which has been increased by the insertion */)) {
                //the insert has regrown the map so try reinserting this displaced item
                if (toRemove != null) {
                    update(key(toRemove), toRemove, mode, null); //recurse, maybe set a limit on this cuckoo-like resize
                    toRemove = null;
                }
            }
        }

        if (toRemove != null) {
            _onRemoved(toRemove);
        }

        if (rank != null && toAdd == null) {

            if (attemptRegrowForSize(size() + 1)) {
                return update(k, incoming, mode, overflowing); //try once more
            }

        }

        return toReturn;
    }

    protected int hash(Object x) {

        //return x.hashCode(); //default

        //identityComparisons ? System.identityHashCode(key)

        // "Applies a supplemental hash function to a given hashCode, which defends against poor quality hash functions."
        //return Util.hashWangJenkins(x.hashCode());

        return x.hashCode();
    }

    @Override
    public float pri(V key) {
        return pri(hash(key), key, Float.NaN, false, 0);
    }

    private float pri(int keyHash, V key, float finiteToPutOrNaNToGet, boolean alreadyAcquiredMutex, int alreadyAcquiredMutexHash) {
        AtomicReferenceArray<V> map;
        AtomicIntegerArray maf;

        do {
            maf = this.maf;
            map = this.map;
        } while (maf.length() != 2 * map.length()); //wait for any resizing operation to end

        return pri(keyHash, key, finiteToPutOrNaNToGet, alreadyAcquiredMutex, alreadyAcquiredMutexHash, map, maf);
    }

    private float pri(int keyHash, V key, float finiteToPutOrNaNToGet, boolean alreadyAcquiredMutex, int alreadyAcquiredMutexHash, AtomicReferenceArray<V> map, AtomicIntegerArray maf) {
        int c = map.length();
        if (c == 0)
            return Float.NaN;//not found

        int start = (keyHash % c); //Math.min(Math.abs(hash), Integer.MAX_VALUE - reprobes - 1); //dir ? iStart : (iStart + reprobes) - 1;
        if (start < 0)
            start += c; //Fair wraparound: ex, -1 -> (c-1)

        //TODO provide optional hint where to start looking among the reprobes
        for (int i = start, probe = reprobes; probe > 0; probe--, i++) {
            if (i == c)
                i = 0;
            int ih = maf.get(i*2);
            if (ih == keyHash) {
                boolean acquire = !alreadyAcquiredMutex || alreadyAcquiredMutexHash != ih;
                int m = acquire ? mutex.start(id, ih) : -1;
                try {
                    V vi = map.get(i);
                    if (vi!=null && key.equals(vi)) {
                        if (finiteToPutOrNaNToGet==finiteToPutOrNaNToGet) {
                            maf.set(i * 2 + 1, Float.floatToIntBits(finiteToPutOrNaNToGet));
                            return finiteToPutOrNaNToGet;
                        } else
                            return Float.intBitsToFloat(maf.get(i * 2 + 1));
                    }
                } finally {
                    if (acquire)
                        mutex.end(m);
                }
            }
        }

        return Float.NaN;
    }

    private void merge(AtomicIntegerArray maf, int index, float incoming, @Nullable MutableFloat overflowing) {
        float existing = Float.intBitsToFloat(maf.get(index*2+1));
        float result = merge(existing, incoming);
        if(result > 1) {
            overflowing.add(result-1f);
            result = 1f;
            if (result!=existing) {
                maf.set(index*2+1, Float.floatToIntBits(result));
            }
        }
    }

    abstract protected float merge(float existing, float incoming);

    /**
     * true if the incoming priority is sufficient to replace the existing value
     * can override in subclasses for custom replacement policy.
     * <p>
     * a potential eviction can be intercepted here
     */
    protected boolean replace(float newValue, int index, V existing, AtomicReferenceArray<V> map, AtomicIntegerArray maf) {
        float e = pri(hash(existing), existing, Float.NaN /* GET */,false, 0, map, maf);
        return e != e || replace(newValue, e);
    }

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

    /**
     * roulette fair
     */
    protected boolean hijackFair(float newPri, float oldPri, float temperature) {


        float priEpsilon = Prioritized.EPSILON;

        if (oldPri > priEpsilon) {
            //assert (temperature < reprobes);

            float newPriSlice = temperature * newPri / reprobes;
            float thresh = newPriSlice / (newPriSlice + oldPri);
            return rng.nextFloat() < thresh;
        } else {
            return (newPri >= priEpsilon) || (rng.nextFloat() < (1.0f / reprobes));
        }
    }

    protected boolean hijackSoftmax2(float newPri, float oldPri, Random random) {
        //newPri = (float) Math.exp(newPri*2*reprobes); //divided by temperature, reprobes ~ 0.5/temperature
        //oldPri = (float) Math.exp(oldPri*2*reprobes);
        newPri = newPri * newPri * reprobes;
        oldPri = oldPri * oldPri * reprobes;
        if (oldPri > 2 * Float.MIN_VALUE) {
            float thresh = 1.0f - (1.0f - (oldPri / (newPri + oldPri)));
            return random.nextFloat() > thresh;
        } else {
            return (newPri >= Float.MIN_VALUE) || random.nextBoolean();// / reprobes;
            // random.nextBoolean(); //50/50 chance
        }
    }

    /**
     */
    @Override
    @Deprecated public final V put(/*@NotNull*/ V v,  /* TODO */ @Nullable MutableFloat overflowing) {

        throw new UnsupportedOperationException();
//        float p = pri(v);
//        if (p != p)
//            return null; //deleted pri = reject at input
//
//        return put(v, p, overflowing);
    }

    /** prefferred put method */
    public final V put(V v, float p) {
        return put(v, p, null);
    }

    /** prefferred put method */
    public final V put(V v, float p, @Nullable MutableFloat overflowing) {
        K k = key(v);
        if (k == null)
            return null;


        V x = update(k, v, p, overflowing);
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
    public HijackBag2<K, V> sample(/*@NotNull*/ Random random, BagCursor<? super V> each) {
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
                    //(1 + reprobes) * 2 //ESTIMATE HUERISTIC
                    (1 + reprobes) //ESTIMATE HUERISTIC
            );
            float[] wPri = new float[windowCap];
            Object[] wVal = new Object[windowCap];

            /** emergency null counter, in case map becomes totally null avoids infinite loop*/
            int nulls = 0;

            //0. seek to some non-null item
            int prefilled = 0;
            while ((nulls + prefilled) < c /*&& size > 0*/) {
                V v = map.get(i);
                //.getPlain(i);

                //move ahead now in case it terminates on the first try, it wont remain on the same value when the next phase starts
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


            //2. slide window, roulette sampling from it as it changes

            nulls = 0;
            while (nulls < c/* && size > 0*/) {
                V v0 = map.get(i);
                //.getPlain(i);
                float p;
                if (v0 == null) {
                    nulls++;
                } else if ((p = pri(v0)) == p /* not deleted*/) {
                    nulls = 0; //reset contiguous null counter

                    //shift window down, erasing value (if any) in position 0
                    System.arraycopy(wVal, 1, wVal, 0, windowCap - 1);
                    System.arraycopy(wPri, 1, wPri, 0, windowCap - 1);
                    wVal[windowCap - 1] = v0;
                    wPri[windowCap - 1] = Util.max(p, Prioritized.EPSILON); //to differentiate from absolute zero

                    int which = Roulette.selectRoulette(windowCap, (r) -> wPri[r], random);
                    V v = (V) wVal[which];
                    if (v == null)
                        continue; //shouldnt happen but just in case

                    BagSample next = each.next(v);
                    if (next.remove) {
                        if (map.compareAndSet(i, v, null)) {
                            //modified = true;
                            //else: already removed

                            sizeUpdater.addAndGet(this, -1);
                            _onRemoved(v);
                        }
                    }

                    if (next.stop) {
                        break;
                    } else if (next.remove) {
                        //prevent the removed item from further selection
                        if (which == windowCap - 1) {
                            //if it's in the last place, it will be replaced in next cycle anyway
                            wVal[which] = null;
                            wPri[which] = 0;
                        } else if (wVal[0] != null) {
                            //otherwise swap a non-null value in the 0th place with it, because it will be removed in the next shift
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

    @NotNull
    @Override
    public Iterator<V> iterator() {
        return stream().iterator();
    }

    @Override
    public Stream<V> stream() {
        final AtomicReferenceArray<V> map = this.map;
        return IntStream.range(0, map.length()).mapToObj(map::get).filter(Objects::nonNull);
    }

    /**
     * always >= 0
     */
    @Override
    public float depressurize() {
        return Math.max(0, pressure.getAndSet(0.0f));  //max() in case it becomes negative
    }

    @Override
    public float mass() {
        return mass;
    }


    //final AtomicBoolean busy = new AtomicBoolean(false);

    @Override
    public float priMin() {
        return min;
    }

    @Override
    public float priMax() {
        return max;
    }

    @Override
    public HijackBag2<K, V> commit(@Nullable Consumer<V> update) {
        AtomicReferenceArray<V> map;
        AtomicIntegerArray maf;
        int c;
        do {
            maf = this.maf;
            map = this.map;
        } while (maf.length() != 2 * (c = map.length())); //wait for any resizing operation to end


//        try {
//        if (update != null) {
//            update(update);
//        }

        float mass = 0;
        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;

        int count = 0;

        for (int i = 0; i < c; i++) {

            V f = map.get(i);
            if (f == null)
                continue;

            int fh = maf.get(i*2);
            float p = Float.intBitsToFloat(maf.get(i*2+1));

            if (p == p) {

                float pp = p;

                if (update != null) {
                    update.accept(f);
                    p = Util.numOr(p, 0);//HACK in case it changed TODO make update.accept return the float pri
                }
                mass += p;
                if (p > max) max = p;
                if (p < min) min = p;
                count++;
                if (map.get(i) != f) {
                    //changed already; ignore entry
                    continue;
                }

                if (pp!=p)
                    maf.set(i*2+1, Float.floatToIntBits(p));

            } else {
                if (maf.compareAndSet(i*2, fh, 0)) {
                    if (map.compareAndSet(i, f, null)) {
                        _onRemoved(f); //TODO this may call onRemoved unnecessarily if the map has changed (ex: resize)
                    } else {
                        //?
                    }
                } else {
                    //?
                }
            }
        }

        //assert(size() == count);
        sizeUpdater.set(this, count);

        this.mass = mass;
        if (count > 0) {
            this.min = min;
            this.max = max;
        } else {
            this.min = this.max = 0;
        }

//        } finally {
//            //   busy.set(false);
//        }

        return this;
    }

    private void _onAdded(V x) {

        onAdd(x);

    }

    private boolean attemptRegrowForSize(int s) {
        //grow if load is reached
        int sp = space();
        int cp = capacity;
        if (sp < cp && s >= (int) (loadFactor * sp)) {

            int ns = Util.lerp(growthLerpRate, sp, cp);
            if ((cp - ns) / ((float) cp) >= loadFactor)
                ns = cp; //just grow to full capacity, it is close enough

            if (ns != sp) {
                resize(ns, true);
                return true;
            }
        }
        return false;
    }

    @Deprecated
    private void _onRemoved(V x) {
        onRemove(x);
    }

    @NotNull
    protected HijackBag2<K, V> update(@Nullable Consumer<V> each) {

        if (each != null) {
            forEach(each);
        }

        return this;
    }

//    /**
//     * ID of this bag, for use in constructing keys for the global treadmill
//     */
//     private final int id = TreadmillMutex.newTarget();
//
//    /**
//     * lock-free int -> int mapping used as a ticket barrier
//     */
//    static final class TreadmillMutex {
//
//        static final ConcurrentLongSet map = new ConcurrentLongSet(Util.MAX_CONCURRENCY * 2);
//
//        static final AtomicInteger ticket = new AtomicInteger(0);
//
//        public static int newTarget() {
//            return ticket.incrementAndGet();
//        }
//
//        public static long start(int target, int hash) {
//
//            long ticket = (((long) target) << 32) | hash;
//
//            map.putIfAbsentRetry(ticket);
//
//            return ticket;
//        }
//
//        public static void end(long ticket) {
//            map.remove(ticket);
//        }
//    }


    /*private static int i(int c, int hash, int r) {
        return (int) ((Integer.toUnsignedLong(hash) + r) % c);
    }*/


    //    /**
//     * beam width (tolerance range)
//     * searchProgress in range 0..1.0
//     */
//    private static float tolerance(int j, int jLimit, int b, int batchSize, int cap) {
//
//        float searchProgress = ((float) j) / jLimit;
//        //float selectionRate =  ((float)batchSize)/cap;
//
//        /* raised polynomially to sharpen the selection curve, growing more slowly at the beginning */
//        return Util.sqr(Util.sqr(searchProgress * searchProgress));// * searchProgress);
//
//        /*
//        float exp = 6;
//        return float) Math.pow(searchProgress, exp);// + selectionRate;*/
//    }

}

