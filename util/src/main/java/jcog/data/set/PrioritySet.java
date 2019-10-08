package jcog.data.set;

import com.google.common.collect.Iterators;
import jcog.Util;
import jcog.pri.PLink;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.PriorityQueue;

/** caches float prioritization of a set of items for use in priority queue comparison function */
public class PrioritySet<X> extends AbstractQueue<X> {

	final PriorityQueue<PLink<X>> queue;

	private static int compare(float a, float b) {
		if (a < b)
			return -1;
		else if (a > b)
			return 1;
		else
			return 0;
	}

	public PrioritySet(FloatFunction<X> rank) {
		queue = new PriorityQueue<>((a, b)-> a!=b ?
			compare(b.priElse(rank), a.priElse(rank))
			: 0);
	}

	@Override
	public boolean offer(X x) {
		return queue.offer(new PLink<>(x, Float.NaN));
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

	@Nullable private static <X> X id(@Nullable PLink<X> p) {
		return p != null ? p.id : null;
	}

	@Override
	public Iterator<X> iterator() {
		switch (queue.size()) {
			case 0: return Util.emptyIterator;
			case 1: return Iterators.singletonIterator(queue.peek().id);
			default: return Iterators.transform(queue.iterator(), (PLink<X> x)-> x.id);
		}
	}

	@Override
	public int size() {
		return queue.size();
	}

}
