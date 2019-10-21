package jcog.data.set;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import jcog.Util;
import jcog.pri.NLink;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractQueue;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

/** caches float prioritization of a set of items for use in priority queue comparison function.
 *  priorities can be any value, not only 0..1.0 since NLink<X> is used.
 * */
public class CachedPriorityQueue<X> extends AbstractQueue<X> {

	final PriorityQueue<NLink<X>> queue;

	private static int compare(float a, float b) {
		return Float.compare(a, b);
	}

	public CachedPriorityQueue(FloatFunction<X> rank) {
		queue = new PriorityQueue<>(new Comparator<NLink<X>>() {
            @Override
            public int compare(NLink<X> a, NLink<X> b) {
                return a != b ?
                        compare(b.priElse(rank), a.priElse(rank))
                        : 0;
            }
        });
	}

	@Override
	public boolean offer(X x) {
		return queue.offer(new NLink<>(x, Float.NaN));
	}

	@Override
	public X poll() {
		return id(queue.poll());
	}

	@Override
	public X peek() {
		return id(queue.peek());
	}

	@Override
	public void clear() {
		queue.clear();
	}

	private static @Nullable <X> X id(@Nullable NLink<X> p) {
		return p != null ? p.id : null;
	}

	@Override
	public Iterator<X> iterator() {
		switch (queue.size()) {
			case 0: return Util.emptyIterator;
			case 1: return Iterators.singletonIterator(queue.peek().id);
			default: return Iterators.transform(queue.iterator(), new Function<NLink<X>, X>() {
                @Override
                public @org.checkerframework.checker.nullness.qual.Nullable X apply(@org.checkerframework.checker.nullness.qual.Nullable NLink<X> x) {
                    return x.id;
                }
            });
		}
	}

	@Override
	public int size() {
		return queue.size();
	}

}
