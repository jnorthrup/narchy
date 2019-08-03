package jcog.data.iterator;

import jcog.TODO;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

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
		return next != null || (next = find()) != null;
	}

	private E find() {
		E next = null;
		E[] a = this.array;
		int index = this.index;
		final int limit = this.limit;
		while (++index < limit) {
			if ((next = a[index])!=null)
				break;
		}
		this.index = index;
		return next;
	}

	@Override
	public E next() {
		E n = next;
		if (n == null) {
			//called next without prior hasNext, so call that now.
			if ((n = find())==null)
				throw new NoSuchElementException();
		}
		this.next = null; //force update if not calling hasNext() for next iteration
		return n;
	}

	@Override
	public void forEachRemaining(Consumer<? super E> action) {
		throw new TODO();
	}
	@Override
	public void forEach(Consumer<? super E> action) {
		throw new TODO();
	}

	/** if already started, return a fresh copy */
	@Override public Iterator<E> iterator() {
		return (index != -1) ? clone() : this;
		//return (index != -1 && !(index==0 && next!=null)) ? clone() : this;
	}

	public ArrayIteratorNonNull<E> clone() {
		return new ArrayIteratorNonNull<>(array);
	}
}
