package jcog.sort;

import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;

import java.util.function.IntFunction;

/**
 * with set of entries to merge (filter) duplicates
 */
public class CachedTopN<X> extends TopN<X> {

    final ObjectFloatHashMap<X> seen = new ObjectFloatHashMap(8);

    public CachedTopN(X[] target, FloatFunction<X> rank) {
        super(target, rank);
    }

    @Override
    public float rank(X x) {
        return seen.getIfAbsentPutWithKey(x, super::rank);
    }

    @Override
    public boolean add(X x) {
        if (seen.containsKey(x))
            return false; //may already be in the top list, but this is signaling here that nothing was added during this call

        return addUnique(x);
    }

    protected boolean addUnique(X x) {
        return super.add(x);
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
