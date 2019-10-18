package jcog.data.iterator;

import java.util.Iterator;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

/**
 * Iterates over Cartesian product for an array of Iterables, 
 * Returns an array of Objects on each step containing values from each of the Iterables.
 *
 * @author jacek.p.kolodziejczyk@gmail.com
 * @created 01-08-2010
 *
 * https:
 */
public class CartesianIterator<X> implements Iterator<X[]> {
	private final Iterable<X>[] iterables;
	private final Iterator<X>[] iterators;
	private final X[] values;

	/**
	 * Constructor
	 * @param iterables array of Iterables being the source for the Cartesian product.
	 */
	@SafeVarargs
	public CartesianIterator(IntFunction<X[]> arrayBuilder, Iterable<X>... iterables) {
		int size = iterables.length;

		Iterator[] iterators = new Iterator[size];

		boolean empty = false;

		for (int i = 0; i < size; i++) {
			Iterator<X> ii = iterables[i].iterator();
			if (!ii.hasNext()) {
				empty = true;
				break;
			}
			iterators[i] = ii;
		}

		
		if (!empty) {
			this.iterators = iterators;
			this.iterables = iterables;
			values = arrayBuilder.apply(size);
			next(0, size-1);
		} else {
			this.values = null;
			this.iterators = null;
			this.iterables = null;
		}
	}

	@Override
	public boolean hasNext() {
		if (values==null) return false;
		int size = iterables.length;
		for (int i = 0; i < size; i++) {
			if (iterators[i].hasNext()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public X[] next() {
		
		int cursor;
		int size = iterables.length;
		for (cursor = size-1; cursor >= 0; cursor--)
			if (iterators[cursor].hasNext())
				break;
		
		for (int i = cursor+1; i < size; i++)
			iterators[i] = iterables[i].iterator();

		return next(cursor, size);
	}

	private X[] next(int cursor, int size) {

		for (int i = cursor; i < size; i++)
			values[i] = iterators[i].next();

		return cloneNext() ? values.clone() : values;
	}

	/** if true, the value returned by next() will be cloned.  otherwise it is re-used in next iteration */
	protected static boolean cloneNext() {
		return false;
	}

	@Override
	public void remove() { throw new UnsupportedOperationException(); }

}
  

