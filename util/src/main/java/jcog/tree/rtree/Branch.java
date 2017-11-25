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


import com.google.common.base.Joiner;
import jcog.Util;
import jcog.list.ArrayIterator;
import jcog.tree.rtree.util.CounterNode;
import jcog.tree.rtree.util.Stats;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * RTree node that contains leaf nodes
 * <p>
 * Created by jcairns on 4/30/15.
 */
public final class Branch<T> implements Node<T, Node<T, ?>> {

    public final Node<T, ?>[] child;

    private HyperRegion bounds;
    private short size;

    public Branch(int cap) {
        this.bounds = null;
        this.size = 0;
        this.child = new Node[cap];
    }

    Branch(int cap, Leaf<T> a, Leaf<T> b) {
        this(cap);
        assert (cap >= 2);
        assert (a != b);
        child[0] = a;
        child[1] = b;
        this.size = 2;
        this.bounds = a.region.mbr(b.region);
    }

    @Override
    public boolean contains(T t, HyperRegion b, Spatialization<T> model) {

        if (!this.bounds.contains(b)) //do pre-test if >2
            return false;

        int s = size;
        Node[] c = this.child;
        for (int i = 0; i < s; i++) {
            if (c[i].contains(t, b, model))
                return true;
        }

        return false;
    }


    /**
     * Add a new node to this branch's list of children
     *
     * @param n node to be added (can be leaf or branch)
     * @return position of the added node
     */
    public int addChild(final Node<T, ?> n) {
        if (size < child.length) {
            child[size++] = n;

            HyperRegion nr = n.bounds();
            bounds = bounds != null ? bounds.mbr(nr) : nr;
            return size - 1;
        } else {
            throw new RuntimeException("Too many children");
        }
    }

    @Override
    public final Node<T, ?> get(int i) {
        return child[i];
    }

    @Override
    public final boolean isLeaf() {
        return false;
    }

    @Override
    public final HyperRegion bounds() {
        return bounds;
    }

    /**
     * Adds a data entry to one of the child nodes of this branch
     *
     * @param t      data entry to add
     * @param parent
     * @param model
     * @param added
     * @return Node that the entry was added to
     */
    @Override
    public Node<T, ?> add(final T t, Nodelike<T> parent, Spatialization<T> model, boolean[] added) {

        final HyperRegion tRect = model.bounds(t);

        Node[] child = this.child;

        if (bounds.contains(tRect)) {
            //MERGE STAGE:
            for (int i = 0; i < size; i++) {
                Node ci = child[i];
                if (ci.bounds().contains(tRect)) {
                    //check for existing item
                    //                if (ci.contains(t, model))
                    //                    return this; // duplicate detected (subtree not changed)

                    Node m = ci.add(t, null, model, null);
                    if (m == null) {
                        return null; //merged
                    }

                    //                if (reportNextSizeDelta(parent)) {
                    //                    child[i] = m;
                    //                    grow(m); //subtree was changed
                    //                    return this;
                    //                }
                }
            }
            if (parent == null)
                return this; //done for this stage
        }

        if (added == null)
            return this; //merge stage only

        assert (!added[0]);

        //INSERTION STAGE:

        if (size < child.length) {

            // no overlapping node - grow
            grow(addChild(model.newLeaf().add(t, parent, model, added)));
            assert (added[0]);

            return this;

        } else {

            final int bestLeaf = chooseLeaf(t, tRect, parent, model);

            Node nextBest = child[bestLeaf].add(t, this, model, added);
            assert (added[0]);

            if (nextBest == null) {
                return null; //merged
            }

            child[bestLeaf] = nextBest;


            grow(nextBest);

            // optimize on split to remove the extra created branch when there
            // is space for the children here
            if (size < child.length && nextBest.size() == 2 && !nextBest.isLeaf()) {
                Node[] bc = ((Branch<T>) nextBest).child;
                child[bestLeaf] = bc[0];
                child[size++] = bc[1];
            }

//            } else {
//                //? duplicate was found in sub-tree but we checked for duplicates above
//
//
//                if (nextBest.contains(t, model))
//                    return null;
//
//                assert (false) : "what to do with: " + t + " in " + parent;
//                //probably ok, just merged with a subbranch?
//                //return null;
//            }

            return this;
        }
    }

    private void grow(int i) {
        grow(child[i]);
    }

    private void grow(Node node) {
        bounds = bounds.mbr(node.bounds());
    }

    private static HyperRegion grow(HyperRegion region, Node node) {
        return region.mbr(node.bounds());
    }

    @Override
    public Node<T, ?> remove(final T x, HyperRegion xBounds, Spatialization<T> model, boolean[] removed) {

        assert (removed[0] == false);

        for (int i = 0; i < size; i++) {
            Node<T, ?> cBefore = child[i];
            if (cBefore.bounds().contains(xBounds)) {

                Node<T, ?> cAfter = cBefore.remove(x, xBounds, model, removed);

                if (removed[0]) {
                    if (child[i].size() == 0) {
                        System.arraycopy(child, i + 1, child, i, size - i - 1);
                        child[--size] = null;
                        //if (size > 0) i--;
                    }

                    if (size > 0) {

                        if (size == 1) {
                            // unsplit branch
                            return child[0];
                        }

                        Node[] cc = this.child;
                        HyperRegion region = cc[0].bounds();
                        for (int j = 1; j < size; j++) {
                            region = grow(region, cc[j]);
                        }
                        this.bounds = region;
                    }

                    break;
                }
            }
        }


        return this;
    }

