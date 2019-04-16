package jcog.tree.rtree;

import jcog.sort.RankedN;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * BFS that descends through RTree visiting nodes and leaves in an order determined
 * by a score function that ranks the next nodes to either provide via an Iterator<X>-like interface
 * or to expand the ranked buffer to find more results.
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

    public static <X> void iterate(ConcurrentRTree<X> tree, Supplier<FloatFunction<X>> rank, Predicate<X> whle) {


        tree.read(t -> {
            int s = t.size();
            switch (s) {
                case 0:
                    return;
                case 1:
                    t.forEach(whle::test);
                    break;
                default: {
                    int cursorCapacity = s; //TODO determine if this can safely be smaller like log(s)/branching or something

                    HyperIterator<X> h = new HyperIterator(tree.model(), new Object[cursorCapacity], rank.get());
                    h.start(t.root());
                    while (h.hasNext() && whle.test(h.next())) {
                    }

                    break;
                }
            }
        });


    }

    public HyperIterator(Spatialization/*<X>*/ model, Object/*X*/[] x, FloatFunction/*<? super HyperRegion>*/ rank) {
        this.plan = new RankedN(x, (FloatFunction) r -> {
            HyperRegion y =
                    r instanceof HyperRegion ?
                        ((HyperRegion) r)
                        :
                        (r instanceof Node ?
                            ((Node) r).bounds()
                            :
                            model.bounds(r)
                        );

            if (y == null)
                return Float.NaN; //HACK
            else
                return rank.floatValueOf(y);
        });
    }

    private void start(Node<X> start) {
        plan.add(start);
    }


    private Object inline(Object x) {
        //inline 1-arity branches for optimization
        while (x instanceof Node && (((Node)x).size() == 1)) {
            x = ((Node) x).get(0);
        }

//        //dont filter root node (traversed while plan is null)
//        if ((x instanceof Node) && nodeFilter != null && !plan.isEmpty() && !nodeFilter.tryVisit((Node)x))
//            return null;

        return x;
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
            if (z instanceof Node) {
                Node nz = (Node) z;
                int s = nz.size();
                for (int i = 0; i < s; i++)
                    plan.add(inline(nz.get(i)));
            } else
                break;
        }

        return (this.next = (X) z) != null;
    }

    public final X next() {
        X n = this.next;
//        if (n == null)
//            throw new NoSuchElementException();
        return n;
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
