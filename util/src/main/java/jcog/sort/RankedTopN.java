package jcog.sort;

import jcog.Util;
import jcog.data.pool.MetalPool;
import jcog.pri.Ranked;

import java.util.function.IntFunction;

/** caches the rank inside a Ranked instance for fast entry comparison as each entry */
public class RankedTopN<X> extends TopN<Ranked<X>> {

    /** source of ranked instances */
    MetalPool<Ranked> r = null;

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
    });

    public RankedTopN(IntFunction<Ranked<X>[]> arrayBuilder, int initialCapacity) {
        super(arrayBuilder.apply(initialCapacity));
    }

    /** call this on start */
    public TopN<Ranked<X>> ranking(FloatRank<X> rank) {
        r = rr.get();
        return super.rank((Ranked<X> r, float min) -> {
            float p = r.pri;
            if (p == p)
                return p;
            else
                return r.pri = rank.rank(r.x, min);
        });
    }

    public X[] itemsArray(IntFunction<X[]> arrayBuilder) {
        int s = size();
        return Util.map(i->i.x, arrayBuilder.apply(s), s, items);
    }

    public boolean addRanked(X task) {
        return add(r.get().set(task));
    }

    @Override
    public void clear() {
        if (size > 0)
            forEach(r::put);
        super.clear();
    }
}
