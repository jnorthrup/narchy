package jcog.pri.bag.impl;

import jcog.Util;
import jcog.WTF;
import jcog.data.NumberX;
import jcog.data.iterator.ArrayIterator;
import jcog.data.list.FasterList;
import jcog.data.list.table.Table;
import jcog.pri.Prioritizable;
import jcog.pri.Prioritized;
import jcog.pri.ScalarValue;
import jcog.pri.bag.Bag;
import jcog.pri.bag.Sampler;
import jcog.pri.op.PriMerge;
import jcog.signal.wave1d.ArrayHistogram;
import jcog.sort.SortedArray;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.StampedLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static jcog.Util.lerp;


/**
 * A bag implemented as a combination of a Map and a SortedArrayList
 * TODO extract a version of this which will work for any Prioritized, not only BLink
 */
abstract public class ArrayBag<X, Y extends Prioritizable> extends Bag<X, Y> {

    private final StampedLock lock = new StampedLock();
    private final MySortedListTable table;


    private transient ArrayHistogram hist = null;


    ArrayBag(PriMerge merge, Map<X, Y> map) {
        this(merge, 0, map);
    }

    protected ArrayBag(PriMerge merge, @Deprecated int cap, Map<X, Y> map) {
        table = new MySortedListTable(new SortedArray<>(), map);
        setCapacity(cap);
        merge(merge);
    }

    @Override
    public @Nullable Y remove(X x) {
        return table.remove(x);
    }

    @Override
    public void clear() {
        pressureZero();
        popBatch(Integer.MAX_VALUE, this::onRemove);
    }

    @Override
    public final int size() {
        return table.size();
    }

    @Override
    public @Nullable Y get(Object key) {
        return table.get(key);
    }

    public final @Nullable Y get(int index) {
        return table.items.get(index);
    }

    @Override
    protected void onCapacityChange(int before, int after) {
        if (before > after) {
            //capacity decrease
            commit(null);
        }
    }

    /**
     * gets the scalar float value used in a comparison of BLink's
     * essentially the same as b.priIfFiniteElseNeg1 except it also includes a null test. otherwise they are interchangeable
     */
    private static float pCmp(@Nullable Object b) {
        return b == null ? -2.0f : ((Prioritized) b).priElseNeg1();
    }


    @Override
    public Stream<Y> stream() {
        int s = table.size();
        if (s == 0) return Stream.empty();
        else {
            Object[] x = table.items.array();
            return ArrayIterator.stream(x).map(o -> (Y) o).filter(y -> y != null && !y.isDeleted());
        }
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
            s = table.size();

        int c = capacity();

        boolean free = s + 1 <= c;

        Y lastToRemove;
        if (!free) {
            lastToRemove = table.items.last();
            float priMin = pri(lastToRemove);
            if (toAddPri <= priMin)
                return false;

        } else {
            //space has been cleared for the new item
            lastToRemove = null;
        }


        if (lastToRemove!=null) {
            //removeFromMap(items.removeLast());
            boolean removed = table.items.removeFast(lastToRemove, s-1);
            //assert(removed);
            if (!removed)
                throw new WTF();
            removeFromMap(lastToRemove);
        }

        int i = table.items.add(toAdd, table); assert (i >= 0);

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
        int s = table.size();
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


        table.items.sort(ScalarValue::priComparable, from, to);
    }


    private int clean(@Nullable Consumer<Y> update) {

//        float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY, mass = 0;
        int s = table.size();

        SortedArray<Y> items2 = table.items;
        final Object[] l = items2.array();

        float above = Float.POSITIVE_INFINITY;
        boolean sorted = true;

        float m = 0;

        int c = capacity();
        int histRange = s;
        int bins = histogramBins();
        ArrayHistogram hist = ArrayBag.this.hist;
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
            removeFromMap(table.items.removeLast());
            s--;
        }

        if (hist != null) {
            ArrayBag.this.hist = hist;
            massSet(hist.mass = m);
        }

