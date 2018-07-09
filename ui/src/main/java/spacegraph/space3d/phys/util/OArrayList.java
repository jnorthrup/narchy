/*
 * Java port of Bullet (c) 2008 Martin Dvorak <jezek2@advel.cz>
 *
 * Bullet Continuous Collision Detection and Physics Library
 * Copyright (c) 2003-2008 Erwin Coumans  http:
 *
 * This software is provided 'as-is', without any express or implied warranty.
 * In no event will the authors be held liable for any damages arising from
 * the use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose, 
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 * 1. The origin of this software must not be misrepresented; you must not
 *    claim that you wrote the original software. If you use this software
 *    in a product, an acknowledgment in the product documentation would be
 *    appreciated but is not required.
 * 2. Altered source versions must be plainly marked as such, and must not be
 *    misrepresented as being the original software.
 * 3. This notice may not be removed or altered from any source distribution.
 */

package spacegraph.space3d.phys.util;

import org.eclipse.collections.api.block.predicate.primitive.IntObjectPredicate;
import org.eclipse.collections.api.block.procedure.primitive.IntObjectProcedure;
import org.jetbrains.annotations.NotNull;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.RandomAccess;
import java.util.function.Predicate;

/**
 *
 * @author jezek2
 */
public final class OArrayList<T> extends AbstractList<T> implements RandomAccess, Externalizable {

	private T[] array;
	private int size;

	public OArrayList() {
		this(4);
	}
	
	@SuppressWarnings("unchecked")
	public OArrayList(int initialCapacity) {
		array = (T[])new Object[initialCapacity];
	}


	@SafeVarargs
	public final void addAll(T... v) {
		if (size + v.length >= array.length) {
			expand();
			
		}

		T[] array = this.array;
		for (T x : v) {
			array[size++] = x;
		}
	}

	public final void forEachWithIndex(IntObjectPredicate<? super T> each) {
		int s = size();
		T[] array = this.array;
		for (int i = 0; i < s; i++) {
			if (!each.accept(i, array[i]))
				break;
		}
	}
	public final void forEachWithIndexProc(IntObjectProcedure<? super T> each) {
		int s = size();
		T[] array = this.array;
		for (int i = 0; i < s; i++) {
			each.value(i, array[i]);
		}
	}

	@Override
	public boolean add(T value) {
		if (size == array.length) {
			expand();
		}
		
		array[size++] = value;
		return true;
	}

	@Override
	public void add(int index, T value) {
		if (size == array.length) {
			expand();
		}

		T[] a = array;
		int num = size - index;
		if (num > 0) {
			System.arraycopy(a, index, a, index+1, num);
		}

		a[index] = value;
		size++;
	}

	@Override
	public T remove(int index) {
		
		T[] a = this.array;
		T prev = a[index];
		System.arraycopy(a, index+1, a, index, size-index-1);
		a[--size] = null;
		return prev;
    }

	@Override
	public boolean remove(Object o) {
		if (isEmpty())
			return false;
		int i = indexOf(o);
		if (i == -1)
			return false;
		remove(i);
		return true;
	}

	@Override
	public final boolean removeIf(Predicate<? super T> filter) {
		int s = size();
		if (s == 0)
			return false;
		int ps = s;
		T[] a = this.array;
		for (int i = 0; i < s; ) {
			if (filter.test(a[i])) {
				s--;
				System.arraycopy(a, i+1, a, i, s - i);
				Arrays.fill(a, s, ps,null);
			} else {
				i++;
			}
		}
		if (ps!=s) {
			this.size = s;
			return true;
		}

		return false;
	}

	@SuppressWarnings("unchecked")
	private void expand() {
		T[] newArray = (T[])new Object[array.length << 1];
		System.arraycopy(array, 0, newArray, 0, array.length);
		array = newArray;
	}

	public void removeFast(int index) {
		System.arraycopy(array, index+1, array, index, size - index - 1);
		array[--size] = null;
	}

	@Override
    public final T get(int index) {
		
		return array[index];
	}

	@Override
	public T set(int index, T value) {
		
		T[] a = this.array;
		T old = a[index];
		a[index] = value;
		return old;
	}

	public void setFast(int index, T value) {
		array[index] = value;
	}

	@Override
    public int size() {
		return size;
	}
	
	public int capacity() {
		return array.length;
	}
	
	@Override
	public void clear() {
		size = 0;
	}

	@Override
	public int indexOf(@NotNull Object o) {
		int _size = size;
		T[] _array = array;
		for (int i=0; i<_size; i++) {
			T x = _array[i];
			if (o.equals(x))
				return i;
			
				
			
		}
		return -1;
	}

	@Override
    public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(size);
		for (int i=0; i<size; i++) {
			out.writeObject(array[i]);
		}
	}

	@Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		size = in.readInt();
		int cap = 16;
		while (cap < size) cap <<= 1;
		array = (T[])new Object[cap];
		for (int i=0; i<size; i++) {
			array[i] = (T)in.readObject();
		}
	}

	public final T removeLast() {
		return remove(size - 1);
	}
}
