package spacegraph.space2d.widget;

import com.jogamp.opengl.GL2;
import jcog.data.map.CellMap;
import jcog.data.pool.DequePool;
import jcog.list.FasterList;
import jcog.tree.rtree.rect.RectFloat2D;
import jcog.util.Flip;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Scale;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.grid.MutableMapContainer;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.meta.AutoSurface;
import spacegraph.video.Draw;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * 2D directed/undirected graph widget
 */
public class Graph2D<X> extends MutableMapContainer<X, Graph2D.NodeVis<X>> {


    final List<Graph2DLayer<X>> layers = new FasterList();
    private final DequePool<EdgeVis<X>> edgePool = new DequePool<>(8 * 1024) {
        @Override
        public EdgeVis<X> create() {
            return new EdgeVis<>();
        }
    };
    volatile Graph2DLayout<X> layout = (c, d) -> {
    };


    public Graph2D() {

    }

    public <Y> Graph2D<X> layer(Graph2DLayer<X> layout) {
        layers.add(layout);
        return this;
    }

    public Graph2D<X> layout(Graph2DLayout<X> layout) {
        this.layout = layout;
        return this;
    }

    public Surface configWidget() {
        Gridding g = new Gridding();
        g.add(new AutoSurface(layout));
        for (Graph2DLayer l : layers) {
            g.add(new AutoSurface(l));
        }
        return g;
    }

    @Override
    public void prePaint(int dtMS) {
        layout.layout(this, dtMS);
        super.prePaint(dtMS);
    }

    @Override
    protected void paintBelow(GL2 gl) {
        cellMap.forEachValue(n -> {
            if (n.visible()) {
                n.paintEdges(gl);
            }
        });
    }

    @Nullable
    protected EdgeVis<X> edgeBuilder(X target) {
        @Nullable NodeVis<X> t = cellMap.getValue(target);
        if (t == null || !t.visible()) {
            return null;
        } else {
            EdgeVis<X> e = edgePool.get();
            e.to = t;
            return e;
        }
    }


    public Graph2D<X> add(Iterable<X> nodes) {
        return update(nodes, true);
    }

    public Graph2D<X> set(Iterable<X> nodes) {
        return update(nodes, false);
    }

    //TODO remove(Iterable...)

    @Override
    public void clear() {
        set(List.of());
    }

    protected Graph2D<X> update(Iterable<X> nodes, boolean addOrReplace) {

        if (parent == null)
            return this; //wait for ready

        Set<X> dontRemain;

        if (!addOrReplace && !cellMap.cache.isEmpty())
            dontRemain = new LinkedHashSet<>(cellMap.cache.keySet());
        else
            dontRemain = null; //unused

        nodes.forEach((x) -> {
            if (x == null)
                return; //ignore nulls in the input

            //TODO computeIfAbsent and re-use existing model
            CellMap.CacheCell nv = cellMap.compute(x, xx -> {
                if (xx == null) {
                    NodeVis<X> n = new NodeVis<>(x);
                    layout.initialize(this, n);
                    return n;
                } else
                    return xx; //re-use existing
            });

            if (dontRemain!=null)
                dontRemain.remove(x);
        });

        if (dontRemain!=null && !dontRemain.isEmpty()) {
            cellMap.removeAll(dontRemain);
        }

        cellMap.forEachValue((NodeVis<X> nv) -> {

            List<EdgeVis<X>> edgesNext = nv.edgeOut.write();
            edgesNext.forEach(edgePool::take);
            edgesNext.clear();

            layers.forEach(layer -> layer.node(nv, (tgt) -> {
                @Nullable EdgeVis<X> ee = edgeBuilder(tgt);
                if (ee != null) {
                    edgesNext.add(ee);
                    return ee;
                } else {
                    return null;
                }
            }, this));

            nv.edgeOut.commit();
        });


        return this;
    }

    public interface Graph2DLayout<X> {

        void layout(Graph2D<X> g, int dtMS);

        /**
         * set an initial location (and/or size) for a newly created NodeVis
         */
        default void initialize(Graph2D<X> g, NodeVis<X> n) {
            float gw = g.w();
            float gh = g.h();
            int count = g.cellMap.cache.size();
            float defaultSize = (float) (Math.min(gw, gh) / Math.sqrt(count+1));

            n.pos(RectFloat2D.XYWH(
                    g.x() + (gw/2-gw/4) + (float) Math.random() * gw /2f,
                    g.y() + (gh/2-gh/4) + (float) Math.random() * gh /2f,
                    defaultSize, defaultSize
            ));
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
        void node(NodeVis<X> node, Function<X, EdgeVis<X>> edgeBuilder, Graph2D<X> graph);

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

    public static class NodeVis<X> extends Gridding {


        public final X id;
        public final Flip<List<EdgeVis<X>>> edgeOut = new Flip<>(() -> new FasterList<>());
        private float r, g, b;

        NodeVis(X id) {
            this.id = id;

            set(
                    new Scale(new PushButton(id.toString()), 0.5f)
            );

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
    }

    public static class EdgeVis<X> {
        public NodeVis<X> to;
        public float r = 0.5f,
                g = 0.5f,
                b = 0.5f;
        public float weight = 1f;

        public EdgeVis<X> to(NodeVis<X> n) {
            this.to = n;
            return this;
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

        public void draw(GL2 gl, NodeVis<X> from) {
            gl.glColor3f(r, g, b);
            float x = from.cx();
            float y = from.cy();
            gl.glLineWidth(1f + weight * 4f);
            Draw.line(gl, x, y, to.cx(), to.cy());
        }
    }


}
