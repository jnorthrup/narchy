package jcog.data.graph.hgraph;

import com.google.common.collect.Iterators;
import jcog.list.Cons;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.BooleanObjectPair;
import org.eclipse.collections.impl.tuple.Tuples;

import java.util.*;

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
    public List<BooleanObjectPair<Edge<N, E>>> path = null;
    protected Node<N, E> at = null;

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

    protected boolean bfs(Node<N, E> start, Queue<Pair<List<BooleanObjectPair<Edge<N, E>>>, Node<N, E>>> q) {


        q.add(Tuples.pair(path = Collections.emptyList(), start));


        Pair<List<BooleanObjectPair<Edge<N, E>>>, Node<N, E>> current;
        while ((current = q.poll()) != null) {

            Node<N, E> at = this.at = current.getTwo();

            List<BooleanObjectPair<Edge<N, E>>> path = current.getOne();
            this.path = path;


            Iterator<Edge<N, E>> ee = next(at).iterator();
            while (ee.hasNext()) {
                Edge<N, E> e = ee.next();
                Node<N, E> next = e.other(at);
                if (!log.visit(next))
                    continue;

                List<BooleanObjectPair<Edge<N, E>>> pp = Cons.the(path, pair(next == e.to, e));
                q.add(Tuples.pair(pp, next));
            }


            Node<N, E> next = current.getTwo();
            if (start != next) {
                BooleanObjectPair<Edge<N, E>> move =
                        path instanceof Cons ? ((Cons<BooleanObjectPair<Edge<N, E>>>) path).tail : path.get(path.size() - 1);
                //guard
                if (!next(move, next))
                    return false; //leaves path intact on exit

            }

        }


        return true;
    }

    protected boolean dfs(Node<N, E> current) {

        if (!log.visit(current))
            return true; //skip

        Iterator<Edge<N, E>> n = next(current).iterator();
        if (!n.hasNext())
            return true;

        this.at = current;

        return Iterators.all(n, e -> {

            Node<N, E> next = e.other(this.at);

            if (log.hasVisited(next))
                return true; //pre-skip, avoiding some work

            BooleanObjectPair<Edge<N, E>> move = pair(next == e.to, e);

            //push
            path.add(move);

            //guard
            if (!next(move, next) || !dfs(next))
                return false; //leaves path intact on exit

            //pop
            this.at = current;
            path.remove(path.size() - 1);

            return true;
        });

    }

    protected Iterable<Edge<N, E>> next(Node<N, E> current) {
        return current.edges(true, true);
    }

    abstract protected boolean next(BooleanObjectPair<Edge<N, E>> move, Node<N, E> next);
}
