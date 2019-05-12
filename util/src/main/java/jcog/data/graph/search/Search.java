package jcog.data.graph.search;

import com.google.common.collect.Iterators;
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

    private static final List empty = List.of();
    public final TraveLog log;
    public List<BooleanObjectPair<FromTo<Node<N, E>, E>>> path = null;
    private Node<N, E> at = null;

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

    abstract protected boolean next(BooleanObjectPair<FromTo<Node<N, E>, E>> move, Node<N, E> next);

    private void start() {

    }

    private void stop() {
        at = null;
        path = null;
    }

    private boolean bfsNode(Node<N, E> start, Queue<Pair<List<BooleanObjectPair<FromTo<Node<N, E>, E>>>, Node<N, E>>> q) {
        if (start == null)
            return true;  //??


        q.add(Tuples.pair(this.path = empty, start));

        if (!log.visit(start))
            return true; //reached a root via a previous root

        Pair<List<BooleanObjectPair<FromTo<Node<N, E>, E>>>, Node<N, E>> current;
        while ((current = q.poll()) != null) {

            final Node<N, E> at = this.at = current.getTwo();

            this.path = current.getOne();

            for (FromTo<Node<N, E>, E> e : next(at)) {
                Node<N, E> next = next(e, at);
                if (next == null || !log.visit(next))
                    continue;


                q.add(Tuples.pair(
                        Cons.the(this.path, pair(next == e.to(), e)),
                        next));
            }


            if (!this.path.isEmpty()) {

//                if (start != at) {
                    BooleanObjectPair<FromTo<Node<N, E>, E>> move =
                            this.path instanceof Cons ? ((Cons<BooleanObjectPair<FromTo<Node<N, E>, E>>>) this.path).tail : this.path.get(this.path.size() - 1);

                    if (!next(move, at))
                        return false;
//                }
            }

        }


        return true;
    }


    /**
     * can be overridden to hijack the determined next destination
     */
    @Nullable
    protected Node<N, E> next(FromTo<Node<N, E>, E> e, Node<N, E> at) {
        return e.other(at);
    }


    public boolean dfs(Iterable<Node> startingNodes) {
        return dfs(startingNodes, null);
    }

    public boolean dfs(Iterable startingNodes, @Nullable NodeGraph g) {

        start();

        try {

            path = new FasterList(8);

            for (Object n : startingNodes) {
                Node nn;
                if (n instanceof Node)
                    nn = (Node) n;
                else {
                    nn = g.node(n);
                    if (nn == null)
                        throw new WTF();
                }

                if (!dfsNode(nn))
                    return false;
            }

            return true;
        } finally {
            stop();
        }
    }


    private boolean dfsNode(Node<N, E> n) {

        if (!log.visit(n))
            return true;

        Iterator<FromTo<Node<N, E>, E>> ii = next(n).iterator(); //Iterable?
        if (!ii.hasNext())
            return true;

        this.at = n;

        return Iterators.all(ii, e -> {

            Node<N, E> next = next(e, at);

            if (next == null || log.hasVisited(next))
                return true;

            BooleanObjectPair<FromTo<Node<N, E>, E>> move = pair(next == e.to(), e);


            path.add(move);


            if (!next(move, next) || !dfsNode(next))
                return false;


            this.at = n;
            path.remove(path.size() - 1);

            return true;
        });

    }

    protected Iterable<FromTo<Node<N, E>, E>> next(Node<N, E> n) {
        return n.edges(true, true);
    }

    /**
     * q is recycleable between executions automatically. just provide a pre-allocated ArrayDeque or similar.
     */
    public boolean bfs(Iterable startingNodes, Queue<Pair<List<BooleanObjectPair<FromTo<Node<N, E>, E>>>, Node<N, E>>> q, NodeGraph g) {

        start();

        try {

            for (Object n : startingNodes) {
                Node nn;
                if (n instanceof Node)
                    nn = (Node) n;
                else {
                    nn = g.node(n);
                    if (nn == null)
                        continue; //assume it has been removed after invocation start
                }

                q.clear();
                if (!bfsNode(nn, q))
                    return false;
            }

            return true;
        } finally {
            stop();
            q.clear();
        }
    }
}
