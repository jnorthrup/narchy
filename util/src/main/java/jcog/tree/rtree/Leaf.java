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

import jcog.Util;
import jcog.data.iterator.ArrayIterator;
import jcog.tree.rtree.util.CounterNode;
import jcog.tree.rtree.util.Stats;

import java.util.Arrays;
import java.util.Collections;
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
public final class Leaf<X> extends AbstractNode<X> {

    public final X[] data;


    protected Leaf(int mMax) {
        this((X[]) new Object[mMax]);
    }

    private Leaf(X[] arrayInit) {
        this.bounds = null;
        this.data = arrayInit;
        this.size = 0;
    }


    @Override
    public Iterator iterateNodes() {
        return Collections.emptyIterator();
    }

    @Override
    public Stream streamNodes() {
        return Stream.empty();
    }

    @Override
    public Iterator<X> iterateValues() {
        return ArrayIterator.get(data, size);
    }

    @Override
    public Stream<X> streamValues() {
        return ArrayIterator.streamNonNull(data, size);
    }

    @Override
    public Stream<?> streamLocal() {
        return streamValues();
    }

    @Override
    public Iterator<?> iterateLocal() {
        return iterateValues();
    }

    public X get(int i) {
        return data[i];
    }

//    public double variance(int dim, Spatialization<X> model) {
//        int s = size();
//        if (s < 2)
//            return 0;
//        double mean = bounds().center(dim);
//        double sumDiffSq = 0;
//        for (int i = 0; i < s; i++) {
//            X c = get(i);
//            if (c == null) continue;
//            double diff = model.bounds(c).center(dim) - mean;
//            sumDiffSq += diff * diff;
//        }
//        return sumDiffSq / s - 1;
//    }


    @Override
    public Node<X> add(/*@NotNull*/ final X t, Nodelike<X> parent, /*@NotNull*/ Spatialization<X> model, boolean[] added) {

        final HyperRegion tb = model.bounds(t);

        if (parent != null) {
            boolean mightContain = size > 0 && bounds.contains(tb);

            Node<X> next;

            for (int i = 0, s = size; i < s; i++) {
                X x = data[i];

                if (x == null) continue;

                if (mightContain) {
                    if (x.equals(t)) {
                        model.merge(x, t);
                        return null;
                    }
                }
            }


            if (size < model.max) {

                grow(tb);
                data[this.size++] = t;

                next = this;
            } else {
                next = model.split(t, this);
            }

            added[0] = true;

            return next;
        } else {
            return contains(t, tb, model) ? null : this;
        }
    }


    @Override
    public boolean AND(Predicate<X> p) {
        X[] data = this.data;
        int s = size;
        for (int i = 0; i < s; i++) {
            X d = data[i];
            if (/*d!=null && */!p.test(d))
                return false;
        }
        return true;
    }

    @Override
    public boolean OR(Predicate<X> p) {
        X[] data = this.data;
        short s = this.size;
        for (int i = 0; i < s; i++) {
            X d = data[i];
            if (/*d!=null && */p.test(d))
                return true;
        }
        return false;
    }

    @Override
    public boolean contains(X x, HyperRegion b, Spatialization<X> model) {

        final int s = size;
        if (s > 0) {
            if (!bounds.contains(b))
                return false;

            X[] data = this.data;
            for (int i = 0; i < s; i++) {
                X t = data[i];
                if (t == null) continue;
                if (t.equals(x)) {
                    if (t != x)
                        model.merge(t, x);
                    return true;
                }
            }
        }
        return false;
    }


