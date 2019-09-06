package jcog.tree.rtree;

import jcog.Util;
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




    public void dfs(RNode<X> root, Predicate whle) {
        plan.add(root);
        while (hasNext() && whle.test(next())) { }
    }

    /** gets a set of LeafNode's before round-robin visiting their contents as an iterator */
    public void bfs(RNode<X> root, Predicate whle) {
        if (root instanceof RLeaf) {
            leaf((RLeaf) root, whle);
        } else {

            plan.add(root);
            boolean findingLeaves;
            restart: do {
                findingLeaves = false;
                Object[] items = plan.items;
                for (int i = 0, itemsLength = plan.size(); i < itemsLength;) {
                    Object x = items[i];
                    if (x instanceof RBranch) {
                        plan.remove(i);
                        RBranch xb = (RBranch) x;
                        Object[] data = xb.data;
                        boolean added = false;
                        for (int j = 0, dataLength = xb.size; j < dataLength; j++) {
                            Object y = data[j];
                            added |= plan.add(y);
                            if (added && y instanceof RBranch)
                                findingLeaves = true;
                        }
                        if (added)
                            continue restart;
                        else {
                            itemsLength--;
                        }
                    } else
                        i++;
                }
            //} while (findingLeaves);
            } while (Util.or(plan.items, x -> x instanceof RBranch)); //HACK


            int leaves = plan.size();
            assert(leaves > 0);
            if (leaves == 1)
                leaf((RLeaf<X>) plan.first(), whle);
            else {
                //round-robin visit
                int[] prog = new int[leaves];
                int n = 0;
                for (int i = 0; i < leaves; i++) {
                    short is = ((RLeaf) plan.get(i)).size;
                    prog[i] = is;
                    n+= is;
                }
                int c = 0;
                int k = 0;
                Object[] pp = plan.items;
                do {
                    if (k == leaves) k = 0;
                    int pk = prog[k];
                    if (pk > 0) {
                        if (!whle.test( ((RLeaf<X>) pp[k]).data[ --prog[k] ] )) {
                            break;
                        }
                    }
                    k++;
                } while (c++ < n);

            }
        }
    }

    private static <X> void leaf(RLeaf<X> rl, Predicate whle) {
        short ls = rl.size;
        X[] rld = rl.data;
        for (int i = 0; i < ls; i++) {
            if (!whle.test(rld[i]))
                break;
        }
    }

    @Deprecated public HyperIterator(Spatialization/*<X>*/ model, Object/*X*/[] x, FloatRank/*<? super HyperRegion>*/ rank) {
        this(x, new HyperIteratorRanker<>(model::bounds, rank));
    }

    public <H extends HyperRegion> HyperIterator(Object/*X*/[] buffer, HyperIteratorRanker<X,H> ranking) {
        this.plan = new RankedN<>(buffer, ranking);
        this.planAdd = plan::add;
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

    public static final class HyperIteratorRanker<X,R extends HyperRegion> implements FloatRank<Object> {
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
