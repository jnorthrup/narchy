package jcog.bag.impl;

import jcog.Util;
import jcog.bag.Bag;
import jcog.bag.Sampler;
import jcog.list.FasterList;
import jcog.list.table.SortedListTable;
import jcog.pri.Priority;
import jcog.pri.ScalarValue;
import jcog.pri.op.PriMerge;
import jcog.sort.SortedArray;
import jcog.util.AtomicFloatFieldUpdater;
import jcog.util.NumberX;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;


/**
 * A bag implemented as a combination of a Map and a SortedArrayList
 * TODO extract a version of this which will work for any Prioritized, not only BLink
 */
abstract public class ArrayBag<X, Y extends Priority> extends SortedListTable<X, Y> implements Bag<X, Y> {
    private static final AtomicFloatFieldUpdater<ArrayBag> mass =
            new AtomicFloatFieldUpdater(
                    AtomicIntegerFieldUpdater.newUpdater(ArrayBag.class, "_mass"));
    private static final AtomicFloatFieldUpdater<ArrayBag> pressure =
            new AtomicFloatFieldUpdater(
                    AtomicIntegerFieldUpdater.newUpdater(ArrayBag.class, "_pressure"));

    final PriMerge mergeFunction;

    private volatile int _mass, _pressure;

    protected ArrayBag(PriMerge mergeFunction, int capacity) {
        this(mergeFunction, new HashMap<>(capacity, 0.99f));
        setCapacity(capacity);
    }

    protected ArrayBag(PriMerge mergeFunction, Map<X, Y> map) {
        this(0, mergeFunction, map);
    }

    protected ArrayBag(@Deprecated int cap, PriMerge mergeFunction, Map<X, Y> map) {
        super(new SortedPLinks(), map);
        this.mergeFunction = mergeFunction;
        setCapacity(cap);


    }

    static boolean cmpGT(@Nullable Priority o1, float o2) {
        return (pCmp(o1) < o2);
    }

    static boolean cmpGT(float o1, float o2) {
        return (o1 < o2);
    }

    static boolean cmpLT(@Nullable Priority o1, float o2) {
        return (pCmp(o1) > o2);
    }

    /**
     * gets the scalar float value used in a comparison of BLink's
     * essentially the same as b.priIfFiniteElseNeg1 except it also includes a null test. otherwise they are interchangeable
     */
    public static float pCmp(@Nullable Priority b) {
        return (b == null) ? -2f : b.priElseNeg1();


    }

    static int sortSize(int s) {

        if (s < 16)
            return 4;
        if (s < 64)
            return 6;
        if (s < 128)
            return 8;
        if (s < 2048)
            return 16;
        else
            return 32;
    }

    /**
     * http:
     */

    public static void qsort(int[] stack, Object[] c, int left, int right) {
        int stack_pointer = -1;
        int cLenMin1 = c.length - 1;
        while (true) {
            int i, j;
            if (right - left <= 7) {

                for (j = left + 1; j <= right; j++) {
                    Priority swap = (Priority) c[j];
                    i = j - 1;
                    float swapV = pCmp(swap);
                    while (i >= left && cmpGT((Priority) c[i], swapV)) {
                        swap(c, i + 1, i--);
                    }
                    c[i + 1] = swap;
                }
                if (stack_pointer != -1) {
                    right = stack[stack_pointer--];
                    left = stack[stack_pointer--];
                } else {
                    break;
                }
            } else {

                int median = (left + right) / 2;
                i = left + 1;
                j = right;

                swap(c, i, median);

                float cl = pCmp((Priority) c[left]);
                float cr = pCmp((Priority) c[right]);
                if (cmpGT(cl, cr)) {
                    swap(c, right, left);
                    float x = cr;
                    cr = cl;
                    cl = x;
                }
                float ci = pCmp((Priority) c[i]);
                if (cmpGT(ci, cr)) {
                    swap(c, right, i);
                    ci = cr;
                }
                if (cmpGT(cl, ci)) {
                    swap(c, i, left);

                }

                Priority temp = (Priority) c[i];
                float tempV = pCmp(temp);

                while (true) {
                    while (i < cLenMin1 && cmpLT((Priority) c[++i], tempV)) ;
                    while (j > 0 && /* <- that added */ cmpGT((Priority) c[--j], tempV)) ;
                    if (j < i) {
                        break;
                    }
                    swap(c, j, i);
                }

                c[left + 1] = c[j];
                c[j] = temp;

                int a, b;
                if ((right - i + 1) >= (j - left)) {
                    a = i;
                    b = right;
                    right = j - 1;
                } else {
                    a = left;
                    b = j - 1;
                    left = i;
                }

                stack[++stack_pointer] = a;
                stack[++stack_pointer] = b;
            }
        }
    }

    static void swap(Object[] c, int x, int y) {
        Object swap = c[y];
        c[y] = c[x];
        c[x] = swap;
    }

    @Override
    public float mass() {
        return mass.get(_mass);
    }

