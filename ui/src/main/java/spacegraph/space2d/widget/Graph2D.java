package spacegraph.space2d.widget;

import com.jogamp.opengl.GL2;
import jcog.WTF;
import jcog.data.graph.ImmutableDirectedEdge;
import jcog.data.graph.NodeGraph;
import jcog.data.map.CellMap;
import jcog.data.pool.DequePool;
import jcog.list.FasterList;
import jcog.tree.rtree.rect.RectFloat2D;
import jcog.util.Flip;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.Scale;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.grid.MutableMapContainer;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.meta.AutoSurface;
import spacegraph.space2d.widget.windo.Windo;
import spacegraph.video.Draw;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * 2D directed/undirected graph widget
 */
public class Graph2D<X> extends MutableMapContainer<X, Graph2D.NodeVis<X>> {


    //int edgesMax = 1024;
    protected final AtomicBoolean busy = new AtomicBoolean(false);
    final List<Graph2DLayer<X>> layers = new FasterList<>();

    private final DequePool<NodeVis<X>> nodePool = new DequePool<>(1 * 256) {
        @Override
        public NodeVis<X> create() {
            return new NodeVis<>();
        }
    };
    private final DequePool<EdgeVis<X>> edgePool = new DequePool<>(1 * 256) {
        @Override
        public EdgeVis<X> create() {
            return new EdgeVis<>();
        }
    };

    /** hard limit on # nodes */
    protected int nodesMax = 2048;

    volatile Graph2DLayout<X> layout = (c, d) -> {
    };

    private final transient Set<CellMap.CacheCell<X, Graph2D.NodeVis<X>>> dontRemain = new LinkedHashSet();

    final Consumer<NodeVis<X>> DEFAULT_NODE_BUILDER = x -> {
        x.set(
                new Scale(new PushButton(x.id.toString()), 0.75f)
        );
    };
    private transient Consumer<NodeVis<X>> nodeBuilder = DEFAULT_NODE_BUILDER;

    public Graph2D() {

    }

    public Graph2D<X> layer(Graph2DLayer<X> layout) {
        layers.add(layout);
        return this;
    }

    public Graph2D<X> nodeBuilder(Consumer<NodeVis<X>> nodeBuilder) {
        this.nodeBuilder = nodeBuilder;
        return this;
    }

    public Graph2D<X> layout(Graph2DLayout<X> layout) {
        this.layout = layout;
        return this;
    }

    public Gridding configWidget() {
        Gridding g = new Gridding();
        g.add(new AutoSurface(layout));
        for (Graph2DLayer l : layers) {
            g.add(new AutoSurface(l));
        }
        return g;
    }

    @Override
    protected boolean prePaint(SurfaceRender r) {
        if (super.prePaint(r)) {
            if (!showing())
                throw new WTF();
            layout.layout(this, r.dtMS);
            return true;
        }
        return false;
    }


    @Override
    protected void paintBelow(GL2 gl) {
        cellMap.forEachValue(n -> {
            if (n.visible()) {
                n.paintEdges(gl);
            }
        });
    }

    public Graph2D<X> add(Iterable<X> nodes) {
        if (!busy.get()) //avoids creating the iterator if the graph is busy
            return add(nodes.iterator());
        return this;
    }

    public Graph2D<X> add(Iterator<X> nodes) {
        return update(nodes, true);
    }

    public Graph2D<X> set(Iterable<X> nodes) {
        if (!busy.get()) //avoids creating the iterator if the graph is busy
            return set(nodes.iterator());
        return this;
    }

    //TODO remove(Iterable...)

    public Graph2D<X> set(Iterator<X> nodes) {
        return update(nodes, false);
    }

    @Override
    public void clear() {
        //TODO this wont work if bag is busy
        set(List.of());
    }

    protected Graph2D<X> update(Iterator<X> nodes, boolean addOrReplace) {

        if (!busy.compareAndSet(false, true)) {
            //TODO trigger refresh?
            return this;
        }
        try {

            updateNodes(nodes, addOrReplace);

            updateEdges();

        } finally {
            busy.set(false);
        }

        return this;
    }


