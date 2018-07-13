package jcog.pri.bag.impl;

import jcog.TODO;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.math.LightObjectFloatPair;
import jcog.pri.PriMap;
import jcog.pri.ScalarValue;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.predicate.primitive.ObjectFloatPredicate;
import org.eclipse.collections.api.tuple.primitive.ObjectFloatPair;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * lighter-weight 2nd-generation arraybag
 * https:
 * http:
 */
public class Baggie<X> extends PriMap<X> {
    public int capacity;

    /**
     * holds the hash indices of the items in sorted order
     */
    public int[] sorted = ArrayUtils.EMPTY_INT_ARRAY;

    public final ShortShortToShortFunction merge = (x, y) -> clamp(x + y);
    short min, max;
    int pressure = 0;
    int mass = 0;

    public Baggie(int capacity) {
        super(0);
        setCapacity(capacity);
    }

    public boolean put(X x, float pri) {
        return put(x, shortPri(pri));
    }

    public boolean put(@NotNull X x, short pri) {
        assert (pri >= 0);


        short from, to;

        final List[] trash = {null};

        synchronized (this) {
            boolean full = isFull();
            if (full) {
                
                if (pri < min && !containsKey(x)) {
                    pressure += pri;
                    return false; 
                }
            }

            int ch = update(x, pri, merge, full ? () -> {
                trash[0] = removeLowest(1);
            } : null);
            from = Util.short2Int(ch, true);
            to = Util.short2Int(ch, false);
            if (from != to) {
                if (from>0 && to>0)
                    pressure += to - from;
                update(from, to); 
            } else {
                return true; 
            }
        }

        

        if (trash[0] != null) {
            ((List<X>) trash[0]).forEach(this::onRemoved);
        }

        if (from == -1) {
            onAdded(x);
        }

        return true;
    }

    @Override
    public void clear() {
        synchronized (this) {
            super.clear();
            this.sorted = ArrayUtils.EMPTY_INT_ARRAY;
            this.min = this.max = -1;
        }
    }

    protected List<X> removeLowest(int toRemove) {
        assert (toRemove > 0);

        List<X> trash = new FasterList<>(toRemove);
        for (int i = 0; i < toRemove; i++) {
            int lowest = sorted[sorted.length - 1 - i];
            Object k = keys[lowest];
            assert (k != REMOVED_KEY);
            trash.add((X) k);
        }

        
        trash.forEach(this::removeKey);

        return trash;
    }

    /**
     * from and to are the range of values that would have changed, so that a partial sort can be isolated to the sub-range of the list that has changed
     * returns trashed items, if any, or null if none
     */
    protected void update(short from, short to) {
        assert (size > 0);

        
        reBuildSort(from, to);




    }

    private void reBuildSort(short from, short to) {

        int slen = size; 
        if (slen == 0) {
            clear();
            return;
        }

        if (sorted.length != slen) {
            this.sorted = new int[slen];
        } else {
        }

        int[] s = this.sorted;

        int i = 0;
        for (int index = 0, keysLength = (short) keys.length; index < keysLength; index++) {
            Object o = keys[index];
            if (isNonSentinel(o))
                s[i++] = index;
        }
        assert (i == size);

        reSort(from, to);
    }

    /**
     * TODO partial sort the affected range
     */
    private void reSort(short from, short to) {
        int[] s = this.sorted;
        ArrayUtils.sort(s, 0, s.length - 1, (int x) -> values[x]); 
        this.max = values[s[0]];
        this.min = values[s[s.length - 1]];
    }

    public X lowest() {
        synchronized (this) {
            int i = lowestIndex();
            return i < 0 ? null : (X) keys[i];
        }
    }

    public X highest() {
        synchronized (this) {
            int i = highestIndex();
            return i < 0 ? null : (X) keys[i];
        }
    }

    private int lowestIndex() {
        int s = this.size;
        return s > 0 ? sorted[s - 1] : -1;
    }

    private int highestIndex() {
        return size > 0 ? sorted[0] : -1;
    }

    @Override
    public void remove(Object key) {
        removeIt((X) key);
    }

    public boolean removeIt(@NotNull X key) {
        synchronized (this) {
            boolean x = removeKey(key);
            if (x) {
                reBuildSort((short) -1, (short) -1);
            }
            return x;
        }
    }

