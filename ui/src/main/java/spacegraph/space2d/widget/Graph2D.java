package spacegraph.space2d.widget;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.data.graph.ImmutableDirectedEdge;
import jcog.data.graph.NodeGraph;
import jcog.data.list.FasterList;
import jcog.data.map.CellMap;
import jcog.data.map.ConcurrentFastIteratingHashMap;
import jcog.data.pool.DequePool;
import jcog.util.Flip;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.Scale;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.grid.MutableMapContainer;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space2d.widget.windo.Windo;
import spacegraph.util.MovingRectFloat2D;
import spacegraph.video.Draw;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static jcog.Util.sqr;

/**
 * 2D directed/undirected graph widget
 * TODO generify for use in Dynamics3D
 */
public class Graph2D<X> extends MutableMapContainer<X, Graph2D.NodeVis<X>> {

    private final AtomicBoolean busy = new AtomicBoolean(false);
    private final List<Graph2DLayer<X>> layers = new FasterList<>();

    private final DequePool<NodeVis<X>> nodePool = new DequePool<>(1024) {
        @Override
        public NodeVis<X> create() {
            return new NodeVis<>();
        }
    };
    private final DequePool<EdgeVis<X>> edgePool = new DequePool<>(2048) {
        @Override
        public EdgeVis<X> create() {
            return new EdgeVis<>();
        }

        @Override
        public void take(EdgeVis<X> i) {
            i.clear();
            super.take(i);
        }
    };

    private int nodesMax = 2048;

    private volatile Graph2DLayout<X> layout = (c, d) -> {
    };

    private final transient Set<X> dontRemain = new LinkedHashSet();

    private final Consumer<NodeVis<X>> DEFAULT_NODE_BUILDER = x -> x.set(
            new Scale(new PushButton(x.id.toString()), 0.75f)
    );
    private transient Consumer<NodeVis<X>> nodeBuilder = DEFAULT_NODE_BUILDER;

    public Graph2D() {

    }

    public Graph2D<X> layer(Graph2DLayer<X> l) {
        layers.add(l);
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
        g.add(new ObjectSurface(layout));
        for (Graph2DLayer l : layers) {
            g.add(new ObjectSurface(l));
        }
        return g;
    }

    public int nodes() {
        return cellMap.size();
    }

    @Override
    protected boolean prePaint(SurfaceRender r) {
        if (super.prePaint(r)) {
            layout.layout(this, r.dtMS);
            return true;
        }
        return false;
    }


    @Override
    protected void doLayout(int dtMS) {

    }

    @Override
    protected void paintBelow(GL2 gl) {
        cellMap.forEachValue(n -> {
            //if (n.visible()) {
                n.paintEdges(gl);
            //}
        });
    }

    public final Graph2D<X> add(Stream<X> nodes) {
        return add(nodes::iterator);
    }
    public final Graph2D<X> set(Stream<X> nodes) {
        return set(nodes::iterator);
    }
    public final Graph2D<X> add(Iterable<X> nodes) {
        return update(nodes, true);
    }
    public final Graph2D<X> set(Iterable<X> nodes) {
        return update(nodes, false);
    }

    @Override
    public void clear() {
        
        set(List.of());
    }

    private Graph2D<X> update(Iterable<X> nodes, boolean addOrReplace) {

        if (!busy.weakCompareAndSetAcquire(false, true)) {
            return this;
        }
        try {

            updateNodes(nodes.iterator(), addOrReplace);

            updateEdges();

        } finally {
            busy.setRelease(false);
        }

        return this;
    }


    private void updateNodes(Iterator<X> nodes, boolean addOrReplace) {
        dontRemain.clear();
        if (!addOrReplace) {
            cellMap.cache.forEach((k,v)->dontRemain.add(k));
        }

        int nCount = 0;
        while (nodes.hasNext()) {
            X x = nodes.next();
            if (x == null)
                continue; 

            
            CellMap.CacheCell nv = cellMap.compute(x, xx -> {
                if (xx == null) {
                    xx = nodePool.get();
                    xx.reset(x);
                    nodeBuilder.accept(xx);
                    layout.initialize(this, xx);
                    xx.show();
                    return xx;
                } else
                    return xx; 
            });

            dontRemain.remove(nv.key);

            if (nCount++ == nodesMax())
                break; 
        }

        if (!dontRemain.isEmpty()) {
            dontRemain.forEach(d -> {
                cellMap.remove(d, false);
            });
            cellMap.cache.invalidate();
        }
    }

    @Override
    protected void removing(X key, NodeVis<X> dv) {
        dv.hide();
        nodePool.take(dv);
    }

    private void updateEdges() {

        cellMap.forEachValue((NodeVis<X> nv) -> {
            ConcurrentFastIteratingHashMap<X, EdgeVis<X>> edgesNext = nv.edgeOut.write();
            if (!edgesNext.isEmpty()) {
                edgesNext.forEachValue(edgePool::take);
                edgesNext.clear();
            }
        });

        //cellMap.forEachValue((NodeVis<X> nv) -> layers.forEach(layer -> layer.node(nv, builder)));
        layers.forEach(layer -> cellMap.forEachValue((NodeVis<X> nv) -> layer.node(nv, builder)));

        cellMap.forEachValue(NodeVis::commitEdges);
    }

    /** hard limit on # nodes */
    public int nodesMax() {
        return nodesMax;
    }

    public void nodesMax(int nodesMax) {
        this.nodesMax = nodesMax;
    }

    /** wraps all graph construction procedure in this interface for which layers construct graph with */
    public static class GraphBuilder<X> {

        private final Graph2D<X> graph;

        GraphBuilder(Graph2D<X> g) {
            this.graph = g;
        }