    private void updateNodes(Iterator<X> nodes, boolean addOrReplace) {
        dontRemain.clear();
        if (!addOrReplace && !cellMap.cache.isEmpty()) {
            Collections.addAll(dontRemain, cellMap.cache.valueArray());
        }

        int nCount = 0;
        while (nodes.hasNext()) {
            X x = nodes.next();
            if (x == null)
                continue; //ignore nulls in the input

            //TODO computeIfAbsent and re-use existing model
            CellMap.CacheCell nv = cellMap.compute(x, xx -> {
                if (xx == null) {
                    xx = nodePool.get();
                    xx.reset(x);
                    nodeBuilder.accept(xx);
                    layout.initialize(this, xx);
                    return xx;
                } else
                    return xx; //re-use existing
            });

            dontRemain.remove(nv);

            if (nCount++ == nodesMax)
                break; //reached node limit
        }

        if (!dontRemain.isEmpty()) {
            dontRemain.forEach(d -> {
                NodeVis<X> dv = d.value;
                cellMap.remove(d.key, false);
                if (dv!=null) {
                    dv.hide();
                    nodePool.take(dv);
                }
            });
            cellMap.cache.invalidate();
        }
    }

    private void updateEdges() {

        cellMap.forEachValue((NodeVis<X> nv) -> {
            List<EdgeVis<X>> edgesNext = nv.edgeOut.write();
            if (!edgesNext.isEmpty()) {
                edgesNext.forEach(edgePool::take);
                edgesNext.clear();
            }
        });

        cellMap.forEachValue((NodeVis<X> nv) -> {
            layers.forEach(layer -> layer.node(nv, builder));
        });

        cellMap.forEachValue((NodeVis<X> nv) -> {
            nv.edgeOut.commit();
        });
    }

    /** wraps all graph construction procedure in this interface for which layers construct graph with */
    public static class GraphBuilder<X> {

        private final Graph2D<X> graph;

        public GraphBuilder(Graph2D<X> g) {
            this.graph = g;
        }

        /** adds a visible edge between two nodes, if they exist and are visible */
        public @Nullable EdgeVis<X> edge(Object from, Object to) {
            @Nullable NodeVis<X> fromNode = graph.cellMap.get(from);
            return fromNode != null ? edge(fromNode, to) : null;
        }

        /** adds a visible edge between two nodes, if they exist and are visible */
        public @Nullable EdgeVis<X> edge(NodeVis<X> from, Object to) {
            if (!from.visible())
                return null;

            @Nullable NodeVis<X> t = graph.cellMap.getValue(to);
            if (t == null || !t.visible())
                return null;

            EdgeVis<X> result = graph.edgePool.get();
            result.to = t;
            from.edgeOut.write().add(result);
            return result;
        }
    }

    final GraphBuilder builder = new GraphBuilder(this);


    public interface Graph2DLayout<X> {

        void layout(Graph2D<X> g, int dtMS);

        /**
         * set an initial location (and/or size) for a newly created NodeVis
         */
        default void initialize(Graph2D<X> g, NodeVis<X> n) {
            float gw = g.w();
            float gh = g.h();
            int count = g.cellMap.cache.size();
            float defaultSize = (float) (Math.min(gw, gh) / Math.sqrt(count + 1));

            n.pos(RectFloat2D.XYWH(
                    g.x() + (gw / 2 - gw / 4) + (float) Math.random() * gw / 2f,
                    g.y() + (gh / 2 - gh / 4) + (float) Math.random() * gh / 2f,
                    defaultSize, defaultSize
            ));
        }
    }

