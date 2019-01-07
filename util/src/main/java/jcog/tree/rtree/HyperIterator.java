package jcog.tree.rtree;

import jcog.data.pool.MetalPool;
import jcog.sort.RankedTopN;
import jcog.sort.TopN;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;

/**
 * BFS that descends through RTree visiting nodes and leaves in an order determined
 * by a score function that ranks the next nodes to either provide via an Iterator<X>-like interface
 * or to expand the ranked buffer to find more results.
 */
public class HyperIterator<X> implements AutoCloseable {


    private static final ThreadLocal<MetalPool<RankedTopN>> pool = TopN.newRankedPool();

    /**
     * next available item
     */
    X next;


    @Nullable
    private NodeFilter<X> nodeFilter = null;

    /**
     * at each level, the plan is slowly popped from the end growing to the beginning (sorted in reverse)
     */
    final RankedTopN plan;


    //                    new DequePool() {
//                        @Override
//                        public TopN create() {
//                            return new TopN(32, (x)->0);
//                        }
//                    }
//            );

    public HyperIterator(RTree<X> tree, FloatFunction<HyperRegion> rank) {
        this(tree.model, tree.root(), rank);
    }

    public HyperIterator(Spatialization model, Node<X> start, FloatFunction<HyperRegion> rank) {
        this.plan = TopN.pooled(pool, 256, (FloatFunction)r -> rank.floatValueOf(
                r instanceof Node ? ((Node) r).bounds() : model.bounds(r)
                //model.bounds(r)
                ));

        plan.addRanked(start);
    }

    @Override public final void close() {

        TopN.unpool(pool, plan);
    }



    /**
     * finds the next item given the current item and
     */
    @Nullable
    private X find() {

        Object z;
        while ((z = plan.popRanked())!=null) {
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
        if (at.size() == 0)
            return;

        at.forEachLocal(itemOrNode -> {
            if (itemOrNode instanceof Node) {
                Node node = (Node) itemOrNode;

                //inline 1-arity branches for optimization
                while (node.size() == 1) {
                    Object first = node.get(0);
                    if (first instanceof Node)
                        node = (Node) first; //this might indicate a problem in the tree structure that could have been flattened automatically
                    else {

                        //dont filter root node (traversed while plan is null)
                        addPlan(node, first);
                        return;
                    }
                }

                addPlan(node, node);

            } else {
                plan.addRanked(itemOrNode);
            }
        });


    }

    private void addPlan(Node node, Object first) {
        if (nodeFilter == null || plan.isEmpty() || nodeFilter.tryVisit(node)) {
            plan.addRanked(first);
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
