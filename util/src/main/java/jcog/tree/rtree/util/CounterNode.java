package jcog.tree.rtree.util;

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

import jcog.tree.rtree.HyperRegion;
import jcog.tree.rtree.Node;
import jcog.tree.rtree.Nodelike;
import jcog.tree.rtree.Spatialization;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Created by jcovert on 6/18/15.
 */
public final class CounterNode<T> implements Node<T, Object> {
    public static int searchCount;
    public static int bboxEvalCount;
    private final Node<T, Object> node;

    public CounterNode(final Node<T, Object> node) {
        this.node = node;
    }


    @Override
    public Object get(int i) {
        return node.get(i);
    }

    @Override
    public Stream<T> stream() {
        return node.stream();
    }

    @Override
    public Stream<Object> streamNodes() {
        return node.streamNodes();
    }

    @Override
    public Iterator<Object> iterateNodes() {
        return node.iterateNodes();
    }

    @Override
    public boolean isLeaf() {
        return this.node.isLeaf();
    }

    @Override
    public HyperRegion bounds() {
        return this.node.bounds();
    }

    @Override
    public Node<T, ?> add(T t, Nodelike<T> parent, Spatialization<T> model, boolean[] added) {
        return this.node.add(t, parent!=null ? this : null, model, added);
    }

    @Override
    public Node<T, ?> remove(T x, HyperRegion xBounds, Spatialization<T> model, boolean[] removed) {
        return this.node.remove(x, xBounds, model, removed);
    }

    @Override
    public Node<T, ?> replace(T told, T tnew, Spatialization<T> model) {
        return this.node.replace(told, tnew, model);
    }

    @Override
    public boolean AND(Predicate<T> p) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public boolean OR(Predicate<T> p) {
        throw new UnsupportedOperationException("TODO");
    }


    @Override
    public boolean containing(HyperRegion rect, Predicate<T> t, Spatialization<T> model) {
        searchCount++;
        bboxEvalCount += this.node.size();
        return this.node.containing(rect, t, model);
    }






    @Override
    public int size() {
        return this.node.size();
    }

    @Override
    public void forEach(Consumer<? super T> consumer) {
        this.node.forEach(consumer);
    }

    @Override
    public boolean intersecting(HyperRegion rect, Predicate<T> t, Spatialization<T> model) {
        this.node.intersecting(rect, t, model);
        return false;
    }

    @Override
    public void collectStats(Stats stats, int depth) {
        this.node.collectStats(stats, depth);
    }

    @Override
    public Node<T, ?> instrument() {
        return this;
    }






    @Override
    public boolean contains(T t, HyperRegion b, Spatialization<T> model) {
        return node.contains(t, b, model);
    }
}
