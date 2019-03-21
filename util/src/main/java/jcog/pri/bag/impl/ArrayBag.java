package jcog.pri.bag.impl;

import jcog.Util;
import jcog.WTF;
import jcog.data.NumberX;
import jcog.data.atomic.AtomicFloatFieldUpdater;
import jcog.data.iterator.ArrayIterator;
import jcog.data.list.FasterList;
import jcog.data.list.table.SortedListTable;
import jcog.pri.Prioritizable;
import jcog.pri.Prioritized;
import jcog.pri.ScalarValue;
import jcog.pri.bag.Bag;
import jcog.pri.bag.Sampler;
import jcog.pri.op.PriMerge;
import jcog.signal.wave1d.ArrayHistogram;
import jcog.sort.SortedArray;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static jcog.Util.assertUnitized;
import static jcog.Util.lerp;


/**
 * A bag implemented as a combination of a Map and a SortedArrayList
 * TODO extract a version of this which will work for any Prioritized, not only BLink
 */
abstract public class ArrayBag<X, Y extends Prioritizable> extends SortedListTable<X, Y> implements Bag<X, Y> {
    private static final AtomicFloatFieldUpdater<ArrayBag> MASS =
            new AtomicFloatFieldUpdater(ArrayBag.class, "mass");
    private static final AtomicFloatFieldUpdater<ArrayBag> PRESSURE =
            new AtomicFloatFieldUpdater(ArrayBag.class, "pressure");

    private final StampedLock lock = new StampedLock();


    @Deprecated
    private final PriMerge mergeFunction;

    private transient ArrayHistogram hist = null;

    private volatile int mass, pressure;

    protected ArrayBag(PriMerge mergeFunction, int capacity) {
        this(mergeFunction,
                //new HashMap<>(capacity, 0.5f)
                new UnifiedMap<>(capacity, 0.5f)
        );
        setCapacity(capacity);
    }

    ArrayBag(PriMerge mergeFunction, Map<X, Y> map) {
        this(mergeFunction, 0, map);
    }

    protected ArrayBag(PriMerge mergeFunction, @Deprecated int cap, Map<X, Y> map) {
        super(new SortedArray<>(), map);
        this.mergeFunction = mergeFunction;
        setCapacity(cap);
    }

    @Override
    public float pressure() {
        return PRESSURE.getOpaque(this);
    }

    /**
     * gets the scalar float value used in a comparison of BLink's
     * essentially the same as b.priIfFiniteElseNeg1 except it also includes a null test. otherwise they are interchangeable
     */
    private static float pCmp(@Nullable Object b) {
        return b == null ? -2.0f : ((Prioritized) b).priElseNeg1();
    }


    @Override
    public final float mass() {
        return MASS.getOpaque(this);
    }

    @Override
    public final float floatValueOf(Y y) {
        return -pCmp(y);
    }

    @Override
    public Stream<Y> stream() {
        int s = size();
        if (s == 0) return Stream.empty();
        else {
            Object[] x = items.array();
            return ArrayIterator.stream(x).map(o -> (Y) o).filter(y -> y != null && !y.isDeleted());
        }
    }


