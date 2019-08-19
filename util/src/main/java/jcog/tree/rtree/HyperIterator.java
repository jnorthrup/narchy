package jcog.tree.rtree;

import jcog.sort.FloatRank;
import jcog.sort.RankedN;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * BFS that descends through RTree visiting nodes and leaves in an order determined
 * by a score function that ranks the next nodes to either provide via an Iterator<X>-like interface
 * or to expand the ranked buffer to find more results.
 *
 * TODO generalize for non-rtree uses, subclass that specifically for RTree and implement an abstract growth method for its lazy Node<> iteration
 */
public class HyperIterator<X>  {


    /**
     * next available item
     */
    private X next;


//    @Nullable
//    private NodeFilter<X> nodeFilter = null;

    /**
     * at each level, the plan is slowly popped from the end growing to the beginning (sorted in reverse)
     */
    private final RankedN plan;
    private final Consumer planAdd;


    public static <X,R extends HyperRegion> void iterate(ConcurrentRTree<X> tree, HyperIteratorRanker<X,R> rank, int bufferCap, Predicate whle) {

        //tree.readOptimistic(
        tree.read(
            t -> {
            int s = t.size();
            switch (s) {
                case 0:
                    return;
                case 1:
                    t.forEach(whle::test);
                    break;
                default: {
                    int cursorCapacity =
                        //s; //TODO determine if this can safely be smaller like log(s)/branching or something
                        bufferCap;

                    HyperIterator<X> h = new HyperIterator<>(new Object[cursorCapacity], rank);
                    h.start(t.root());
                    while (h.hasNext() && whle.test(h.next())) {
                    }

                    break;
                }
            }
        });


    }

    @Deprecated public HyperIterator(Spatialization/*<X>*/ model, Object/*X*/[] x, FloatRank/*<? super HyperRegion>*/ rank) {
        this(x, new HyperIteratorRanker<>(model::bounds, rank));
    }

    public <H extends HyperRegion> HyperIterator(Object/*X*/[] buffer, HyperIteratorRanker<X,H> ranking) {
        this.plan = new RankedN(buffer, ranking);
        this.planAdd = plan::add;
    }

    private void start(RNode<X> start) {
        plan.add(start);
    }


    //    /**
//     * surveys the contents of the node, producing a new 'stack frame' for navigation
//     */
//    private void expand(Node<X> at) {
//        at.forEachLocal(this::push);
//    }


    public final boolean hasNext() {

        Object z;
        while ((z = plan.pop()) != null) {
            if (!(z instanceof RNode))
                break;

            ((AbstractRNode) z).drainLayer(planAdd);
        }

        return (this.next = (X) z) != null;
    }

    public final X next() {
        X n = this.next;
//        if (n == null)
//            throw new NoSuchElementException();
        return n;
    }

    public static class HyperIteratorRanker<X,R extends HyperRegion> implements FloatRank {
        private final Function<X, R> bounds;
        private final FloatRank<R> rank;

        public HyperIteratorRanker(Function<X, R> bounds, FloatFunction<R> rank) {
            this(bounds, FloatRank.the(rank));
        }

        public HyperIteratorRanker(Function<X, R> bounds, FloatRank<R> rank) {
            this.bounds = bounds;
            this.rank = rank;
        }

        @Override
        public float rank(Object r, float min) {
            HyperRegion y =
                r instanceof HyperRegion ?
                    ((HyperRegion) r)
                    :
                    (r instanceof RNode ?
                        ((RNode) r).bounds()
                        :
                        bounds.apply((X) r)
                    );

            return y == null ? Float.NaN : rank.rank((R)y, min);
        }
    }

//    public void setNodeFilter(NodeFilter<X> n) {
//        this.nodeFilter = n;
//    }


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
