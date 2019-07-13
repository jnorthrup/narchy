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


import com.google.common.base.Joiner;
import com.google.common.collect.Iterators;
import jcog.Util;
import jcog.data.iterator.ArrayIterator;
import jcog.tree.rtree.util.CounterRNode;
import jcog.tree.rtree.util.Stats;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * RTree node that contains leaf nodes
 * <p>
 * Created by jcairns on 4/30/15.
 */
public class RBranch<X> extends AbstractRNode<X> {

    public final RNode<X>[] data;

    protected RBranch(int cap, RNode<X>[] data) {
        //assert (cap >= 2);
        this.data = data.length == cap ? data : Arrays.copyOf(data, cap); //TODO assert all data are unique; cache hash?
        this.size = (short) data.length;
        this.bounds = HyperRegion.mbr(data);
    }

    @Override
    public boolean contains(X x, HyperRegion b, Spatialization<X> model) {

        if (!this.bounds.contains(b))
            return false;

//        int s = size;
//        if (s > 0) {
//            Node<X>[] c = this.data;
            for (RNode c : data) {
//            for (int i = 0; i < s; i++) {
                if (c == null)
                    break; //null-terminator
                if (c.contains(x, b, model))
                    return true;

            }
//        }

        return false;
    }

    @Override
    public final RNode<X> get(int i) {
        return data[i];
    }

    /**
     * Add a new node to this branch's list of children
     *
     * @param n node to be added (can be leaf or branch)
     * @return position of the added node
     */
    private void addChild(final RNode<X> n) {
        data[this.size++] = n;
        HyperRegion b = this.bounds;
        this.bounds = Util.maybeEqual(b, b.mbr(n.bounds()));
    }


    @Override
    public final boolean isLeaf() {
        return false;
    }


    /**
     * Adds a data entry to one of the child nodes of this branch
     *
     * @param parent
     * @param x
     * @return Node that the entry was added to
     */
    @Override
    public RNode<X> add(RInsertion<X> x) {

        final RNode<X>[] data = this.data;

        boolean addOrMerge = x.isAddOrMerge(); //save now

        //1. test containment
        x.setAdd(false);
        if (x.maybeContainedBy(bounds)) {

            int s = this.size;
            boolean merged = false;
            for (int i = 0; i < s; i++) {
                RNode<X> ci = data[i];

                RNode<X> di = ci.add(x);

                if (di == null) {
                    merged = true;
                    break;
                } else if (ci!=di) {
                    data[i] = di;
                    merged = true;
                    break;
                }
            }

            if (merged) {
                //x.setMerged();
                if (x.stretched)
                    updateBounds();
                return null;
            }
        }


        if (!addOrMerge)
            return this;
        else {
            x.setAdd(true);
            return insert(x);
        }
    }

    @Nullable
    private RNode<X> insert(RInsertion<X> x) {

        if (size < data.length) {

            addChild(x.model.newLeaf().insert(x));
            grow(x.bounds);

            return this;

        } else {

            final int bestLeaf = chooseLeaf(x.bounds);

            HyperRegion before = data[bestLeaf].bounds();
            RNode<X> nextBest = data[bestLeaf].add(x);
            if (nextBest == null) {
                if (!before.equals(data[bestLeaf].bounds()))
                    updateBounds();
                return null; /*merged*/
            }

            //inline
            if (size < data.length && nextBest.size() == 2 && !nextBest.isLeaf()) {
                RNode<X>[] bc = ((RBranch<X>) nextBest).data;
                data[bestLeaf] = bc[0];
                data[size++] = bc[1];
            } else {
                data[bestLeaf] = nextBest;
            }

            updateBounds();

        }
        return this;
    }