    /**
     * returns whether the capacity has changed
     */
    @Override
    public final void setCapacity(int nextCapacity) {
        if (setCapacityIfChanged(nextCapacity)) {
            commit(null);
        }
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
    public void depressurize(float f) {
        if (f == f && Math.abs(f) > Float.MIN_NORMAL)
            PRESSURE.update(this, (p, a) -> Math.max(0, p - a), f);
    }

    @Override
    public float depressurizePct(float percentage) {
        assertUnitized(percentage);

        return
                PRESSURE.getAndUpdate(this, (p, factor) -> p * factor, 1 - percentage);
    }


    /**
     * returns true unless failed to add during 'add' operation or becomes empty
     * call within synchronized
     *
     * @return List of trash items
     * trash must be removed from the map, outside of critical section
     * may include the item being added
     */
    private boolean tryInsertFull(Y toAdd, float toAddPri, long[] lock, @Nullable Consumer<Y> update) {

        lock[0] = writeFromRead(lock[0]);

        int s;
        if (cleanIfFull())
            s = clean(update);
        else
            s = size();

        int c = capacity();

        boolean free = s + 1 <= c;

        Y lastToRemove;
        if (!free) {
            lastToRemove = items.last();
            float priMin = pri(lastToRemove);
            if (toAddPri <= priMin)
                return false;

        } else {
            //space has been cleared for the new item
            lastToRemove = null;
        }


        if (lastToRemove!=null) {
            //removeFromMap(items.removeLast());
            boolean removed = items.removeFast(lastToRemove, s-1);
            //assert(removed);
            if (!removed)
                throw new WTF();
            removeFromMap(lastToRemove);
        }

        int i = items.add(toAdd, this); assert (i >= 0);

        return true;
    }

    /**
     * allows an implementation to remove items which may have been deleted
     * (by anything) since commit checked for them
     */
    protected boolean cleanIfFull() {
        return false;
    }

    protected float sortedness() {
        return 1f;
    }

    protected void sort() {
        int s = size();
        if (s <= 1)
            return;

        float c = sortedness();
        int from, to;

        if (c >= 1f - Float.MIN_NORMAL) {
            //int from /* inclusive */, int to /* inclusive */
            from = 0;
            to = s;
        } else {
            int toSort = (int) Math.ceil(c * s);
            float f = ThreadLocalRandom.current().nextFloat();
            int center = (int) (Util.sqr(f) * (s - toSort) + toSort / 2); //sqr adds curve to focus on the highest priority subsection
            from = Math.max(center - toSort / 2, 0);
            to = Math.min(center + toSort / 2, s);
        }


        items.sort(ScalarValue::priComparable, from, to);
    }


    private int clean(@Nullable Consumer<Y> update) {

//        float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY, mass = 0;
        int s = size();

        SortedArray<Y> items2 = this.items;
        final Object[] l = items2.array();

        float above = Float.POSITIVE_INFINITY;
        boolean sorted = true;

        float m = 0;

        int c = capacity();
        int histRange = s;
        int bins = histogramBins();
        ArrayHistogram hist = this.hist;
        if (hist == null) {
            if (bins > 0)
                hist = new ArrayHistogram(0, histRange - 1, bins);
        } else {
            hist = hist.clear(0, histRange - 1, bins);
        }
//        float q = Float.NaN;
        for (int i = 0; i < s; ) {
            Y y = (Y) l[i];
            //assert y != null;
            float p = pri(y);

            if (update != null && p == p) {
                update.accept(y);
                p = pri(y);
            }

            if (p == p) {

                if (hist != null)
                    hist.addWithoutSettingMass(i, p);

                m += p;

                if (sorted) {
                    if (p - above >= ScalarValue.EPSILON / 2) {
                        sorted = false;
                    } else {
                        above = p;
                    }
                }

//                if (q == q && p - q >= ScalarValue.EPSILON / 2) {
//                    //swap with previous (early progressive sorting pass)
//                    Object x = l[i - 1];
//                    l[i - 1] = y;
//                    l[i] = x;
//                    //q remains the previous of any next item
//                } else {
//                    q = p;
//                }

                i++;


            } else {
                removeFromMap(items2.remove(i));
                s--;
//                q = Float.NaN;
            }
        }

        if (!sorted) {
            sort();
        }

        while (s > c) {
            removeFromMap(this.items.removeLast());
            s--;
        }

        if (hist != null) {
            this.hist = hist;
            ArrayBag.MASS.set(this, hist.mass = m);
        }

        return s;
    }

    /**
     * override and return 0 to effectively disable histogram sampling (for efficiency if sampling isnt needed)
     */
    protected int histogramBins() {
        return histogramBins(size());
    }

    static int histogramBins(int s) {
        //TODO refine
        if (s < 4)
            return 2;
        else if (s < 8)
            return 4;
        else if (s < 16)
            return 6;
        else if (s < 32)
            return 8;
        else if (s < 64)
            return 10;
        else if (s < 128)
            return 12;
        else if (s < 256)
            return 14;
        else
            return 16;
    }

    /**
     * chooses a starting index randomly then iterates descending the list
     * of items. if the sampling is not finished it restarts
     * at the top of the list. so for large amounts of samples
     * it will be helpful to call this in batches << the size of the bag.
     */
    @Override
    public void sample(Random rng, Function<? super Y, SampleReaction> each) {


        //while (true) {
        Object[] ii;
        int s;
        int i;
        while ((s = Math.min((ii = items.array()).length, size())) > 0) {

            i = sampleNext(rng, s);

            Object x = ii[i];

            if (x != null) {
                Y y = (Y) x;
                float yp = pri(y);
                if (yp != yp) {
                    tryRemove(y, i); //deleted, remove
                } else {

                    SampleReaction next = each.apply(y);

                    if (next.remove)
                        tryRemove(y, i);

                    if (next.stop)
                        return;
                }
            }

        }


        //}

    }

    /**
     * size > 0
     */
    private int sampleNext(@Nullable Random rng, int size) {
        assert (size > 0);
        if (size == 1 || rng == null)
            return 0;
        else {
            ArrayHistogram h = this.hist;
            if (h == null || h.mass < ScalarValue.EPSILON * size)
                return rng.nextInt(size);
            else {
                int index = (int) h.sample(rng);
                if (index >= size)
                    index = size - 1;
                return index;
            }
            //return sampleNextLinear(rng, size);
            //return sampleNextBiLinear(rng, size);
        }
    }


    /**
     * raw selection by index, with x^2 bias towards higher pri indexed items
     */
    private static int sampleNextLinear(Random rng, int size) {
        float targetIndex = rng.nextFloat();

        return Util.bin(targetIndex * targetIndex, size);
    }

//    @Override public ArrayBag sample(Random rng, int max, Consumer<? super Y> each) {
//
//    }

    /**
     * samples the distribution with the assumption that it is flat
     */
    private int sampleNextLinearNormalized(Random rng, int size) {
        float min = this.priMin(), max = this.priMax();

        float targetPercentile = rng.nextFloat();

        float indexNorm =
                Util.lerp((max - min), targetPercentile /* flat */, (targetPercentile * targetPercentile) /* curved */);

        return Util.bin(indexNorm, size);
    }

    /**
     * evaluates the median value to model the distribution as 2-linear piecewise function
     * experimental NOT working yet
     */
    private int sampleNextBiLinear(Random rng, int size) {
        if (size == 2) {
            //special case
            return sampleNextLinear(rng, size);
        }

        float targetPercentile = rng.nextFloat();
        float min = this.priMin(), max = this.priMax(), med = priMedian();
        float range = max - min;
        float indexNorm;

        if (range > ScalarValue.EPSILON) {
            float balance = (max - med) / range; //measure of skewness or something; 0.5 = centered (median~=mean)
            //balance < 0.5: denser distribution of elements below the median
            //        > 0.5: denser dist above
            if (balance >= 0.5f)
                targetPercentile = Util.lerp(2 * (balance - 0.5f), targetPercentile, 1); //distort to the extremum
            else
                targetPercentile = Util.lerp(2 * (0.5f - balance), targetPercentile, 0); //distort to the extremum
        }

        indexNorm =
                lerp(range, targetPercentile /* flat */, (targetPercentile * targetPercentile) /* curved */);

        return Util.bin(indexNorm, size);
    }


//    /**
//     * size > 0
//     */
//    protected int sampleNext(@Nullable Random rng, int size) {
//        assert (size > 0);
//        if (size == 1 || rng == null)
//            return 0;
//        else {
//            float min = this.priMin(), max = this.priMax();
//            float diff = max - min;
//
//            float h = rng.nextFloat();
//
//            float i = Util.lerp(diff, h /* flat */, (h * h) /* curved */);
//
//            int j = Math.round(i * (size-0.5f));
//            //assert(j >= 0 && j < size);
//            return j;
//        }
//    }

//    /** experimental / notes */
//    protected int sampleNext0(@Nullable Random rng, int size) {
//        assert (size > 0);
//        if (size == 1 || rng == null)
//            return 0;
//        else {
//
//            float h = rng.nextFloat();
//
//            float min = this.priMin(), max = this.priMax(), median = priMedian();
//            float i;
//
//            {
//              /*
//
//              https://en.wikipedia.org/wiki/Quantile_function
//              https://en.wikipedia.org/wiki/Median#Inequality_relating_means_and_medians
//
//              https://en.wikipedia.org/wiki/Importance_sampling
//              https://en.wikipedia.org/wiki/Inverse_transform_sampling\
//              https://en.wikipedia.org/wiki/Selection_algorithm#Median_selection_as_pivot_strategy
//
//                top heavy   bottom heavy    flat
//                      x          xxxx           xx
//                     xx        xxxxxx         xxxx
//                xxxxxxx       xxxxxxx       xxxxxx
//               med < mid     mid > med
//
//              */
//
//                //probably slow to calculate all of this; much can be cached and done in better ways to approximate the percentile curvature
////                float mean = (max + min) / 2;
////                float range = max - min;
////                float divergence = (median - mean) / range; //normalized
////
////                if (!Float.isFinite(divergence))
////                    i = h; /* flat */
////                else
////                    i = Util.lerp(divergence,
////                            divergence > 0 ? ((float) Math.sqrt(h)) : h /* flat */,
////                            divergence > 0 ? h : (h * h) /* curved */);
//            }
//
//            int j = Math.round(i * (size-0.5f)); //assert(j >= 0 && j < size);
//            return j;
//        }
//    }

//    protected int sampleNext(@Nullable Random rng, int size, int i) {
//        if (rng == null) {
//            if (++i >= size)
//                i = 0;
//
//            return i;
//        } else {
//
//
//            if (--i >= 0)
//                return i; //decrease toward high end
//            else
//                return sampleStart(rng, size);
//        }
//
//    }

    @Nullable
    @Override
    public Y remove(X x) {
        Y removed;
        long l = lock.writeLock();
        try {
            removed = super.remove(x);
        } finally {
            lock.unlockWrite(l);
        }
        if (removed != null) {
            removed(removed);
        }
        return removed;
    }


    private void remove(Y y, int suspectedPosition) {
        remove(y, suspectedPosition, 0, false);
    }
    private void tryRemove(Y y, int suspectedPosition) {
        remove(y, suspectedPosition, 0, true);
    }

    private void remove(Y y, int suspectedPosition, long l, boolean weak) {

        boolean close = false;
        if (l == 0) {
            if (weak) {
                l = lock.tryReadLock();
                if (l == 0) {
                    y.delete(); return; //just delete it for now
                }
            } else {
                l = lock.readLock();
            }
            close = true;
        }

        try {
            if (items.get(suspectedPosition) == y) {


                long ll = weak ? tryWriteFromRead(l) : writeFromRead(l);
                if (ll == 0) {
                    y.delete();
                    return;
                }
                l = ll;

                items.remove(suspectedPosition);
                removeFromMap(y);
            } else {
                super.remove(key(y)); //wasnt found with provided index, use standard method by key
            }
        } finally {
            if (close)
                lock.unlock(l);
        }

    }

//    @Override
//    public void putAll(Consumer<Consumer<Y>> c) {
//
//    }

    @Override
    public Y put(final Y incoming, final NumberX overflow) {

        final int capacity = this.capacity();
        if (capacity == 0)
            return null;

        X key = key(incoming);

        boolean inserted;


        float p = incoming.priElseZero();

        long l = lock.readLock();
        try {

            Y existing = map.get(key);

            if (existing == incoming)
                return incoming; //exact same instance

            if (existing != null) {
                l = writeFromRead(l);
                return merge(existing, incoming, l, overflow);
            } else {

                int s = size();

                if (s >= capacity) {

                    long[] lock = new long[] { l };
                    inserted = tryInsertFull(incoming, p, lock,null);
                    l = lock[0];

                } else {

                    l = writeFromRead(l);

                    int i = items.add(incoming, -p, this);
                    assert i >= 0;
                    inserted = true;

                }

                if (inserted) {
                    Y exists = map.put(key, incoming);
                    assert (exists == null);
                }

            }


        } catch (Throwable t) {
            throw new RuntimeException(t);
        } finally {
            lock.unlock(l);
        }

        pressurize(p);

        if (!inserted) {

            onReject(incoming);

            incoming.delete();

            return null;

        } else {
            MASS.add(this, p);

            onAdd(incoming);

            return incoming;
        }

    }

    protected long writeFromRead(long l) {
        long ll = lock.tryConvertToWriteLock(l);
        if (ll != 0) { l = ll; } else { lock.unlockRead(l);l = lock.writeLock(); }
        return l;
    }
    protected long tryWriteFromRead(long l) {
        long ll = lock.tryConvertToWriteLock(l);
        if (ll != 0) { l = ll; } else { return 0; }
        return l;
    }

//    @Override
//    public final Y get(Object key) {
//        Y y = super.get(key);
//        //check that it's the right element, because the map may not be thread-safe
////        if (y!=null && !(map instanceof ConcurrentMap)) {
////            if (!key.equals(key(y))) {
////                return null; //wasn't it.
////            }
////        }
//        return y;
//    }

//    protected boolean fastMergeMaxReject() {
//        return false;
//    }

    /**
     * will not need to be sorted after calling this; the index is automatically updated
     */
    private Y merge(Y existing, Y incoming, long lock, @Nullable NumberX overflow) {

        int posBefore = items.indexOf(existing, this, true);

        Y result;

        float priBefore = existing.pri();

        float overflo = merge(existing, incoming);
        float priAfter = existing.pri();
        if (priAfter != priAfter) {
            priAfter = 0;
            result = null;
        } else {
            result = existing;
        }

        if (overflo != 0 && overflow != null)
            overflow.add(overflo);

        float delta = priAfter - priBefore;


        if (result != null && Math.abs(delta) >= ScalarValue.EPSILON) {
            items.reprioritize(existing, posBefore, delta, priAfter, this);
        } else if (result == null) {
            //got deleted
            if (!items.removeFast(existing, posBefore))
                throw new ConcurrentModificationException();
            remove(existing, posBefore, lock, false);
        }

        if (Math.abs(delta) > Float.MIN_NORMAL) {
            pressurize(delta);
            MASS.add(this, delta);
        }

        incoming.delete();

        return result;
    }

    protected float merge(Y existing, Y incoming) {
        return mergeFunction.merge(existing, incoming);
    }

    private Y removeFromMap(Y y, boolean callRemoved) {
        Y removed = map.remove(key(y));
        if (removed == null)
            throw new WTF();

        if (callRemoved)
            removed(removed);
        return removed;

    }

    private Y removeFromMap(Y y) {
        return removeFromMap(y, true);
    }

//    @Nullable
//    private Y tryRemoveFromMap(Y x) {
//        Y removed = map.remove(key(x));
//        return removed;
//    }

    @Override
    public Bag<X, Y> commit(Consumer<Y> update) {

        long l = lock.writeLock();
        try {
            if (!isEmpty())
                clean(update);
        } finally {
            lock.unlockWrite(l);
        }

        return this;
    }

    private void removed(Y y) {
        MASS.add(this, -priElse(y, 0));
        onRemove(y);
        //y.delete();
    }

    @Override
    public final void clear() {
        popBatch(-1, ScalarValue::delete);
        MASS.zero(this);
        PRESSURE.zero(this);
    }

    public final void clear(Consumer<? super Y> each) {
        clear(-1, each);
    }

    /**
     * removes the top n items
     *
     * @param n # to remove, if -1 then all are removed
     */
    private void clear(int n, Consumer<? super Y> each) {

        assert (n != 0);

        Collection<Y> popped = new FasterList<>(n > 0 ? Math.min(n, size()) : size());

        popBatch(n, popped::add);

        popped.forEach(each);

    }

    @Override
    public final Sampler<Y> pop(Random rng, int max, Consumer<? super Y> each) {
        if (rng == null) {
            //high-efficiency non-random pop
            clear(max, each);
            return this;
        } else {
            return Bag.super.pop(rng, max, each);
        }
    }


    public Sampler<Y> popBatch(int n, @Nullable Consumer<Y> popped) {

        if (n == 0) return this;

        long l = lock.writeLock();
        try {

            int s = size();
            if (s > 0) {

                int toRemove = n == -1 ? s : Math.min(s, n);

                items.removeRange(0, toRemove,
                        popped != null ? e -> popped.accept(removeFromMap(e))
                                :
                                this::removeFromMap
                );

            }
        } finally {
            lock.unlockWrite(l);
        }

        return this;
    }

    @Override
    public float pri(Y key) {
        return key.pri();
    }


    @Override
    public void forEach(Consumer<? super Y> action) {

//        long r = lock.readLock();
//        try {

        int s = size();
        if (s <= 0)
            return;

        Object[] yy = items.array();
        s = Math.min(yy.length, s);
//        float r = Float.POSITIVE_INFINITY;
//        boolean commit = false;
        for (int i = 0; i < s; i++) {

            Y y =
                    //ITEM.getOpaque(yy, i);
                    (Y) yy[i];

            if (y == null)
                continue; //throw new WTF();

            float p = pri(y);
            if (p == p) {
                action.accept(y);
//                if (!commit) {
//                    if (r <= p - ScalarValue.EPSILON)
//                        commit = true; //out of order detected
//                    r = p;
//                }
            } /*else {
                commit = true;
            }*/
        }
//        } finally {
//            lock.unlockRead(r);
//        }

//        if (commit) {
//            commit(null);
//        }
    }


    @Override
    public String toString() {
        return super.toString() + '{' + items.getClass().getSimpleName() + '}';
    }

    @Override
    public float priMax() {
        Y x = items.first();
        return x != null ? priElse(x, -1) : 0;
    }

    /**
     * priority of the middle index item, if exists; else returns average of priMin and priMax
     */
    private float priMedian() {

        Object[] ii = items.items;
        int s = Math.min(ii.length, size());
        if (s > 2)
            return pri((Y) ii[s / 2]);
        else if (s > 1)
            return (priMin() + priMax()) / 2;
        else
            return priMin();
    }

    @Override
    public float priMin() {
        Y x = items.last();
        return x != null ? priElse(x, -1) : 0;
    }

//    private static final class SortedPLinks extends SortedArray {
////        @Override
////        protected Object[] newArray(int s) {
////            return new Object[s == 0 ? 2 : s + Math.max(1, s / 2)];
////        }
//
////        @Override
////        protected int grow(int oldSize) {
////            return super.grow(oldSize);
////        }
//
//        @Override
//        protected boolean grows() {
//            return false;
//        }
//    }


}


































