package jcog.data.graph.search;

import com.google.common.collect.Iterators;
import jcog.data.graph.ImmutableDirectedEdge;
import jcog.data.graph.NodeGraph;
import jcog.data.list.Cons;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.BooleanObjectPair;
import org.eclipse.collections.impl.tuple.Tuples;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

/**
 * a search process instance
 * <p>
 * general purpose recursive search for DFS/BFS/A* algorithms
 * backtrack/cyclic prevention guaranteed to visit each vertex at most once.
 * - an instance may be recycled multiple times
 * - multiple instances may concurrently access the same graph
 */
abstract public class Search<N, E> {

    public final TraveLog log;
    public List<BooleanObjectPair<ImmutableDirectedEdge<N, E>>> path = null;
    protected NodeGraph.Node<N, E> at = null;

    protected Search() {
        this(new TraveLog.IntHashTraveLog());
    }

    protected Search(TraveLog log) {
        this.log = log;
    }

    public void start() {

    }

    public void stop() {
        at = null;
        log.clear();
        path = null;
    }

    public boolean bfs(NodeGraph.Node<N,E> start, Queue<Pair<List<BooleanObjectPair<ImmutableDirectedEdge<N, E>>>, NodeGraph.Node<N,E>>> q) {


        q.add(Tuples.pair(path = Collections.emptyList(), start));
        log.visit(start);

        Pair<List<BooleanObjectPair<ImmutableDirectedEdge<N, E>>>, NodeGraph.Node<N,E>> current;
        while ((current = q.poll()) != null) {

            NodeGraph.Node<N, E> at = this.at = current.getTwo();

            List<BooleanObjectPair<ImmutableDirectedEdge<N, E>>> path = current.getOne();
            this.path = path;


            for (ImmutableDirectedEdge<N, E> e : next(at)) {
                NodeGraph.Node<N, E> next = e.other(at);
                if (!log.visit(next))
                    continue;

                List<BooleanObjectPair<ImmutableDirectedEdge<N, E>>> pp = Cons.the(path, pair(next == e.to, e));
                q.add(Tuples.pair(pp, next));
            }


            NodeGraph.Node<N,E> next = current.getTwo();
            if (start != next) {
                BooleanObjectPair<ImmutableDirectedEdge<N, E>> move =
                        path instanceof Cons ? ((Cons<BooleanObjectPair<ImmutableDirectedEdge<N, E>>>) path).tail : path.get(path.size() - 1);
                
                if (!next(move, next))
                    return false; 

            }

        }


        return true;
    }

    public boolean dfs(NodeGraph.Node<N, E> current) {

        if (!log.visit(current))
            return true; 

        Iterator<ImmutableDirectedEdge<N, E>> n = next(current).iterator();
        if (!n.hasNext())
            return true;

        this.at = current;

        return Iterators.all(n, e -> {

            NodeGraph.Node<N, E> next = e.other(this.at);

            if (log.hasVisited(next))
                return true; 

            BooleanObjectPair<ImmutableDirectedEdge<N, E>> move = pair(next == e.to, e);

            
            path.add(move);

            
            if (!next(move, next) || !dfs(next))
                return false; 

            
            this.at = current;
            path.remove(path.size() - 1);

            return true;
        });

    }

    protected Iterable<ImmutableDirectedEdge<N, E>> next(NodeGraph.Node<N, E> current) {
        return current.edges(true, true);
    }

    abstract protected boolean next(BooleanObjectPair<ImmutableDirectedEdge<N, E>> move, NodeGraph.Node<N,E> next);
}
