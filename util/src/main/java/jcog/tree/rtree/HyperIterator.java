package jcog.tree.rtree;

import jcog.sort.RankedTopN;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;
import java.util.function.Consumer;

/**
 * BFS that descends through RTree visiting nodes and leaves in an order determined
 * by a score function that ranks the next nodes to either provide via an Iterator<X>-like interface
 * or to expand the ranked buffer to find more results.
 */
public class HyperIterator<X> implements AutoCloseable {


    //private static final ThreadLocal<MetalPool<RankedTopN>> pool = RankedTopN.newRankedPool();

    /**
     * next available item
     */
    X next;


    @Nullable
    private NodeFilter<X> nodeFilter = null;

    /**
     * at each level, the plan is slowly popped from the end growing to the beginning (sorted in reverse)
     */
    final RankedTopN<Object> plan;


    //                    new DequePool() {
//                        @Override
//                        public TopN create() {
//                            return new TopN(32, (x)->0);
//                        }
//                    }
//            );

    public static <X> void iterate(ConcurrentRTree<X> tree, int capacity, FloatFunction<HyperRegion> rank, Consumer<HyperIterator<X>> with) {

        try (HyperIterator<X> h = new HyperIterator(tree.model(), new Object[capacity], rank) {
            @Override
            public void start(Node start) {
                super.start(start);
                with.accept(this);
            }
        }) {
            tree.read((t) -> {
                Node<X> r = t.root();
                if (r!=null) {
                    h.start(r);
                }
            });
        }

    }

    public HyperIterator(Spatialization<X> model, X[] x, FloatFunction<? super HyperRegion> rank) {
        this.plan = new RankedTopN(x, (FloatFunction) r -> rank.floatValueOf(
                r instanceof HyperRegion ? ((HyperRegion) r) :
                        (r instanceof Node ? ((Node) r).bounds() :
                                model.bounds((X) r))
        ));
    }

    public void start(Node<X> start) {
        plan.add(start);
    }

    @Override
    public final void close() {

    }

    /**
     * finds the next item given the current item and
     */
    @Nullable
    private X find() {

        Object z;
        while ((z = plan.pop()) != null) {
            if (z instanceof Node) {
                expand((Node<X>) z);
            } else {
                return (X) z;
            }
        }

        return null;
    }


    private void local(Object itemOrNode) {
        if (itemOrNode instanceof Node) {
            Node node = (Node) itemOrNode;

            //inline 1-arity branches for optimization
            while (node.size() == 1) {
                Object next = node.get(0);
                if (next instanceof Node)
                    node = (Node) next; //this might indicate a problem in the tree structure that could have been flattened automatically
                else {

                    //dont filter root node (traversed while plan is null)
                    addPlan(node, next);
                    return;
                }
            }

            addPlan(node, node);

        } else {
            plan.add(itemOrNode);
        }

    }
    /**
     * surveys the contents of the node, producing a new 'stack frame' for navigation
     */
    private void expand(Node<X> at) {
        if (at.size() == 0)
            return;

        at.forEachLocal(this::local);

    }

    private void addPlan(Node node, Object first) {
        if (nodeFilter == null || plan.isEmpty() || nodeFilter.tryVisit(node)) {
            plan.add(first);
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