    @Override
    public Node<X> remove(final X x, HyperRegion xBounds, Spatialization<X> model, boolean[] removed) {

        final int size = this.size;
        assert(size>0); //        if (size == 0)            return this;
        X[] data = this.data;
        int i;
        for (i = 0; i < size; i++) {
            X d = data[i];
            if (x.equals(d))
                break; 
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

        if (this.size > 0) {
            bounds = HyperRegion.mbr(model.bounds, data, this.size);
            return this;
        } else {
            bounds = null;
            return null;
        }

    }

    @Override
    public Node<X> replace(final X told, final X tnew, Spatialization<X> model) {
        final int s = size;
        if (s <= 0)
            return this;

        X[] data = this.data;
        HyperRegion r = null;
        for (int i = 0; i < s; i++) {
            X d = data[i];
            if (/*d!=null && */d.equals(told)) {
                data[i] = tnew;
            }

            r = i == 0 ? model.bounds(data[0]) : r.mbr(model.bounds(data[i]));
        }

        this.bounds = r;

        return this;
    }


    @Override
    public boolean intersecting(HyperRegion rect, Predicate<X> t, Spatialization<X> model) {
        short s = this.size;
        if (s > 0) {
            boolean containsAll = s > 1 ? rect.contains(bounds) : false; 
            X[] data = this.data;
            for (int i = 0; i < s; i++) {
                X d = data[i];
                if (/*d != null && */ (containsAll || rect.intersects(model.bounds(d))) && !t.test(d))
                    return false;
            }
        }
        return true;
    }

    @Override
    public boolean containing(HyperRegion rect, Predicate<X> t, Spatialization<X> model) {
        short s = this.size;
        if (s > 0) {
            boolean containsAll = rect.contains(bounds); 
            X[] data = this.data;
            for (int i = 0; i < s; i++) {
                X d = data[i];
                if (/*d != null && */(containsAll || rect.contains(model.bounds(d))) && !t.test(d))
                    return false;
            }
        }
        return true;
    }


    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public void forEach(Consumer<? super X> consumer) {
        short s = this.size;
        if (s > 0) {
            X[] data = this.data;
            for (int i = 0; i < s; i++) {
                X d = data[i];
                if (d != null)
                    consumer.accept(d);
            }
        }
    }

    @Override
    public final void forEachLocal(Consumer c) {
        forEach(c);
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
     * @param x      data entry to be added
     * @param model
     */
    public final void transfer(final Node<X> l1Node, final Node<X> l2Node, final X x, Spatialization<X> model) {

        final HyperRegion tRect = model.bounds(x);
        double tCost = tRect.cost();

        final HyperRegion l1Region = l1Node.bounds();
        final HyperRegion l1Mbr = l1Region.mbr(tRect);
        double l1c = l1Mbr.cost();
        final double l1CostInc = Math.max(l1c - (l1Region.cost() + tCost), 0.0);

        final HyperRegion l2Region = l2Node.bounds();
        final HyperRegion l2Mbr = l2Region.mbr(tRect);
        double l2c = l2Mbr.cost();
        final double l2CostInc = Math.max(l2c - (l2Region.cost() + tCost), 0.0);

        Node<X> target;
        double eps = model.epsilon();
        if (Util.equals(l1CostInc, l2CostInc, eps)) {
            if (Util.equals(l1c, l2c, eps)) {

                final double l1MbrMargin = l1Mbr.perimeter(), l2MbrMargin = l2Mbr.perimeter();

                if (Util.equals(l1MbrMargin, l2MbrMargin, eps)) {
                    
                    target = ((l1Node.size() <= l2Node.size()) ? l1Node : l2Node);
                } else {
                    target = (l1MbrMargin <= l2MbrMargin) ? l1Node : l2Node;
                }
            } else {
                target = (l1c <= l2c) ? l1Node : l2Node;
            }
        } else {
            target = (l1CostInc <= l2CostInc) ? l1Node : l2Node;
        }

        boolean[] added = new boolean[1];
        target.add(x, this, model, added);
        assert (added[0]);
    }

    @Override
    public Node<X> instrument() {
        return new CounterNode(this);
    }

    @Override
    public String toString() {
        return "Leaf" + '{' + bounds + 'x' + size + '}';
    }
}
