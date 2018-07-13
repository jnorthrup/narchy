package jcog.data.iterator;

import java.util.Iterator;
import java.util.function.IntFunction;

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
		this.iterators = iterators;

		
		if (!empty) {
			values = arrayBuilder.apply(size);
			for (int i = 0; i < size-1; i++) setNextValue(i);
		} else {
			values = null;
		}
		this.iterables = iterables;
	}

	@Override
	public boolean hasNext() {
		if (values==null) return false;
		int size = iterables.length;
		for (int i = 0; i < size; i++)
			if (iterators[i].hasNext())
				return true;
		return false;
	}

	@Override
	public X[] next() {
		
		int cursor;
		int size = iterables.length;
		for (cursor = size-1; cursor >= 0; cursor--)
			if (iterators[cursor].hasNext()) break;
		
		for (int i = cursor+1; i < size; i++)
			iterators[i] = iterables[i].iterator();
		
		
		for (int i = cursor; i < size; i++) setNextValue(i);
		return values.clone();
	}
	/**
	 * Gets the next value provided there is one from the iterator at the given index. 
	 * @param index
	 */
	private void setNextValue(int index) {
		Iterator<X> it = iterators[index];
		if (it.hasNext())
			values[index] = it.next();
	}
	@Override
	public void remove() { throw new UnsupportedOperationException(); }
}
  

