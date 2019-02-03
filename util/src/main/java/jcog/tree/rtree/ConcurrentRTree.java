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
public class ConcurrentRTree<X> extends LambdaStampedLock implements Space<X> {

    private final RTree<X> tree;

    public ConcurrentRTree(RTree<X> tree) {
        super();
        this.tree = tree;
    }


    @Override
    public boolean OR(Predicate<X> o) {
        return root().OR(o);
    }

    @Override
    public boolean AND(Predicate<X> o) {
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
    public int containedToArray(HyperRegion rect, X[] t) {
        return read(() -> tree.containedToArray(rect, t));
    }

    @Override
    public final Node<X> root() {
        return tree.root();
    }

    /**
     * Blocking locked addAt
     *
     * @param t - entry to addAt
     */
    @Override
    public boolean add(X t) {
        return write(() -> tree.add(t));
    }


    /**
     * prefer this instead of add() in multithread environments, because it elides what might ordinarily involve a lock wait
     */
    @Override
    public void addAsync(X t) {
        add(t);
    }

    @Override
    public void removeAsync(X t) {
        remove(t);
    }


    @Override
    public boolean remove(X x) {
        return write(() -> tree.remove(x));
    }

    public void removeAll(Iterable<? extends X> t) {
        write(() -> t.forEach(this::remove));
    }


    public void read(Consumer<RTree<X>> x) {
        read(() -> x.accept(tree));
    }

    /**
     * doesnt lock, use at your own risk
     */
    public void readDirect(Consumer<Space<X>> x) {
        x.accept(tree);
    }

    public void write(Consumer<Space<X>> x) {
        write(() -> x.accept(tree));
    }

    public boolean write(Predicate<Space<X>> x) {
        return write(() -> x.test(tree));
    }

    public void readOptimistic(Consumer<Space<X>> x) {
        readOptimistic(() -> {
            x.accept(tree);
        });
    }

    @Override
    public HyperRegion bounds(X task) {
        return tree.bounds(task);
    }


    /**
     * Blocking locked update
     *
     * @param told - entry to update
     * @param tnew - entry with new value
     */
    @Override
    public boolean replace(X told, X tnew) {
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
    public void forEach(Consumer<? super X> consumer) {
        read(() -> tree.forEach(consumer));
    }

    public void forEachOptimistic(Consumer<? super X> consumer) {
        readOptimistic(() -> tree.forEach(consumer));
    }

    @Override
    public boolean containsWhile(HyperRegion rect, Predicate<X> t) {
        read(() -> tree.containsWhile(rect, t));
        return false;
    }

    @Override
    public boolean intersectsWhile(HyperRegion rect, Predicate<X> t) {
        read(() -> tree.intersectsWhile(rect, t));
        return false;
    }

    /**
     * warning: not locked
     */
    @Override
    public Stream<X> stream() {
        return root().streamValues().filter(Objects::nonNull);
    }

    /**
     * warning: not locked
     */
    @Override
    public Iterator<X> iterator() {
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
    public boolean contains(X t, HyperRegion b, Spatialization<X> model) {
        return read(() -> tree.contains(t, b, model));
    }

    @Override
    public boolean contains(X t) {
        return read(() -> tree.contains(t));
    }


    public final Spatialization<X> model() {
        return tree.model;
    }
}
