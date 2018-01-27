package spacegraph.test;

import com.google.common.collect.Iterables;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import jcog.data.graph.hgraph.NodeGraph;
import jcog.list.FasterList;
import jcog.math.random.XoRoShiRo128PlusRandom;
import spacegraph.SpaceGraph;
import spacegraph.Spatial;
import spacegraph.SpatialCache;
import spacegraph.phys.Dynamic;
import spacegraph.render.Draw;
import spacegraph.space.DynamicListSpace;
import spacegraph.space.EDraw;
import spacegraph.space.SpaceWidget;

import java.util.List;
import java.util.Random;
import java.util.function.Function;

/**
 * display a directed graph by wrapping its elements in NAR concepts (HACK)
 */
public class SimpleGraph1<X> extends DynamicListSpace<X> {


//    final Random rng = new XoRoShiRo128PlusRandom(1);

    /** default */
    protected static final SpaceWidget.SimpleNodeVis<SpaceWidget<?>> defaultVis = w -> {



        w.scale(16, 16, 2);

        //w.body.setMass(100);
        //w.body.setDamping(0.1f, 0.1f);

        Draw.colorHash(w.id, w.shapeColor);

        w.edges().forEach(x -> {
            x.r = 1;
            x.g = 0.5f;
            x.b = 0;
            x.a = 1;
            x.width = x.pri()*4;
            //x.priSet(0.5f);
            x.attraction = 1;
            x.attractionDist = 1;
        });
    };
    private SpatialCache<X, DefaultSpaceWidget<X>> cache;
    protected final SpaceWidget.SimpleNodeVis vis;

    public SimpleGraph1() {
        this((SpaceWidget.SimpleNodeVis)defaultVis);
    }

    public SimpleGraph1(SpaceWidget.SimpleNodeVis<SpaceWidget<X>> vis) {
        super();
        this.vis = vis;
    }

    @Override
    public void start(SpaceGraph space) {
        synchronized (this) {
            cache = new SpatialCache<>(space, 512);
        }
    }

    @Override
    public void stop() {
        synchronized (this) {
            cache.clear();
            cache = null;
        }
    }

    @Override
    protected List<? extends Spatial<X>> get() {
        vis.accept(active);
        return active;
    }

    final static Random rng2 = new XoRoShiRo128PlusRandom(1);

    public static class DefaultSpaceWidget<X> extends SpaceWidget<X> {

        public final List<EDraw<SpaceWidget>> edges = new FasterList();


        public DefaultSpaceWidget(X x) {
            super(x);

            move((rng2.nextFloat()-0.5f)*1, (rng2.nextFloat()-0.5f)*1f, (rng2.nextFloat()-0.5f)*1f);

        }

        @Override
        public Dynamic newBody(boolean collidesWithOthersLikeThis) {
            Dynamic d = super.newBody(collidesWithOthersLikeThis);
            d.setMass(100);
            d.setDamping(0.9f, 0.1f);
            return d;
        }

        @Override
        public Iterable<EDraw<SpaceWidget>> edges() {
            return edges;
        }
    }


    /** adapts guava Graph as input */
    public SimpleGraph1 commit(Graph<X> g) {
        return commit(g.nodes(), g::successors);
    }

    public SimpleGraph1 commit(NodeGraph<X,Object> g) {
        return commit(
                Iterables.transform(g.nodes(), x-> x.id),
                x-> Iterables.transform(
                    g.node(x).edges(false,true),
                        //zz -> zz.id //edge label
                        zz -> zz.to.id //edge target
                ));
    }

    public SimpleGraph1 commit(Iterable<X> nodes, Function<X,Iterable<X>> edges) {
        List<SpaceWidget<X>> n2 = new FasterList();

        nodes.forEach((x)->{
        //g.nodes().forEach(x -> {
            //HACK todo use proxyterms in a cache
            //c.termlinks().clear();

            DefaultSpaceWidget<X> src = cache.getOrAdd(x, DefaultSpaceWidget::new);

            edges.apply(x).forEach((X edge) ->
            //g.successors(x).forEach((Term y) ->
                    src.edges.add(new EDraw<>(
                            src, cache.getOrAdd(edge, DefaultSpaceWidget::new), 0.5f))
            );

            n2.add(src);
        });

        this.active = n2;
        return this;
    }





    public static void main(String[] args) {

        MutableGraph g = GraphBuilder.directed().build();
        g.putEdge(("x"), ("y"));
        g.putEdge(("y"), ("z"));
        g.putEdge(("y"), ("w"));

        NodeGraph h = new NodeGraph();
        h.add(("x"));
        h.add(("y"));
        h.add(("z"));
        h.add(("a"));
        h.edgeAdd(("x"), ("xy"), ("y"));
        h.edgeAdd(("x"), ("xz"), ("z"));
        h.edgeAdd(("y"), ("yz"), ("z"));
        h.edgeAdd(("a"), ("ay"), ("y"));

        SimpleGraph1 cs = new SimpleGraph1() {
            @Override
            public void start(SpaceGraph space) {
                super.start(space);
                commit(h);
            }
        };

        cs.show(800, 600, false);

    }

}
