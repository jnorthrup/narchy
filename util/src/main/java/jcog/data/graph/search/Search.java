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
 *
 * NOT multi-thread safe in any way.
 */
abstract public class Search<N, E> {

    public final TraveLog log;
    public List<BooleanObjectPair<FromTo<Node<N,E>,E>>> path = null;
    private Node<N, E> at = null;

    protected Search() {
        this(new TraveLog.IntHashTraveLog());
    }

    private Search(TraveLog log) {
        this.log = log;
    }

    abstract protected boolean next(BooleanObjectPair<FromTo<Node<N,E>,E>> move, Node<N,E> next);

    private void start() {

    }

    private void stop() {
        at = null;
        path = null;
    }

    public static <N,E> Node<N,E> pathStart(List<BooleanObjectPair<FromTo<Node<N,E>,E>>> path, int n) {
        BooleanObjectPair<FromTo<Node<N,E>,E>> step = path.get(n);
        return step.getTwo().from(step.getOne());
    }

    public static <N,E> Node<N,E> pathStart(List<BooleanObjectPair<FromTo<Node<N,E>,E>>> path) {
        return pathStart(path, 0);
    }

    /** optimized for Cons usage */
    public static <N,E> Node<N,E> pathEnd(List<BooleanObjectPair<FromTo<Node<N, E>, E>>> path) {
        BooleanObjectPair<FromTo<Node<N,E>,E>> step = path instanceof Cons ?
                ((Cons<BooleanObjectPair<FromTo<Node<N,E>,E>>>) path).tail : path.get(path.size() - 1);
        return step.getTwo().to(step.getOne());
    }
    
    private boolean bfsNode(Node<N,E> start, Queue<Pair<List<BooleanObjectPair<FromTo<Node<N,E>,E>>>, Node<N,E>>> q) {
        if (start==null)
            return true;  //??

        q.add(Tuples.pair(path = Collections.emptyList(), start));

        if (!log.visit(start)) {
            return true; //reached a root via a previous root
        }

        Pair<List<BooleanObjectPair<FromTo<Node<N,E>,E>>>, Node<N,E>> current;
        while ((current = q.poll()) != null) {

            Node<N, E> at = this.at = current.getTwo();

            path = current.getOne();

            Iterator<FromTo<Node<N, E>, E>> iterator = next(at);

            for (; iterator.hasNext(); ) {
                FromTo<Node<N, E>, E> e = iterator.next();
                Node<N, E> next = next(e, at);
                if (next == null || !log.visit(next))
                    continue;


                q.add(Tuples.pair(
                        Cons.the(path, pair(next == e.to(), e)),
                        next));
            }


            if (!path.isEmpty()) {
                Node<N, E> next = current.getTwo();
                if (start != next) {
                BooleanObjectPair<FromTo<Node<N, E>, E>> move =
                        path instanceof Cons ? ((Cons<BooleanObjectPair<FromTo<Node<N, E>, E>>>) path).tail : path.get(path.size() - 1);

                if (!next(move, next))
                    return false;

                }
            }

        }


        return true;
    }



    /** can be overridden to hijack the determined next destination */
    @Nullable
    protected Node<N, E> next(FromTo<Node<N, E>, E> e, Node<N, E> at) {
        return e.other(at);
    }



    public boolean dfs(Iterable<Node> startingNodes){
        return dfs(startingNodes, null);
    }

    public boolean dfs(Iterable startingNodes, @Nullable NodeGraph g){

        start();

        try {

            path = new FasterList(8);

            for (Object n : startingNodes) {
                Node nn;
                if (n instanceof Node)
                    nn = (Node)n;
                else {
                    nn = g.node(n);
                    if (nn==null)
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

        Iterator<FromTo<Node<N,E>,E>> ii = next(n);
        if (!ii.hasNext())
            return true;

        this.at = n;

        return Iterators.all(ii, e -> {

            Node<N,E> next = next(e, at);

            if (next == null || log.hasVisited(next))
                return true; 

            BooleanObjectPair<FromTo<Node<N,E>,E>> move = pair(next == e.to(), e);

            
            path.add(move);

            
            if (!next(move, next) || !dfsNode(next))
                return false; 

            
            this.at = n;
            path.remove(path.size() - 1);

            return true;
        });

    }

    protected Iterator<FromTo<Node<N,E>,E>> next(Node<N,E> current) {
        return current.edgeIterator(true, true);
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
                    nn = (Node)n;
                else {
                    nn = g.node(n);
                    if (nn==null)
                        throw new WTF();
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
