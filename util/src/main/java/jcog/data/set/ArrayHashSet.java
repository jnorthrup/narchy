/*
 * Copyright (c) 2013, SRI International
 * All rights reserved.
 * Licensed under the The BSD 3-Clause License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 * http:
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
package jcog.data.set;

import jcog.TODO;
import jcog.data.list.FasterList;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.*;
import java.util.function.Consumer;

/**
 * Analogous to {@link java.util.LinkedHashSet}, but with an {@link java.util.ArrayList} instead of a {@link java.util.LinkedList},
 * offering the same advantages (random access) and disadvantages (slower addition and removal of elements),
 * but with the extra advantage of offering an iterator that is actually a {@link java.util.ListIterator}.
 *
 * @param <X> the type of the elements
 *            <p>
 *            from: https:
 * @author braz
 */
public class ArrayHashSet<X> extends AbstractSet<X> implements ArraySet<X> {

    public static ArrayHashSet EMPTY = new ArrayHashSet(0) {
        @Override
        public boolean add(Object element) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object first() {
            return null;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public ListIterator listIterator() {
            return Collections.emptyListIterator();
        }

        @Override
        public ListIterator listIterator(int index) {
            assert (index == 0);
            return Collections.emptyListIterator();
        }

        @Override
        public Iterator iterator() {
            return Collections.emptyListIterator();
        }
    };

    public final List<X> list;
    protected Set<X> set = emptySet();


    public ArrayHashSet() {
        this(4);
    }

    public ArrayHashSet(int capacity) {
        this(new FasterList<>(capacity));
    }

    public ArrayHashSet(List<X> list) {
        this.list = list;
    }


    @Override
    public boolean equals(Object o) {
        return set.equals(((ArrayHashSet)o).set);
    }

    @Override
    public int hashCode() {
        return set.hashCode();
    }

    public static <X> ArrayHashSet<X> of(X... x) {
        ArrayHashSet a = new ArrayHashSet(x.length);
        Collections.addAll(a, x);
        return a;
    }

    @Override
    public boolean addAll(Collection<? extends X> c) {
        //throw new TODO("optimized bulk add");
        boolean added = false;
        for (X x : c) {
            added |= add(x);
        }
        return added;
    }

    @Override
    public void forEach(Consumer<? super X> action) {
        list.forEach(action);
    }

    @Override
    public ListIterator<X> listIterator() {
        return size()==0 ? Collections.emptyListIterator() : new ArrayHashSetIterator();
    }

    @Override
    public ListIterator<X> listIterator(int index) {
        return new ArrayHashSetIterator(index);
    }

    public boolean OR(org.eclipse.collections.api.block.predicate.Predicate<? super X> test) {
        return jcog.Util.or(test, list);
    }

    protected int setSize() {
        return set.size();
    }

    public boolean AND(org.eclipse.collections.api.block.predicate.Predicate<? super X> test) {
        return jcog.Util.and(test, list);
    }


    @Override
    public X get(int index) {
        return list.get(index);
    }

    @Override
    public boolean add(X element) {
        switch (list.size()) {
            case 0:
                set = newSet();
                set.add(element);
                addedUnique(element);
                return true;
            default:
                if (set.add(element)) {
                    addedUnique(element);
                    return true;
                }
                return false;
        }

    }

    protected void addedUnique(X element) {
        list.add(element);
    }

    public Set<X> newSet() {
        return new UnifiedSet<>(2);
        //return new HashSet(2);
    }

    @Override
    public Iterator<X> iterator() {
        return listIterator();
    }


    @Override
    public int size() {
        return list.size();
    }

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
        int s = size();
        if ( s== 0) return false;
        boolean removed = set.remove(o);
        if (removed) {
            s--;
            list.remove(o);

            switch (s) {
                case 0:
                    set = emptySet();
                    break;
            }
        }

        return removed;
    }


    @Override
    public void clear() {

        if (!list.isEmpty()) {
            list.clear();
            set.clear();
        }

    }


    @Override
    public X remove(Random random) {
        int s = size();
        if (s == 0) return null;
        int index = s == 1 ? 0 : random.nextInt(s);
        X removed;
        remove(removed = list.remove(index));
        return removed;
    }

    @Override
    public void shuffle(Random random) {
        Collections.shuffle(list, random);
    }

    private final class ArrayHashSetIterator implements ListIterator<X> {

        private final ListIterator<X> arrayListIterator;
        private X lastElementProvided;

        ArrayHashSetIterator() {
            this(-1);
        }

        ArrayHashSetIterator(int index) {
            this.arrayListIterator = index == -1 ? list.listIterator() : list.listIterator(index);
        }

        @Override
        public boolean hasNext() {
            return arrayListIterator.hasNext();
        }

        @Override
        public X next() {
            return lastElementProvided = arrayListIterator.next();
        }

        @Override
        public void add(X element) {
            if (set == null)
                throw new TODO();

            if (set.add(element)) {
                addedUnique(element);
            }
        }

        @Override
        public boolean hasPrevious() {
            return arrayListIterator.hasPrevious();
        }

        @Override
        public int nextIndex() {
            return arrayListIterator.nextIndex();
        }

        @Override
        public X previous() {
            return lastElementProvided = arrayListIterator.previous();
        }

        @Override
        public int previousIndex() {
            return arrayListIterator.previousIndex();
        }

        @Override
        public void remove() {
            arrayListIterator.remove();
            boolean removed = set.remove(lastElementProvided);
            if (removed) {
                //assert (removed);
                if (set.isEmpty()) {
                    set = emptySet();
                }
            }

        }

        @Override
        public void set(X element) {
            if (element.equals(lastElementProvided)) {

            } else {
                if (set.contains(element)) {

                    throw new IllegalArgumentException("Cannot set already-present element in a different position in ArrayHashSet.");
                } else {
                    arrayListIterator.set(element);
                    set.remove(lastElementProvided);
                    set.add(element);
                }
            }
        }
    }


    static Set emptySet() {
        return Set.of();
    }
}