    @Override
    public RNode<X> remove(final X x, HyperRegion xBounds, Spatialization<X> model, boolean[] removed) {

        if (size > 1 && !bounds().contains(xBounds))
            return this; //not here

        int nsize = this.size;
        for (int i = 0; i < nsize; i++) {
            RNode<X> y = data[i];
            @Nullable RNode<X> cAfter = y.remove(x, xBounds, model, removed);

            if (removed[0]) {

                data[i] = cAfter;

                if (cAfter == null) {
                    if (i < --size)
                        Arrays.sort(data, NullCompactingComparator);
                }


                switch (size) {
                    case 0:
                        bounds = null;
                        return null;
                    case 1:
                        bounds = null;
                        return data[0]; //reduce to only leaf
                    default: {
                        //TODO possibly rebalance

//                                if (Util.and((Node z) -> z instanceof Leaf, data)) {
//                                    int values = Util.sum((ToIntFunction<Node>) Node::size, data);
//                                    if (values <= model.max) {
//                                        Leaf<X> compacted = model.newLeaf();
//                                        int p = 0;
//                                        for (int k = 0, dataLength = size(); k < dataLength; k++) {
//                                            Node<X> z = data[k];
//                                            X[] data1 = ((Leaf<X>) z).data;
//                                            for (int j = 0, data1Length = z.size(); j < data1Length; j++) {
//                                                X zz = data1[j];
//                                                compacted.data[p++] = zz;
//                                                compacted.grow(model.bounds(zz));
//                                            }
//                                        }
//                                        compacted.size = (short) p;
//                                        return compacted;
//                                    }
//                                }

                        bounds = Util.maybeEqual(bounds, HyperRegion.mbr(data));
                            updateBounds();
                            return this;
//                            }

                    }

                }

            } else {
                assert (cAfter == y);
            }
        }


        return this;
    }

    private void updateBounds() {
//        Node<X>[] dd = this.data;
//        HyperRegion region = dd[0].bounds();
//        for (int j = 1; j < size; j++)
//            region = region.mbr(dd[j].bounds());
//        if (bounds == null || !bounds.equals(region))
//            this.bounds = region;
        bounds = Util.maybeEqual(bounds, HyperRegion.mbr(data));
    }

    @Override
    public RNode<X> replace(final X OLD, HyperRegion oldBounds, final X NEW, Spatialization<X> model) {

        short s = this.size;
        if (s > 0 && oldBounds.intersects(bounds)) {
            boolean found = false;

            RNode<X>[] cc = this.data;
            HyperRegion region = null;

            for (int i = 0; i < s; i++) {
                if (!found && oldBounds.intersects(cc[i].bounds())) {
                    cc[i] = cc[i].replace(OLD, oldBounds, NEW, model);
                    found = true;
                }
                region = i == 0 ? cc[0].bounds() : region.mbr(cc[i].bounds());
            }
            if (found) {
                this.bounds = Util.maybeEqual(bounds, region);
            }
        }
        return this;
    }


    private int chooseLeaf(final HyperRegion tRect) {
        RNode<X>[] cc = this.data;
        if (size > 0) {
            int bestNode = -1;

            double leastEnlargement = Double.POSITIVE_INFINITY;
            double leastPerimeter = Double.POSITIVE_INFINITY;

            short s = this.size;
            for (int i = 0; i < s; i++) {
                HyperRegion cir = cc[i].bounds();
                HyperRegion childMbr = tRect.mbr(cir);
                final double nodeEnlargement =
                        (cir != childMbr ? childMbr.cost() - (cir.cost() /* + tCost*/) : 0);

                int dc = Double.compare(nodeEnlargement, leastEnlargement);
                if (nodeEnlargement < leastEnlargement) {
                    leastEnlargement = nodeEnlargement;
                    leastPerimeter = childMbr.perimeter();
                    bestNode = i;
                } else if (dc == 0) {
                    double perimeter = childMbr.perimeter();
                    if (perimeter < leastPerimeter) {
                        leastEnlargement = nodeEnlargement;
                        leastPerimeter = perimeter;
                        bestNode = i;
                    }
                }

            }
            if (bestNode == -1) {
                throw new RuntimeException("rtree fault");
            }

            return bestNode;
        } else {


            throw new RuntimeException("shouldnt happen");
        }
    }


    @Override
    public void forEach(Consumer<? super X> consumer) {
        for (RNode<X> x : data) {
            if (x == null)
                break; //null terminator
            x.forEach(consumer);
        }
    }

