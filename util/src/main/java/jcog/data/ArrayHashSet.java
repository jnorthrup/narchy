/*
 * Copyright (c) 2013, SRI International
 * All rights reserved.
 * Licensed under the The BSD 3-Clause License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 * 
 * http://opensource.org/licenses/BSD-3-Clause
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * Redistributions of source code must retain the above copyright
 * notice, this arrayList of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this arrayList of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of the aic-expresso nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jcog.data;

import java.util.*;

import jcog.list.FasterList;

/**
 * Analogous to {@link java.util.LinkedHashSet}, but with an {@link java.util.ArrayList} instead of a {@link java.util.LinkedList},
 * offering the same advantages (random access) and disadvantages (slower addition and removal of elements),
 * but with the extra advantage of offering an iterator that is actually a {@link java.util.ListIterator}.
 * @author braz
 *
 * @param <X> the type of the elements
 *
 * from: https://github.com/aic-sri-international/aic-util/blob/master/src/main/java/com/sri/ai/util/collect/ArrayHashSet.java
 */
public class ArrayHashSet<X> extends AbstractSet<X> implements ArraySet<X> {

	private HashSet<X>   set;
	private List<X> list;

	final static List EMPTY = List.of();
	
	public ArrayHashSet() {
		this.set  = new HashSet<X>();
		this.list = EMPTY;//new ArrayList<E>();
	}
	
	public ArrayHashSet(int capacity) {
		this.set  = new HashSet<X>(capacity);
		this.list = EMPTY; //new ArrayList<E>(capacity);
	}
	
	public ArrayHashSet(Collection<X> collection) {
		this();
		addAll(collection);
	}
	
	// ArraySet methods
	
	@Override
	public ListIterator<X> listIterator() {
		return list.listIterator();
	}

	@Override
	public ListIterator<X> listIterator(int index) {
		return list.listIterator(index);
	}

	@Override
	public X get(int index) {
		return list.get(index);
	}

//	@Override
//	public void set(int index, E element) {
//		ArrayHashSet<E>.ArrayHashSetIterator listIterator = listIterator(index);
//		listIterator.next();
//		listIterator.set(element);
//	}

	// end of ArraySet methods
	
	// required implementations
	
	@Override
	public boolean add(X element) {
		boolean modified = set.add(element);
		if (modified) {
			if (list ==EMPTY)
				list = new FasterList<>(1);
			list.add(element);
		}
		return modified;
	}

	@Override
	public Iterator<X> iterator() {
		return list.listIterator();
	}

	@Override
	public int size() {
		return list.size();
	}
	
	// end of required implementations

	// methods not required to be implemented, but more efficient
	
	@Override
	public boolean contains(Object o) {
		return set.contains(o);
	}
	
	@Override
	public Object[] toArray() {
		return list.toArray();
	}
	
	@Override
	public <T> T[] toArray(T[] a) {
		return list.toArray(a);
	}
	
	@Override
	public boolean remove(Object o) {
		boolean removed = set.remove(o);
		if (removed) {
			list.remove(o);
			if (list.isEmpty())
				list = EMPTY;
		}
		return removed;
	}
	
	@Override
	public void clear() {
		if (list!=EMPTY) {
			set.clear();
			list = EMPTY;
		}
	}

	@Override
	public boolean isEmpty() {
		return list==EMPTY;
	}



	// end of methods not required to be implemented, but more efficient

//	private class ArrayHashSetIterator implements ListIterator<E> {
//
//		private ListIterator<E> arrayListIterator;
//		private E lastElementProvided;
//
//		public ArrayHashSetIterator(ListIterator<E> arrayListIterator) {
//			this.arrayListIterator = arrayListIterator;
//		}
//
//		@Override
//		public boolean hasNext() {
//			return arrayListIterator.hasNext();
//		}
//
//		@Override
//		public E next() {
//			return lastElementProvided = arrayListIterator.next();
//		}
//
//		@Override
//		public void add(E element) {
//			if (set.add(element)) {
//				arrayListIterator.add(element);
//			}
//		}
//
//		@Override
//		public boolean hasPrevious() {
//			return arrayListIterator.hasPrevious();
//		}
//
//		@Override
//		public int nextIndex() {
//			return arrayListIterator.nextIndex();
//		}
//
//		@Override
//		public E previous() {
//			return lastElementProvided = arrayListIterator.previous();
//		}
//
//		@Override
//		public int previousIndex() {
//			return arrayListIterator.previousIndex();
//		}
//
//		@Override
//		public void remove() {
//			arrayListIterator.remove();
//			set.remove(lastElementProvided);
//		}
//
//		@Override
//		public void set(E element) {
//			if (element.equals(lastElementProvided)) {
//				// no need to do anything
//			}
//			else {
//				if (set.contains(element)) {
//					// cannot add because element would appear more than once
//					throw new IllegalArgumentException("Cannot set already-present element in a different position in ArrayHashSet.");
//				}
//				else {
//					arrayListIterator.set(element);
//					set.remove(lastElementProvided);
//					set.add(element);
//				}
//			}
//		}
//	}
}
