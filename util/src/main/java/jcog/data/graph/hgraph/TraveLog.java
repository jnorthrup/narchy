package jcog.data.graph.hgraph;

import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;

/** search log history for detecting cycles, reachability, etc */
public interface TraveLog {
    void clear();

    /** returns false if it was already added */
    boolean visit(Node n);


    void unvisit(Node n);

    boolean hasVisited(Node n);


    //TODO: reachable, etc

    class IntHashTraveLog implements TraveLog {

        final IntHashSet visit = new IntHashSet(8);

        @Override
        public void clear() {
            visit.clear();
        }

        @Override
        public boolean visit(Node n) {
            return visit.add(n.serial);
        }

        @Override
        public void unvisit(Node n) {
            visit.remove(n.serial);
        }

        @Override
        public boolean hasVisited(Node n) {
            return visit.contains(n.serial);
        }

    }

    /** TODO a bitvector based travelog, with a failsafe max limit  */

}
