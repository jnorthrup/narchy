package jcog.sort;

import jcog.Util;
import jcog.util.ArrayUtils;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;

import java.util.Arrays;

/** caches the rank inside a Ranked instance for fast entry comparison as each entry
 * TODO maybe make autocloseable to enforce .clear (aka .close()) for returning Ranked to pools
 * */
public class RankedTopN<X> extends TopN<X> {

    /** cached rank/strength/weight/value table; maintained to be synchronized with the items array */
    private float[] value = null;


//    static final ThreadLocal<MetalPool<Ranked>> rr = ThreadLocal.withInitial(()->new MetalPool<>() {
//        @Override
//        public Ranked create() {
//            return new Ranked();
//        }
//
//        @Override
//        public void put(Ranked i) {
//            i.clear();
//            super.put(i);
//        }
//
//        @Override
//        public void put(Ranked[] items, int size) {
//            for (int i = 0; i < size; i++)
//                items[i].clear();
//            super.put(items, size);
//        }
//    });

//    public RankedTopN(IntFunction<Ranked<X>[]> arrayBuilder, int initialCapacity) {
//        this(arrayBuilder.apply(initialCapacity));
//    }

    public RankedTopN(X[] items) {
        super(items);
    }

    @Override
    public final void setCapacity(int capacity) {
        if (capacity != capacity()) {
            super.setCapacity(capacity);
            value = value !=null ? Arrays.copyOf(value, capacity) : new float[capacity];
        }
    }

//    public RankedTopN(int capacity) {
//        this();
//        setCapacity(capacity);
//    }

    public RankedTopN(X[] buffer, FloatFunction<X> ranking) {
        this(buffer, FloatRank.the(ranking));
    }

    public RankedTopN(X[] buffer, FloatRank<X> ranking) {
        this(buffer);
        rank(ranking, buffer.length);
    }

//    public static ThreadLocal<MetalPool<RankedTopN>> newRankedPool() {
//        return MetalPool.threadLocal(() -> {
//            int initialCapacity = 32;
//            return new RankedTopN(new Object[initialCapacity]);
//        });
//    }

    @Override
    protected int addEnd(X e, float elementRank) {
        int i = super.addEnd(e, elementRank);
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
        return Util.fastCompare(this.value[item], value);
    }

    private void insertValue(int i, float elementRank) {
        System.arraycopy(value, i, value, i+1, size-1-i );
        value[i] = elementRank;
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

    public X[] itemsArray() {
        int s = size();
        //return Util.map(i->i.x, arrayBuilder.apply(s), s, items);
        return s > 0 ? Arrays.copyOf(items, s) : (X[]) ArrayUtils.EMPTY_OBJECT_ARRAY;
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
            //Arrays.fill(ranked, Float.NEGATIVE_INFINITY);
        }
        super.clear();
    }
}
