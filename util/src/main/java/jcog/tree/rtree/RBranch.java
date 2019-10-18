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
import jcog.data.list.FasterList;
import jcog.tree.rtree.util.CounterRNode;
import jcog.tree.rtree.util.Stats;
import jcog.util.ArrayUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.System.arraycopy;

/**
 * RTree node that contains leaf nodes
 * <p>
 * Created by jcairns on 4/30/15.
 */
public class RBranch<X> extends AbstractRNode<X,RNode<X>> {


    protected RBranch(int cap) {
        super(new RNode[cap]);
        this.size = 0; this.bounds = null;
	}
    protected RBranch(int cap, RNode<X>[] data) {
        super(data.length == cap ? data : Arrays.copyOf(data, cap)); //TODO assert all data are unique; cache hash?
        //assert (cap >= 2);
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
        //            for (int i = 0; i < s; i++) {
        //null-terminator
        //        }

        return Arrays.stream(data).takeWhile(Objects::nonNull).anyMatch(c -> c.contains(x, b, model));
    }


    /**
     * Add a new node to this branch's list of children
     *
     * @param n node to be added (can be leaf or branch)
     * @return position of the added node
     */
    private void addChild(RInsertion<X> x) {
        RNode<X> n = x.model.newLeaf().insert(x);
        data[this.size++] = n;
        HyperRegion b = this.bounds;
        HyperRegion nb = n.bounds();
        this.bounds = b==null ? nb : Util.maybeEqual(b, b.mbr(nb));
        //grow(x.bounds);
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


        boolean addOrMerge = x.addOrMerge; //save here before anything

        //1. test containment
        if (addOrMerge)
            x.addOrMerge = false; //temporarily set to contain/merge mode

        RNode<X>[] data = this.data;
        if (x.maybeContainedBy(bounds)) {

            int s = this.size;
            boolean merged = false;
            for (int i = 0; i < s; i++) {
                RNode<X> ci = data[i];
                RNode<X> di = ci.add(x);
                if (ci!=di) {
                    if (di!=null)
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

        //INSERT
        x.addOrMerge = true; //restore to add mode


        int l = data.length;
        if (size < l) {

            addChild(x);

        } else {

            int bestLeaf = chooseLeaf(x.bounds);
            RNode<X> dbf = data[bestLeaf];

            HyperRegion before = dbf.bounds();
            RNode nextBest = dbf.add(x);
            if (nextBest == null) {
                if (!before.equals(dbf.bounds()))
                    updateBounds();
                return null; /*merged*/
            }

            //inline

            RNode bl;
            if (!(nextBest instanceof RLeaf) && size < l && nextBest.size() == 2) {
                RNode[] bc = ((RBranch<X>) nextBest).data;
                data[size++] = bc[1];
                bl = bc[0];
            } else {
                bl = nextBest;
            }
            data[bestLeaf] = bl;

            updateBounds();

        }
        return this;
    }

    @Override
    public RNode<X> remove(X x, HyperRegion xBounds, Spatialization<X> model, int[] removed) {

        int nsize = this.size;
        if (nsize > 1 && !bounds.contains(xBounds))
            return this; //not here

        RNode<X>[] data = this.data;
        for (int i = 0; i < nsize; i++) {
            RNode<X> nBefore = data[i];

            @Nullable RNode<X> nAfter = nBefore.remove(x, xBounds, model, removed);

            if (nAfter!=nBefore) {
                data[i] = nAfter;

                if (nAfter == null) {
                    --size;
                    switch (size) {
                        case 0:
                            //emptied
                            return null;
                        case 1:
                            //return the only remaining item
                            RNode<X> only = firstNonNull();
                            size = 0;
                            return only;
                        default:

                            //sort nulls to end
                            if (i < size) {
                                arraycopy(data, i + 1, data, i, size - i);
                                data[size] = null;
                            }
                            break;
                    }

                }

                if (nAfter instanceof RLeaf) {
                    RNode<X> next = consolidate(model, removed);
                    if (next != null)
                        return next;
                }

                updateBounds();
                return this;
            }
        }

        return this;
    }

    private RNode<X> firstNonNull() {
        for (RNode d : data) {
            if (d!=null)
                return d;
        }
        throw new UnsupportedOperationException();
    }

    /** consolidate under-capacity leafs by reinserting
     * @return*/
    private RNode<X> consolidate(Spatialization<X> model, int[] removed) {
        int childItems = 0, leafs = 0;
        for (RNode<X> x : data) {
            if (x == null) break;
            if (!(x instanceof RLeaf)) continue;
            short ls = ((RLeaf<X>) x).size;
            childItems += ls;
            leafs++;
        }
        if (childItems <=1 || leafs <= 1 || (childItems > (leafs - 1) * model.max)) {
            return null;
        }

        FasterList<X> xx = new FasterList<>(childItems);
        Consumer<X> adder = xx::addWithoutResize;
        for (int i = 0, dataLength = data.length; i < dataLength; i++) {
            RNode<X> x = data[i];
            if (x == null) break;
            if (!(x instanceof RLeaf)) continue;
            x.forEach(adder);
            data[i] = null;
            size--;
        }
        RNode<X> target;
        switch (size) {
            case 0:
                bounds = null;
                target = model.newLeaf();
                break;
            case 1:
                target = firstNonNull();
                size = 0;
                break;
            default:
                ArrayUtil.sortNullsToEnd(data);
                updateBounds();
                target = this;
                break;
        }


        for (X xxx : xx)
            target = reinsert(xxx, target, model, removed);

        return target;

    }

//    private RNode<X> reinsert(Spatialization<X> model, int[] removed) {
////        RNode<X>[] d = data.clone();
////        Arrays.fill(data, null);
////        size = 0;
//        RNode<X> t = null;
//
//        Iterator<X> v = iterateValues();
//        while (v.hasNext()) {
//            X x = v.next();
//            /*parent.  ? */
//
//            if (t == null) {
//                t = model.newLeaf();
//                ((RLeaf)t).data[0] = x;  //HACK
//                ((RLeaf)t).size = 1; //HACK
//                ((RLeaf)t).bounds = model.bounds(x);
//            } else {
//                t = reinsert(x, t, model, removed);
//            }
//
//
//        }
//
//
//        //return ArrayUtil.equalsIdentity(data, t.data) ? this : t;
//        return t;
//    }

    private static <X> RNode<X> reinsert(X x, RNode<X> target, Spatialization<X> model, int[] removed) {
        RInsertion<X> reinsertion = model.insertion(x, true);
        RNode<X> u = target.add(reinsertion);
        boolean merged = reinsertion.merged();
        if (merged)
            removed[0]++;
        else {
            assert(reinsertion.added());
        }
//            if (!((reinsertion.added() && u != null) ))
//                throw new WTF("unable to add");
        if (u!=null) //HACK
            target = u;
        return target;
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
    public RNode<X> replace(X OLD, HyperRegion oldBounds, X NEW, Spatialization<X> model) {

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


    private int chooseLeaf(HyperRegion tRect) {
        RNode<X>[] cc = this.data;
        if (size > 0) {
            int bestNode = -1;

            double leastEnlargement = Double.POSITIVE_INFINITY;
            double leastPerimeter = Double.POSITIVE_INFINITY;

            short s = this.size;
            for (int i = 0; i < s; i++) {
                HyperRegion cir = cc[i].bounds();
                HyperRegion childMbr = tRect.mbr(cir);
                double nodeEnlargement =
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
        //null terminator
        return Arrays.stream(data).takeWhile(Objects::nonNull).anyMatch(x -> x.OR(p));
    }

    @Override
    public boolean AND(Predicate<X> p) {
        //null terminator
        return Arrays.stream(data).takeWhile(Objects::nonNull).allMatch(x -> x.AND(p));
    }
    public boolean ANDlocal(Predicate<RNode<X>> p) {
        RNode<X>[] n = this.data;
        short s = this.size;
        return IntStream.range(0, s).allMatch(i -> p.test(n[i]));
    }
    public boolean ORlocal(Predicate<RNode<X>> p) {
        RNode<X>[] n = this.data;
        short s = this.size;
        return IntStream.range(0, s).anyMatch(i -> p.test(n[i]));
    }



    @Override
    public boolean containing(HyperRegion rect, Predicate<X> t, Spatialization<X> model) {
        HyperRegion b = this.bounds;
        if (b != null && rect.intersects(b)) {
            int s = size;
            //                if (d == null)
            //                    continue;
            /*else */
            return Arrays.stream(data, 0, s).allMatch(d -> d.containing(rect, t, model));
        }
        return true;
    }

    @Override
    public boolean intersectingNodes(HyperRegion rect, Predicate<RNode<X>> t, Spatialization<X> model) {
        HyperRegion b = this.bounds;
        if (b != null && rect.intersects(b) && t.test(this)) {
            int s = size;
            return IntStream.range(0, s).allMatch(i -> data[i].intersectingNodes(rect, t, model));
        }
        return true;
    }

    @Override
    public boolean intersecting(HyperRegion rect, Predicate<X> t, Spatialization<X> model) {
        HyperRegion b = this.bounds;
        if (b != null && rect.intersects(b)) {
            int s = size;
            return IntStream.range(0, s).allMatch(i -> data[i].intersecting(rect, t, model));
        }
        return true;
    }

    @Override
    public Stream<RNode<X>> streamNodes() {
        return streamLocal();
    }

    @Override
    public Stream<X> streamValues() {
        return streamNodes().flatMap(RNode::streamValues);
    }

    @Override
    public Iterator<X> iterateValues() {
        return Iterators.concat(Iterators.transform(iterateLocal(), RNode::iterateValues));
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


}
