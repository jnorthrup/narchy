package jcog.sort;

import org.eclipse.collections.api.block.function.primitive.FloatFunction;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * with set of entries to merge (filter) duplicates
 */
public class TopNUnique<X> extends TopN<X> {

    final Map<X, X> map;

    public TopNUnique(X[] target, FloatFunction<X> rank) {
        super(target, rank);
        map = new HashMap(target.length);
    }

    @Override
    public boolean add(X x) {
        if (map.merge(x, x, (p, n) -> {
            float rankBefore = rank.floatValueOf(p);
            merge(p, n);
            float rankAfter = rank.floatValueOf(p);
            if (rankAfter != rankBefore) {
                sort();
            }
            return p;
        }) == x) {
            return super.add(x);
        } else {
            return false;//duplicate
        }

    }

    @Override
    protected void rejectExisting(X x) {
        super.rejectExisting(x);
        map.remove(x);
    }

    @Override
    public void clear() {
        super.clear();
        map.clear();
    }

    //forces a sort
    protected void sort() {
        Arrays.sort(list, 0, size, (a, b) -> Float.compare(-rank.floatValueOf(a), -rank.floatValueOf(b)));
    }

    protected void merge(X existing, X next) {

    }
}
