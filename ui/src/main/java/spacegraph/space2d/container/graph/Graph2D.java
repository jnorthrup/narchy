package spacegraph.space2d.container.graph;

import com.jogamp.opengl.GL2;
import jcog.data.graph.NodeGraph;
import jcog.data.map.CellMap;
import jcog.data.pool.MetalPool;
import jcog.data.pool.Pool;
import jcog.data.set.ArrayHashSet;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.collection.MutableMapContainer;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.unit.Clipped;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.space2d.widget.textedit.TextEdit;
import spacegraph.video.Draw;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * 2D directed/undirected graph widget
 * designed for high-performance realtime animated visualization of many graph nodes and edges
 * that can appear, disappear, and re-appear between frames
 *
 * TODO generify for use in Dynamics3D
 */
public class Graph2D<X> extends MutableMapContainer<X, NodeVis<X>> {


    private final AtomicBoolean busy = new AtomicBoolean(false);


    //public AtomicFloat scale = new AtomicFloat(1);


    private List<Graph2DRenderer<X>> renderers = Collections.EMPTY_LIST;

    private final Pool<EdgeVis<X>> edgePool = new MetalPool<>() {
        @Override
        public EdgeVis<X> create() {
            return new EdgeVis<>();
        }

        @Override
        public void put(EdgeVis<X> i) {
            i.clear();
            super.put(i);
        }
    };

    @Override
    public boolean delete() {
        if (super.delete()) {
            edgePool.delete();
//            nodeCache.clear();
            //TODO anything else?
            return true;
        }
        return false;
    }

//    final MRUMap<X, NodeVis<X>> nodeCache = new MRUMap<>(8 * 1024) {
//        @Override
//        protected void onEvict(Map.Entry<X, NodeVis<X>> entry) {
//            NodeVis<X> s = entry.getValue();
//            if (s.id == null)
//                s.stop();
//        }
//
//        @Override
//        public void clear() {
//            forEachValue(Surface::delete);
//            super.clear();
//        }
//    }; //TODO set capacity good


    @Override
    protected void hide(NodeVis<X> key, Surface s) {
        s.hide();
    }

    private volatile Graph2DUpdater<X> updater = NullUpdater;

    private static final Graph2DUpdater NullUpdater = (c, d) -> {
    };

    private final transient Set<X> wontRemain = new ArrayHashSet();

    private final Consumer<NodeVis<X>> DEFAULT_NODE_BUILDER = x -> x.set(
            //new Scale(
                    new PushButton(new VectorLabel(x.id.toString()))
                    //, 0.75f)
    );
    private transient Consumer<NodeVis<X>> builder = DEFAULT_NODE_BUILDER;

    public Graph2D() {
        this(NullUpdater);
    }

    public Graph2D(Graph2DUpdater<X> updater) {
        super();
//        clipBounds = false;
        update(updater);
    }
    public Surface widget() {
        return widget(null);
    }

    /** TODO impl using MetaFrame menu */
    public Surface widget(Object controls) {
        Gridding cfg = configWidget();

        if (controls!=null) {
            cfg.add(new ObjectSurface(controls));
        }

        addControls(cfg);

        return new Splitting(new Clipped(
                this
        ) {
            @Override
            protected void paintIt(GL2 gl, ReSurface r) {

                gl.glColor4f(0,0,0, 0.9f);
                Draw.rect(bounds, gl);

                super.paintIt(gl, r);
            }
        }, 0.1f, cfg).resizeable();
    }

//    @Override
//    public void renderContents(GL2 gl, SurfaceRender r) {
//        SurfaceRender rr = r.clone(scale.floatValue(), new v2()); //HACK
//        super.renderContents(gl, rr);
//    }

    protected void addControls(Gridding cfg) {

    }

    /**
     * adds a rendering stage.  these are applied successively at each visible node
     */
    public Graph2D<X> render(Graph2DRenderer<X>... renderStages) {
        List<Graph2DRenderer<X>> nextRenderStages = List.of(renderStages);
        if (!renderers.equals(nextRenderStages)) {
            renderers = nextRenderStages;
            layout();
        }
        return this;
    }

    public Graph2D<X> build(Consumer<NodeVis<X>> builder) {
        if (this.builder!=builder) {
            this.builder = builder;
            layout();
        }
        return this;
    }

    /**
     * TODO to support dynamically changing updater, apply the updater's init procdure to all existing nodes.  do this in between frames so there is no interruption if rendering current frame.  this means saving a 'nextUpdater' reference to delay application
     */
    public Graph2D<X> update(Graph2DUpdater<X> u) {
        if (this.updater!=u) {
            this.updater = u;
            layout();
        }
        return this;
    }

    public Gridding configWidget() {
        Gridding g = new Gridding();
        g.add(new ObjectSurface(updater));
        for (Graph2DRenderer l : renderers) {
            g.add(new ObjectSurface(l));
        }
        return g;
    }

    public int nodes() {
        return cells.size();
    }

