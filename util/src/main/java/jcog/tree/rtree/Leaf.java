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
         *      http://www.apache.org/licenses/LICENSE-2.0
         *
         * Unless required by applicable law or agreed to in writing, software
         * distributed under the License is distributed on an "AS IS" BASIS,
         * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
         * See the License for the specific language governing permissions and
         * limitations under the License.
         * #L%
         */

import jcog.Util;
import jcog.list.ArrayIterator;
import jcog.tree.rtree.util.CounterNode;
import jcog.tree.rtree.util.Stats;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Node that will contain the data entries. Implemented by different type of SplitType leaf classes.
 * <p>
 * Created by jcairns on 4/30/15.
 * <p>
 */
public class Leaf<T> extends AbstractNode<T, T> {

    public final T[] data;


    protected Leaf(int mMax) {
        this((T[]) new Object[mMax]);
    }

    private Leaf(T[] arrayInit) {
        this.bounds = null;
        this.data = arrayInit;
        this.size = 0;
    }

    @Override
    public Stream<T> stream() {
        return streamNodes();
    }

    @Override
    public Iterator<T> iterator() {
        return iterateNodes();
    }

    @Override
    public Iterator<T> iterateNodes() {
        return ArrayIterator.get(data, size);
    }

    @Override
    public T get(int i) {
        return data[i];
    }

    public double variance(int dim, Spatialization<T> model) {
        int s = size();
        if (s < 2)
            return 0;
        double mean = bounds().center(dim);
        double sumDiffSq = 0;
        for (int i = 0; i < s; i++) {
            T c = get(i);
            if (c == null) continue;
            double diff = model.bounds(c).center(dim) - mean;
            sumDiffSq += diff * diff;
        }
        return sumDiffSq / s - 1;
    }


    @Override
    public Node<T, ?> add(/*@NotNull*/ final T t, Nodelike<T> parent, /*@NotNull*/ Spatialization<T> model, boolean[] added) {

        final HyperRegion tb = model.bounds(t);
        boolean ctm = contains(t, tb, model);

        if (parent != null && !ctm) {
            Node<T, ?> next;

            if (size < model.max) {
                grow(tb);


                //TEMPORARY
                for (Object x : data) {
                    if (x == t) {
                        String s = "uplicate: " + x + " " + t;
                        System.out.println(s);
                        throw new RuntimeException(s);
                    }
                }
                //TEMPORARY

                data[size++] = t;

                next = this;
            } else {
                next = model.split(t, this);
            }

            added[0] = true;

            return next;
        } else {

            return (parent == null && ctm) ? null : this;
        }
    }




    @Override
    public boolean AND(Predicate<T> p) {
        T[] data = this.data;
        for (int i = 0; i < size; i++)
            if (!p.test(data[i]))
                return false;
        return true;
    }

    @Override
    public boolean OR(Predicate<T> p) {
        T[] data = this.data;
        for (int i = 0; i < size; i++)
            if (p.test(data[i]))
                return true;
        return false;
    }

    @Override
    public boolean contains(T t, HyperRegion b, Spatialization<T> model) {

        final int s = size;
        if (s > 0) {
            if (!bounds.contains(b))
                return false;

            T[] data = this.data;
            for (int i = 0; i < s; i++) {
                T d = data[i];
                if (t.equals(d)) {
                    model.merge(d, t);
                    return true;
                }
            }
        }
        return false;
    }


    @Override
    public Node<T, ?> remove(final T t, HyperRegion xBounds, Spatialization<T> model, boolean[] removed) {

        final int size = this.size;
        if (size == 0)
            return this;
        T[] data = this.data;
        int i;
        for (i = 0; i < size; i++) {
            T d = data[i];
            if (t.equals(d))
                break; //found
        }
        if (i == size)
            return this; //not found

        final int j = i + 1;
        if (j < size) {
            final int nRemaining = size - j;
            System.arraycopy(data, j, data, i, nRemaining);
            Arrays.fill(data, size - 1, size, null);
        } else {
            Arrays.fill(data, i, size, null);
        }

        this.size--;
        removed[0] = true;

        bounds = this.size > 0 ? HyperRegion.mbr(model.bounds, data, this.size) : null;

        return this;

    }

