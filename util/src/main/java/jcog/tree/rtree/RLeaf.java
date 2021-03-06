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
import jcog.tree.rtree.util.CounterRNode;
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
public class RLeaf<X> extends AbstractRNode<X,X> {


    RLeaf(int mMax) {
        this((X[]) new Object[mMax]);
    }

    public RLeaf(X[] xx) {
        super(xx);
    }

    public RLeaf(Spatialization<X> model, X[] sortedMbr, int from, int to) {
        super(Arrays.copyOfRange(sortedMbr, from, to));
        this.size = (short) data.length;
        this.bounds = model.mbr(data);
    }

    @Override
    public boolean intersectingNodes(HyperRegion rect, Predicate<RNode<X>> t, Spatialization<X> model) {
        return !rect.intersects(bounds) || t.test(this);
    }

    @Override
    public Iterator<RNode<X>> iterateNodes() {
        return Util.emptyIterator;
    }

    @Override
    public Stream<RNode<X>> streamNodes() {
        return Stream.empty();
    }

    @Override
    public final Stream<RNode<X>> streamNodesRecursively() {
        return Stream.of(this);
    }

    @Override
    public Iterator<X> iterateValues() {
        return iterateLocal();
    }

    @Override
    public Stream<X> streamValues() {
        return streamLocal();
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
    public RNode<X> add(RInsertion<X> x) {
        int s = (int) size;
        if (s > 0 && x.maybeContainedBy(bounds)) {
            X[] data = this.data;

            //test for identity/equality
            X xx = x.x;
            for (int i = 0; i < s; i++) {
                X y = data[i];
                if (xx.equals(y)) {
                    x.mergeEqual(y);
                    return null; //identical instance found
                }
            }

            if (x.model.canMerge()) {
                for (int i = 0; i < s; i++) {
                    X y = data[i];
                    X xy = x.merge(y);
                    if (xy != null) {
                        merged(xy, x, y, i);
                        return null;
                    }
                }
            }
        }

        return x.addOrMerge ? insert(x) : this;
    }


    private void merged(X merged, RInsertion<X> x, X existing, int i) {
        if (merged == existing)
            return;

        data[i] = merged;

        Spatialization<X> m = x.model;

        if (!m.bounds(existing).equals(m.bounds(merged))) {
            //recompute bounds
            HyperRegion newBounds = HyperRegion.mbr(m, data);
            if (!bounds.equals(newBounds)) {
                this.bounds = newBounds;
                x.stretched = true;
            }
        }
    }

    RNode<X> insert(RInsertion<X> r) {
        Spatialization<X> model = r.model;
        RNode<X> result = insert(r.x, r.bounds, model);
        r.inserted();
        model.commit(this);
        return result;
    }

    RNode<X> insert(X x, Spatialization<X> model) {
        return insert(x, model.bounds(x), model);
    }

    RNode<X> insert(X x, HyperRegion bounds, Spatialization<X> model) {
        if ((int) size < data.length) {

            data[(int) this.size++] = x;
            grow(bounds);

            return this;
        } else {
            return model.split(x, this);
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

        int s = (int) size;
        if (s > 0 && bounds.contains(b)) {

            X[] data = this.data;
            for (int i = 0; i < s; i++) {
                X t = data[i];
                if (x == t || model.mergeContain(x, t))
                    return true;
            }
        }
        return false;
    }


    @Override
    public RNode<X> remove(X x, HyperRegion xBounds, Spatialization<X> model, int[] removed) {

        int size = (int) this.size;
        if (size > 1 && !bounds.contains(xBounds))
            return this; //not found

        X[] data = this.data;
        int i;
        for (i = 0; i < size; i++) {
            if (x.equals(data[i]))
                break;
        }
        if (i == size)
            return this; //not found
        else {
            int j = i + 1;
            if (j < size) {
                int nRemaining = size - j;
                System.arraycopy(data, j, data, i, nRemaining);
                Arrays.fill(data, size - 1, size, null);
            } else {
                Arrays.fill(data, i, size, null);
            }

            this.size--;
            removed[0]++;

            if ((int) this.size > 0) {
                bounds = Util.maybeEqual(bounds, model.mbr(data));
                return this;
            } else {
                bounds = null;
                return null;
            }
        }
    }

    @Override
    public RNode<X> replace(X told, HyperRegion oldBounds, X tnew, Spatialization<X> model) {
        int s = (int) size;
        if (s > 0 && bounds.contains(oldBounds)) {
            X[] data = this.data;
            HyperRegion r = null;
            for (int i = 0; i < s; i++) {
                X d = data[i];
                if (d.equals(told))
                    data[i] = tnew;

                r = i == 0 ? model.bounds(data[0]) : r.mbr(model.bounds(data[i]));
            }

            this.bounds = r;
        }
        return this;
    }


    @Override
    public boolean intersecting(HyperRegion rect, Predicate<X> t, Spatialization<X> model) {
        short s = this.size;
        if ((int) s > 0 && rect.intersects(bounds)) {
            boolean containsAll = (int) s > 1 && rect.contains(bounds);
            X[] data = this.data;
            for (int i = 0; i < (int) s; i++) {
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
        if ((int) s > 0 && rect.intersects(bounds)) {
            boolean fullyContained = (int) s > 1 && rect.contains(bounds);
            X[] data = this.data;
            for (int i = 0; i < (int) s; i++) {
                X d = data[i];
                if (/*d != null && */(fullyContained || rect.contains(model.bounds(d))) && !t.test(d))
                    return false;
            }
        }
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
        stats.countEntriesAtDepth((int) size, depth);
    }

    /**
     * Figures out which newly made leaf node (see split method) to add a data entry to.
     *
     * @param a     left node
     * @param b     right node
     * @param x     data entry to be added
     * @param model
     */
    public final void transfer(RLeaf<X> a, RLeaf<X> b, X x, Spatialization<X> model) {

        HyperRegion xReg = model.bounds(x);
        double tCost = xReg.cost();

        HyperRegion aReg = a.bounds();
        HyperRegion aMbr = aReg != null ? xReg.mbr(aReg) : xReg;
        double axCost = aMbr.cost();
        double aCostInc = Math.max(axCost - ((/*aReg!=null ? */aReg.cost() /*: 0*/) + tCost), 0.0);

        HyperRegion bReg = b.bounds();
        HyperRegion bMbr = xReg.mbr(bReg);
        double bxCost = bMbr.cost();
        double bCostInc = Math.max(bxCost - ((/*bReg!=null ? */ bReg.cost()/* : 0*/) + tCost), 0.0);

        RLeaf<X> target;
        double eps = Spatialization.epsilon();
        if (Util.equals(aCostInc, bCostInc, eps)) {
            if (Util.equals(axCost, bxCost, eps)) {

                double aMbrMargin = aMbr.perimeter(), bMbrMargin = bMbr.perimeter();

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


        //target.add(new RInsertion<>(x, true, model));
        target.insert(x, model);
        //assert (added[0]); <-- TODO check this
    }

    @Override
    public RNode<X> instrument() {
        return new CounterRNode(this);
    }

    @Override
    public String toString() {
        return "Leaf" + '{' + bounds + 'x' + size + '}';
    }
}
