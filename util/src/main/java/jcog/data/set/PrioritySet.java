package jcog.data.set;

import org.eclipse.collections.api.block.function.primitive.FloatFunction;

import java.util.Set;

public class PrioritySet<X> extends CachedPriorityQueue<X> {

	private final Set<X> set;

	public PrioritySet(FloatFunction<X> rank, Set<X> set) {
		super(rank);
		this.set = set;
	}

	@Override
	public boolean offer(X x) {
		return set.add(x) ? super.offer(x) : false;
	}

	@Override
	public boolean add(X x) {
		if (set.add(x))
			return super.offer(x);

		return true; //avoids: java.lang.IllegalStateException: Queue full
	}

	@Override
	public X poll() {
		X x = super.poll();
		if (x!=null) {
			set.remove(x); //assert(removed)
		}
		return x;
	}

	@Override
	public void clear() {
		if (!isEmpty()) {
			super.clear();
			set.clear();
		}
	}
}