    public void onAdded(X x) {

    }

    public void onRemoved(X x) {

    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public boolean isFull() {
        return size() == capacity;
    }

    public boolean contains(X b) {
        return containsKey(b);
    }

    public boolean forEach(ObjectFloatPredicate<X> each) {
        synchronized (this) {
            int[] sorted = this.sorted;
            for (int i = 0; i < size; i++) {
                int ii = sorted[i];
                short v = values[ii];
                if (v >= 0) {
                    if (!each.accept((X) keys[ii], priShort(v)))
                        return false;
                }
            }
        }
        return true;
    }

    public List<ObjectFloatPair<X>> toList() {
        synchronized (this) {
            
            return streamDirect().collect(Collectors.toList());
        }
    }

    /**
     * use with caution, only for non-concurrent situations
     */
    public Stream<LightObjectFloatPair<X>> streamDirect() {
        return IntStream.range(0, size).mapToObj((int i) -> {
            int ii = sorted[i];
            if (ii >= 0)
                return new LightObjectFloatPair<>((X) keys[ii], priShort(values[ii]));
            else
                return null;
        }).filter(Objects::nonNull);
    }

    /**
     * creates a copy for concurrency purposes
     */
    public Stream<ObjectFloatPair<X>> stream() {
        return toList().stream();
    }


    /**
     * iterate all elements while each returns true, applying changed values afterward and batch sorting at the end
     */
    public void commit(Random rng, Predicate<LightObjectFloatPair<X>> each) {
        throw new TODO();
    }

    public short forgetShare(float rate) {
        synchronized (this) {
            if (pressure > 0 && size > 0) {
                short toForget = clamp(Math.round(rate * pressure / capacity) );
                pressure = Math.max(0, pressure - toForget);
                return toForget;
            } else {
                return 0;
            }
        }
    }

    public void depressurize() {
        pressure = 0;
    }

    public void sample(Random rng, Predicate<ChangeAwareLightObjectFloatPair<X>> each) {
        ChangeAwareLightObjectFloatPair<X> l = new ChangeAwareLightObjectFloatPair<>();

        while (size > 0) {
            X x;
            synchronized (this) {

                assert (sorted.length == size) : "sorted=" + sorted.length + " but size=" + size;

                int i = sorted[sample(rng)];
                x = (X) keys[i];
                assert (x != null);
                short v = values[i];
                assert (v >= 0);
                l.set(x, v);
            }

            boolean done = !each.test(l);

            if (l.nextPri != l.pri) {
                boolean removed = false;
                synchronized (this) {
                    if (l.nextPri < 0) {
                        
                        if (removeIt(x)) {
                            removed = true;
                        }
                    } else {
                        if (containsKey(x)) {
                            
                            set(x, l.nextPri);
                            reSort(l.pri, l.nextPri);
                        }
                        
                    }
                }

                if (removed) { 
                    onRemoved(x);
                }

            }

            if (done)
                break;
        }
    }

    protected int sample(Random random) {
        int size = this.size;
        if (size == 1 || random == null)
            return 0;
        else {
            float min = priShort(this.min);
            float max = priShort(this.max);
            float diff = max - min;
            if (diff > ScalarValue.EPSILON * size) {
                float i = random.nextFloat(); 
                
                i = Util.lerp(diff, i /* flat */, (i * i) /* curved */);
                int j = (int) Math.floor(i * (size - 0.5f));
                if (j >= size) j = size - 1;
                if (j < 0) j = 0;
                return j;
            } else {
                return random.nextInt(size);
            }
        }
    }

    public float priMax() {
        return priShort(max);
    }

    public float priMin() {
        return priShort(min);
    }

    public class ChangeAwareLightObjectFloatPair<X>  {

        X the;
        short nextPri, pri;

        /**
         * called by this before iteration
         */
        public void set(X x, short v) {
            this.the = x;
            this.nextPri = pri = v;
        }

        public float pri() {
            return priShort(this.pri);
        }

        /**
         * called by callee during iteration
         */
        public void set(float v) {
            this.nextPri = shortPri(v);
        }

        public void forget(float rate) {
            this.nextPri = clamp(Math.max(0, pri - forgetShare(rate)));
        }

        public X get() {
            return the;
        }
    }
}
