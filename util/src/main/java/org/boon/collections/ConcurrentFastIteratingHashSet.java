/*
 * Copyright 2013-2014 Richard M. Hightower
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * __________                              _____          __   .__
 * \______   \ ____   ____   ____   /\    /     \ _____  |  | _|__| ____    ____
 *  |    |  _//  _ \ /  _ \ /    \  \/   /  \ /  \\__  \ |  |/ /  |/    \  / ___\
 *  |    |   (  <_> |  <_> )   |  \ /\  /    Y    \/ __ \|    <|  |   |  \/ /_/  >
 *  |______  /\____/ \____/|___|  / \/  \____|__  (____  /__|_ \__|___|  /\___  /
 *         \/                   \/              \/     \/     \/       \//_____/
 *      ____.                     ___________   _____    ______________.___.
 *     |    |____ ___  _______    \_   _____/  /  _  \  /   _____/\__  |   |
 *     |    \__  \\  \/ /\__  \    |    __)_  /  /_\  \ \_____  \  /   |   |
 * /\__|    |/ __ \\   /  / __ \_  |        \/    |    \/        \ \____   |
 * \________(____  /\_/  (____  / /_______  /\____|__  /_______  / / ______|
 *               \/           \/          \/         \/        \/  \/
 */

package org.boon.collections;


import jcog.util.ArrayIterator;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * TODO this is untested
 *
 * TODO it would be better to use a plain Array as the cached
 * "linked list" but this must be synchronized with the
 * ConcurrentHashMap's modifications..
 * which is possible but requires more care than what is done here
 */
public class ConcurrentFastIteratingHashSet<T> extends AbstractSet<T> {

    final T[] emptyArray;
    volatile T[] list = null;
    final Map<T,T> set = new ConcurrentHashMap<>() {

        /** without synchronizing this entire method, the best this can do is
         * a near atomic invalidation of the list after the hashmap method returns */
        @Override
        public T putIfAbsent(T key, T value) {
            T r;
            if ((r = super.putIfAbsent(key, value)) == null) {
                list = null;
                return null;
            }
            return r;
        }

        /** without synchronizing this entire method, the best this can do is
         * a near atomic invalidation of the list after the hashmap method returns */
        @Override
        public T remove(@NotNull Object key) {
            T r = super.remove(key);
            if (r != null) {
                list = null;
            }
            return r;
        }

        @Override
        public void clear() {
            super.clear();
            list = null;
        }
    };

    public ConcurrentFastIteratingHashSet(T[] emptyArray) {
        this.emptyArray = emptyArray;
    }

    @Override
    public int size() {
        return set.size();
    }

    @Override
    public boolean isEmpty() {
        return set.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return set.containsKey(o);
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        T[] x = toArray();
        for (T t : x)
            action.accept(t);
    }

    @Override
    public Iterator<T> iterator() {
        return new ArrayIterator(toArray());
    }

    @Override
    public T[] toArray() {
        T[] x = list;
        if (x == null) {
            return this.list = set.keySet().toArray(emptyArray);
        } else {
            return x;
        }
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean add(T t) {
        return set.putIfAbsent(t,t)==null;
    }

    @Override
    public boolean remove(Object o) {
        return set.remove(o)!=null;
    }

    @Override
    public void clear() {
        set.clear();
    }
}