    @Override
    public final void forEachLocal(Consumer c) {
        forEach(c);
//        for (Node x : data) {
//            if (x != null)
//                c.accept(x);
//            else
//                break; //null-terminator reached
//        }
    }
    @Override
    public boolean OR(Predicate<X> p) {
        for (RNode<X> x : data) {
            if (x == null) break; //null terminator
            if (x.OR(p))
                return true;
        }
        return false;
    }

    @Override
    public boolean AND(Predicate<X> p) {
        for (RNode<X> x : data) {
            if (x == null) break; //null terminator
            if (!x.AND(p))
                return false;
        }
        return true;
    }
    public boolean ANDlocal(Predicate<RNode<X>> p) {
        RNode<X>[] n = this.data;
        short s = this.size;
        for (int i = 0; i < s; i++) {
            if (!p.test(n[i]))
                return false;
        }
        return true;
    }
    public boolean ORlocal(Predicate<RNode<X>> p) {
        RNode<X>[] n = this.data;
        short s = this.size;
        for (int i = 0; i < s; i++) {
            if (p.test(n[i]))
                return true;
        }
        return false;
    }



    @Override
    public boolean containing(final HyperRegion rect, final Predicate<X> t, Spatialization<X> model) {
        HyperRegion b = this.bounds;
        if (b != null && rect.intersects(b)) {
            int s = size;
            for (int i = 0; i < s; i++) {
                RNode d = data[i];
//                if (d == null)
//                    continue;
                /*else */
                if (!d.containing(rect, t, model))
                    return false;
            }
        }
        return true;
    }

    @Override
    public boolean intersectingNodes(HyperRegion rect, Predicate<RNode<X>> t, Spatialization<X> model) {
        HyperRegion b = this.bounds;
        if (b != null && rect.intersects(b) && t.test(this)) {
            int s = size;
            for (int i = 0; i < s; i++) {
                if (!data[i].intersectingNodes(rect, t, model))
                    return false;
            }
        }
        return true;
    }

    @Override
    public boolean intersecting(HyperRegion rect, Predicate<X> t, Spatialization<X> model) {
        HyperRegion b = this.bounds;
        if (b != null && rect.intersects(b)) {
            int s = size;
            for (int i = 0; i < s; i++) {
                if (!data[i].intersecting(rect, t, model))
                    return false;
            }
        }
        return true;
    }

    @Override
    public Stream<RNode<X>> streamNodes() {
        return ArrayIterator.streamNonNull(data, size);
    }

    @Override
    public Stream<X> streamValues() {
        return streamNodes().flatMap(RNode::streamValues
                //x -> x != null ?
                        //((Node) x).streamValues()
        //: Stream.empty()
        );
    }

    @Override
    public Iterator<X> iterateValues() {
        return Iterators.concat(Iterators.transform(iterateLocal(), RNode::iterateValues));
    }

    @Override
    public Iterator<RNode<X>> iterateLocal() {
        return ArrayIterator.iterateN(data, size);
    }

    @Override
    public Stream<RNode<X>> streamLocal() {
        return streamNodes();
    }

    @Override
    public void collectStats(Stats stats, int depth) {
        for (int i = 0; i < size; i++)
            data[i].collectStats(stats, depth + 1);
        stats.countBranchAtDepth(depth);
    }

    @Override
    public RNode<X> instrument() {
        for (int i = 0; i < size; i++)
            data[i] = data[i].instrument();
        return new CounterRNode(this);
    }

    @Override
    public String toString() {
        return "Branch" + '{' + bounds + 'x' + size + ":\n\t" + Joiner.on("\n\t").skipNulls().join(data) + "\n}";
    }


    private static final Comparator NullCompactingComparator = (o1, o2) -> {
        if (o1 == o2) {
            return 0;
        }
        if (o1 == null) {
            return 1;
        }
        if (o2 == null) {
            return -1;
        }
        return Integer.compare(
                System.identityHashCode(o1),
                System.identityHashCode(o2)
        );
    };

}