    @Override
    protected boolean canRender(ReSurface r) {
        if (super.canRender(r)) {
            if (busy.compareAndSet(false, true)) {
                try {
                    updater.update(this, r.dtS());
                } finally {
                    busy.set(false);
                }
            }
            return true;
        }
        return false;
    }


    @Override
    protected void paintIt(GL2 gl, ReSurface r) {
        cells.forEachValue(n -> {
            if (n.visible())
                n.paintEdges(gl);
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

    public final Graph2D<X> set(NodeGraph g) {
        return set(g.nodes());
    }
    public final Graph2D<X> add(NodeGraph g) {
        return add(g.nodes());
    }

    public final Graph2D<X> set(Iterable<X> nodes) {
        return update(nodes, false);
    }

    @Override
    public TextEdit clear() {
        set(Collections.EMPTY_LIST);
        return null;
    }

    private Graph2D<X> update(Iterable<X> nodes, boolean addOrReplace) {

        if (!busy.compareAndSet(false, true))
            return this;

        try {

            updateNodes(nodes, addOrReplace);

            render();

        } finally {
            busy.set(false);
        }

        return this;
    }

    @Override
    protected void stopping() {
//            nodeCache.values().forEach(ContainerSurface::delete);
//            nodeCache.clear();
        edgePool.delete();
        super.stopping();
    }

    private void updateNodes(Iterable<X> nodes, boolean addOrReplace) {
        if (!addOrReplace) {
            cells.map.forEach((k, v) -> wontRemain.add(k));
        }


        nodes.forEach(x -> {
            CellMap.CacheCell<X, NodeVis<X>> xxx =
                    compute(x, xx -> xx == null ? materialize(x) : rematerialize(xx));

            NodeVis<X> cv = xxx.value;
            if (cv.parent == null)
                cv.start(this);

            cv.show();
        });

        if (!wontRemain.isEmpty()) {
            cells.removeAll(wontRemain);
            wontRemain.clear();
        }

    }

    private NodeVis<X> materialize(X x0) {
//        NodeVis<X> yy = nodeCache.computeIfAbsent(x, x0 -> {
            NodeVis<X> yy = new NodeVis<>();
            yy.start(x0);
            builder.accept(yy);
//            return y;
//        });
        yy.id = x0; //in case x different instance but equal
        updater.init(this, yy);
        return yy;
    }


    /**
     * node continues being materialized
     */
    private NodeVis<X> rematerialize(NodeVis<X> xx) {
        xx.update();
        wontRemain.remove(xx.id);
        return xx;
    }

    @Override
    protected void unmaterialize(NodeVis<X> v) {
        v.end(edgePool);
        v.delete();
    }

    private void render() {
        renderers.forEach(layer -> layer.nodes(cells,edit));
    }


    /**
     * wraps all graph construction procedure in this interface for which layers construct graph with
     */
    public static final class GraphEditing<X> {

        final Graph2D<X> graph;

        GraphEditing(Graph2D<X> g) {
            this.graph = g;
        }

        /**
         * adds a visible edge between two nodes, if they exist and are visible
         */
        public @Nullable EdgeVis<X> edge(Object from, Object to) {
            @Nullable NodeVis<X> fromNode = graph.cells.get(from);
            return fromNode != null ? edge(fromNode, to) : null;
        }

        /**
         * adds a visible edge between two nodes, if they exist and are visible
         */
        public @Nullable EdgeVis<X> edge(NodeVis<X> from, Object to) {

            @Nullable NodeVis<X> t = graph.cells.getValue(to);
            if (t == null)
                return null;

            if (from == t) {
//                if (Param.DEBUG)
//                    throw new TODO("self referencing edges not supported yet");
//                else
                    return null; //ignored
            }

            return from.out(t, graph.edgePool);
        }

    }

    private final GraphEditing<X> edit = new GraphEditing<>(this);


    /**
     * iterative animated geometric update; processes the visual representation of the content
     */
    @FunctionalInterface public interface Graph2DUpdater<X> {

        void update(Graph2D<X> g, float dtS);

        /**
         * set an initial location (and/or size) for a newly created NodeVis
         */
        default void init(Graph2D<X> g, NodeVis<X> newNode) {
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
     * one of zero or more sequentially-applied "layers" of the representation of the graph,
     * responsible for materializing/decorating/rendering each individual node it is called for,
     * and the graph that holds it (including edges, etc) via the supplied GraphEditing interface.
     */
    @FunctionalInterface
    public interface Graph2DRenderer<X> {

        /**
         * called for each node being processed.  can edit the NodeVis
         * and generate new links from it to target nodes.
         */
        void node(NodeVis<X> node, GraphEditing<X> graph);

        default void nodes(CellMap<X, NodeVis<X>> cells, GraphEditing<X> edit) {
            cells.forEachValue(nv -> {
                if (nv.visible())
                    node(nv, edit);
            });
        }

    }

    /**
     * invalidates all edges by setting their dirty flag
     */
    protected static Graph2DRenderer InvalidateEdges = (n, g) -> n.invalidateEdges();

}
