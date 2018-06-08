package jcog.sort;

import com.google.common.collect.Iterators;
import jcog.pri.NLink;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;

/**
 * with set of entries to merge (filter) duplicates
 */
public class CachedTopN<X> implements Iterable<X> {

    public UnifiedSet<X> seen = null;
    protected final TopN<NLink<X>> top;
    private final FloatFunction<X> rank;

    public CachedTopN(int limit, FloatFunction<X> rank) {
        this(new NLink[limit], rank);
    }

    public CachedTopN(NLink<X>[] target, FloatFunction<X> rank) {
        top = new TopN<>(target, NLink::pri);
        this.rank = rank;
    }

    /** implementations can filter unique inputs before ranking them here */
    public boolean valid(X x) {
        return true;
    }

    public boolean add(X x) {
        if (seen == null)
            seen = new UnifiedSet<>(1);

        if (seen.add(x) && valid(x)) {
            float r = rank.floatValueOf(x);
            if (r > top.minValueIfFull())
                top.add(new NLink<>(x, r));
        }
        return true;
    }

    public void forEach(Consumer<? super X> target) {
        top.forEach(x -> target.accept(x.id));
    }

    public float pri(int y) {
        return top.list[y].pri();
    }

    public X get(int y) {
        return top.list[y].id;
    }

    public int size() {
        return top.size();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public Iterator<X> iterator() {
        return Iterators.transform(top.iterator(), n -> n.id);
    }

    public X[] array(IntFunction<X[]> arrayBuilder) {
        int s = top.size();
        X[] x = arrayBuilder.apply(s);
        NLink<X>[] l = top.list;
        for (int i = 0; i < s; i++) {
            x[i] = l[i].id;
        }
        return x;
    }

}
