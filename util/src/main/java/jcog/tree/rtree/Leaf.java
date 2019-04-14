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
public class Leaf<X> extends AbstractNode<X> {

    public final X[] data;


    protected Leaf(int mMax) {
        this((X[]) new Object[mMax]);
    }

    public Leaf(X[] emptyArray) {
        this.data = emptyArray;
        this.size = 0;
        this.bounds = null;
    }

    public Leaf(Spatialization<X> model, X[] sortedMbr, int from, int to) {
        this.data = Arrays.copyOfRange(sortedMbr, from, to);
        this.size = (short) data.length;
        this.bounds = model.mbr(data);
    }

    @Override
    public boolean intersectingNodes(HyperRegion rect, Predicate<Node<X>> t, Spatialization<X> model) {
        if (rect.intersects(bounds))
            return t.test(this);
        else
            return true;
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
        return ArrayIterator.iterateN(data, size);
    }

    @Override
    public Stream<X> streamValues() {
        return ArrayIterator.streamNonNull(data, size); //TODO null-terminator iterator eliding 'size'
    }

    @Override
    public Stream<?> streamLocal() {
        return streamValues();
    }

    @Override
    public Iterator<?> iterateLocal() {
        return iterateValues();
    }

    public final X get(int i) {
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
    public Node<X> add(/*@NotNull*/ final X x, boolean addOrMerge, Spatialization<X> model, boolean[] added) {

        final HyperRegion B = model.bounds(x);

        if (addOrMerge) {
            boolean mightContain = size > 0 && (/* TODO model.mergeEqualOnly() ? bounds.contains(tb) : */ bounds.intersects(B));
            if (mightContain) {
                for (int i = 0, s = size; i < s; i++) {
                    X y = data[i];
                    if (y == x)
                        return null; //identical instance found

                    X xy = model.merge(y, x);
                    if (xy != null) {
                        if (xy != y) {

                            data[i] = xy;

                            bounds = Util.maybeEqual(bounds, HyperRegion.mbr(model, data));

//                            HyperRegion xtb = model.bounds(xy);
//                            if (!bounds.contains(xtb)) {
////                                //TODO use grow()
//                            HyperRegion b = i == 0 ? xtb : model.bounds(data[0]);
//                            for (int k = 1; k < s; k++)
//                                b = b.mbr(i == k ? xtb : model.bounds(data[k]));
//                            bounds = b;
////                            }
                        }
                        return null;
                    }
                }
            }

            added[0] = true;

            if (size < data.length) {

                data[this.size++] = x;
                grow(B);

                return this;
            } else {
                return model.split(x, this);
            }

        } else {
            return contains(x, B, model) ? null : this;
        }
    }


    @Override
    public boolean AND(Predicate<X> p) {
        for (X x : data) {
            if (x != null) {
                if (!p.test(x))
                    return false;
            } else
                break; //null-terminator reached
        }
        return true;
    }

    @Override
    public boolean OR(Predicate<X> p) {
        for (X x : data) {
            if (x != null) {
                if (p.test(x))
                    return true;
            } else
                break; //null-terminator reached
        }
        return false;
    }

    @Override
    public boolean contains(X x, HyperRegion b, Spatialization<X> model) {

        final int s = size;
        if (s > 0 && bounds.contains(b)) {

            X[] data = this.data;
            for (int i = 0; i < s; i++) {
                X t = data[i];
                if (x == t || model.mergeContain(x, t)) {
                    return true;
                }
            }
        }
        return false;
    }


    @Override
    public Node<X> remove(final X x, HyperRegion xBounds, Spatialization<X> model, boolean[] removed) {

        final int size = this.size;
        assert (size > 0); //        if (size == 0)            return this;
        X[] data = this.data;
        int i;
        for (i = 0; i < size; i++) {
            if (x.equals(data[i]))
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
            bounds = Util.maybeEqual(bounds, HyperRegion.mbr(model.bounds, data, this.size));
            return this;
        } else {
            bounds = null;
            return null;
        }

    }

    @Override
    public Node<X> replace(final X told, HyperRegion oldBounds, final X tnew, Spatialization<X> model) {
        final int s = size;
        if (s > 0 && bounds.contains(oldBounds)) {
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
        }
        return this;
    }


    @Override
    public boolean intersecting(HyperRegion rect, Predicate<X> t, Spatialization<X> model) {
        short s = this.size;
        if (s > 0 && rect.intersects(bounds)) {
            boolean containsAll = s > 1 && rect.contains(bounds);
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
        if (s > 0 && rect.intersects(bounds)) {
            boolean containsAll = s > 1 && rect.contains(bounds);
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
    public final boolean isLeaf() {
        return true;
    }

    @Override
    public final void forEach(Consumer<? super X> consumer) {
//        short s = this.size;
//        if (s > 0) {
//            X[] data = this.data;
//            for (int i = 0; i < s; i++) {
//                X d = data[i];
//                if (d != null)
//                    consumer.accept(d);
//            }
//        }
        for (X x : data)
            if (x != null)
                consumer.accept(x);
            else
                break; //null-terminator reached
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
     * @param a     left node
     * @param b     right node
     * @param x     data entry to be added
     * @param model
     */
    public final void transfer(final Node<X> a, final Node<X> b, final X x, Spatialization<X> model) {

        final HyperRegion xReg = model.bounds(x);
        double tCost = xReg.cost();

        final HyperRegion aReg = a.bounds();
        final HyperRegion aMbr = aReg != null ? xReg.mbr(aReg) : xReg;
        double axCost = aMbr.cost();
        final double aCostInc = Math.max(axCost - ((/*aReg!=null ? */aReg.cost() /*: 0*/) + tCost), 0.0);

        final HyperRegion bReg = b.bounds();
        final HyperRegion bMbr = xReg.mbr(bReg);
        double bxCost = bMbr.cost();
        final double bCostInc = Math.max(bxCost - ((/*bReg!=null ? */ bReg.cost()/* : 0*/) + tCost), 0.0);

        Node<X> target;
        double eps = model.epsilon();
        if (Util.equals(aCostInc, bCostInc, eps)) {
            if (Util.equals(axCost, bxCost, eps)) {

                final double aMbrMargin = aMbr.perimeter(), bMbrMargin = bMbr.perimeter();

                if (Util.equals(aMbrMargin, bMbrMargin, eps)) {

                    target = ((a.size() <= b.size()) ? a : b);
                } else {
                    target = (aMbrMargin <= bMbrMargin) ? a : b;
                }
            } else {
                target = (axCost <= bxCost) ? a : b;
            }
        } else {
            target = (aCostInc <= bCostInc) ? a : b;
        }

        boolean[] added = new boolean[1];
        target.add(x, true, model, added);
        //assert (added[0]); <-- TODO check this
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
