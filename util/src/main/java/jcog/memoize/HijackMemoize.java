package jcog.memoize;

import jcog.Texts;
import jcog.data.NumberX;
import jcog.pri.PriProxy;
import jcog.pri.ScalarValue;
import jcog.pri.bag.impl.HijackBag;
import jcog.pri.bag.impl.hijack.PriHijackBag;
import org.eclipse.collections.api.block.procedure.primitive.ObjectLongProcedure;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * TODO add an instrumentation wrapper to collect statistics
 * about cache efficiency and also processing time of the calculations
 */
public class HijackMemoize<X, Y> extends AbstractMemoize<X,Y> {

    protected final MyHijackBag bag;
    private final Function<X, Y> func;
    private final boolean soft;

    protected float DEFAULT_VALUE, CACHE_HIT_BOOST, CACHE_REJECT_CUT;

    public HijackMemoize(Function<X, Y> f, int initialCapacity, int reprobes) {
        this(f, initialCapacity, reprobes, false);
    }


    public HijackMemoize(Function<X, Y> f, int initialCapacity, int reprobes, boolean soft) {
        this.soft = soft;
        this.func = f;

        bag = newBag(initialCapacity, reprobes);
        bag.resize(initialCapacity);
    }


    protected HijackMemoize<X, Y>.MyHijackBag newBag(int initialCapacity, int reprobes) {
        return new MyHijackBag(initialCapacity, reprobes);
    }

    @Override
    public final void clear() {
        bag.clear();
    }

    public float statReset(ObjectLongProcedure<String> eachStat) {

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
    public Y getIfPresent(Object k) {
        PriProxy<X, Y> exists = bag.get(k);
        if (exists != null) {
            Y e = exists.get();
            if (e != null) {
                exists.priAdd(CACHE_HIT_BOOST);
                return e;
            }
        }
        return null;
    }

    @Nullable
    public Y removeIfPresent(X x) {
        @Nullable PriProxy<X, Y> exists = bag.remove(x);
        if (exists != null) {
            return exists.get();
        }
        return null;
    }

    public PriProxy<X, Y> put(X x, Y y) {
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
    protected void onIntern(X x) {

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


    protected class MyHijackBag extends PriHijackBag<X, PriProxy<X,Y>> {
        public MyHijackBag(int cap, int reprobes) {
            super(cap, reprobes);
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
        public void setCapacity(int c) {
            super.setCapacity(c);
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
            HijackMemoize.this.DEFAULT_VALUE =
                    //0.5f / reprobes;
                    1f / sc;
            HijackMemoize.this.CACHE_HIT_BOOST = 1f/sc;
            HijackMemoize.this.CACHE_REJECT_CUT = CACHE_HIT_BOOST / reprobes;

            assert(DEFAULT_VALUE > ScalarValue.EPSILON);
            assert(CACHE_HIT_BOOST > ScalarValue.EPSILON);
            assert(CACHE_REJECT_CUT > ScalarValue.EPSILON);
        }

        @Override
        protected boolean attemptRegrowForSize(int s) {
            return false;
        }

        @Override
        public void pressurize(float f) {

        }

        @Override
        public void depressurize(float x) {

        }

        @Override
        public HijackBag<X, PriProxy<X, Y>> commit(@Nullable Consumer<PriProxy<X, Y>> update) {
            return this;
        }

        @Override
        public X key(PriProxy<X, Y> value) {
            return value.x();
        }

        @Override
        protected boolean replace(float incomingPri, PriProxy<X, Y> existing) {
            if (super.replace(incomingPri, existing)) {
                return true;
            } else {
                //remains, gradually weaken
                existing.priSub(CACHE_REJECT_CUT);
                return false;
            }
        }

        @Override
        protected boolean keyEquals(Object k, PriProxy<X, Y> p) {
            return p.x().equals(k);
        }

        @Override
        public Consumer<PriProxy<X, Y>> forget(float rate) {
            return null;
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
