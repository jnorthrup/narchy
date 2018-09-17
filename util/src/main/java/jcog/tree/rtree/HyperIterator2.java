package jcog.tree.rtree;

import jcog.data.pool.DequePool;
import jcog.sort.CachedTopN;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;

/**
 * BFS that descends through RTree visiting nodes and leaves in an order determined
 * by a score function that ranks the next nodes to either provide via an Iterator<X>-like interface
 * or to expand the ranked buffer to find more results.
 */
public class HyperIterator2<X> {


    /**
     * next available item
     */
    X next;


    @Nullable
    private NodeFilter<X> nodeFilter = null;

    /**
     * at each level, the plan is slowly popped from the end growing to the beginning (sorted in reverse)
     */
    final CachedTopN plan;


    final static ThreadLocal<DequePool<CachedTopN>> pool =
            //HEAP
            //() -> new CachedFloatRank<>(64);

            ThreadLocal.withInitial(()->
                    new DequePool() {
                        @Override
                        public CachedTopN create() {
                            return new CachedTopN(32, (x)->0);
                        }
                    }
            );

    public HyperIterator2(Spatialization model, Node<X> start, FloatFunction<HyperRegion> rank) {
//        this.model = model;

//        this.target = target;

        FloatFunction rr = r -> rank.floatValueOf(
                r instanceof Node? ((Node)r).bounds() : model.bounds(r)
        );
        this.plan = /*new CachedTopN(32, rank);*/
                    pool.get().get().clear(rr);

        plan.accept(start);
    }



    /**
     * finds the next item given the current item and
     */
    @Nullable
    private X find() {

        Object z;
        while ((z = plan.pop())!=null) {
            if (z instanceof Node) {
                expand((Node<X>) z);
            } else {
                return (X) z;
            }
        }

        return null;
    }


    /**
     * surveys the contents of the node, producing a new 'stack frame' for navigation
     */
    private void expand(Node<X> at) {
        //initialize plan at current node
        int atSize = at.size();
        if (atSize == 0) {
            return;
        } /* TODO : else if (at.isLeaf() && atSize == 1) {

        } */

        boolean notNodeFiltering = (nodeFilter == null || plan.isEmpty()); //dont filter root node (traversed while plan is null)

        at.forEachLocal(itemOrNode -> {
            if (itemOrNode instanceof Node) {
                Node node = (Node) itemOrNode;

//                //inline 1-arity branches for optimization
//                while (node.size() == 1) {
//                    Object first = node.get(0);
//                    if (first instanceof Node)
//                        node = (Node) first; //this might indicate a problem in the tree structure that could have been flattened automatically
//                    else {
//                        if (notNodeFiltering || nodeFilter.tryVisit(node)) {
//                            plan.accept(first);
//                        }
//                        return;
//                    }
//                }

                if (notNodeFiltering || nodeFilter.tryVisit(node))
                    plan.accept(node);

            } else {
                plan.accept(itemOrNode);
            }
        });


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


//    public static final HyperIterator2 Empty = new HyperIterator2() {
//
//        @Override
//        public boolean hasNext() {
//            return false;
//        }
//
//        @Override
//        public Object next() {
//            throw new NoSuchElementException();
//        }
//    };




}