    @Override
    public final float floatValueOf(Y y) {
        return -pCmp(y);
    }

    @Override
    public Stream<Y> stream() {
        int s = size();
        Object[] x = items.array();
        return IntStream.range(0, s).mapToObj(i -> (Y) x[i]).filter(y -> y != null && !y.isDeleted());
    }

    /**
     * returns whether the capacity has changed
     */
    @Override
    public final void setCapacity(int newCapacity) {
        if (newCapacity != this.capacity) {
            this.capacity = newCapacity;

            commit(null);


        }

    }

    /**
     * WARNING this is a duplicate of code in hijackbag, they ought to share this through a common Pressure class extending AtomicDouble or something
     */
    @Override
    public float depressurize() {
        return Util.max(0, pressure.getAndZero(this));
    }

    @Override
    public void pressurize(float f) {
        pressure.add(this, f);
    }

    /**
     * returns true unless failed to add during 'add' operation or becomes empty
     * call within synchronized
     *
     * @return List of trash items
     * trash must be removed from the map, outside of critical section
     * may include the item being added
     */
    @Nullable
    protected void update(@Nullable Y toAdd, @Nullable Consumer<Y> update, boolean commit, final FasterList<Y> trash) {

        int s = size();
        if (s == 0) {
            mass.zero(this);
            if (toAdd == null)
                return;
        } else {
            s = update(toAdd != null, s, trash, update,
                    commit /*|| (s == capacity) && get(0) instanceof PLinkUntilDeleted*/);
        }


        if (toAdd != null) {
            int c = capacity();
            float toAddPri = toAdd.priElseZero();
            if (s < c) {

                items.add(toAdd, this);
                mass.add(this, toAddPri);
            } else {

                Y removed;
                if (size() > 0) {
                    if (toAddPri > priMin()) {

                        assert (size() == s);


                        removed = items.removeLast();
                        float massDelta = -removed.priElseZero();

                        items.add(toAdd, this);
                        massDelta += toAddPri;

                        mass.add(this, massDelta);

                    } else {
                        removed = toAdd;
                    }

                    trash.add(removed);
                }
            }
        }

    }

    protected void sort(int from /* inclusive */, int to /* inclusive */) {
        Object[] il = items.items;

        int[] stack = new int[sortSize(to - from) /* estimate */];
        qsort(stack, il, from /*dirtyStart - 1*/, to);


    }

    @Override
    public final float priUpdate(Y key) {
        return key.priCommit();
    }

    private int update(@Deprecated boolean toAdd, int s, List<Y> trash, @Nullable Consumer<Y> update, boolean commit) {

        float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY, mass = 0;


        SortedArray items2 = this.items;
        final Object[] l = items2.array();

        float above = Float.POSITIVE_INFINITY;
        int mustSort = -1;
        for (int i = 0; i < s; i++) {
            Y x = (Y) l[i];
            assert (x != null);
            float p = (commit ? priUpdate(x) : pri(x));
            if (update != null && p == p) {
                update.accept(x);
                p = pri(x);
            }
            if (p == p) {
                min = Util.min(min, p);
                max = Util.max(max, p);
                mass += p;
                if (p - above >= ScalarValue.EPSILON)
                    mustSort = i;

                above = p;
            } else {
                trash.add(x);
                items2.removeFast(i);
                s--;
            }
        }


        final int c = capacity;
        if (s > c) {


            SortedArray<Y> items1 = this.items;
            while (s > 0 && ((s - c) + (toAdd ? 1 : 0)) > 0) {
                Y w1 = items1.removeLast();
                mass -= w1.priElseZero();
                trash.add(w1);
                s--;
            }
        }


        ArrayBag.mass.set(this, mass);

        if (mustSort != -1)
            sort(0, mustSort);

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

        newItemsArray:
        while (true) {
            Object[] ii;
            int s;
            int i = -1;
            while ((s = Math.min((ii = items.array()).length, size())) > 0) {

                if (i < 0) {
                    i = sampleStart(rng, s);
                } else {
                    i = sampleNext(rng, s, i);
                }

                Object x = ii[i];

                if (x != null) {
                    Y y = (Y) x;
                    float yp = priUpdate(y);
                    if (yp != yp) {
                        remove(key(y)); //deleted, remove
                    } else {

                        SampleReaction next = each.apply(y);

                        if (next.remove)
                            remove(key(y));

                        if (next.stop)
                            return;
                    }
                }

            }

            return;
        }

    }

    /**
     * size > 0
     */
    protected int sampleStart(@Nullable Random rng, int size) {
        return 0;
    }

    protected int sampleNext(Random rng, int size, int i) {
        if (++i >= size)
            i = 0;

        return i;
    }

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

