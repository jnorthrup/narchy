package jcog.memoize;

import jcog.Texts;
import jcog.data.NumberX;
import jcog.pri.PriProxy;
import jcog.pri.ScalarValue;
import jcog.pri.bag.Bag;
import jcog.pri.bag.impl.hijack.PriHijackBag;
import org.eclipse.collections.api.block.procedure.primitive.ObjectLongProcedure;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 *
 * a wrapper of HijackBag
 * adds a condition to decrease the priority of a cell which does not get replaced.  this gradually erodes the priority of existing cells and the rate this occurrs determines the aggregate lifespan of entries in the cache.  they can also be prioritized differently on insert, so in a memoization situation it can be helpful to increase the priority of a more expensive work item so it is more likely to replace a less expensive or less used existing entry.
 *
 * TODO add an instrumentation wrapper to collect statistics
 * about cache efficiency and also processing time of the calculations
 *
 *
 */
public class HijackMemoize<X, Y> extends AbstractMemoize<X,Y> {

    static final boolean ALLOW_DUPLICATES = true;

    protected final MemoizeHijackBag bag;
    private final Function<X, Y> func;
    private final boolean soft;

    protected float DEFAULT_VALUE, CACHE_HIT_BOOST, CACHE_SURVIVE_COST;

    public HijackMemoize(Function<X, Y> f, int initialCapacity, int reprobes) {
        this(f, initialCapacity, reprobes, false);
    }


    public HijackMemoize(Function<X, Y> f, int initialCapacity, int reprobes, boolean soft) {
        this.soft = soft;
        this.func = f;

        bag = new MemoizeHijackBag(initialCapacity, reprobes);
        bag.resize(initialCapacity);
    }


    @Override
    public final void clear() {
        bag.clear();
    }

    private float statReset(ObjectLongProcedure<String> eachStat) {

        long H, M, R, E;
        eachStat.accept("H" /* hit */, H = hit.getAndSet(0));
        eachStat.accept("M" /* miss */, M = miss.getAndSet(0));
        eachStat.accept("R" /* reject */, R = reject.getAndSet(0));
        eachStat.accept("E" /* evict */, E = evict.getAndSet(0));
        return (H / ((float) (H + M + R /* + E */)));
    }

    /**
     * estimates the value of computing the input.
     * easier/frequent items will introduce lower priority, allowing
     * harder/infrequent items to sustain longer
     */
    public float value(X x, Y y) {
        return DEFAULT_VALUE;
    }

    @Nullable
    public final Y getIfPresent(Object k) {
        PriProxy<X, Y> exists = bag.get(k);
        if (exists != null) {
            Y e = exists.get();
            if (e != null) {
                boost(exists);
                return e;
            } else
                throw new NullPointerException();
        }
        return null;
    }

    /** gain of priority on cache hit */
    protected void boost(PriProxy<X, Y> p) {
        p.priAdd(CACHE_HIT_BOOST);
    }

    protected void cut(PriProxy<X, Y> p) {
        p.priSub(CACHE_SURVIVE_COST);
    }



    @Nullable
    public Y removeIfPresent(X x) {
        @Nullable PriProxy<X, Y> exists = bag.remove(x);
        return exists != null ? exists.get() : null;
    }

    protected PriProxy<X, Y> put(X x, Y y) {
        return bag.put(computation(x, y));
    }

    @Override
    @Nullable
    public Y apply(X x) {
        Y y = getIfPresent(x);
        if (y == null) {
            y = func.apply(x);
            PriProxy<X, Y> input = computation(x, y);
            PriProxy<X, Y> output = bag.put(input);
            boolean interned = (output == input);
            if (interned) {
                miss.getAndIncrement();
                onIntern(x);
            } else {
                if (output!=null) {
                    //result obtained before inserting ours, use that it is more likely to be shared
                    y = output.get();
                    miss.getAndIncrement(); //technically, this is a combination of a hit and a miss
                } else {
                    reject.getAndIncrement();
                }
            }
        } else {
            hit.getAndIncrement();
        }
        return y;
    }


    /**
     * can be overridden in implementations to compact or otherwise react to the interning of an input key
     */
    private void onIntern(X x) {

    }