    @Override
    public Node<T, ?> update(final T OLD, final T NEW, Spatialization<T> model) {
        final HyperRegion tRect = model.bounds(OLD);

        //TODO may be able to avoid recomputing bounds if the old was not found
        boolean found = false;
        Node[] cc = this.child;
        HyperRegion region = null;
        short s = this.size;
        for (int i = 0; i < s; i++) {
            if (!found && tRect.intersects(cc[i].bounds())) {
                cc[i] = cc[i].update(OLD, NEW, model);
                found = true;
            }
            region = i == 0 ? cc[0].bounds() : grow(region, cc[i]);
        }
        this.bounds = region;

        return this;

    }


    /**
     * @return number of child nodes
     */
    @Override
    public int size() {
        return size;
    }

    private int chooseLeaf(final T t, final HyperRegion tRect, Nodelike<T> parent, Spatialization<T> model) {
        Node<T, ?>[] cc = this.child;
        if (size > 0) {
            int bestNode = -1;
            double tCost = Double.POSITIVE_INFINITY;
            double leastEnlargement = Double.POSITIVE_INFINITY;
            double leastPerimeter = Double.POSITIVE_INFINITY;

            short s = this.size;
            for (int i = 0; i < s; i++) {
                HyperRegion cir = cc[i].bounds();
                HyperRegion childMbr = tRect.mbr(cir);
                final double nodeEnlargement = childMbr.cost() - (cir.cost() + tCost);
                if (nodeEnlargement < leastEnlargement) {
                    leastEnlargement = nodeEnlargement;
                    leastPerimeter = childMbr.perimeter();
                    bestNode = i;
                } else if (Util.equals(nodeEnlargement, leastEnlargement, RTree.EPSILON)) {
                    double perimeter = childMbr.perimeter();
                    if (perimeter < leastPerimeter) {
                        leastEnlargement = nodeEnlargement;
                        leastPerimeter = perimeter;
                        bestNode = i;
                    }
                } // else its not the least

            }
            if (bestNode == -1) {
                throw new RuntimeException("rtree fault");
            }
            //assert(bestNode != -1);
            return bestNode;
        } else {
            final Node<T, ?> n = model.newLeaf();
            cc[size++] = n;
            return size - 1;
        }
    }

    /**
     * Return child nodes of this branch.
     *
     * @return array of child nodes (leaves or branches)
     */
    public Node<T, ?>[] children() {
        return child;
    }

    @Override
    public void forEach(Consumer<? super T> consumer) {
        Node<T, ?>[] cc = this.child;
        short s = this.size;
        for (int i = 0; i < s; i++) {
            cc[i].forEach(consumer);
        }
    }

    @Override
    public boolean AND(Predicate<T> p) {
        Node<T, ?>[] c = this.child;
        short s = this.size;
        for (int i = 0; i < s; i++) {
            if (!c[i].AND(p))
                return false;
        }
        return true;
    }

    private boolean nodeAND(Predicate<Node<T, ?>> p) {
        Node<T, ?>[] c = this.child;
        int s = size;
        for (int i = 0; i < s; i++) {
            if (!p.test(c[i]))
                return false;
        }
        return true;
    }

    @Override
    public boolean OR(Predicate<T> p) {
        Node<T, ?>[] c = this.child;
        int s = size;
        for (int i = 0; i < s; i++) {
            if (c[i].OR(p))
                return true;
        }
        return false;
    }

    @Override
    public boolean containing(final HyperRegion rect, final Predicate<T> t, Spatialization<T> model) {
        HyperRegion b = this.bounds;
        if (b!=null && rect.intersects(b)) {
            short s = this.size;
            Node[] data = this.child;
            for (int i = 0; i < s; i++) {
                Node d = data[i];
                if (d!=null && !d.containing(rect, t, model))
                    return false;
            }
        }
        return true;
    }

    @Override
    public boolean intersecting(HyperRegion rect, Predicate<T> t, Spatialization<T> model) {
        HyperRegion b = this.bounds;
        if (b!=null && rect.intersects(b)) {
            short s = this.size;
            Node[] data = this.child;
            for (int i = 0; i < s; i++) {
                Node d = data[i];
                if (d!=null && !d.intersecting(rect, t, model))
                    return false;
            }
        }
        return true;
    }

    @Override
    public Stream<T> stream() {
        return streamNodes().flatMap(Node::stream);
    }

    @Override
    public Iterator<Node<T, ?>> iterateNodes() {
        return ArrayIterator.get(child, size);
    }

    @Override
    public void collectStats(Stats stats, int depth) {
        for (int i = 0; i < size; i++)
            child[i].collectStats(stats, depth + 1);
        stats.countBranchAtDepth(depth);
    }

    @Override
    public Node<T, ?> instrument() {
        for (int i = 0; i < size; i++)
            child[i] = child[i].instrument();
        return new CounterNode(this);
    }

    @Override
    public String toString() {
        return "Branch" + '{' + bounds + 'x' + size + ":\n\t" + Joiner.on("\n\t").skipNulls().join(child) + "\n}";
    }


//    @Override
//    public double perimeter(Spatialization<T> model) {
//        double maxVolume = 0;
//        for (int i = 0; i < size; i++) {
//            Node<T, ?> c = child[i];
//            double vol = c.perimeter(model);
//            if (vol > maxVolume)
//                maxVolume = vol;
//        }
//        return maxVolume;
//    }
}
