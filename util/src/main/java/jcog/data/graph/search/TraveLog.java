package jcog.data.graph.search;

import jcog.data.graph.Node;
import jcog.data.graph.NodeGraph;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;

/** search log history for detecting cycles, reachability, etc */
public interface TraveLog {
    void clear();

    /** returns false if it was already added */
    boolean visit(Node n);


    void unvisit(Node n);

    boolean hasVisited(Node n);


    

    class IntHashTraveLog implements TraveLog {

        final IntHashSet visit = new IntHashSet(8);

        @Override
        public void clear() {
            visit.clear();
        }

        @Override
        public boolean visit(Node n) {
            return visit.add(((NodeGraph.AbstractNode)n).serial);
        }

        @Override
        public void unvisit(Node n) {
            visit.remove(((NodeGraph.AbstractNode)n).serial);
        }

        @Override
        public boolean hasVisited(Node n) {
            return visit.contains(((NodeGraph.AbstractNode)n).serial);
        }

    }

    /** TODO a bitvector based travelog, with a failsafe max limit  */

}