    @Override
    public Y put(final Y incoming, final NumberX overflow) {

        final int capacity = this.capacity;

        if (capacity == 0) {
            incoming.delete();
            return null;
        }

        float p = incoming.priElseZero();
        pressurize(p);


        X key = key(incoming);


        Y inserted;

        @Nullable FasterList<Y> trash = null;

        synchronized (items) {

            Y existing = getExisting(key);

            if (existing != null) {
                if (existing != incoming) {
                    return merge(existing, incoming, overflow);
                } else {
                    if (overflow != null)
                        overflow.add(p);
                    return incoming; //exact same instance
                }
            } /* else { ...*/

            trash = new FasterList<>(4);

            if (insert(incoming, trash)) {
                map.put(key, inserted = incoming);
            } else {
                inserted = null; //rejected
            }


            /*
            inserted = map.compute(key, (kk, existing) -> {
                Y v;
                if (existing != null) {
                    if (existing != incoming) {
                        v = merge(existing, incoming, overflow);
                    } else {
                        if (overflow != null)
                            overflow.add(p);
                        v = existing;
                    }
                } else {
                    if (insert(incoming, trash)) {
                        v = incoming;
                    } else {
                        v = null;
                    }
                }
                return v;
            });
            */


            trash.removeIf(x -> {
                if (x != incoming) {
                    mapRemove(x);
                    return false; //keep
                }
                return true; //exclude from trash
            });

        }


        trash.forEach(this::removed);

        if (inserted == null) {
            incoming.delete();
            onReject(incoming);
        } else if (inserted == incoming) {
            onAdd(inserted);
        } else {

        }

        return inserted;


    }

    protected Y getExisting(X key) {
        return map.get(key);
    }

    private boolean insert(Y incoming, FasterList<Y> trash) {


        if (size() == capacity) {

            update(incoming, null, false, trash);
            return !trash.remove(incoming);


        } else {
            float p = pri(incoming);
            int i = items.add(incoming, -p, this);

            assert (i >= 0);
            mass.add(this, p);
        }

        return true;
    }

    /**
     * will not need to be sorted after calling this; the index is automatically updated
     */
    private Y merge(Y existing, Y incoming, @Nullable NumberX overflow) {


        int posBefore = items.indexOf(existing, this);
        if (posBefore == -1) {
            throw new RuntimeException("Bag Map and List became unsynchronized: " + existing + " not found");
        }

        float priBefore = existing.priCommit();
        Y result;
        float delta;
        if (priBefore != priBefore) {

            items.array()[posBefore] = incoming;
            result = incoming;
            delta = incoming.priElseZero();
        } else {
            float oo = merge(existing, incoming);
            delta = existing.priElseZero() - priBefore;
            if (overflow != null)
                overflow.add(oo);
            result = existing;
        }


        if (Math.abs(delta) >= ScalarValue.EPSILON) {
            items.adjust(posBefore, this);

            mass.add(this, delta);
        }

        incoming.delete();

        return result;
    }

    protected float merge(Y existing, Y incoming) {
        return mergeFunction.merge(existing, incoming);
    }

    private Y mapRemove(Y x) {
        return map.remove(key(x));
    }

    @Override

    public Bag<X, Y> commit(Consumer<Y> update) {


        int s = size();
        if ((update != null && s > 0) || (update == null && (s > capacity))) {
            @Nullable FasterList<Y> trash = new FasterList(Math.max(s / 8, 4));
            synchronized (items) {

                update(null, update, true, trash);

                trash.forEach(this::mapRemove);
            }


            trash.forEach(this::removed);
        }

        return this;
    }

    protected void removed(Y y) {
        onRemove(y);
        y.delete();
    }

    @Override
    public final void clear() {
        clear(-1, this::removed);
    }

    /**
     * removes the top n items
     * @param n # to remove, if -1 then all are removed
     */
    public final void clear(int n, Consumer<? super Y> each) {
        List<Y> trash;

        synchronized (items) {

            int s = size();
            if (s > 0) {
                int toRemove = n==-1 ? s : Math.min(s, n);
                trash = new FasterList<>(toRemove);

                items.removeRange(0, toRemove, x -> trash.add(mapRemove(x)));

            } else {
                trash = null;
            }
        }

        depressurize();

        if (trash != null)
            trash.forEach(each);

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
        if (s > 0) {
            Object[] x = items.array();
            for (int i = 0; i < Math.min(x.length, s); i++) {
                Object a = x[i];
                if (a != null) {
                    Y b = (Y) a;
                    float p = pri(b);
                    if (p == p) {
                        action.accept(b);
                    }
                }
            }
        }


    }


    @Override
    public String toString() {
        return super.toString() + '{' + items.getClass().getSimpleName() + '}';
    }

    @Override
    public float priMax() {
        Y x = items.first();
        return x != null ? pri(x) : 0;
    }

    @Override
    public float priMin() {
        Y x = items.last();
        return x != null ? pri(x) : 0;
    }

    static final class SortedPLinks extends SortedArray {
        @Override
        protected Object[] newArray(int oldSize) {
            return new Object[oldSize == 0 ? 2 : oldSize + (Math.max(1, oldSize / 2))];
        }
    }


}


































