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

import com.google.common.collect.Iterators;
import jcog.TODO;
import jcog.data.iterator.ArrayIterator;
import jcog.data.list.FasterList;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Analogous to {@link java.util.LinkedHashSet}, but with an {@link java.util.ArrayList} instead of a {@link java.util.LinkedList},
 * offering the same advantages (random access) and disadvantages (slower addition and removal of elements),
 * but with the extra advantage of offering an iterator that is actually a {@link java.util.ListIterator}.
 *
 * @param <X> the type of the elements
 *            <p>
 *            from: https:
 * @author braz
 *
 * TODO configurable min,max capacity to determine when to free or just clear an internal collection
 */
public class ArrayHashSet<X> extends AbstractSet<X> implements ArraySet<X> {


    private static final int DEFAULT_SET_CAPACITY = 4;
    public final FasterList<X> list;
    Set<X> set = emptySet();


    public ArrayHashSet() {
        this(DEFAULT_SET_CAPACITY);
    }

    public ArrayHashSet(int capacity) {
        this(new FasterList<>(capacity));
    }

    protected ArrayHashSet(FasterList<X> list) {
        this.list = list;
    }


    /** unordered equality via set */
    @Override public boolean equals(Object o) {
        return this == o || set.equals(((ArrayHashSet)o).set);
    }

    /** unordered equality via set */
    @Override
    public int hashCode() {
        return set.hashCode();
    }


    @Override
    public boolean addAll(Collection<? extends X> c) {
        //throw new TODO("optimized bulk addAt");
        boolean added = c.stream().map(this::add).reduce(false, (a, b) -> a || b);
        return added;
    }

    @SafeVarargs
    public final boolean addAll(X... c) {
        //throw new TODO("optimized bulk addAt");
        boolean added = Arrays.stream(c).map(this::add).reduce(false, (a, b) -> a || b);
        return added;
    }

    @Override
    public boolean removeAll(Collection c) {
        //throw new TODO("optimized bulk addAt");
        boolean rem = false;
        for (Object x : c)
            rem |= remove(x);
        return rem;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return set.containsAll(c);
    }

    @Override
    public boolean removeIf(Predicate<? super X> filter) {
        switch(size()) {
            case 0: return false;
            case 1: if (filter.test(get(0))) {
                clear();
                return true;
            } else return false;
            default:
                return super.removeIf(filter);
        }
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new TODO();
    }

    @Override
    public void forEach(Consumer<? super X> action) {
        list.forEach(action);
    }

    @Override
    public final Stream<X> stream() {
        return list.stream();
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
    public boolean add(X x) {
        switch (list.size()) {
            case 0:
                set = newSet(DEFAULT_SET_CAPACITY /*list.capacity()*/);
                set.add(x);
                addedUnique(x);
                return true;
            default:
                if (set.add(x)) {
                    addedUnique(x);
                    return true;
                }
                return false;
        }

    }

    protected void addedUnique(X x) {
        list.add(x);
    }

    protected Set<X> newSet(int cap) {
        return new UnifiedSet<>(cap,0.99f);
        //return new HashSet(cap, 0.99f);
    }
    private void clearSet() {
        set.clear();
        //set = emptySet();
    }

    @Override
    public final Iterator<X> iterator() {
        return size() == 0 ? Collections.emptyListIterator() : new FasterList.FasterListIterator(list);
    }

    /** use if remove() not needed */
    public final Iterator<X> iteratorReadOnly() {
        int s = size();
        switch (s) {
            case 0: return Collections.emptyListIterator();
            case 1: return Iterators.singletonIterator(get(0));
            default: return ArrayIterator.iterateN(list.array(), s);
        }
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

//            switch (s) {
//                case 0:
//                    set = emptySet();
//                    break;
//            }
        }

        return removed;
    }


    @Override
    public final void clear() {

        if (list.clearIfChanged())
            clearSet();

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

    public final ArrayHashSet<X> with(X x) {
        add(x);
        return this;
    }

    /** removes the last item in the list, or null if empty */
    public @Nullable X poll() {
        X x = list.poll();
        if (x != null)
            set.remove(x);
        return x;
    }

    public void replace(int i, X y) {
        set.remove(list.get(i));
        list.setFast(i, y);
        set.add(y);
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
            if (!element.equals(lastElementProvided)) {
                if (set.contains(element)) {

                    throw new IllegalArgumentException("Cannot setAt already-present element in a different position in ArrayHashSet.");
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


    public static ArrayHashSet EMPTY = new ArrayHashSet(0) {
        @Override
        public boolean add(Object x) {
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


    };
}
