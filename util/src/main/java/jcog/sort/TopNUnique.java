package jcog.sort;

import org.eclipse.collections.api.block.function.primitive.FloatFunction;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntFunction;

/**
 * with set of entries to merge (filter) duplicates
 */
public class TopNUnique<X> extends TopN<X> {

    final Map<X,X> seen = new HashMap();

    protected TopNUnique(FloatFunction<X> rank) {
        super(rank);
    }

    public TopNUnique(X[] target, FloatFunction<X> rank) {
        super(target, rank);
    }

    @Override
    public boolean add(X x) {
        X p = seen.put(x,x);
        if (p == null) {
            return addUnique(x);
        } else if (p!=x) {
            mergeInto(p, x);
            return true; //keep
        } else {
            return true; //identical
        }
    }

    protected boolean addUnique(X x) {
        return super.add(x);
    }

    @Override
    protected void rejectExisting(X x) {
        super.rejectExisting(x);
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

    //forces a sort
    protected void sort() {
        Arrays.sort(list, 0, size, (a, b) -> Float.compare(-rank.floatValueOf(a), -rank.floatValueOf(b)));
    }

    protected void mergeInto(X existing, X next) {
        //by default this does nothing
    }
}
