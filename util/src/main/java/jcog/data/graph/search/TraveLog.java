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


    final class IntHashTraveLog extends IntHashSet implements TraveLog {


        @Override
        public boolean visit(Node n) {
            return add(((NodeGraph.AbstractNode)n).serial);
        }

        @Override
        public void unvisit(Node n) {
            remove(((NodeGraph.AbstractNode)n).serial);
        }

        @Override
        public boolean hasVisited(Node n) {
            return contains(((NodeGraph.AbstractNode)n).serial);
        }

    }

    /** TODO a bitvector based travelog, with a failsafe max limit  */

}
