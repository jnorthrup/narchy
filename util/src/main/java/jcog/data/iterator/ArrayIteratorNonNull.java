package jcog.data.iterator;

import java.util.Iterator;

/** TODO test, may need a pre-buffering stategy to absolutely contain any Null's */
public class ArrayIteratorNonNull<E> implements Iterator<E>, Iterable<E> {

	private final int limit;
	E next = null;
	protected final E[] array;
	protected int index = -1;

	public ArrayIteratorNonNull(E[] array) {
		this(array, array.length);
	}

	public ArrayIteratorNonNull(E[] array, int limit) {
		this.array = array;
		this.limit = Math.min(array.length, limit);
	}

	@Override
	public boolean hasNext() {
		while (++index < limit) {
			if ((next = array[index])!=null)
				return true;
		}
		return false;
	}

	@Override
	public E next() {
		return next;
	}

	@Override
	public Iterator<E> iterator() {
		return this;
	}
}