    //    public Graph2D<X> commit(Bag<?, X> g) {
//        return commit(g, (nothing) -> null);
//    }
//
//    public Graph2D<X> commit(Graph<X> g) {
//        return commit(g.nodes(), g::successors);
//    }
//
//    public Graph2D<X> commit(SuccessorsFunction<X> g, X start) {
//        return commit(g, List.of(start));
//    }
//
//    public Graph2D<X> commit(SuccessorsFunction<X> s, Iterable<X> start) {
//        return commit(new MapNodeGraph<>(s, start));
//    }
//
//    public Graph2D<X> commit(AdjGraph<X, Object> g) {
//        return commit(
//                Iterables.transform(g.nodes.keySet(), t -> t.v),
//                (X x) -> {
//                    List<X> adj = new FasterList();
//                    g.neighborEdges(x, (v, e) -> {
//                        adj.add(v);
//                    });
//                    return adj;
//                }
//        );
//    }
//
//    public Graph2D<X> commit(MapNodeGraph<X, Object> g) {
//        return commit(
//                Iterables.transform(g.nodes(), x -> x.id),
//                x -> Iterables.transform(
//                        g.node(x).edges(false, true),
//                        //zz -> zz.id //edge label
//                        zz -> zz.to.id //edge target
//                ));
//    }

    /**
     * layer of the graph, responsible for materializing and settng visual properties from input
     */
    @FunctionalInterface
    public interface Graph2DLayer<X> {
        /**
         * called for each node being processed.  can edit the NodeVis
         * and generate new links from it to target nodes.
         */
        void node(NodeVis<X> node, GraphBuilder<X> graph);

    }

    public static class NodeVis<X> extends Windo {

        public X id;
        public final Flip<List<EdgeVis<X>>> edgeOut = new Flip<>(FasterList::new);
        private float r, g, b;

        protected void reset(X id) {
            this.id = id;
            show();
            r = g = b = 0.5f;
        }

        @Override
        public boolean stop() {
            if (super.stop()) {
                this.id = null;
                clear();
                edgeOut.write().clear();
                edgeOut.commit();
                edgeOut.write().clear();
                return true;
            }
            return false;
        }

        protected void paintEdges(GL2 gl) {
            edgeOut.read().forEach(x -> x.draw(gl, this));
        }

        @Override
        protected void paintBelow(GL2 gl) {
            float alpha = 0.8f;//weight * 0.5f + 0.5f; //TODO make variable
            gl.glColor4f(r, g, b, alpha);
            Draw.rect(gl, bounds);
        }

        public void color(float r, float g, float b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }

        public boolean pinned() {
            //TODO
            return false;
        }
    }

    public static class EdgeVis<X> {
        public NodeVis<X> to;
        public float r = 0.5f,
                g = 0.5f,
                b = 0.5f;
        public float weight = 1f;


        public EdgeVis<X> weight(float w) {
            weight = w;
            return this;
        }

        public EdgeVis<X> color(float r, float g, float b) {
            this.r = r;
            this.g = g;
            this.b = b;
            return this;
        }

        public void draw(GL2 gl, NodeVis<X> from) {
            gl.glColor3f(r, g, b);
            float x = from.cx();
            float y = from.cy();
            gl.glLineWidth(1f + weight * 4f);
            Draw.line(gl, x, y, to.cx(), to.cy());
        }
    }

    /**
     * layer which renders NodeGraph nodes and edges
     */
    public static class NodeGraphLayer<N, E> implements Graph2D.Graph2DLayer<N> {
        @Override
        public void node(NodeVis<N> node, GraphBuilder<N> graph) {
            if (node.id instanceof NodeGraph.Node) {
                NodeGraph.Node<N, E> nn = (NodeGraph.Node<N, E>) node.id;
                nn.edges(false, true).forEach((ImmutableDirectedEdge<N, E> e) -> {
                    Graph2D.EdgeVis<N> ee = graph.edge(node, e.other(nn));//.color(0.5f, 0.5f, 0.5f);
                    //ee.color(0.8f, 0.8f, 0.8f);

                });
                node.color(0.5f, 0.5f, 0.5f);
                node.move((float) Math.random() * 100, (float) Math.random() * 100);
                node.size(20f, 10f);
            }
        }
    }


}
