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
import jcog.data.iterator.ArrayIterator;
import jcog.tree.rtree.util.CounterNode;
import jcog.tree.rtree.util.Stats;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * RTree node that contains leaf nodes
 * <p>
 * Created by jcairns on 4/30/15.
 */
public class Branch<T> extends AbstractNode<T, Node<T, ?>> {

    public final Node<T, ?>[] data;

    public Branch(int cap) {
        this.bounds = null;
        this.size = 0;
        this.data = new Node[cap];
    }

    protected Branch(int cap, Leaf<T> a, Leaf<T> b) {
        this(cap);
        assert (cap >= 2);
        assert (a != b);
        data[0] = a;
        data[1] = b;
        this.size = 2;
        this.bounds = a.bounds.mbr(b.bounds);
    }
    @Override
    public Stream<Node<T,?>> streamNodes() {
        return data!=null ? ArrayIterator.stream(data, Math.min(data.length, size)).filter(Objects::nonNull) : Stream.empty();
    }
    @Override
    public boolean contains(T t, HyperRegion b, Spatialization<T> model) {

        if (!this.bounds.contains(b)) 
            return false;

        int s = size;
        if (s > 0) {
            Node[] c = this.data;
            for (int i = 0; i < s; i++) {
                if (c[i].contains(t, b, model))
                    return true;
            }
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
        if (size < data.length) {
            data[size++] = n;

            HyperRegion nr = n.bounds();
            bounds = bounds != null ? bounds.mbr(nr) : nr;
            return size - 1;
        } else {
            throw new RuntimeException("Too many children");
        }
    }

    @Override
    public final Node<T, ?> get(int i) {
        return data[i];
    }

    @Override
    public final boolean isLeaf() {
        return false;
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

        Node[] child = this.data;

        if (bounds.contains(tRect)) {
            
            for (int i = 0; i < size; i++) {
                Node ci = child[i];
                if (ci.bounds().contains(tRect)) {
                    
                    
                    

                    Node m = ci.add(t, null, model, null);
                    if (m == null) {
                        return null; 
                    }


                    
                    
                    
                    
                    
                }
            }
            if (parent == null)
                return this; 
        }

        if (added == null)
            return this; 

        assert (!added[0]);

        

        if (size < child.length) {

            
            grow(addChild(model.newLeaf().add(t, parent, model, added)));
            assert (added[0]);

            return this;

        } else {

            final int bestLeaf = chooseLeaf(tRect);

            Node nextBest = child[bestLeaf].add(t, this, model, added);
            if (nextBest == null) {                return null; /*merged*/             }

            child[bestLeaf] = nextBest;


            grow(nextBest);

            
            
            if (size < child.length && nextBest.size() == 2 && !nextBest.isLeaf()) {
                Node[] bc = ((Branch<T>) nextBest).data;
                child[bestLeaf] = bc[0];
                child[size++] = bc[1];
            }













            return this;
        }
    }

    private void grow(int i) {
        grow(data[i]);
    }

    private static HyperRegion grow(HyperRegion region, Node node) {
        return region.mbr(node.bounds());
    }

    @Override
    public Node<T, ?> remove(final T x, HyperRegion xBounds, Spatialization<T> model, boolean[] removed) {

        assert (!removed[0]);

        for (int i = 0; i < size; i++) {
            Node<T, ?> cBefore = data[i];
            if (cBefore.bounds().contains(xBounds)) {

                Node<T, ?> cAfter = cBefore.remove(x, xBounds, model, removed);

                if (removed[0]) {
                    if (data[i].size() == 0) {
                        System.arraycopy(data, i + 1, data, i, size - i - 1);
                        data[--size] = null;
                        
                    }

                    if (size > 0) {

                        if (size == 1) {
                            
                            return data[0];
                        }

                        Node[] cc = this.data;
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
    public Node<T, ?> replace(final T OLD, final T NEW, Spatialization<T> model) {
        final HyperRegion tRect = model.bounds(OLD);

        
        boolean found = false;
        Node[] cc = this.data;
        HyperRegion region = null;
        short s = this.size;
        for (int i = 0; i < s; i++) {
            if (!found && tRect.intersects(cc[i].bounds())) {
                cc[i] = cc[i].replace(OLD, NEW, model);
                found = true;
            }
            region = i == 0 ? cc[0].bounds() : grow(region, cc[i]);
        }
        if (found) {
            this.bounds = region;
        }
        return this;
    }



    private int chooseLeaf(final HyperRegion tRect) {
        Node<T, ?>[] cc = this.data;
        if (size > 0) {
            int bestNode = -1;
            
            double leastEnlargement = Double.POSITIVE_INFINITY;
            double leastPerimeter = Double.POSITIVE_INFINITY;

            short s = this.size;
            for (int i = 0; i < s; i++) {
                HyperRegion cir = cc[i].bounds();
                HyperRegion childMbr = tRect.mbr(cir);
                final double nodeEnlargement =
                        (cir!=childMbr ? childMbr.cost() - (cir.cost() /* + tCost*/) : 0);
                
                int dc = Double.compare(nodeEnlargement, leastEnlargement);
                if (dc == -1) { 
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
    public void forEach(Consumer<? super T> consumer) {
        short s = this.size;
        if (s > 0) {
            Node<T, ?>[] cc = this.data;
            for (int i = 0; i < s; i++) {
                Node<T, ?> x = cc[i];
                if (x != null)
                    x.forEach(consumer);
            }
        }
    }

    @Override
    public boolean AND(Predicate<T> p) {
        Node<T, ?>[] c = this.data;
        short s = this.size;
        for (int i = 0; i < s; i++) {
            Node<T, ?> x = c[i];
            if (x!=null && !x.AND(p))
                return false;
        }
        return true;
    }











    @Override
    public boolean OR(Predicate<T> p) {
        Node<T, ?>[] c = this.data;
        int s = size;
        for (int i = 0; i < s; i++) {
            Node<T, ?> x = c[i];
            if (x!=null && x.OR(p))
                return true;
        }
        return false;
    }

    @Override
    public boolean containing(final HyperRegion rect, final Predicate<T> t, Spatialization<T> model) {
        HyperRegion b = this.bounds;
        if (b != null) {
            int s = size;
            for (int i = 0; i < s; i++) {
                Node d = data[i];
                if (d == null)
                    continue; 
                else if (!d.containing(rect, t, model))
                    return false;
            }
        }
        return true;
    }

    @Override
    public boolean intersecting(HyperRegion rect, Predicate<T> t, Spatialization<T> model) {
        HyperRegion b = this.bounds;
        if (b != null) {
            int s = size;
            for (int i = 0; i < s; i++) {
                Node d = data[i];
                if (d == null)
                    continue; 
                else if (!d.intersecting(rect, t, model)) 
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
        return ArrayIterator.get(data, size);
    }

    @Override
    public void collectStats(Stats stats, int depth) {
        for (int i = 0; i < size; i++)
            data[i].collectStats(stats, depth + 1);
        stats.countBranchAtDepth(depth);
    }

    @Override
    public Node<T, ?> instrument() {
        for (int i = 0; i < size; i++)
            data[i] = data[i].instrument();
        return new CounterNode(this);
    }

    @Override
    public String toString() {
        return "Branch" + '{' + bounds + 'x' + size + ":\n\t" + Joiner.on("\n\t").skipNulls().join(data) + "\n}";
    }













}