    @Override
    public Node<T, ?> update(final T told, final T tnew, Spatialization<T> model) {
        final int s = size;
        if (s <= 0)
            return this;

        T[] data = this.data;
        HyperRegion r = null;
        for (int i = 0; i < s; i++) {
            if (data[i].equals(told)) {
                data[i] = tnew;
            }

            r = i == 0 ? model.bounds(data[0]) : r.mbr(model.bounds(data[i]));
        }

        this.bounds = r;

        return this;
    }


    @Override
    public boolean intersecting(HyperRegion rect, Predicate<T> t, Spatialization<T> model) {
        short s = this.size;
        if (s > 0) {
            HyperRegion r = this.bounds;
            if (r == null) return true;

            if (rect.intersects(r)) {
                boolean fullyContained = s > 1 && rect.contains(r); //if it contains this node, then we dont need to test intersection for each child. but only do the test if s > 1
                T[] data = this.data;
                for (int i = 0; i < s; i++) {
                    T d = data[i];
                    if (d != null && (fullyContained || rect.intersects(model.bounds(d))) && !t.test(d))
                        return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean containing(HyperRegion rect, Predicate<T> t, Spatialization<T> model) {
        short s = this.size;
        if (s > 0) {
            HyperRegion r = this.bounds;
            if (r!=null && rect.intersects(r)) { //not sure why but it seems this has to be intersects and not contains
                boolean fullyContained = s > 1 && rect.contains(r); //if it contains this node, then we dont need to test intersection for each child. but only do the test if s > 1
                T[] data = this.data;
                for (int i = 0; i < s; i++) {
                    T d = data[i];
                    if (d != null && (fullyContained || rect.contains(model.bounds(d))) && !t.test(d))
                        return false;
                }
            }
        }
        return true;
    }


    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public void forEach(Consumer<? super T> consumer) {
        T[] data = this.data;
        short s = this.size;
        for (int i = 0; i < s; i++) {
            T d = data[i];
            if (d != null)
                consumer.accept(d);
        }
    }


    @Override
    public void collectStats(Stats stats, int depth) {
        if (depth > stats.getMaxDepth()) {
            stats.setMaxDepth(depth);
        }
        stats.countLeafAtDepth(depth);
        stats.countEntriesAtDepth(size, depth);
    }

    /**
     * Figures out which newly made leaf node (see split method) to add a data entry to.
     *
     * @param l1Node left node
     * @param l2Node right node
     * @param t      data entry to be added
     * @param model
     */
    public final void transfer(final Node<T, T> l1Node, final Node<T, T> l2Node, final T t, Spatialization<T> model) {

        final HyperRegion tRect = model.bounds(t);
        double tCost = tRect.cost();

        final HyperRegion l1Region = l1Node.bounds();
        final HyperRegion l1Mbr = l1Region.mbr(tRect);
        double l1c = l1Mbr.cost();
        final double l1CostInc = Math.max(l1c - (l1Region.cost() + tCost), 0.0);

        final HyperRegion l2Region = l2Node.bounds();
        final HyperRegion l2Mbr = l2Region.mbr(tRect);
        double l2c = l2Mbr.cost();
        final double l2CostInc = Math.max(l2c - (l2Region.cost() + tCost), 0.0);

        Node<T, T> target;
        if (Util.equals(l1CostInc, l2CostInc, RTree.EPSILON)) {
            if (Util.equals(l1c, l2c, RTree.EPSILON)) {
                final double l1MbrMargin = l1Mbr.perimeter();
                final double l2MbrMargin = l2Mbr.perimeter();
                if (Util.equals(l1MbrMargin, l2MbrMargin, RTree.EPSILON)) {
                    // break tie by preferring the smaller smaller
                    target = ((l1Node.size() <= l2Node.size()) ? l1Node : l2Node);
                } else if (l1MbrMargin <= l2MbrMargin) {
                    target = l1Node;
                } else {
                    target = l2Node;
                }
            } else {
                if (l1c <= l2c) {
                    target = l1Node;
                } else {
                    target = l2Node;
                }
            }
        } else {
            if (l1CostInc <= l2CostInc) {
                target = l1Node;
            } else {
                target = l2Node;
            }
        }

        boolean[] added = new boolean[1];
        target.add(t, this, model, added);
        assert (added[0]);
    }

    @Override
    public Node<T, Object> instrument() {
        return new CounterNode(this);
    }

    @Override
    public String toString() {
        return "Leaf" + '{' + bounds + 'x' + size + '}';
    }
}
