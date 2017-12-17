package nars.gui.graph.run;

import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import jcog.math.random.XoRoShiRo128PlusRandom;
import nars.$;
import nars.gui.DynamicListSpace;
import nars.gui.graph.TermWidget;
import nars.term.Term;
import spacegraph.SpaceGraph;
import spacegraph.render.Draw;
import spacegraph.space.EDraw;

import java.util.List;
import java.util.Random;

/**
 * display a directed graph by wrapping its elements in NAR concepts (HACK)
 */
public class SimpleGraph1 extends DynamicListSpace<Term,TermWidget<Term>> {

//    final Surface status = new Label("ready");

    final TermWidget.BasicTermVis<TermWidget<Term>> vis = w -> {

        w.scale(5, 5, 5);

        Draw.colorHash(w.id, w.shapeColor);

        w.edges().forEach(x -> {
            x.r = 1;
            x.g = 0.5f;
            x.b = 0;
            x.a = 1;
            x.width = 10;
            x.priSet(0.5f);
            x.attraction = 1;
            x.attractionDist = 1;
        });
    };

    public SimpleGraph1() {
        super();
    }


    final Random rng = new XoRoShiRo128PlusRandom(1);

    class DefaultTermWidget extends TermWidget<Term> {

        public final List<EDraw<TermWidget<Term>>> edges = $.newArrayList();

        public DefaultTermWidget(Term x) {
            super(x);
            move(rng.nextFloat()-0.5f, rng.nextFloat()-0.5f, rng.nextFloat()-0.5f);
        }

        @Override
        public Iterable<EDraw<TermWidget<Term>>> edges() {
            return edges;
        }
    }


    protected SimpleGraph1 commit(Graph<Term> g) {
        List<TermWidget<Term>> n2 = $.newArrayList(g.nodes().size());

        g.nodes().forEach(x -> {
            //HACK todo use proxyterms in a cache
            //c.termlinks().clear();

            DefaultTermWidget src = space.getOrAdd(x, DefaultTermWidget::new);

            g.successors(x).forEach((Term y) ->
                    src.edges.add(new EDraw<>(
                            src, space.getOrAdd(y, DefaultTermWidget::new), 0.5f)));

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


//        NAR n = NARS.tmp();
//        n.input("a:b.","b:c.");
//        n.run(10);
//        AdjGraph<Term, Float> g = TermGraph.termlink(n);

        SimpleGraph1 cs = new SimpleGraph1() {
            @Override
            public void start(SpaceGraph space) {
                super.start(space);
                commit(g);
            }
        };

        cs.show(800, 600, false);

    }

}
