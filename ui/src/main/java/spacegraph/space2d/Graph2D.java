package spacegraph.space2d;

import com.google.common.collect.Iterables;
import com.google.common.graph.Graph;
import com.google.common.graph.SuccessorsFunction;
import com.jogamp.opengl.GL2;
import jcog.bag.Bag;
import jcog.data.graph.AdjGraph;
import jcog.data.graph.MapNodeGraph;
import jcog.list.FasterList;
import jcog.math.random.XoRoShiRo128PlusRandom;
import jcog.tree.rtree.rect.RectFloat2D;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.grid.MutableMapContainer;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.video.Draw;

import java.util.List;
import java.util.Random;
import java.util.function.Function;

import static com.jogamp.opengl.math.FloatUtil.sqrt;

/** 2D directed/undirected graph widget */
public class Graph2D<X> extends MutableMapContainer<X,Graph2D.NodeVis<X>> {


    public static class NodeVis<X> extends Gridding {

        public final X id;
        public volatile NodeVis<X>[] edgeOut = null;

        NodeVis(X id) {
            this.id = id;

            set(
                new PushButton(id.toString())
            );
        }

        @Override
        protected void paintBelow(GL2 gl) {
            NodeVis[] e = edgeOut;
            if (e!=null) {
                float x = cx();
                float y = cy();
                gl.glLineWidth(4f);
                for (NodeVis b : e) {
                    Draw.line(gl, x, y, b.x(), b.y());
                }
            }
        }


    }


    public Graph2D() {
        
    }

    volatile Graph2DLayout<X> layout = (c,d)->{ };

    @FunctionalInterface
    public interface Graph2DLayout<X> {
        void layout(Graph2D<X> g, int dtMS);
    }

    public Graph2D<X> setLayout(Graph2DLayout<X> layout) {
        this.layout = layout;
        return this;
    }

    @Override
    public void prePaint(int dtMS) {
        layout.layout(this, dtMS);
        super.prePaint(dtMS);
    }

    @Override
    protected void doLayout(int dtMS) {

        float w = w();
        float h = h();
        Random rng = new XoRoShiRo128PlusRandom(1);
        float cw = sqrt(w);
        float ch = sqrt(h);

        //TODO model
        forEach(s -> {
            s.pos(RectFloat2D.XYWH(rng.nextFloat() * w, rng.nextFloat() * h, cw, ch));
        });
        super.doLayout(dtMS);
    }

    public Graph2D<X> commit(Bag<?,X> g) {
        return commit(g, (nothing)->null);
    }

    /** adapts guava Graph as input */
    public Graph2D<X> commit(Graph<X> g) {
        return commit(g.nodes(), g::successors);
    }

    public Graph2D<X> commit(SuccessorsFunction<X> g, X start) {
        return commit(g, List.of(start));
    }

    public Graph2D<X> commit(SuccessorsFunction<X> s, Iterable<X> start) {
        return commit(new MapNodeGraph<>(s, start));
    }

    public Graph2D<X> commit(AdjGraph<X,Object> g) {
        return commit(
            Iterables.transform(g.nodes.keySet(), t -> t.v),
            (X x) -> {
                List<X> adj = new FasterList();
                g.neighborEdges(x, (v,e)->{
                    adj.add(v);
                });
                return adj;
            }
        );
    }
    public Graph2D<X> commit(MapNodeGraph<X,Object> g) {
        return commit(
                Iterables.transform(g.nodes(), x-> x.id),
                x-> Iterables.transform(
                        g.node(x).edges(false,true),
                        //zz -> zz.id //edge label
                        zz -> zz.to.id //edge target
                ));
    }

    public Graph2D<X> commit(Iterable<X> nodes, @Nullable Function<X,Iterable<X>> edges) {
        return update(nodes, edges, false);
    }

    public Graph2D<X> update(Iterable<X> nodes, @Nullable Function<X,Iterable<X>> edges, boolean addOrReplace ) {

        if (!addOrReplace)
            clear();

        nodes.forEach((x)->{
            //g.nodes().forEach(x -> {
            //HACK todo use proxyterms in a cache
            //c.termlinks().clear();

            //TODO computeIfAbsent and re-use existing model
            compute(x, xx -> {
                if (xx == null)
                    return new NodeVis(x);
                else
                    return xx; //re-use
            });
        });

        forEachValue((NodeVis<X> v) -> {
            List<NodeVis<X>> outs = new FasterList();
            edges.apply(v.id).forEach((X ve) -> {
                outs.add( getValues(ve) );
            });
            v.edgeOut = ((FasterList<NodeVis<X>>) outs).toArrayRecycled(NodeVis[]::new);
        });


        return this;
    }




}
