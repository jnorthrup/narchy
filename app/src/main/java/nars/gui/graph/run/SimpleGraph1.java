package nars.gui.graph.run;

import com.google.common.collect.Iterables;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import jcog.data.graph.hgraph.NodeGraph;
import jcog.math.random.XoRoShiRo128PlusRandom;
import nars.$;
import nars.gui.DynamicListSpace;
import nars.gui.graph.TermWidget;
import nars.term.Term;
import spacegraph.SpaceGraph;
import spacegraph.SpatialCache;
import spacegraph.render.Draw;
import spacegraph.space.EDraw;

import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * display a directed graph by wrapping its elements in NAR concepts (HACK)
 */
public class SimpleGraph1 extends DynamicListSpace<Term,TermWidget<Term>> {


    final Random rng = new XoRoShiRo128PlusRandom(1);
    final TermWidget.BasicTermVis<TermWidget<Term>> vis = w -> {

        w.moveDelta((rng.nextFloat()-0.5f)*0.1f, (rng.nextFloat()-0.5f)*0.1f, (rng.nextFloat()-0.5f)*0.1f);

        w.scale(5, 5, 5);

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
    private SpatialCache<Term,DefaultTermWidget> cache;

    public SimpleGraph1() {
        super();
    }

    @Override
    public void start(SpaceGraph<Term> space) {
        cache = new SpatialCache(space, 64);
    }



    class DefaultTermWidget extends TermWidget<Term> {

        public final List<EDraw<TermWidget<Term>>> edges = $.newArrayList();

        public DefaultTermWidget(Term x) {
            super(x);
            move((rng.nextFloat()-0.5f)*10,
                    (rng.nextFloat()-0.5f)*10,
                    (rng.nextFloat()-0.5f)*10);
        }

        @Override
        public Iterable<EDraw<TermWidget<Term>>> edges() {
            return edges;
        }
    }


    /** adapts guava Graph as input */
    public SimpleGraph1 commit(Graph<Term> g) {
        return commit(n -> g.nodes().forEach(n), c->c.accept(g::successors));
    }

    public SimpleGraph1 commit(NodeGraph<Term,Term> g) {
        return commit(
                n -> g.nodes().forEach(z -> n.accept(z.id)),
                c->c.accept(x-> Iterables.transform(g.node(x).edges(false,true),
                        //zz -> zz.id //edge label
                        zz -> zz.to.id //edge target
                )));
    }

    public SimpleGraph1 commit(Consumer<Consumer<Term>> nodes, Consumer<Consumer<Function<Term,Iterable<Term>>>> edges) {
        List<TermWidget<Term>> n2 = $.newArrayList(); //g.nodes().size());

        nodes.accept(x->{
        //g.nodes().forEach(x -> {
            //HACK todo use proxyterms in a cache
            //c.termlinks().clear();

            DefaultTermWidget src = cache.getOrAdd(x, DefaultTermWidget::new);

            edges.accept(e -> e.apply(x).forEach(edge ->
            //g.successors(x).forEach((Term y) ->
                    src.edges.add(new EDraw<>(
                            src, cache.getOrAdd(edge, DefaultTermWidget::new), 0.5f)))
            );

            n2.add(src);
        });

        this.active = n2;
        return this;
    }



    @Override
    protected List<TermWidget<Term>> get() {
        vis.accept(active);
        return this.active;
    }

    public static void main(String[] args) {

        MutableGraph g = GraphBuilder.directed().build();
        g.putEdge($.the("x"), $.the("y"));
        g.putEdge($.the("y"), $.the("z"));
        g.putEdge($.the("y"), $.the("w"));

        NodeGraph<Term,Term> h = new NodeGraph();
        h.add($.the("x"));
        h.add($.the("y"));
        h.add($.the("z"));
        h.add($.the("a"));
        h.edgeAdd($.the("x"), $.the("xy"), $.the("y"));
        h.edgeAdd($.the("x"), $.the("xz"), $.the("z"));
        h.edgeAdd($.the("y"), $.the("yz"), $.the("z"));
        h.edgeAdd($.the("a"), $.the("ay"), $.the("y"));

        SimpleGraph1 cs = new SimpleGraph1() {
            @Override
            public void start(SpaceGraph space) {
                super.start(space);
                //commit(g);
                commit(h);
            }
        };

        cs.show(800, 600, false);

    }

}
