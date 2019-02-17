package jcog.sort;

import jcog.Util;
import jcog.data.pool.MetalPool;
import jcog.pri.Ranked;

import java.util.function.IntFunction;

/** caches the rank inside a Ranked instance for fast entry comparison as each entry */
public class RankedTopN<X> extends TopN<Ranked<X>> {

    /** source of ranked instances */
    MetalPool<Ranked> pool = null;

    static final ThreadLocal<MetalPool<Ranked>> rr = ThreadLocal.withInitial(()->new MetalPool<>() {
        @Override
        public Ranked create() {
            return new Ranked();
        }

        @Override
        public void put(Ranked i) {
            i.clear();
            super.put(i);
        }

        @Override
        public void put(Ranked[] items, int size) {
            for (int i = 0; i < size; i++)
                items[i].clear();
            super.put(items, size);
        }
    });

    public RankedTopN(IntFunction<Ranked<X>[]> arrayBuilder, int initialCapacity) {
        super(arrayBuilder.apply(initialCapacity));
    }

    /** call this on start */
    public final TopN<Ranked<X>> ranking(FloatRank<X> rank, int capacity) {
        this.pool = rr.get();
        return super.rank((r, min) -> r.apply(rank, min), capacity);
    }

    @Override
    public final Ranked<X> pop() {
        throw new UnsupportedOperationException("Use popRanked()"); //HACK
    }

    public final X popRanked() {
        Ranked<X> p = super.pop();
        if (p != null) {
            X x = p.x;
            pool.put(p);
            return x;
        } else {
            return null;
        }
    }

    public X[] itemsArray(IntFunction<X[]> arrayBuilder) {
        int s = size();
        return Util.map(i->i.x, arrayBuilder.apply(s), s, items);
    }

    public final boolean addRanked(X task) {
        return add(pool.get().set(task));
    }

    @Override
    protected final void rejectExisting(Ranked<X> e) {
        pool.put(e);
    }

    @Override
    protected final void rejectOnEntry(Ranked<X> e) {
        pool.put(e);
    }

    @Override
    public void clear() {
        int s = this.size;
        if (s > 0)
            pool.put(items, s);
        super.clear();
    }
}