        return s;
    }

    /**
     * override and return 0 to effectively disable histogram sampling (for efficiency if sampling isnt needed)
     */
    protected int histogramBins() {
        return histogramBins(table.size());
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
        while ((s = Math.min((ii = table.items.array()).length, table.size())) > 0) {

            i = sampleNext(rng, s);

            Object x = ii[i];

            if (x != null) {
                Y y = (Y) x;
                float yp = pri(y);
                if (yp != yp) {
                    tryRemove(y, i); //deleted, remove
                } else {

                    SampleReaction next = each.apply(y);

                    if (next.remove) {
                        y.delete();
                        tryRemove(y, i);
                    }

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
            ArrayHistogram h = ArrayBag.this.hist;
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
        float min = ArrayBag.this.priMin(), max = ArrayBag.this.priMax();

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
        float min = ArrayBag.this.priMin(), max = ArrayBag.this.priMax(), med = priMedian();
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


    private void tryRemove(Y y, int suspectedPosition) {
        remove(y, suspectedPosition, 0, true);
    }

    /** if y is or can be deleted by callee,
     *  then the next commit will fully remove the item whether this fails in weak mode or not. */
    private void remove(Y y, int suspectedPosition, long l, boolean weak) {

        boolean close = false;
        if (l == 0) {
            if (weak) {
                l = lock.tryWriteLock();
                if (l == 0)
                    return;
            } else {
                l = lock.readLock();
            }
            close = true;
        }

        try {
            long ll = weak ? tryWriteFromRead(l) : writeFromRead(l);
            if (ll == 0)
                return;

            if (table.items.get(suspectedPosition) == y) {
                boolean removed = table.items.removeFast(y,suspectedPosition);
                assert(removed);
            } else {
                boolean removed = table.removeItem(y);
                assert(removed);
            }
            removeFromMap(y);
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

        X key = table.key(incoming);

        boolean inserted;


        float p = incoming.priElseZero();

        long l = lock.readLock();
        try {

            Y existing = table.map.get(key);

            if (existing == incoming)
                return incoming; //exact same instance

            if (existing != null) {
                l = writeFromRead(l);
                return merge(existing, incoming, overflow);
            } else {

                int s = table.size();

                if (s >= capacity) {

                    long[] lock = new long[] { l };
                    inserted = tryInsertFull(incoming, p, lock,null);
                    l = lock[0];

                } else {

                    l = writeFromRead(l);

                    int i = table.items.add(incoming, -p, table);
                    assert i >= 0;
                    inserted = true;

                }

                if (inserted) {
                    Y exists = table.map.put(key, incoming);
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
            massAdd(p);

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
    private Y merge(Y existing, Y incoming, @Nullable NumberX overflow) {

        int posBefore = table.items.indexOf(existing, table, true);

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
            table.items.reprioritize(existing, posBefore, delta, priAfter, table);
        } else if (result == null) {
            //got deleted
            if (!table.items.removeFast(existing, posBefore))
                throw new ConcurrentModificationException();
            removeFromMap(existing);
        }

        if (Math.abs(delta) > Float.MIN_NORMAL) {
            pressurize(delta);
            massAdd(delta);
        }

        incoming.delete();

        return result;
    }

    protected float merge(Y existing, Y incoming) {
        return merge().merge(existing, incoming);
    }

    private Y removeFromMap(Y y, boolean callRemoved) {
        Y removed = table.map.remove(table.key(y));
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
            //if (!isEmpty())
            clean(update);
        } finally {
            lock.unlockWrite(l);
        }

        return ArrayBag.this;
    }

    private void removed(Y y) {
        massAdd(-priElse(y, 0));
        onRemove(y);
        //y.delete();
    }

    public final void clear(Consumer<? super Y> each) {
        clear(Integer.MAX_VALUE, each);
    }


    @Override
    public void forEach(Consumer<? super Y> action) {

        //        long r = lock.readLock();
        //        try {

        int s = size();
        if (s <= 0)
            return;

        Object[] yy = table.items.array();
        s = Math.min(yy.length, s);
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
            } else {
                tryRemove(y, i); //already deleted
            }
        }
        //        } finally {
        //            lock.unlockRead(r);
        //        }

        //        if (commit) {
        //            commit(null);
        //        }
    }

    /**
     * removes the top n items
     *
     * @param n # to remove, if -1 then all are removed
     */
    private void clear(int n, Consumer<? super Y> each) {

        int s = Math.min(n, table.size());
        if (s > 0) {
            Collection<Y> popped = new FasterList<>(s);

            popBatch(s, popped::add);

            popped.forEach(each);
        }

    }

    @Override
    public final Sampler<Y> pop(Random rng, int max, Consumer<? super Y> each) {
        if (rng == null) {
            //high-efficiency non-random pop
            clear(max, each);
            return ArrayBag.this;
        } else {
            return super.pop(rng, max, each);
        }
    }


    public final Sampler<Y> popBatch(int n, @Nullable Consumer<Y> popped) {
        return popBatch(n, true, popped);
    }

    public Sampler<Y> popBatch(int n, boolean block, @Nullable Consumer<Y> popped) {

        if (n == 0) return ArrayBag.this;

        Consumer<Y> each = popped != null ?
                e -> popped.accept(removeFromMap(e))
                :
                ArrayBag.this::removeFromMap;

        long l = block ? lock.writeLock() : lock.tryWriteLock();
        if (l != 0) {
            try {
                int toRemove = Math.min(n, table.size());
                if (toRemove > 0)
                    table.items.removeRangeSafe(0, toRemove, each);

                if (size()==0) {
                    massZero();
                }

            } finally {
                lock.unlockWrite(l);
            }
        }
        return ArrayBag.this;
    }

    @Override
    public float pri(Y key) {
        return key.pri();
    }


    @Override
    public String toString() {
        return super.toString() + '{' + table.items.getClass().getSimpleName() + '}';
    }

    @Override
    public float priMax() {
        Y x = table.items.first();
        return x != null ? priElse(x, -1) : 0;
    }

    /**
     * priority of the middle index item, if exists; else returns average of priMin and priMax
     */
    private float priMedian() {

        Object[] ii = table.items.items;
        int s = Math.min(ii.length, table.size());
        if (s > 2)
            return pri((Y) ii[s / 2]);
        else if (s > 1)
            return (priMin() + priMax()) / 2;
        else
            return priMin();
    }

    @Override
    public float priMin() {
        Y x = table.items.last();
        return x != null ? priElse(x, -1) : 0;
    }

    public Iterator<Y> iterator() {
        return table.iterator();
    }

    public final boolean isSorted() {
        return isSorted(table);
    }

    public final boolean isSorted(FloatFunction<Y> f) {
        return table.items.isSorted(f);
    }

    @Override public final void forEach(int max, Consumer<? super Y> action) {
        int s = size();
        if (s > 0) {
            Y[] ii = table.items.items;
            int c = Math.min(s, ii.length);
            Bag.forEach(i -> ii[i], c, max, action);
        }
    }

    private class MySortedListTable implements FloatFunction<Y>, Table<X, Y> {

        private final SortedArray<Y> items;

        private final Map<X, Y> map;

        public MySortedListTable(SortedArray<Y> items, Map<X, Y> map) {
            this.map = map;
            this.items = items;
        }

        @Override
        public final int capacity() {
            return ArrayBag.this.capacity();
        }

        @Override
        public final float floatValueOf(Y y) {
            return -pCmp(y);
        }


        @Nullable
        @Override
        public Y remove(X x) {
            Y removed;
            long l = lock.writeLock();
            try {
                Y removed1 = map.remove(x);
                if (removed1 != null) {
                    boolean removedFromList = removeItem(removed1);
                    if (!removedFromList)
                        throw new WTF();
                }
                removed = removed1;
            } finally {
                lock.unlockWrite(l);
            }
            if (removed != null) {
                removed(removed);
            }
            return removed;
        }



        public X key(Y y) {
            return ArrayBag.this.key(y);
        }

        @Override
        public final Iterator<Y> iterator() {
            return items.iterator();
        }

        public final Y get(int i) {
            return items.get(i);
        }

        @Override
        public final int size() {
            return items.size();
        }

        protected final boolean removeItem(Y removed) {
            return items.remove(removed, this);
        }

        protected final void listClear() {
            items.clear();
        }

        @Override
        public void forEachKey(Consumer<? super X> each) {
            forEach(t -> each.accept(key(t)));
        }

        public void clear() {
            map.clear();
            listClear();
        }

        /**
         * Check if an item is in the bag
         *
         * @param k An item
         * @return Whether the Item is in the Bag
         */
        public final boolean contains(/**/ X k) {
            return map.containsKey(k);
        }

        public final void forEach(BiConsumer<X, Y> each) {
            map.forEach(each);
        }

        public final Y get(Object key) {
            return map.get(key);
        }

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


































