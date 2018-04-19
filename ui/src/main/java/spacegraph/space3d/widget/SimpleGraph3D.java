package spacegraph.space3d.widget;

import com.google.common.collect.Iterables;
import com.google.common.graph.Graph;
import com.google.common.graph.SuccessorsFunction;
import jcog.data.graph.MapNodeGraph;
import jcog.list.FasterList;
import jcog.math.random.XoRoShiRo128PlusRandom;
import spacegraph.space3d.SpaceGraphPhys3D;
import spacegraph.space3d.Spatial;
import spacegraph.space3d.phys.Body3D;
import spacegraph.video.Draw;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

/**
 * display a directed graph by wrapping its elements in NAR concepts (HACK)
 */
public class SimpleGraph3D<X> extends DynamicListSpace<X> {


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

    //private SpatialCache<X, DefaultSpaceWidget<X>> cache;
    private final Map<X, DefaultSpaceWidget<X>> cache = new LinkedHashMap();

    protected final SpaceWidget.SimpleNodeVis vis;

    public SimpleGraph3D() {
        this((SpaceWidget.SimpleNodeVis)defaultVis);
    }

    public SimpleGraph3D(SpaceWidget.SimpleNodeVis<SpaceWidget<X>> vis) {
        super();
        this.vis = vis;
    }



    @Override
    public void start(SpaceGraphPhys3D<X> space) {
        //synchronized (this) {
            //cache = new SpatialCache<>(space, 512);
        //}
    }

    @Override
    public void stop() {
        //synchronized (this) {
//            cache.clear();
//            cache = null;
        //}
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

            float initialRadius = 10;
            move((rng2.nextFloat()-0.5f)*initialRadius, (rng2.nextFloat()-0.5f)*initialRadius, (rng2.nextFloat()-0.5f)*initialRadius);

        }

        @Override
        public Body3D newBody(boolean collidesWithOthersLikeThis) {
            Body3D d = super.newBody(collidesWithOthersLikeThis);
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
    public SimpleGraph3D<X> commit(Graph<X> g) {
        return commit(g.nodes(), g::successors);
    }

    public SimpleGraph3D<X> commit(SuccessorsFunction<X> g, X start) {
        return commit(g, List.of(start));
    }

    public SimpleGraph3D<X> commit(SuccessorsFunction<X> s, Iterable<X> start) {
        return commit(new MapNodeGraph<>(s, start));
    }

    public SimpleGraph3D<X> commit(MapNodeGraph<X,Object> g) {
        return commit(
                Iterables.transform(g.nodes(), x-> x.id),
                x-> Iterables.transform(
                    g.node(x).edges(false,true),
                        //zz -> zz.id //edge label
                        zz -> zz.to.id //edge target
                ));
    }

    public SimpleGraph3D<X> commit(Iterable<X> nodes, Function<X,Iterable<X>> edges) {
        return update(nodes, edges, false);
    }

    public SimpleGraph3D<X> update(Iterable<X> nodes, Function<X,Iterable<X>> edges, boolean addOrReplace ) {
        List<Spatial<X>> n2 = new FasterList();

        nodes.forEach((x)->{
        //g.nodes().forEach(x -> {
            //HACK todo use proxyterms in a cache
            //c.termlinks().clear();

            DefaultSpaceWidget<X> src =
                    //cache.getOrAdd(x, DefaultSpaceWidget::new);
                    cache.computeIfAbsent(x, DefaultSpaceWidget::new);

            edges.apply(x).forEach((X edge) ->
            //g.successors(x).forEach((Term y) ->
                    src.edges.add(new EDraw<>(
                            src,
                            //cache.getOrAdd(edge, DefaultSpaceWidget::new)
                            cache.computeIfAbsent(edge, DefaultSpaceWidget::new)
                            , 0.5f))
            );

            n2.add(src);
        });

        if (addOrReplace)
            this.active.addAll(n2);
        else
            this.active = n2;
        return this;
    }






}
