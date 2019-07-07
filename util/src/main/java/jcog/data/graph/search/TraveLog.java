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

    static int serial(Node n) {
        return ((NodeGraph.AbstractNode)n).serial;
    }

    final class IntHashTraveLog extends IntHashSet implements TraveLog {

        public IntHashTraveLog(int cap) {
            super(cap);
        }

        @Override
        public boolean visit(Node n) {
            return add(serial(n));
        }

        @Override
        public void unvisit(Node n) {
            remove(serial(n));
        }

        @Override
        public boolean hasVisited(Node n) {
            return contains(serial(n));
        }

    }

    /** TODO a bitvector based travelog, with a failsafe max limit  */

}
