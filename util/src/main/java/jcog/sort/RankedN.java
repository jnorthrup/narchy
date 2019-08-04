package jcog.sort;

import org.eclipse.collections.api.block.function.primitive.FloatFunction;

import java.util.Arrays;

/** caches the rank inside a Ranked instance for fast entry comparison as each entry
 * TODO maybe make autocloseable to enforce .clear (aka .close()) for returning Ranked to pools
 * */
public class RankedN<X> extends TopN<X> {

    /** cached rank/strength/weight/value table; maintained to be synchronized with the items array */
    private float[] value;


    public RankedN(X[] items) {
        super(items);
    }

    @Override
    public final void setCapacity(int capacity) {
        super.setCapacity(capacity);

        if (value==null || value.length!=capacity)
            value = value !=null ? Arrays.copyOf(value, capacity) : new float[capacity];
    }

//    public RankedTopN(int capacity) {
//        this();
//        setCapacity(capacity);
//    }

    public RankedN(X[] buffer, FloatFunction<X> ranking) {
        this(buffer, FloatRank.the(ranking));
    }

    public RankedN(X[] buffer, FloatRank<X> ranking) {
        this(buffer);
        rank(ranking);
    }

    @Override
    protected int addEnd(X x, float elementRank) {
        int i = super.addEnd(x, elementRank);
        if (i!=-1)
            insertValue(i, elementRank);
        return i;
    }

    @Override
    protected int addAtIndex(int index, X element, float elementRank, int oldSize) {
        int i = super.addAtIndex(index, element, elementRank, oldSize);
        if (i!=-1) {
            insertValue(i, elementRank);
        }
        return i;
    }

    @Override
    protected int compare(int item, float value, FloatFunction<X> cmp) {
        return Float.compare(this.value[item], value);
    }

    private void insertValue(int i, float elementRank) {
        float[] v = this.value;

        int shift = size-1-i;
        if (shift > 0)
            System.arraycopy(v, i, v, i+1, shift );

        v[i] = elementRank;
    }


    @Override
    public X remove(int index) {
        int totalOffset = this.size - index - 1;
        if (totalOffset >= 0) {
            X[] list = this.items;
            X previous = list[index];
            if (totalOffset > 0) {
                System.arraycopy(list, index + 1, list, index, totalOffset);
                System.arraycopy(value, index + 1, value, index, totalOffset);
            }
            list[SIZE.decrementAndGet(this)] = null;
            return previous;
        } else
            return null;
    }


    @Override
    protected final void rejectExisting(X e) {
    }

    @Override
    protected final void rejectOnEntry(X e) {

    }

    @Override
    public void clear() {
        int s = this.size;
        if (s > 0) {
            Arrays.fill(items, 0, s, null);
            super.clear();
            //Arrays.fill(ranked, Float.NEGATIVE_INFINITY);
        }
        min = Float.NEGATIVE_INFINITY;
    }


//    @Nullable
//    public X getRoulette(FloatSupplier rng, Predicate<X> filter, boolean cached) {
//        int n = size();
//        if (n == 0)
//            return null;
//        if (n == 1)
//            return get(0);
//
//        IntToFloatFunction select = i -> filter.test(get(i)) ? (cached ? rankCached(i) : rank.rank(get(i))) : Float.NaN;
//        return get( //n < 8 ?
//                this instanceof RankedN ?
//                        Roulette.selectRoulette(n, select, rng) : //RankedTopN acts as the cache
//                        Roulette.selectRouletteCached(n, select, rng) //must be cached for consistency
//        );
//
//    }
}
