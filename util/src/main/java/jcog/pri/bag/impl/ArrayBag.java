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
import jcog.sort.SortedArray;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
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


    @Deprecated private final PriMerge mergeFunction;

    private volatile int mass, pressure;

    protected ArrayBag(PriMerge mergeFunction, int capacity) {
        this(mergeFunction,
                new HashMap<>(capacity, 0.99f)
                //new UnifiedMap<>(capacity, 0.99f)
        );
        setCapacity(capacity);
    }

    ArrayBag(PriMerge mergeFunction, Map<X, Y> map) {
        this(0, mergeFunction, map);
    }

    protected ArrayBag(@Deprecated int cap, PriMerge mergeFunction, Map<X, Y> map) {
        super(new SortedArray<>(), map);
        this.mergeFunction = mergeFunction;
        setCapacity(cap);
    }

    @Override
    public float pressure() {
        return PRESSURE.get(this);
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
            synchronized (items) {
                if (size() > capacity() /* must check again */)
                    commit(null);
            }
        }

    }

    /**
     * WARNING this is a duplicate of code in hijackbag, they ought to share this through a common Pressure class extending AtomicDouble or something
     */
    @Override
    public void depressurize(float priAmount) {
        PRESSURE.update(this, priAmount, (p, a)->Math.max(0,p-a));
    }

    @Override
    public float depressurizePct(float percentage) {
        assertUnitized(percentage);
        float m = mass();
        return Math.min(m, PRESSURE.getAndUpdate(this, (p,factor)-> Math.min(m /* limit */, p) * factor, 1-percentage)) * percentage;
    }

    @Override
    public void pressurize(float f) {
        PRESSURE.add(this, f);
    }

    /**
     * returns true unless failed to add during 'add' operation or becomes empty
     * call within synchronized
     *
     * @return List of trash items
     * trash must be removed from the map, outside of critical section
     * may include the item being added
     */
    private boolean tryInsertFull(Y toAdd, float toAddPri, @Nullable Consumer<Y> update, final FasterList<Y> trash) {

        int s = cleanIfFull() ? clean(trash, update  /*|| (s == capacity) && get(0) instanceof PLinkUntilDeleted*/) : size();

        int c = capacity();

        if (s + 1 <= c) {

            //space cleared for the new item

            int i = items.add(toAdd, this);
            assert (i >= 0);
            return true;

        } else {

            if (toAddPri > priMin()) {

                Y removed = items.removeLast();

                int i = items.add(toAdd, this);
                assert (i >= 0);

                trash.add(removed);

                return true;
            }
        }

        return false;
    }

    /**
     * allows an implementation to remove items which may have been deleted (by anything) since commit checked for them
     */
    protected boolean cleanIfFull() {
        return false;
    }

    protected void sort(int from /* inclusive */, int to /* inclusive */) {
        items.sort(ScalarValue::priComparable, from, to);
    }


    private int clean(Collection<Y> trash, @Nullable Consumer<Y> update) {

//        float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY, mass = 0;


        SortedArray<Y> items2 = this.items;
        final Object[] l = items2.array();

        float above = Float.POSITIVE_INFINITY;
        int mustSortTo = -1;
        int s = size();
        float m = 0;
        for (int i = 0; i < s; ) {
            Y y = (Y) l[i];
            assert y != null;
            float p = pri(y);
            if (update != null && p == p) {
                update.accept(y);
                p = pri(y);
            }
            if (p == p) {
//                min = Math.min(min, p);
//                max = Math.max(max, p);

                m += p;
                if (p - above >= ScalarValue.EPSILON)
                    mustSortTo = i;

                above = p;
                i++;
            } else {
                trash.add(y);
                items2.removeFast(i);
                s--;
                //dont increment i
            }
        }

        ArrayBag.MASS.set(this, m);


        int c = capacity();

        while (s > c) {
            trash.add(this.items.removeLast());
            s--;
        }


        if (mustSortTo != -1)
            sort(0, Math.min(s, mustSortTo));

        return s;
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
                        remove(y, i); //deleted, remove
                    } else {

                        SampleReaction next = each.apply(y);

                        if (next.remove)
                            remove(y, i);

                        if (next.stop)
                            return;
                    }
                }

            }

            return;
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
            return sampleNextLinear(rng, size);
            //return sampleNextBiLinear(rng, size);
        }
    }


    /** raw selection by index, with x^2 bias towards higher pri indexed items */
    private static int sampleNextLinear(Random rng, int size) {
        float targetIndex = rng.nextFloat();

        return Util.bin(targetIndex*targetIndex, size);
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
                Util.lerpSafe((max - min), targetPercentile /* flat */, (targetPercentile * targetPercentile) /* curved */);

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
            float balance = (max-med) / range; //measure of skewness or something; 0.5 = centered (median~=mean)
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
        synchronized (items) {
            removed = super.remove(x);
        }
        if (removed != null) {
            removed(removed);
        }
        return removed;
    }


    private void remove(Y y, int suspectedPosition) {
        boolean removed;
        synchronized (items) {
            if (removed = (items.get(suspectedPosition) == y)) {
                items.remove(suspectedPosition);
                removeFromMap(y);
            }
        }
        if (removed) {
            removed(y); //outside of synch, call removed
        } else {
            remove(key(y)); //wasnt found with provided index, use standard method by key
        }
    }

    @Override
    public Y put(final Y incoming, final NumberX overflow) {

        final int capacity = this.capacity();

        float p = incoming.priElseZero();

        if (capacity == 0) {
            depressurize(p, overflow);
            return null;
        }

        pressurize(p);


//        //HACK special case for saving a lot of unnecessary work when merge=Max
//        //TODO may also work with average and replace merges
//        //TODO this can only work if the bag is sorted
//        if (this.mergeFunction == PriMerge.max && fastMergeMaxReject() && isFull()) {
//            if (p < priMin()) {
//                return null; //fast drop the novel task due to insufficient priority
//                //TODO feedback the min priority necessary when capacity is reached, and reset to no minimum when capacity returns
//            }
//        }

        X key = key(incoming);


        boolean inserted;

        @Nullable FasterList<Y> trash;

        synchronized (items) {

            Y existing = map.get(key);

            if (existing != null) {
                if (existing != incoming) {
                    return merge(existing, incoming, overflow);
                } else {
                    depressurize(p, overflow);
                    return incoming; //exact same instance
                }
            } else {

                int s = size();

                if (s >= capacity) {

                    trash = new FasterList<>(0);

                    inserted = tryInsertFull(incoming, p, null, trash);

                    if (trash != null && !trash.isEmpty())
                        trash.forEach(this::removeFromMap);
                    else
                        trash = null;

                } else {
                    int i = items.add(incoming, -p, this);
                    assert i >= 0;

                    inserted = true;
                    trash = null;
                }

                if (inserted) {
                    map.put(key, incoming);
                }

            }

        }


        if (trash != null) //outside synch
            trash.forEach(this::removed);

        if (!inserted) {

            onReject(incoming);
            if (overflow != null)
                depressurize(p, overflow);
            incoming.delete();

            return null;

        } else {
            MASS.add(this, p);

            onAdd(incoming);

            return incoming;
        }

    }


