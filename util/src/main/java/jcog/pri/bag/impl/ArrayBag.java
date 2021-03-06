package jcog.pri.bag.impl;

import jcog.TODO;
import jcog.Util;
import jcog.WTF;
import jcog.data.NumberX;
import jcog.data.iterator.ArrayIterator;
import jcog.data.list.FasterList;
import jcog.data.list.table.Table;
import jcog.pri.Prioritizable;
import jcog.pri.ScalarValue;
import jcog.pri.bag.Bag;
import jcog.pri.bag.Sampler;
import jcog.pri.op.PriMerge;
import jcog.pri.op.PriReturn;
import jcog.signal.wave1d.ArrayHistogram;
import jcog.sort.SortedArray;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.StampedLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;


/**
 * A bag implemented as a combination of a Map and a SortedArrayList
 * TODO extract a version of this which will work for any Prioritized, not only BLink
 */
public abstract class ArrayBag<X, Y extends Prioritizable> extends Bag<X, Y> {

    private static final int HISTOGRAM_SUPERSAMPLING = 3;
    private static final int HISTOGRAM_RESOLUTION_FACTOR = 5;
    /**
     * override and return 0 to effectively disable histogram sampling (for efficiency if sampling isnt needed)
     */
    protected int histogramBins(int s) {
        //TODO refine
        int thresh = 4;
        if (s <= thresh)
            return s;
        else {
            //return (int) (thresh + Math.sqrt((s - thresh)));
            return (int)((double) (thresh - 1) + Math.log((double) (1 + s - thresh)) * (double) HISTOGRAM_RESOLUTION_FACTOR);
        }
    }


    private final StampedLock lock = new StampedLock();
    private final MySortedListTable table;


    private final ArrayHistogram hist = new ArrayHistogram();


    ArrayBag(PriMerge merge, Map<X, Y> map) {
        this(merge, 0, map);
    }

    protected ArrayBag(PriMerge merge, @Deprecated int cap, Map<X, Y> map) {
        table = new MySortedListTable(map);
        setCapacity(cap);
        merge(merge);
    }

    private static float rngFloat(@Nullable Random rng) {
        return rng!=null ? rng.nextFloat() : ThreadLocalRandom.current().nextFloat();
    }

    @Override
    public void clear() {
        pressureZero();
        popBatch(Integer.MAX_VALUE,
                //this::removed
                this::onRemove
        );
    }

    public final Object[] items() { return table.items.array(); }

    @Override
    public final int size() {
        return table.items.size();
    }

    @Override
    public @Nullable Y get(Object key) {
        return table.get(key);
    }

    public final @Nullable Y get(int index) {
        return table.items.getSafe(index);
    }

    /** different from sample(..); this will return uniform random results */
    public final @Nullable Y get(Random rng) {
        Object[] items = table.items.items;
        int s = Math.min(items.length, size());
        return s > 0 ? (Y)items[rng.nextInt(s)] : null;
    }

    @Override
    protected void onCapacityChange(int before, int after) {
        if (before > after) {
            //capacity decrease
            commit(null);
        }
    }