    /**
     * produces the memoized computation instance for insertion into the bag.
     * here it can choose the implementation to use: strong, soft, weak, etc..
     */
    public PriProxy<X, Y> computation(X x, Y y) {
        float pri = value(x,y);
        return soft ?
                new PriProxy.SoftProxy<>(x, y, pri) :
                new PriProxy.StrongProxy<>(x, y, pri);
    }

    /**
     * clears the statistics
     */
    @Override
    public String summary() {
        StringBuilder sb = new StringBuilder(64);
        sb.append(" N=").append(bag.size()).append(' ');
        float rate = statReset((k, v) -> {
            sb.append(k).append('=').append(v).append(' ');
        });
        sb.setLength(sb.length() - 1);
        sb.append(" D=").append(Texts.n2percent(bag.density()));
        sb.insert(0, Texts.n2percent(rate));
        return sb.toString();
    }

    public Iterator<PriProxy<X, Y>> iterator() {
        return bag.iterator();
    }


    protected class MemoizeHijackBag extends PriHijackBag<X, PriProxy<X,Y>> {

        MemoizeHijackBag(int cap, int reprobes) {
            super(cap, reprobes);
        }

//        @Override
//        protected boolean optimisticPut() {
//            return true;
//        }

        @Override
        protected boolean allowDuplicates() {
            return ALLOW_DUPLICATES;
        }

        @Override
        protected PriProxy<X, Y> merge(PriProxy<X, Y> existing, PriProxy<X, Y> incoming, NumberX overflowing) {
            if (existing.isDeleted())
                return incoming;
            return super.merge(existing, incoming, overflowing);
        }

        @Override
        protected void resize(int newSpace) {
            if (space() > newSpace)
                return;

            super.resize(newSpace);
        }

        @Override
        protected void onCapacityChange(int oldCap, int c) {

//
//            float boost = i > 0 ?
//                    (float) (1f / Math.sqrt(capacity()))
//                    : 0;
//
//
//            float cut = boost / (reprobes / 2f);
//
//            assert (cut > ScalarValue.EPSILON);
//            HijackMemoize.this.DEFAULT_VALUE = 0.5f / reprobes;
//            HijackMemoize.this.CACHE_HIT_BOOST = boost;

            float sc = (float) Math.sqrt(c);
            DEFAULT_VALUE =
                    //0.5f / reprobes;
                    1f / sc;
            CACHE_HIT_BOOST = 0.5f/sc;
            CACHE_SURVIVE_COST = CACHE_HIT_BOOST / reprobes;

            assert(DEFAULT_VALUE > ScalarValue.EPSILON);
            assert(CACHE_HIT_BOOST > ScalarValue.EPSILON);
            assert(CACHE_SURVIVE_COST > ScalarValue.EPSILON);
        }

        @Override
        public int spaceMin() {
            return capacity();
        }

        @Override
        protected boolean reshrink(int length) {
            return false; //maintain capacity
        }

        @Override
        protected boolean regrowForSize(int s, int c) {
            return false;
        }

        @Override
        public void pressurize(float f) {

        }

        @Override
        public void depressurize(float toRemove) {

        }

        @Override
        public float depressurizePct(float percentToRemove) {
            return 0;
        }

        @Override
        public Bag<X, PriProxy<X,Y>> commit(@Nullable Consumer<PriProxy<X, Y>> update) {
            return this;
        }

        @Override
        public X key(PriProxy<X, Y> value) {
            return value.x();
        }

        @Override
        protected boolean replace(float incomingPri, PriProxy<X, Y> existingValue, float existingPri) {
            if (super.replace(incomingPri, existingValue, existingPri)) {
                return true;
            } else {
                //remains, gradually weaken
                cut(existingValue);
                return false;
            }
        }

        /** loss of priority if survives */
        protected void cut(PriProxy<X,Y> p) {
            p.priSub(CACHE_SURVIVE_COST);
        }

        @Override
        protected boolean keyEquals(Object k, int kHash, PriProxy<X, Y> p) {
            return p.xEquals(k, kHash);
        }

        @Override
        public Consumer<PriProxy<X, Y>> forget(float rate) {
            /* TODO */ return null;
        }

        @Override
        public void onRemove(PriProxy<X, Y> value) {
            removed(value);
            value.delete();
            evict.getAndIncrement();
        }
    }

    /** subclasses can implement removal handler here */
    protected void removed(PriProxy<X,Y> value) {

    }
}
