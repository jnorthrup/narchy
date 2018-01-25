package jcog.data.graph.hgraph;

import com.google.common.collect.Iterators;
import jcog.list.FasterList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.BooleanObjectPair;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.tuple.Tuples;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

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
    public FasterList<BooleanObjectPair<Edge<N, E>>> path = new FasterList();
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

    protected boolean bfs(Node<N, E> start) {

        Queue<Pair<FasterList<BooleanObjectPair<Edge<N, E>>>,Node<N,E>>> q = new ArrayDeque();
        q.add(Tuples.pair(path = new FasterList(0),start));

        log.visit(start);


        Pair<FasterList<BooleanObjectPair<Edge<N, E>>>, Node<N, E>> current;
        while ((current = q.poll())!=null) {

            path = current.getOne();

            Iterator<Edge<N, E>> ee = next(this.at = current.getTwo()).iterator();
            while (ee.hasNext()) {
                Edge<N, E> e = ee.next();
                Node<N, E> next = e.other(at);
                if (!log.visit(next))
                    continue;


                {


                    FastList<BooleanObjectPair<Edge<N, E>>> pp = path.clone();
                    pp.add(pair(next == e.to, e));
                    q.add(Tuples.pair((FasterList) pp,next));



                }


            }

            Node<N, E> next = current.getTwo();
            if (start!=next) {
                BooleanObjectPair<Edge<N, E>> move = current.getOne().getLast();
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
            path.removeLast();

            return true;
        });

    }

    protected Iterable<Edge<N, E>> next(Node<N, E> current) {
        return current.edges(true, true);
    }

    abstract protected boolean next(BooleanObjectPair<Edge<N, E>> move, Node<N, E> next);
}
