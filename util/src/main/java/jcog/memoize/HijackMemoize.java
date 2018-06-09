package jcog.memoize;

import jcog.Texts;
import jcog.Util;
import jcog.bag.impl.HijackBag;
import jcog.bag.impl.hijack.PriorityHijackBag;
import jcog.pri.PLink;
import jcog.pri.Prioritized;
import jcog.pri.Priority;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.eclipse.collections.api.block.procedure.primitive.ObjectLongProcedure;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.SoftReference;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static jcog.Texts.n4;

/**
 * TODO add an instrumentation wrapper to collect statistics
 * about cache efficiency and also processing time of the calculations
 */
public class HijackMemoize<X, Y> extends PriorityHijackBag<X, HijackMemoize.Computation<X, Y>> implements Memoize<X, Y> {

    final Function<X, Y> func;
    final AtomicLong
            hit = new AtomicLong(),
            miss = new AtomicLong(),
            reject = new AtomicLong(),
            evict = new AtomicLong();
    private final boolean soft;
    protected float DEFAULT_VALUE;
    float CACHE_HIT_BOOST;

    public HijackMemoize(Function<X, Y> f, int initialCapacity, int reprobes) {
        this(f, initialCapacity, reprobes, true);
    }


    public HijackMemoize(Function<X, Y> f, int initialCapacity, int reprobes, boolean soft) {
        super(initialCapacity, reprobes);
        resize(initialCapacity);
        this.soft = soft;
        this.func = f;
        this.DEFAULT_VALUE = 0.5f / reprobes;
    }

    @Override
    protected Computation<X, Y> merge(Computation<X, Y> existing, Computation<X, Y> incoming, @Nullable MutableFloat overflowing) {
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
    public float value(X x) {
        return DEFAULT_VALUE;

    }

    @Override
    public void setCapacity(int i) {
        super.setCapacity(i);

        float boost = i > 0 ?
                (float) (1f / Math.sqrt(capacity()))
                : 0;


        float cut = boost / (reprobes / 2f);

        assert (cut > Prioritized.EPSILON);

        this.CACHE_HIT_BOOST = boost;


    }

    @Override
    protected boolean attemptRegrowForSize(int s) {
        return false;
    }

    @Override
    public void pressurize(float f) {

    }

    @Override
    public float depressurize() {
        return 0f;
    }

    @Override
    public HijackBag<X, Computation<X, Y>> commit(@Nullable Consumer<Computation<X, Y>> update) {
        return this;
    }

    @Nullable
    public Y getIfPresent(Object k) {
        Computation<X, Y> exists = get(k);
        if (exists != null) {
            Y e = exists.get();
            if (e != null) {
                exists.priAdd(CACHE_HIT_BOOST);
                hit.incrementAndGet();
                return e;
            }
        }
        return null;
    }

    @Nullable
    public Y removeIfPresent(X x) {
        @Nullable Computation<X, Y> exists = remove(x);
        if (exists != null) {
            return exists.get();
        }
        return null;
    }

    @Override
    @Nullable
    public Y apply(X x) {
        Y y = getIfPresent(x);
        if (y == null) {
            y = func.apply(x);
            Computation<X, Y> input = computation(x, y);
            Computation<X, Y> output = put(input);
            boolean interned = output == input;
            (interned ? miss : reject).incrementAndGet();
            if (interned) {
                onIntern(x);
            }
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
    public Computation<X, Y> computation(X x, Y y) {
        float vx = value(x);
        return soft ?
                new SoftPair<>(x, y, vx) :
                new StrongPair<>(x, y, vx);
    }

    @Override
    public X key(Computation<X, Y> value) {
        return value.x();
    }

    @Override
    protected boolean keyEquals(Object k, Computation<X, Y> p) {
        return p.x().equals(k);
    }

    @Override
    public Consumer<Computation<X, Y>> forget(float rate) {
        return null;
    }

    @Override
    public void onRemove(HijackMemoize.Computation<X, Y> value) {
        value.delete();
        evict.incrementAndGet();
    }

    /**
     * clears the statistics
     */
    @Override
    public String summary() {
        StringBuilder sb = new StringBuilder(64);
        sb.append(" N=").append(size()).append(' ');
        float rate = statReset((k, v) -> {
            sb.append(k).append('=').append(v).append(' ');
        });
        sb.setLength(sb.length() - 1);
        sb.append(" D=").append(Texts.n2percent(density()));
        sb.insert(0, Texts.n2percent(rate));
        return sb.toString();
    }

    public interface Computation<X, Y> extends Priority, Supplier<Y> {
        /**
         * 'x', the parameter to the function
         */
        X x();
    }

    public final static class StrongPair<X, Y> extends PLink<Y> implements Computation<X, Y> {

        public final X x;
        private final int hash;

        public StrongPair(X x, Y y, float pri) {
            super(y, pri);
            this.x = x;
            this.hash = x.hashCode();
        }


        @Override
        public boolean equals(Object obj) {
            StrongPair h = (StrongPair) obj;
            return hash == h.hash && x.equals(h.x);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public X x() {
            return x;
        }
    }

    final static class SoftPair<X, Y> extends SoftReference<Y> implements Computation<X, Y> {

        public final X x;
        private final int hash;
        private volatile float pri;

        public SoftPair(X x, Y y, float pri) {
            super(y);
            this.x = x;
            this.hash = x.hashCode();
            this.pri = pri;
        }

        @Override
        public final X x() {
            return x;
        }

        @Override
        public boolean equals(Object obj) {
            SoftPair h = (SoftPair) obj;
            return hash == h.hash && x.equals(h.x);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public float priSet(float p) {
            float r = this.pri;
            if (r != r)
                return Float.NaN;

            return this.pri = Util.clamp(p, 0, 1);
        }

        @Override
        public String toString() {
            return '$' + n4(pri) + ' ' + get();
        }

        @Override
        public boolean delete() {
            clear();
            this.pri = Float.NaN;
            return true;
        }

        @Override
        public Y get() {
            Y y = super.get();
            if (y == null) {
                this.pri = Float.NaN;
                return null;
            }
            return y;
        }


        @Override
        public float pri() {
            return pri;


        }

        @Override
        public boolean isDeleted() {
            float p = pri;
            return p != p;
        }

    }


}