//    protected boolean fastMergeMaxReject() {
//        return false;
//    }

    /**
     * will not need to be sorted after calling this; the index is automatically updated
     */
    private Y merge(Y existing, Y incoming, @Nullable NumberX overflow) {


        int posBefore = items.indexOf(existing, this);
        if (posBefore == -1) {
//            //try harder: compare by keys, even if the value refuse to respond true to equals()
//            X ki = key(incoming);
//            int s = size();
//            for (int i = 0; i < s; i++) {
//                if (ki.equals(key(items.get(i)))) {
//                    posBefore = i;
//                    break;
//                }
//            }
//            if (posBefore == -1)
            throw new RuntimeException("Bag fault: " + existing + " not found in array");
        }

        Y result;

        float priBefore = existing.pri();
        float oo = merge(existing, incoming);
        float priAfter = existing.pri();
        if (priAfter != priAfter) {
            //got deleted
            remove(key(existing));
            priAfter = 0;
            result = null;
        } else {
            result = existing;
        }

        float delta = priAfter - priBefore;


        depressurize(oo, overflow);


        if (Math.abs(delta) >= ScalarValue.EPSILON) {

            items.adjust(posBefore, this);

            MASS.add(this, delta);
        }

        incoming.delete();

        return result;
    }

    protected float merge(Y existing, Y incoming) {
        return mergeFunction.merge(existing, incoming);
    }

    private Y removeFromMap(Y y) {
        Y removed = map.remove(key(y));
        if (removed == null) {
            throw new WTF();
        }
        return removed;
    }

//    @Nullable
//    private Y tryRemoveFromMap(Y x) {
//        Y removed = map.remove(key(x));
//        return removed;
//    }

    @Override
    public Bag<X, Y> commit(Consumer<Y> update) {

        FasterList<Y> trash = new FasterList(0);

        synchronized (items) {

            clean(trash, update);

            trash.forEach(this::removeFromMap);
        }


        trash.forEach(this::removed);

        return this;
    }

    private void removed(Y y) {
        MASS.add(this, -priElse(y, 0));
        onRemove(y);
        y.delete();
    }

    @Override
    public final void clear() {
        clear(this::removed);
        MASS.zero(this);
        depressurizePct(1);
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

        popBatch(n, popped);

        if (popped != null)
            popped.forEach(each);

    }

    @Override
    public Sampler<Y> pop(Random rng, int max, Consumer<? super Y> each) {
        if (rng == null) {
            //high-efficiency non-random pop
            clear(max, each);
            return this;
        } else {
            return Bag.super.pop(rng, max, each);
        }
    }


    public Sampler<Y> popBatch(int n, Collection<Y> popped) {

        popped.clear();
        synchronized (items) {

            int s = size();
            if (s > 0) {

                int toRemove = n == -1 ? s : Math.min(s, n);

                items.removeRange(0, toRemove, (e) -> popped.add(removeFromMap(e)));

            }
        }

        return this;
    }

    @Override
    public float pri(Y key) {
        return key.pri();
    }

    @Override
    public void forEachKey(Consumer<? super X> each) {
        forEach(x -> each.accept(key(x)));
    }

    @Override
    public void forEach(Consumer<? super Y> action) {


        int s = size();
        if (s <= 0)
            return;

        //synchronized (items) {

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


