        /** adds a visible edge between two nodes, if they exist and are visible */
        public @Nullable EdgeVis<X> edge(Object from, Object to) {
            @Nullable NodeVis<X> fromNode = graph.cellMap.get(from);
            return fromNode != null ? edge(fromNode, to) : null;
        }

        /** adds a visible edge between two nodes, if they exist and are visible */
        public @Nullable EdgeVis<X> edge(NodeVis<X> from, Object to) {
//            if (!from.visible())
//                return null;

            @Nullable NodeVis<X> t = graph.cellMap.getValue(to);
            if (t == null || !t.visible())
                return null;

            EdgeVis<X> result = graph.edgePool.get();
            result.to = t;
            return from.edgeAdd(result);
        }

    }

    private final GraphBuilder builder = new GraphBuilder(this);


    public interface Graph2DLayout<X> {

        void layout(Graph2D<X> g, int dtMS);

        /**
         * set an initial location (and/or size) for a newly created NodeVis
         */
        default void initialize(Graph2D<X> g, NodeVis<X> n) {
//            float gw = g.w();
//            float gh = g.h();
//            int count = g.cellMap.cache.size();
//            float defaultSize = (float) (Math.min(gw, gh) / Math.sqrt(count + 1));
//
//            n.pos(RectFloat2D.XYWH(
//                    g.x() + (gw / 2 - gw / 4) + (float) Math.random() * gw / 2f,
//                    g.y() + (gh / 2 - gh / 4) + (float) Math.random() * gh / 2f,
//                    defaultSize, defaultSize
//            ));
        }
    }

    






































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

        public transient X id;
        public final Flip<ConcurrentFastIteratingHashMap<X,EdgeVis<X>>> edgeOut =
                new Flip<>(()->new ConcurrentFastIteratingHashMap<>(new EdgeVis[0]));

        /** optional priority component */
        public float pri;


        /** current layout movement instance */
        public volatile transient MovingRectFloat2D mover = null;

        private float r, g, b;

        void reset(X id) {
            this.id = id;
            this.mover = null;
            pri = 0.5f;
            edgeOut.write().clear();
            edgeOut.commit();
//            edgeOut.write().clear();
            r = g = b = 0.5f;
        }

        @Override
        public boolean stop() {
            if (super.stop()) {
                reset(null);
                return true;
            }
            return false;
        }

        void paintEdges(GL2 gl) {
            edgeOut.read().forEachValue(x -> x.draw(this, gl));
        }

        @Override
        protected void paintBelow(GL2 gl) {
            float alpha = 0.8f;
            gl.glColor4f(r, g, b, alpha);
            Draw.rect(bounds, gl);
        }

        public void color(float r, float g, float b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }

        public boolean pinned() {
            
            return false;
        }

        public void commitEdges() {
            edgeOut.commitAndGet();
        }

        private EdgeVis<X> edgeAdd(EdgeVis<X> x) {
            EdgeVis<X> y = edgeOut.write().putIfAbsent(x.to.id, x);
            if (y!=x && y!=null) {
                y.merge(x);
                return y;
            } else {
                return x;
            }
        }

    }

    public static class EdgeVis<X> {
        volatile public NodeVis<X> to;
        volatile float r, g, b, a;
        volatile public float weight;
        volatile public EdgeVisRenderer renderer;

        public EdgeVis() {
            clear();
        }

        public void clear() {
            r = g = b = 0.25f;
            a = 0.75f;
            to = null;
            weight = 1f;
            renderer = EdgeVisRenderer.Triangle;
        }

        protected void merge(EdgeVis<X> x) {
            weight += x.weight;
            r = Util.or(r, x.r);
            g = Util.or(g, x.g);
            b = Util.or(b, x.b);
            a = Util.or(a, x.a);
        }

        enum EdgeVisRenderer {
            Line {
                @Override
                public void render(EdgeVis e, NodeVis from, GL2 gl) {
                    float x = from.cx(), y = from.cy();
                    gl.glLineWidth(1f + e.weight * 4f);
                    e.color(gl);
                    NodeVis to = e.to;
                    Draw.line(gl, x, y, to.cx(), to.cy());
                }
            },
            Triangle {
                @Override
                public void render(EdgeVis e, NodeVis from, GL2 gl) {
                    NodeVis to = e.to;
                    if (to == null)
                        return;

                    float fx = from.cx(), fy = from.cy();

                    float tx = to.cx(), ty = to.cy();

                    float scale = Math.min(from.w(), from.h());
                    float base = Util.lerp(e.weight, scale/3f, scale);

                    float len = (float) Math.sqrt( sqr(fx-tx) + sqr(fy-ty ));
                    float theta = (float) (Math.atan2( ty - fy, tx - fx ) * 180/Math.PI) + 90f;

                    //isosceles triangle
                    gl.glPushMatrix();
                    gl.glTranslatef((tx+fx)/2, (ty+fy)/2, 0);
                    gl.glRotatef(theta, 0, 0, 1);
                    e.color(gl);
                    Draw.tri2f(gl, -base/2, -len/2, +base/2, -len/2, 0, +len/2);
                    gl.glPopMatrix();

                }
            };
            abstract public void render(EdgeVis e, NodeVis from, GL2 gl);
        }

        private void color(GL2 gl) {
            gl.glColor4f(r, g, b, a);
        }


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

        final void draw(NodeVis<X> from, GL2 gl) {
           renderer.render(this, from, gl);
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
                    Graph2D.EdgeVis<N> ee = graph.edge(node, e.other(nn));
                    

                });
                node.color(0.5f, 0.5f, 0.5f);
                node.move((float) Math.random() * 100, (float) Math.random() * 100);
                node.size(20f, 10f);
            }
        }
    }


}
