package spacegraph.space3d.widget;

import com.google.common.graph.Graph;
import com.google.common.graph.SuccessorsFunction;
import jcog.data.graph.MapNodeGraph;
import jcog.data.graph.Node;
import jcog.data.graph.path.FromTo;
import jcog.data.list.FasterList;
import jcog.random.XoRoShiRo128PlusRandom;
import spacegraph.space3d.SpaceGraph3D;
import spacegraph.space3d.Spatial;
import spacegraph.space3d.phys.Body3D;
import spacegraph.video.Draw;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * display a directed graph by wrapping its elements in NAR concepts (HACK)
 */
public class SimpleGraph3D<X> extends DynamicListSpace<X> {

    /** default */
    protected static final SpaceWidget.SimpleNodeVis<SpaceWidget<?>> defaultVis = new SpaceWidget.SimpleNodeVis<SpaceWidget<?>>() {
        @Override
        public void each(SpaceWidget<?> w) {

            w.scale(16.0F, 16.0F, 2.0F);

            Draw.colorHash(w.id, w.shapeColor);

            for (EDraw<?> x : w.edges()) {
                x.r = 1.0F;
                x.g = 0.5f;
                x.b = (float) 0;
                x.a = 1.0F;
                x.width = x.pri() * 4.0F;

                x.attraction = 0.1f;
                x.attractionDist = 8.0F;
            }
        }
    };

    
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
    public void start(SpaceGraph3D<X> space) {
        
            
        
    }

    @Override
    public void stop() {
        


        
    }

    @Override
    protected List<? extends Spatial<X>> get() {
        vis.accept(active);
        return active;
    }

    private static final Random rng2 = new XoRoShiRo128PlusRandom(1L);

    public static class DefaultSpaceWidget<X> extends SpaceWidget<X> {

        final List<EDraw<SpaceWidget>> edges = new FasterList();


        DefaultSpaceWidget(X x) {
            super(x);

            float initialRadius = 10.0F;
            move((rng2.nextFloat()-0.5f)*initialRadius, (rng2.nextFloat()-0.5f)*initialRadius, (rng2.nextFloat()-0.5f)*initialRadius);

        }

        @Override
        public Body3D newBody(boolean collidesWithOthersLikeThis) {
            Body3D d = super.newBody(collidesWithOthersLikeThis);
            d.setMass(100.0F);
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

    private SimpleGraph3D<X> commit(SuccessorsFunction<X> s, Iterable<X> start) {
        return commit(new MapNodeGraph<>(s, start));
    }

    public SimpleGraph3D<X> commit(MapNodeGraph<X,Object> g) {
        List<X> list = new ArrayList<>();
        for (Node<X, Object> xObjectNode : g.nodes()) {
            X id = xObjectNode.id();
            list.add(id);
        }
        return commit(
                list,
                new Function<X, Iterable<X>>() {
                    @Override
                    public Iterable<X> apply(X x) {
                        return StreamSupport.stream(g.node(x).edges(false, true).spliterator(), false).map(new Function<FromTo<Node<X, Object>, Object>, X>() {
                            @Override
                            public X apply(FromTo<Node<X, Object>, Object> zz) {
                                return zz.to().id();
                            }
                        })
                                .collect(Collectors.toList());
                    }
                });
    }

    private SimpleGraph3D<X> commit(Iterable<X> nodes, Function<X, Iterable<X>> edges) {
        return update(nodes, edges, false);
    }

    private SimpleGraph3D<X> update(Iterable<X> nodes, Function<X, Iterable<X>> edges, boolean addOrReplace) {
        List<Spatial<X>> n2 = new FasterList();

        for (X x : nodes) {
            DefaultSpaceWidget<X> src = cache.computeIfAbsent(x, DefaultSpaceWidget::new);

            for (X edge : edges.apply(x)) {
                src.edges.add(new EDraw<>(
                        src, cache.computeIfAbsent(edge, DefaultSpaceWidget::new)
                        , 0.5f));
            }

            n2.add(src);
        }

        if (addOrReplace)
            this.active.addAll(n2);
        else
            this.active = n2;
        return this;
    }






}
