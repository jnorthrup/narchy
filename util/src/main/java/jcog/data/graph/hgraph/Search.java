package jcog.data.graph.hgraph;

import com.google.common.collect.Iterables;
import jcog.list.FasterList;
import org.eclipse.collections.api.tuple.primitive.BooleanObjectPair;

import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

/**
 *  a search process instance
 *
 *  general purpose recursive search for DFS/BFS/A* algorithms
 *  backtrack/cyclic prevention guaranteed to visit each vertex at most once.
 *  - an instance may be recycled multiple times
 *  - multiple instances may concurrently access the same graph
 */
abstract public class Search<N, E> {

    public final TraveLog log;
    public final FasterList<BooleanObjectPair<Edge<N, E>>> path = new FasterList();
    protected Node<N, E> at = null;

    protected Search() {
        this(new TraveLog.IntHashTraveLog());
    }

    protected Search(TraveLog log) {
        this.log = log;
    }

    public void start() {
        path.clear();
        log.clear();
        at = null;
    }

    public void stop() {


    }





    /**
     * the (set of) edges provided to be traversed in the next exploration iteration.
     * subclasses may filter, reorder, or grow.
     * after each call, the state of this instance will have changed appropriately.
     * the callback recipient can remain stateless
     * */
    protected boolean visit(Node<N, E> current) {

        if (!log.visit(current))
            return true; //skip

        this.at = current;

        return Iterables.all(next(current), e -> {

            Node<N, E> next = e.other(this.at);

            if (log.isVisited(next))
                return true; //pre-skip, avoiding some work

            BooleanObjectPair<Edge<N, E>> move = pair(next == e.to, e);

            //push
            path.add(move);

            //guard
            if (!next(move, next))
                return false;

            //recurse
            if (!visit(next))
                return false; //leaves path intact on exit

            //pop
            this.at = current;
            path.removeLast();

            return true;
        });

    }

    protected Iterable<Edge<N, E>> next(Node<N, E> current) {
        return current.edges(true, true);
    }

    abstract protected boolean next(BooleanObjectPair<Edge<N, E>> move, Node<N, E> next);
}
