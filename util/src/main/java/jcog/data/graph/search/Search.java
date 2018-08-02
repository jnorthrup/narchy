package jcog.data.graph.search;

import com.google.common.collect.Iterators;
import jcog.data.graph.FromTo;
import jcog.data.graph.Node;
import jcog.data.list.Cons;
import jcog.data.list.FasterList;
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
 *
 * NOT multi-thread safe in any way.
 */
abstract public class Search<N, E> {

    public final TraveLog log;
    public List<BooleanObjectPair<FromTo<Node<N,E>,E>>> path = null;
    protected Node<N, E> at = null;

    protected Search() {
        this(new TraveLog.IntHashTraveLog());
    }

    protected Search(TraveLog log) {
        this.log = log;
    }

    abstract protected boolean next(BooleanObjectPair<FromTo<Node<N,E>,E>> move, Node<N,E> next);

    public void start() {

    }

    public void stop() {
        at = null;
        log.clear();
        path = null;
    }

    private boolean bfs(Node<N,E> start, Queue<Pair<List<BooleanObjectPair<FromTo<Node<N,E>,E>>>, Node<N,E>>> q) {


        q.add(Tuples.pair(path = Collections.emptyList(), start));
        log.visit(start);

        Pair<List<BooleanObjectPair<FromTo<Node<N,E>,E>>>, Node<N,E>> current;
        while ((current = q.poll()) != null) {

            Node<N, E> at = this.at = current.getTwo();

            List<BooleanObjectPair<FromTo<Node<N,E>,E>>> path = current.getOne();
            this.path = path;


            for (FromTo<Node<N,E>,E> e : next(at)) {
                Node<N,E> next = e.other(at);
                if (!log.visit(next))
                    continue;

                List<BooleanObjectPair<FromTo<Node<N,E>,E>>> pp = Cons.the(path, pair(next == e.to(), e));
                q.add(Tuples.pair(pp, next));
            }


            Node<N,E> next = current.getTwo();
            if (start != next) {
                BooleanObjectPair<FromTo<Node<N,E>,E>> move =
                        path instanceof Cons ? ((Cons<BooleanObjectPair<FromTo<Node<N,E>,E>>>) path).tail : path.get(path.size() - 1);
                
                if (!next(move, next))
                    return false; 

            }

        }


        return true;
    }

    public boolean dfs(Iterable<Node<N,E>> startingNodes){
        return dfsNodes(startingNodes);
    }

    private boolean dfsNodes(Iterable<Node<N,E>> startingNodes) {

        start();
        try {

            path = new FasterList(8);

            for (Node n : startingNodes)
                if (!dfs(n))
                    return false;

            return true;
        } finally {
            stop();
        }
    }


    private boolean dfs(Node<N, E> current) {

        if (!log.visit(current))
            return true; 

        Iterator<FromTo<Node<N,E>,E>> n = next(current).iterator();
        if (!n.hasNext())
            return true;

        this.at = current;

        return Iterators.all(n, e -> {

            Node<N,E> next = e.other(this.at);

            if (log.hasVisited(next))
                return true; 

            BooleanObjectPair<FromTo<Node<N,E>,E>> move = pair(next == e.to(), e);

            
            path.add(move);

            
            if (!next(move, next) || !dfs(next))
                return false; 

            
            this.at = current;
            path.remove(path.size() - 1);

            return true;
        });

    }

    protected Iterable<FromTo<Node<N,E>,E>> next(Node<N,E> current) {
        return current.edges(true, true);
    }



    public boolean bfs(Node<N,E> startingNodes) {
        return bfs(List.of(startingNodes));
    }

    public boolean bfs(Iterable<Node<N,E>> startingNodes) {
        return bfs(startingNodes, new ArrayDeque());
    }

    /**
     * q is recycleable between executions automatically. just provide a pre-allocated ArrayDeque or similar.
     */
    public boolean bfs(Iterable<Node<N,E>> startingNodes, Queue<Pair<List<BooleanObjectPair<FromTo<Node<N, E>, E>>>, Node<N, E>>> q) {

        start();
        try {

            for (Node<N,E> n : startingNodes) {
                q.clear();
                if (!bfs(n, q))
                    return false;
            }

            return true;
        } finally {
            stop();
            q.clear();
        }
    }
}
