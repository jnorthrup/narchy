package jcog.data.graph.search;

import jcog.data.graph.NodeGraph;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;

/** search log history for detecting cycles, reachability, etc */
public interface TraveLog {
    void clear();

    /** returns false if it was already added */
    boolean visit(NodeGraph.Node n);


    void unvisit(NodeGraph.Node n);

    boolean hasVisited(NodeGraph.Node n);


    //TODO: reachable, etc

    class IntHashTraveLog implements TraveLog {

        final IntHashSet visit = new IntHashSet(8);

        @Override
        public void clear() {
            visit.clear();
        }

        @Override
        public boolean visit(NodeGraph.Node n) {
            return visit.add(n.serial);
        }

        @Override
        public void unvisit(NodeGraph.Node n) {
            visit.remove(n.serial);
        }

        @Override
        public boolean hasVisited(NodeGraph.Node n) {
            return visit.contains(n.serial);
        }

    }

    /** TODO a bitvector based travelog, with a failsafe max limit  */

}