    @Override
    public Stream<Y> stream() {
        int s = size();
        if (s == 0)
            return Stream.empty();
        else {
            Object[] x = items();
            return ArrayIterator.stream(x).filter(Objects::nonNull)
                    .map(new Function<Object, Y>() {
                        @Override
                        public Y apply(Object o) {
                            return (Y) o;
                        }
                    })
                    .filter(new Predicate<Y>() {
                        @Override
                        public boolean test(Y y) {
                            return !y.isDeleted();
                        }
                    });
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
    private boolean tryInsertFull(X key, Y incoming, float toAddPri) {

        int s = size();
        int c = capacity();
        boolean free = s + 1 <= c;

        if (!free && cleanAuto()) {
            s = update(null);
            free = s + 1 <= c;
        }

        SortedArray<Y> a = table.items;
        if (!free) {
            Y lastToRemove = a.last();
            float priMin = pri(lastToRemove);
            if (toAddPri < priMin)
                return false;

            Y removed = a.removeLast();
            assert (removed == lastToRemove);
            //if (!removed) throw new WTF(); //assert(removed);

            removeFromMap(lastToRemove);

        } else {
            //space has been cleared for the new item
        }


        tryInsert(key, incoming, toAddPri);

        return true;
    }

    /**
     * allows an implementation to remove items which may have been deleted
     * (by anything) since commit checked for them
     */
    protected static boolean cleanAuto() {
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
            int toSort = (int) Math.ceil((double) (c * (float) s));
            float f = ThreadLocalRandom.current().nextFloat();
            int center = (int) (Util.sqr(f) * (float) (s - toSort) + (float) (toSort / 2)); //sqr adds curve to focus on the highest priority subsection
            from = Math.max(center - toSort / 2, 0);
            to = Math.min(center + toSort / 2, s);
        }


        table.items.sort(ScalarValue::priComparable, from, to);
        //assert(isSorted(ScalarValue::priComparable)); //TEMPORARY

    }

    /** update histogram, remove values until under capacity */
    private int update(@Nullable Consumer<Y> update) {

        int s = size();
        if (s == 0)
            return 0;

        SortedArray<Y> items = table.items;
        Object[] a = items.array();


        ArrayHistogram.HistogramWriter hist;
        int bins = histogramBins(s);
        if (bins > 0) {
            hist = this.hist.write(0, s, bins);
        } else {
            hist = null; //disabled
        }

        float m = (float) 0;
        boolean sorted = true;
        float above = Float.POSITIVE_INFINITY;
        for (int i = 0; i < s; ) {
            Object _y = a[i];

            if (_y == null) {
                a[i] = null;
                s--;
            } else {
                Y y = (Y) _y;

                float p = pri(y);

                if (update != null && p == p) {
                    update.accept(y);
                    p = pri(y);
                }

                if (p == p) {

                    if (hist != null && p > Float.MIN_NORMAL) {
                        hist.add(i, p, HISTOGRAM_SUPERSAMPLING);
                        m += p;
                    }

                    if (sorted) {
                        if (p - above >= ScalarValue.Companion.getEPSILON() / 2.0F) {
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

                    items.removeFast(y, i);
                    removeFromMap(y);
                    s--;
                }
            }
        }

        if (!sorted)
            sort();

        massSet(m); //set mass here because removedFromMap will decrement mass

        int c = capacity();
        while (s > c) {
            removeFromMap(table.items.removeLast());
            s--;
        }

        if (hist!=null)
            hist.commit();

        return s;
    }



    /** immediate sample */
    @Override public @Nullable Y sample(Random rng) {
        Object[] ii = this.items();
        int s = Math.min(ii.length, size());
        if (s == 0)
            return null;
        int i = sampleNext(rng, s);
        return (Y) ii[i];
    }

    /**
     * chooses a starting index randomly then iterates descending the list
     * of items. if the sampling is not finished it restarts
     * at the top of the list. so for large amounts of samples
     * it will be helpful to call this in batches << the size of the bag.
     */
    @Override
    public void sample(Random rng, Function<? super Y, SampleReaction> each) {

        Object[] ii;
        int s;

        while ((s = Math.min((ii = items()).length, size())) > 0) {

            int i = sampleNext(rng, s);

            Object x = ii[i];

            if (x != null) {
                Y y = (Y) x;
                float yp = pri(y);
                if (yp == yp) {

                    SampleReaction next = each.apply(y);

                    if (next.remove) {

                        //explicit removal
                        boolean d = deleteOnPop();
                        if (d)
                            y.delete();

                        samplePopRemove(y, i, !d, next.stop, rng);

                    } else {
                        if (rng==null)
                            throw new TODO("without a fix, this will continue to spin on 0th item"); //HACK
                    }

                    if (next.stop)
                        return;
                }
            }
        }


        //}

    }

    @Override
    public void sampleUnique(Random rng, Predicate<? super Y> kontinue) {
        int s = size();
        if (s == 0) return;

        Object[] ll = items();
        s = Math.min(s, ll.length);
        if (s == 0) return;

        //starting point, sampled from bag histogram
        int p = sampleNext(rng,s);
        if (p >= s) p = s-1;

        //scan up then down
        for (int j = 0; j < s; j++) {
            int above = p - j;
            if (above >= 0) {
                Object a = ll[above];
                if (a != null && !kontinue.test((Y) a))
                    return;
            } else
                break;
        }
        for (int j = 0; j < s; j++) {
            int below = p + j + 1;
            if (below < s) {
                Object b = ll[below];
                if (b!=null && !kontinue.test((Y) b))
                    return;
            } else
                break;
        }

//        //scan radially around point, O(N)
//        for (int j = 0; j < s; j++) {
//
//            int done = 0;
//            int above = p - j;
//            if (above >= 0) {
//                Object a = ll[above];
//                if (a!=null && !kontinue.test((Y) a))
//                    return;
//            } else done++;
//
//            int below = p + j + 1;
//            if (below < s) {
//                Object b = ll[below];
//                if (b!=null && !kontinue.test((Y) b))
//                    return;
//            } else done++;
//
//            if (done == 2)
//                break;
//        }

    }

    /** whether to delete immediately on pop, even if it cant be immediately removed from the bag during sampling */
    protected static boolean deleteOnPop() {
        return false;
    }

    /**
     * called periodically after a sample pop's, since the bag will have changed.  the smaller the bag,
     * the more influence that pop will have had on the statistics that will decide further sampling.
     * the larger the bag, the less frequently this will be called so there is a balanced tradeoff.
     */
    private void tryRecommit(Random rng, int size) {
        if (size > 0) {
            if (size==1 || (rngFloat(rng) < 1f / (float) size)) {
                //if (!lock.isWriteLocked()) {
                    commit(null);
                //}
            }
        }
    }

    public int sampleNext(@Nullable Random rng, int size) {
        return (rng == null || size <= 1) ? 0 :
            hist.sampleInt(rng);
            //sampleNextLinearNormalized(rng,size);
            //sampleNextBiLinearNormalized(rng,size);
            //sampleNextLinear(rng, size);
    }

    /**
     * raw selection by index, with x^2 bias towards higher pri indexed items
     */
    private static int sampleNextLinear(Random rng, int size) {
        float targetIndex = rng.nextFloat();

        return Util.bin(targetIndex * targetIndex, size);
    }

    /**
     * samples the distribution with the assumption that it is flat
     */
    private int sampleNextLinearNormalized(Random rng, int size) {
        return sampleNextLinearNormalized(rng, 0, size);
    }

    private float priSafe(int i) {
        Object[] a = table.items.array();
        if (i < a.length) {
            Y v = (Y) a[i];
            if (v!=null)
                return priElse(v, (float) 0);
        }
        return (float) (i == 0 ? 0 : 1);
    }

    private int sampleNextLinearNormalized(Random rng, int start, int end) {
        float max = priSafe(start), min = priSafe(end);

        float targetPercentile = rng.nextFloat();

        float indexNorm =
                Util.lerp((max - min), targetPercentile /* flat */, (targetPercentile * targetPercentile) /* curved */);

        return start + Util.bin(indexNorm, (end-start));
    }

    /** estimates percentile curve with 2-segment linear approximation */
    private int sampleNextBiLinearNormalized(Random rng, int size) {
        if (size<=2)
            return sampleNextLinearNormalized(rng, size);

        float max = priSafe(0), min = priSafe(size-1);
        if (max - min < 1f/ (float) (size * size))
            return sampleNextLinearNormalized(rng, size); //HACK elide recompute pri

        int mii = size / 2;
        float mid = priSafe(mii);


        float skew = (mid - (max-min)/ 2.0F)/(max-min);
        if (rng.nextFloat() > skew) {
            return sampleNextLinearNormalized(rng, 0, mii);
        } else {
            return sampleNextLinearNormalized(rng, mii, size);
        }

    }

//    /**
//     * evaluates the median value to model the distribution as 2-linear piecewise function
//     * experimental NOT working yet
//     */
//    private int sampleNextBiLinear(Random rng, int size) {
//        if (size == 2) {
//            //special case
//            return sampleNextLinear(rng, size);
//        }
//
//        float targetPercentile = rng.nextFloat();
//        float min = ArrayBag.this.priMin(), max = ArrayBag.this.priMax(), med = priMedian();
//        float range = max - min;
//        float indexNorm;
//
//        if (range > ScalarValue.EPSILON) {
//            float balance = (max - med) / range; //measure of skewness or something; 0.5 = centered (median~=mean)
//            //balance < 0.5: denser distribution of elements below the median
//            //        > 0.5: denser dist above
//            if (balance >= 0.5f)
//                targetPercentile = Util.lerp(2 * (balance - 0.5f), targetPercentile, 1); //distort to the extremum
//            else
//                targetPercentile = Util.lerp(2 * (0.5f - balance), targetPercentile, 0); //distort to the extremum
//        }
//
//        indexNorm =
//                lerp(range, targetPercentile /* flat */, (targetPercentile * targetPercentile) /* curved */);
//
//        return Util.bin(indexNorm, size);
//    }


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


    /**
     * if y is or can be deleted by callee,
     * then the next commit will fully remove the item whether this fails in weak mode or not.
     */
    private void samplePopRemove(Y y, int suspectedPosition, boolean strong, boolean stop, Random rng) {

        long l = strong ? lock.writeLock() : lock.tryWriteLock();
        if (l == 0L)
            return;

        try {

            y = removeFromMapIfExists(y);
            if (y == null)
                return; //already removed

            boolean removed = table.items.removeFast(y, suspectedPosition);
            if (!removed) {
                removed = table.removeItem(y); //exhaustive
            }
            assert (removed);

            if (!stop)
                tryRecommit(rng, size());

        } finally {
            lock.unlockWrite(l);
        }

    }

    @Override
    public final @Nullable Y remove(X x) {
        Y removed;
        long l = lock.writeLock();
        try {
            Y rx = table.map.remove(x);
            if (rx != null) {
                boolean removedFromList = table.removeItem(rx);
                if (!removedFromList)
                    throw new ConcurrentModificationException("inconsistency while attempting removal: " + x + " -> " + rx);
            }
            removed = rx;
        } finally {
            lock.unlockWrite(l);
        }
        if (removed != null)
            removed(removed);
        return removed;
    }

//    @Override
//    public void putAll(Consumer<Consumer<Y>> c) {
//
//    }

    @Override
    public Y put(Y x, NumberX overflow) {

        int capacity = this.capacity();
        if (capacity == 0)
            return null;

        float xp = x.pri();
        if (xp != xp)
            return null; //already deleted

        X key = key(x);

        Map<X, Y> map = table.map;
//        if (map instanceof ConcurrentMap) {
//            //check map first, and elide acquiring a lock if merge can be performed and doesnt affect priority
//        } else {
        //l = lock.readLock();
//        }
        long l = lock.writeLock();

        Y existing = map.get(key);

        if (existing == null || existing == x) {
            Y y;
            if (existing == x) {
                //exact same instance
                if (l != 0L)
                    lock.unlock(l);
                y = x;
            } else {
                //l = Util.readToWrite(l, this.lock);
                y = insert(x, key, xp, l);
            }

            pressurize(xp);
            return y;
        } else {
            //l = Util.readToWrite(l, this.lock);

            //merge() handles the correct delta pressurization, so prssurize after the lock is released
            return merge(existing, x, xp, overflow, l);
        }


    }

    private Y insert(Y incoming, X key, float pri, long wl) {
        boolean inserted;
        try {
            int capacity = capacity();
            int s = size();
            if (s >= capacity) {
                inserted = tryInsertFull(key, incoming, pri);
            } else {
                tryInsert(key, incoming, pri);
                inserted = true;
            }
        } finally {
            this.lock.unlockWrite(wl);
        }


        if (!inserted) {

            onReject(incoming);

            //incoming.delete();

            return null;

        } else {
            massAdd(pri);

            onAdd(incoming);

            return incoming;
        }
    }

    private void tryInsert(X key, Y incoming, float p) {
        int i = table.items.addSafe(incoming, -p, table);
        Y exists = table.map.put(key, incoming);
        assert(i >= 0 && exists == null);
    }


    /**
     * will not need to be sorted after calling this; the index is automatically updated
     * <p>
     * handles delta pressurization
     * postcondition: write-lock will be unlocked asap
     */
    private Y merge(Y existing, Y incoming, float incomingPri, @Nullable NumberX overflow, long wl) {

        Y result;

        float delta;
        float priBefore = existing.pri();

        try {

            delta = merge(existing, incoming, incomingPri);

//            float priAfter = existing.pri();
//            if (priAfter != priAfter) {
//                priAfter = 0;
//                result = null;
//            } else {
                result = existing;
//            }

            //delta = priAfter - priBefore;

            if (sortContinuously()) {
                if (Math.abs(delta) >= ScalarValue.Companion.getEPSILON()) {
                    //if removed, or significant change occurred

                    //if (result != null) {
                    float priAfter = priBefore + delta;
                    table.items.reprioritize(existing, posBefore(existing, priBefore), delta, priAfter, table);
//                    } else {
//                        //got deleted
//                        if (!table.items.removeFast(existing, posBefore(existing, priBefore)))
//                            throw new ConcurrentModificationException();
//                        removeFromMap(existing);
//                    }
                }
            }
        } finally {
            lock.unlockWrite(wl);
        }

        //incoming.delete();

        if (overflow != null) {
            float over = Math.max((float) 0, incomingPri - delta);
            if (over > (float) 0)
                overflow.add(over);
        }

        if (Math.abs(delta) > Float.MIN_NORMAL) {
            pressurize(delta);
            massAdd(delta);
        }


        return result;
    }

    protected float merge(Y existing, Y incoming, float incomingPri) {
        return merge().merge(existing, incomingPri, PriReturn.Delta);
    }

    /**
     * whether to attempt re-sorting the list after each merge, in-between commits
     */
    protected static boolean sortContinuously() {
        //TODO try policy of: randomly in proportion to bag fill %
        return true;
        //return false;
    }


    private int posBefore(Y existing, float priBefore) {
        return table.items.indexOf(existing, priBefore, table, true, false);
    }

    /**
     * remove from list should have occurred before the map removal
     */
    private Y removeFromMap(Y y) {

        Y removed = removeFromMapIfExists(y);
        if (removed == null)
            throw new WTF();

        return removed;
    }

    private @Nullable Y removeFromMapIfExists(Y y) {
        Y removed = table.map.remove(key(y));
        if (removed == null)
            return null;

        removed(removed);
        return removed;
    }

    @Override
    public Bag<X, Y> commit(Consumer<Y> update) {

//        long l = lock.readLock();
        long l = lock.writeLock();
        try {
            //if (!isEmpty()) {
//                l = Util.readToWrite(l, lock);
                update(update);
            //}
        } finally {
            lock.unlockWrite(l);
        }

        return this;
    }

    private void removed(Y y) {
        massAdd(-priElse(y, (float) 0));
        onRemove(y);
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

        Object[] yy = items();
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
            } /*else {
                tryRemove(y, i); //already deleted
            }*/
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
    public void clear(int n, Consumer<? super Y> each) {

        int s = Math.min(n, size());
        if (s > 0) {
            Collection<Y> popped = new FasterList<>(s);

            popBatch(s, popped::add);

            for (Y y : popped) {
                each.accept(y);
            }
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


    private Sampler<Y> popBatch(int n, @Nullable Consumer<Y> popped) {
        return popBatch(n, false, popped);
    }

    public Sampler<Y> popBatch(int n, boolean block, @Nullable Consumer<Y> popped) {

        if (n == 0) return ArrayBag.this;


        long l = block ? lock.writeLock() : lock.tryWriteLock();
        if (l != 0L) {
            try {
                int toRemove = Math.min(n, size());
                if (toRemove > 0) {

                    Consumer<Y> each = popped != null ?
                            new Consumer<Y>() {
                                @Override
                                public void accept(Y e) {
                                    popped.accept(ArrayBag.this.removeFromMap(e));
                                }
                            }
                            :
                            ArrayBag.this::removeFromMap;

                    table.items.removeRangeSafe(0, toRemove, each);
                }

                if (size() == 0) {
                    massZero();
                }

            } finally {
                lock.unlockWrite(l);
            }
        }
        return ArrayBag.this;
    }

    @Override
    public final float pri(Y value) {
        return value.pri();
    }


    @Override
    public float priMax() {
        Y x = table.items.first();
        return x != null ? priElse(x, (float) 0) : (float) 0;
    }

//    /**
//     * priority of the middle index item, if exists; else returns average of priMin and priMax
//     */
//    private float priMedian() {
//
//        Object[] ii = table.items.items;
//        int s = Math.min(ii.length, size());
//        if (s > 2)
//            return pri((Y) ii[s / 2]);
//        else if (s > 1)
//            return (priMin() + priMax()) / 2;
//        else
//            return priMin();
//    }

    @Override
    public float priMin() {
        Y x = table.items.last();
        return x != null ? priElse(x, (float) 0) : (float) 0;
    }

    public Iterator<Y> iterator() {
        return table.iterator();
        //return stream().iterator(); //has null and deletion filtering
    }

    public final boolean isSorted() {
        return isSorted(table);
    }

    public final boolean isSorted(FloatFunction<Y> f) {
        return table.items.isSorted(f);
    }

    @Override
    public final void forEach(int max, Consumer<? super Y> action) {
        int s = size();
        if (s > 0) {
            Y[] ii = table.items.items;
            int c = Math.min(s, ii.length);
            Bag.forEach(new IntToObjectFunction<Y>() {
                @Override
                public Y valueOf(int i) {
                    return ii[i];
                }
            }, c, max, action);
        }
    }

    private class MySortedListTable implements FloatFunction<Y>, Table<X, Y> {

        private final SortedArray<Y> items = new SortedArray<>();

        private final Map<X, Y> map;

        MySortedListTable(Map<X, Y> map) {
            this.map = map;
        }

        @Override
        public final int capacity() {
            return ArrayBag.this.capacity();
        }

        @Override
        public final float floatValueOf(Y y) {
            return -y.priElseNeg1();
        }


        @Override
        public @Nullable Y remove(X x) {
            throw new UnsupportedOperationException();
        }

        @Override
        public final Iterator<Y> iterator() {
            return items.iteratorNonNull();
        }

        public final Y get(int i) {
            return items.get(i);
        }

        @Override
        public final int size() {
            return items.size();
        }

        final boolean removeItem(Y removed) {
            return items.remove(removed, this);
        }

        final void listClear() {
            items.clear();
        }

        @Override
        public void forEachKey(Consumer<? super X> each) {
            for (Y t : this) {
                each.accept(key(t));
            }
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
            for (Map.Entry<X, Y> entry : map.entrySet()) {
                X key = entry.getKey();
                Y value = entry.getValue();
                each.accept(key, value);
            }
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


































