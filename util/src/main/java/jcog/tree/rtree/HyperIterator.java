package jcog.tree.rtree;

import jcog.TODO;
import jcog.data.list.FasterList;
import org.eclipse.collections.api.block.SerializableComparator;
import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.Functions;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;

import static jcog.tree.rtree.Space.BoundsMatch.ANY;
import static org.eclipse.collections.impl.tuple.Tuples.pair;

/**
 * TODO Hyperdimensional Iterator
 * an iterator/cursor that traverses a the tree along a mutable trajectory (ex: per-dimension range conditions)
 * with ability for traversing results (sorted approximately or perfectly) along specified gradients, if provided
 */
public class HyperIterator<X> {

    //string of -1, 0, +1 for each dimension, indicating direction
    @Nullable
    final byte[] gradient;

    final HyperRegion target;

    final Space.BoundsMatch mode;

    final Node<X> start;
    private final Spatialization<X> model;

    /**
     * current node
     */
    Node<X> at;

    /**
     * next available item
     */
    X next;


    @Nullable private NodeFilter<X> nodeFilter = null;

    /** at each level, the plan is slowly popped from the end growing to the beginning (sorted in reverse) */
    FasterList<FasterList> plan = null;


    @Nullable private SerializableComparator<Pair<Object, HyperRegion>> distanceComparator;

    private HyperIterator() {
        model = null;
        mode = null;
        start = null;
        target = null;
        gradient = null;
    }


    protected HyperIterator(Spatialization<X> model, Node<X> start, HyperRegion<X> target, Space.BoundsMatch mode, @Nullable byte[] gradient) {
        this.model = model;
        this.start = this.at = start;
        this.target = target;
        this.mode = mode;
        this.gradient = gradient;
    }

    private FasterList level() {
        return plan.getLast();
    }

    /**
     * finds the next item given the current item and
     */
    @Nullable private X find() {
        if (plan == null) {
            FasterList firstLevel = push(start);
            if (firstLevel != null) {
                this.plan = new FasterList<>(8);
                this.plan.add(firstLevel);
            } else
                return null;
        }

        do {
            FasterList p = level();
//            if (p == null)
//                return null;

            while (!p.isEmpty()) {

                Object z = p.removeLast();
                if (z instanceof Node) {
                    FasterList nextLevel = push((Node<X>) z);
                    if (nextLevel != null && !nextLevel.isEmpty()) {
                        //System.out.println("[" + plan.size() + "] push: " + nextLevel.size());
                        plan.add(p = nextLevel);
                    }

                } else {
                    return (X) z;
                }

            }

            plan.removeLast();

        } while (!plan.isEmpty());

        return null;
    }



    /**
     * surveys the contents of the node, producing a new 'stack frame' for navigation
     */
    private FasterList<Object> push(Node<X> at) {
        //initialize plan at current node
        int atSize = at.size();
        if (atSize == 0) {
            return null;
        } /* TODO : else if (at.isLeaf() && atSize == 1) {

        } */

        FasterList p = new FasterList<>(atSize);

        boolean notNodeFiltering = (nodeFilter==null || plan==null ); //dont filter root node (traversed while plan is null)

        at.forEachLocal(itemOrNode -> {
            if (itemOrNode instanceof Node) {
                Node node = (Node) itemOrNode;
                HyperRegion nodeBounds = node.bounds();
                if (mode.acceptNode(target, nodeBounds)) {

                    //inline 1-arity branches for optimization
                    while (node.size() == 1) {
                        Object first = node.get(0);
                        if (first instanceof Node)
                            node = (Node) first; //this might indicate a problem in the tree structure that could have been flattened automatically
                        else {
                            if (notNodeFiltering || nodeFilter.tryVisit(node)) {
                                tryItem(p, (X) first);
                            }
                            return;
                        }
                    }

                    if (notNodeFiltering || nodeFilter.tryVisit(node))
                        p.add(pair(node, nodeBounds));
                }
            } else {
                tryItem(p, (X) itemOrNode);
            }
        });

        int pn = p.size();
        if (pn == 0) return null;

        if (pn > 1)
            sort(p);

        //replace pair(item,bounds) to items in the list
        p.replaceAll(x -> ((Pair)x).getOne());

        return p;
    }

    private void tryItem(FasterList p, X item) {
        HyperRegion<X> itemBounds = model.bounds(item);
        if (mode.acceptItem(target, itemBounds))
            p.add(pair(item, itemBounds));
    }

    /** sort highest priority to the end of the list so it will be popped first */
    private void sort(FasterList<Pair<Object,HyperRegion>> p) {
        if (mode == ANY) {

            p.sortThis(distanceComparator);

        } else if (gradient!=null) {
            throw new TODO();
        }
    }

    public boolean hasNext() {
        return (this.next = find()) != null;
    }

    public X next() {
        X n = this.next;
        if (n == null)
            throw new NoSuchElementException();
        return n;
    }

    public void setNodeFilter(NodeFilter<X> n) {
        this.nodeFilter = n;
    }

    public final HyperIterator<X> fork(Node<X> start, HyperRegion bounds, Space.BoundsMatch mode) {
        return fork(start, bounds, mode, null);
    }

    /**
     * for changing direction without affecting current state
     */
    public final HyperIterator<X> fork(Node<X> start, HyperRegion bounds, Space.BoundsMatch mode, @Nullable byte[] gradient) {
        return new HyperIterator<>(model, start, bounds, mode, gradient);
    }


    public static final HyperIterator Empty = new HyperIterator() {

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            throw new NoSuchElementException();
        }
    };

    public void setDistanceFunction(DoubleFunction h) {
        this.distanceComparator = Functions.toDoubleComparator((Pair<Object,HyperRegion> b) -> h.applyAsDouble(b.getTwo()));
    }

}
