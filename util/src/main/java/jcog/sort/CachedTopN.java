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
public class CachedTopN<X> extends UnifiedSet<X> implements Iterable<X> {

    protected final TopN<NLink<X>> top;
    private final FloatFunction<X> rank;

    public CachedTopN(int limit, FloatFunction<X> rank) {
        this(new NLink[limit], rank);
    }

    public CachedTopN(NLink<X>[] target, FloatFunction<X> rank) {
        super(0);
        top = new TopN<>(target, NLink::pri);
        this.rank = rank;
    }

    /** implementations can filter unique inputs before ranking them here */
    public boolean valid(X x) {
        return true;
    }

    public boolean add(X x) {
        if (super.add(x) && valid(x)) {
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
        X[] x = arrayBuilder.apply(top.size());
        int s = top.size();
        NLink<X>[] l = top.list;
        for (int i = 0; i < s; i++) {
            x[i] = l[i].id;
        }
        return x;
    }

//    @Override
//    public boolean add(NLink<X> x) {
//        final boolean[] adding = {false};
//        float rank = seen.getIfAbsentPutWithKey(x, (xx)->{
//            float r = rank(xx);
//            adding[0] = (r == r); //add if valid
//            return r;
//        });
//        if (!adding[0]/* || rank!=rank*/)
//            return false; //may already be in the top list, but this is signaling here that nothing was added during this call
//
//        return add(x, rank, seen::get)>=0;
//    }


//    //forces a sort
//    protected void sort() {
//        Arrays.sort(list, 0, size, (a, b) -> Float.compare(rank(a), rank(b))));
//    }

}
