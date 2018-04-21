package jcog.sort;

import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;

import java.util.function.IntFunction;

/**
 * with set of entries to merge (filter) duplicates
 */
public class CachedTopN<X> extends TopN<X> {

    final ObjectFloatHashMap<X> seen = new ObjectFloatHashMap<>(8);

    public CachedTopN(X[] target, FloatFunction<X> rank) {
        super(target, rank);
    }

    @Override
    public boolean add(X x) {
        final boolean[] adding = {false};
        float rank = seen.getIfAbsentPutWithKey(x, (xx)->{
            float r = rank(xx);
            adding[0] = (r == r); //add if valid
            return r;
        });
        if (!adding[0]/* || rank!=rank*/)
            return false; //may already be in the top list, but this is signaling here that nothing was added during this call

        return add(x, rank, seen::get)>=0;
    }

    @Override
    public void clear() {
        if (size > 0) {
            seen.clear();
            super.clear();
        }
    }

    @Override
    public void clear(int newCapacity, IntFunction<X[]> newArray) {
        seen.clear();
        super.clear(newCapacity, newArray);
    }

//    //forces a sort
//    protected void sort() {
//        Arrays.sort(list, 0, size, (a, b) -> Float.compare(rank(a), rank(b))));
//    }

}
