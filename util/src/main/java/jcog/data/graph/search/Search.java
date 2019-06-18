package jcog.data.graph.search;

import jcog.WTF;
import jcog.data.graph.Node;
import jcog.data.graph.NodeGraph;
import jcog.data.graph.path.FromTo;
import jcog.data.list.Cons;
import jcog.data.list.FasterList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.BooleanObjectPair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
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
 * <p>
 * NOT multi-thread safe in any way.
 */
abstract public class Search<N, E> {

    protected static final List empty = List.of();
    public final TraveLog log;


    protected Search() {
        this(new TraveLog.IntHashTraveLog());
    }

    private Search(TraveLog log) {
        this.log = log;
    }

    public static <N, E> Node<N, E> pathStart(List<BooleanObjectPair<FromTo<Node<N, E>, E>>> path, int n) {
        BooleanObjectPair<FromTo<Node<N, E>, E>> step = path.get(n);
        return step.getTwo().from(step.getOne());
    }

    public static <N, E> Node<N, E> pathStart(List<BooleanObjectPair<FromTo<Node<N, E>, E>>> path) {
        return pathStart(path, 0);
    }

    /**
     * optimized for Cons usage
     */
    public static <N, E> Node<N, E> pathEnd(List<BooleanObjectPair<FromTo<Node<N, E>, E>>> path) {
        BooleanObjectPair<FromTo<Node<N, E>, E>> step = path instanceof Cons ?
                ((Cons<BooleanObjectPair<FromTo<Node<N, E>, E>>>) path).tail : path.get(path.size() - 1);
        return step.getTwo().to(step.getOne());
    }

    abstract protected boolean go(List<BooleanObjectPair<FromTo<Node<N, E>, E>>> path, Node<N, E> next);

    private boolean bfsNode(Node<N, E> start, Queue<Pair<List<BooleanObjectPair<FromTo<Node<N, E>, E>>>, Node<N, E>>> q) {
        if (start == null)
            return true;  //??

        if (!log.visit(start))
            return true; //reached a root via a previous root

        q.clear();

        q.add(Tuples.pair(empty, start));

        Pair<List<BooleanObjectPair<FromTo<Node<N, E>, E>>>, Node<N, E>> n;
        while ((n = q.poll()) != null) {

            final Node<N, E> at = n.getTwo();

            List<BooleanObjectPair<FromTo<Node<N, E>, E>>> path = n.getOne();

            for (FromTo<Node<N, E>, E> e : find(at, path)) {
                Node<N, E> next = next(e, at, path);
                if (next == null || !log.visit(next))
                    continue;

                q.add(Tuples.pair(
                        Cons.the(path, pair(next == e.to(), e)),
                        next));
            }

            if (!path.isEmpty() && !go(path, at))
                return false;
        }

        return true;
    }


    /**
     * can be overridden to hijack the determined next destination
     */
    @Nullable
    protected Node<N, E> next(FromTo<Node<N, E>, E> e, Node<N, E> at, List<BooleanObjectPair<FromTo<Node<N, E>, E>>> path) {
        return e.other(at);
    }


    public boolean dfs(Iterable<Node> startingNodes) {
        return dfs(startingNodes, null);
    }

    public boolean bfs(Iterable<Node> startingNodes) {
        return bfs(startingNodes, new ArrayDeque(), null);
    }

    public boolean dfs(Iterable startingNodes, @Nullable NodeGraph g) {

        List<BooleanObjectPair<FromTo<Node<N, E>, E>>> path = new FasterList(8);

        for (Object n : startingNodes) {
            Node nn;
            if (n instanceof Node)
                nn = (Node) n;
            else {
                nn = g.node(n);
                if (nn == null)
                    throw new WTF();
            }

            if (!dfsNode(nn, path))
                return false;
        }

        return true;
    }


    private boolean dfsNode(Node<N, E> n, List<BooleanObjectPair<FromTo<Node<N, E>, E>>> path) {

        if (!log.visit(n))
            return true;

        Node<N, E> at;
        Iterator<FromTo<Node<N, E>, E>> ii = find(at = n, path).iterator(); //Iterable?

        while (ii.hasNext()) {
            FromTo<Node<N, E>, E> e = ii.next();

            Node<N, E> next = next(e, at, path);

            if (next == null || log.hasVisited(next))
                continue;

            BooleanObjectPair<FromTo<Node<N, E>, E>> move = pair(next == e.to(), e);

            path.add(move);

            if (!go(path, next) || !dfsNode(next, path))
                return false;

            at = n;

            path.remove(path.size() - 1);
        }

        return true;
    }

    protected Iterable<FromTo<Node<N, E>, E>> find(Node<N, E> n, List<BooleanObjectPair<FromTo<Node<N, E>, E>>> path) {
        return n.edges(true, true);
    }

    /**
     * q is recycleable between executions automatically. just provide a pre-allocated ArrayDeque or similar.
     */
    public boolean bfs(Iterable startingNodes, Queue<Pair<List<BooleanObjectPair<FromTo<Node<N, E>, E>>>, Node<N, E>>> q, NodeGraph g) {

        for (Object n : startingNodes) {
            Node nn;
            if (n instanceof Node)
                nn = (Node) n;
            else {
                nn = g.node(n);
                if (nn == null)
                    continue; //assume it has been removed after invocation start
            }

            if (!bfsNode(nn, q))
                return false;
        }

        return true;

    }
}
