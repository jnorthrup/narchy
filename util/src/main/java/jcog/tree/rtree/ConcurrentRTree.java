package jcog.tree.rtree;

/*
 * #%L
 * Conversant RTree
 * ~~
 * Conversantmedia.com © 2016, Conversant, Inc. Conversant® is a trademark of Conversant, Inc.
 * ~~
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import jcog.tree.rtree.util.Stats;
import jcog.util.LambdaStampedLock;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Created by jcovert on 12/30/15.
 */
public class ConcurrentRTree<T> extends LambdaStampedLock implements Space<T> {

    private final RTree<T> tree;

    public ConcurrentRTree(RTree<T> tree) {
        super();
        this.tree = tree;
    }


    @Override
    public boolean OR(Predicate<T> o) {
        return root().OR(o);
    }

    @Override
    public boolean AND(Predicate<T> o) {
        return root().AND(o);
    }

    /**
     * Blocking locked search
     *
     * @param rect - HyperRect to search
     * @param t    - array to hold results
     * @return number of entries found
     */
    @Override
    public int containedToArray(HyperRegion rect, T[] t) {
        return read(() -> tree.containedToArray(rect, t));
    }

    @Override
    public final Node<T> root() {
        return tree.root();
    }

    /**
     * Blocking locked addAt
     *
     * @param t - entry to addAt
     */
    @Override
    public boolean add(T t) {
        return write(() -> tree.add(t));
    }


    /**
     * prefer this instead of add() in multithread environments, because it elides what might ordinarily involve a lock wait
     */
    @Override
    public void addAsync(T t) {
        add(t);
    }

    @Override
    public void removeAsync(T t) {
        remove(t);
    }


    @Override
    public boolean remove(T x) {
        return write(() -> tree.remove(x));
    }

    public void removeAll(Iterable<? extends T> t) {
        write(() -> t.forEach(this::remove));
    }


    public void read(Consumer<RTree<T>> x) {
        read(() -> x.accept(tree));
    }

    /**
     * doesnt lock, use at your own risk
     */
    public void readDirect(Consumer<Space<T>> x) {
        x.accept(tree);
    }

    public void write(Consumer<Space<T>> x) {
        write(() -> x.accept(tree));
    }

    public boolean write(Predicate<Space<T>> x) {
        return write(() -> x.test(tree));
    }

    public void readOptimistic(Consumer<Space<T>> x) {
        readOptimistic(() -> {
            x.accept(tree);
        });
    }

    @Override
    public HyperRegion bounds(T task) {
        return tree.bounds(task);
    }


    /**
     * Blocking locked update
     *
     * @param told - entry to update
     * @param tnew - entry with new value
     */
    @Override
    public boolean replace(T told, T tnew) {
        write(() -> tree.replace(told, tnew));
        return false;
    }

    @Override
    public int size() {
        return tree.size();

    }

    @Override
    public void clear() {
        write(tree::clear);
    }

    @Override
    public void forEach(Consumer<? super T> consumer) {
        read(() -> tree.forEach(consumer));
    }

    public void forEachOptimistic(Consumer<? super T> consumer) {
        readOptimistic(() -> tree.forEach(consumer));
    }

    @Override
    public boolean containsWhile(HyperRegion rect, Predicate<T> t) {
        read(() -> tree.containsWhile(rect, t));
        return false;
    }

    @Override
    public boolean intersectsWhile(HyperRegion rect, Predicate<T> t) {
        read(() -> tree.intersectsWhile(rect, t));
        return false;
    }

    /**
     * warning: not locked
     */
    @Override
    public Stream<T> stream() {
        return root().streamValues().filter(Objects::nonNull);
    }

    /**
     * warning: not locked
     */
    @Override
    public Iterator<T> iterator() {
        return stream().iterator();
    }

    @Override
    public Stats stats() {
        return read(tree::stats);
    }

    @Override
    public String toString() {
        return tree.toString();
    }

    @Override
    public boolean contains(T t, HyperRegion b, Spatialization<T> model) {
        return read(() -> tree.contains(t, b, model));
    }

    @Override
    public boolean contains(T t) {
        return read(() -> tree.contains(t));
    }


}
