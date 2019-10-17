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
import java.util.Collections;
import java.util.List;
import java.util.Queue;

import static jcog.data.graph.search.TraveLog.id;
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
public abstract class Search<N, E> {

    public final TraveLog log;


    protected Search() {
        this(
            //new TraveLog.IntHashTraveLog(0)
            new TraveLog.RoaringHashTraveLog()
        );
    }

    private Search(TraveLog log) {
        this.log = log;
    }

    public void clear() {
        log.clear();
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

    protected abstract boolean go(List<BooleanObjectPair<FromTo<Node<N, E>, E>>> path, Node<N, E> next);

    protected boolean visit(Node n) {
        return log.visit(id(n));
    }
    protected boolean visited(Node n) {
        return log.hasVisited(id(n));
    }

    private boolean bfsNode(Node<N, E> start, Queue<Pair<List<BooleanObjectPair<FromTo<Node<N, E>, E>>>, Node<N, E>>> q) {
//        if (start == null)
//            return true;  //??

        if (!visit(start))
            return true; //reached a root via a previous root

        q.clear();

        q.add(Tuples.pair(Collections.EMPTY_LIST, start));

        Pair<List<BooleanObjectPair<FromTo<Node<N, E>, E>>>, Node<N, E>> n;
        while ((n = q.poll()) != null) {

            final Node<N, E> at = n.getTwo();

            List<BooleanObjectPair<FromTo<Node<N, E>, E>>> path = n.getOne();

            for (FromTo<Node<N, E>, E> e : find(at, path)) {
                Node<N, E> next = next(e, at, path);
                if (next == null || !visit(next))
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
    protected @Nullable Node<N, E> next(FromTo<Node<N, E>, E> e, Node<N, E> at, List<BooleanObjectPair<FromTo<Node<N, E>, E>>> path) {
        return e.other(at);
    }

    public boolean dfs(Iterable<?> startingNodes) {
        return dfs(startingNodes, null);
    }

    public boolean bfs(Object startingNode) {
        return bfs(List.of(startingNode));
    }

    public boolean bfs(Iterable<?> startingNodes) {
        return bfs(startingNodes, new ArrayDeque<>(), null);
    }

    public boolean dfs(Iterable<?> startingNodes, @Nullable NodeGraph<N,E> g) {

        List<BooleanObjectPair<FromTo<Node<N, E>, E>>> path = new FasterList<>(4);

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


    private boolean dfsNode(final Node<N, E> n, List<BooleanObjectPair<FromTo<Node<N, E>, E>>> path) {
        boolean result = true;

        if (visit(n)) {
            for (FromTo<Node<N, E>, E> e : find(n, path)) {

                Node<N, E> next = next(e, n, path);

                if (next == null || visited(next))
                    continue;

                BooleanObjectPair<FromTo<Node<N, E>, E>> move = pair(next == e.to(), e);

                path.add(move);

                if (!go(path, next) || !dfsNode(next, path)) {
                    result = false;
                    break;
                }

                ((FasterList)path).removeLastFast();
            }
        }

        return result;
    }

    protected Iterable<FromTo<Node<N, E>, E>> find(Node<N, E> n, List<BooleanObjectPair<FromTo<Node<N, E>, E>>> path) {
        return n.edges(true, true);
    }

    /**
     * q is recycleable between executions automatically. just provide a pre-allocated ArrayDeque or similar.
     */
    public boolean bfs(Iterable<?> startingNodes, Queue<Pair<List<BooleanObjectPair<FromTo<Node<N, E>, E>>>, Node<N, E>>> q, NodeGraph g) {

        for (Object n : startingNodes) {
            Node<N,E> nn;
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
