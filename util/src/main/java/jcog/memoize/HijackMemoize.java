package jcog.memoize;

import jcog.Texts;
import jcog.bag.impl.HijackBag;
import jcog.bag.impl.hijack.PriorityHijackBag;
import jcog.data.MwCounter;
import jcog.math.random.XorShift128PlusRandom;
import jcog.pri.PLink;
import jcog.pri.Prioritized;
import jcog.pri.Priority;
import org.eclipse.collections.api.block.procedure.primitive.ObjectLongProcedure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.SoftReference;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static jcog.Texts.n4;

/**
 * TODO add an instrumentation wrapper to collect statistics
 * about cache efficiency and also processing time of the calculations
 */
public class HijackMemoize<X, Y> extends PriorityHijackBag<X, HijackMemoize.Computation<X, Y>> implements Memoize<X, Y> {

    private final Random rng = new XorShift128PlusRandom();

    public interface Computation<X, Y> extends Priority, Supplier<Y> {
        /** 'x', the parameter to the function */
        X x();
    }

    public static class StrongPair<X, Y> extends PLink<Y> implements Computation<X,Y> {

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

    public static class SoftPair<X, Y> extends SoftReference<Y>
            ///*WeakReference*/SoftReference<V>
            implements Computation<X, Y> {

        public final X x;
        private final int hash;
        private float pri;

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
//            if (get()!=null)
            float r = this.pri;
            if (r != r)
                return Float.NaN;

            return this.pri = p;
//            else
//                return Float.NaN;
        }


        @Override
        public @Nullable Priority clonePri() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return '$' + n4(pri) + ' ' + x;
        }

        @Override
        public boolean delete() {
            super.clear();
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
            float p = pri;
            if (p==p) {
                Y x = get();
                if (x != null)
                    return pri;
            }
            return Float.NaN;
        }

        @Override
        public boolean isDeleted() {
            float p = pri();
            return p != p;
        }

    }

    float CACHE_HIT_BOOST;
    float CACHE_DENY_DAMAGE; //damage taken by a cell in rejecting an attempted hijack

    final Function<X, Y> func;

    final MwCounter
            hit = new MwCounter(),  //existing item retrieved
            miss = new MwCounter(),  //a new item inserted that has not existed
            reject = new MwCounter(), //item prevented from insertion by existing items
            evict = new MwCounter(); //removal of existing item on insertion of new item


    //hit + miss + reject = total insertions


    public HijackMemoize(Function<X, Y> f, int initialCapacity, int reprobes) {
        super(initialCapacity, reprobes);
        resize(initialCapacity);
        this.func = f;
    }

    @Override
    protected void resize(int newSpace) {
        if (space() > newSpace)
            return; //dont shrink

        super.resize(newSpace);
    }

    public float statReset(ObjectLongProcedure<String> eachStat) {
        //eachStat.accept("S" /* size */, size() );
        long H, M, R, E;
        eachStat.accept("H" /* hit */, H = hit.getThenZero());
        eachStat.accept("M" /* miss */, M = miss.getThenZero());
        eachStat.accept("R" /* reject */, R = reject.getThenZero());
        eachStat.accept("E" /* evict */, E = evict.getThenZero());
        return (H / ((float) (H + M + R /* + E */)));
    }

    /**
     * estimates the value of computing the input.
     * easier items will introduce lower priority, allowing
     * harder items to sustain longer
     */
    public float value(X x) {
        return 0.5f;
        //return reprobes * 2 * CACHE_HIT_BOOST;
    }

    @Override
    public void setCapacity(int i) {
        super.setCapacity(i);

        float boost = i > 0 ?
                //0.02f
                (float) (1f / Math.sqrt(capacity()))
                : 0;

        //note: cut should probably be some factor less than 1/reprobes
        // for example, 1/(N*reprobes)
        // to ammortize additional attempts where the cut was not necessary
        //TODO make this a momentum parameter
        float cut = boost / (reprobes * 2);

        assert (cut > Prioritized.EPSILON);

        set(boost, cut);

        //reprobes / (float)Math.sqrt(i) : 0;

        //return true;

        //return false;
    }

    public void set(float boost, float cut) {
        this.CACHE_HIT_BOOST = boost;
        this.CACHE_DENY_DAMAGE = cut;
    }

    @Override
    protected boolean attemptRegrowForSize(int s) {
        return false;
    }

    @Override
    public void pressurize(float f) {
        //pressurize disabled
    }

    @NotNull
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
                hit.inc();
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
            ((put(computation(x, y)) != null) ? miss : reject).inc();
        }
        return y;
    }

    /** produces the memoized computation instance for insertion into the bag.
     * here it can choose the implementation to use: strong, soft, weak, etc.. */
    public Computation<X, Y> computation(X x, Y y) {
        //return new SoftPair<>(x, y, 0.5f);
        return new StrongPair<>(x, y, 0.5f);
    }

    @Override
    protected float replace(float incoming, Computation<X, Y> existing) {
        float remain = super.replace(incoming, existing);
        if (remain!=Float.POSITIVE_INFINITY) {
            existing.priSub(CACHE_DENY_DAMAGE);
        }
        return remain;
    }

    @Override
    protected Random random() {
        return rng;
    }

    @NotNull
    @Override
    public X key(Computation<X, Y> value) {
        return value.x();
    }


    @Override
    public Consumer<Computation<X, Y>> forget(float rate) {
        return null;
    }

    @Override
    public void onRemove(HijackMemoize.Computation<X, Y> value) {
        value.delete();
        evict.inc();
    }


    /**
     * clears the statistics
     */
    @Override
    public String summary() {
        StringBuilder sb = new StringBuilder(64);
        sb.append("N=").append(size()).append(' ');
        float rate = statReset((k, v) -> {
            sb.append(k).append('=').append(v).append(' ');
        });
        sb.setLength(sb.length() - 1); //remove last ' '
        sb.append(" D=").append(Texts.n2percent(density()));
        sb.insert(0, Texts.n2percent(rate));
        return sb.toString();
    }


}
