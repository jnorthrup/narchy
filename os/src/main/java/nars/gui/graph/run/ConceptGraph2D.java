package nars.gui.graph.run;

import com.jogamp.opengl.GL2;
import jcog.math.IntRange;
import jcog.pri.ScalarValue;
import jcog.pri.bag.util.Bagregate;
import nars.NAR;
import nars.concept.Concept;
import nars.control.DurService;
import nars.gui.NARui;
import nars.term.ProxyTerm;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.container.*;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.Graph2D;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.meta.AutoSurface;
import spacegraph.video.Draw;

import java.util.concurrent.atomic.AtomicBoolean;

public class ConceptGraph2D extends Graph2D<Concept> {

    private final NAR nar;
    private DurService on;

    Iterable<Concept> source;


    public class Controls {
        public final IntRange maxNodes = new IntRange(64, 2, 128) {
            @Override
            public void set(int value) {
                super.set(value);
                nodesMax(value);
            }
        };
        public final AtomicBoolean update = new AtomicBoolean(true);
    }

    public final Controls controls = new Controls();

    public ConceptGraph2D(NAR n) {
        this(new Bagregate<>(() -> n.conceptsActive().iterator(), 128, 0.001f).
                        iterable(activate -> activate.id),
                n);
    }

    public ConceptGraph2D(Iterable<Concept> source, NAR n) {
        super();

        this.nar = n;
        this.source = source;

        nodeBuilder((nn)->{
           nn.set(
               new Scale(
                   new PushButton(nn.id.toString())
                       .click(()-> {
                           Concept t = nn.id;
                           if (t!=null)
                            NARui.conceptWindow(t, nar);
                       }),
                0.8f
               )
           );
        });

        this.layout(new ForceDirected2D<>() {

            @Override
            public void initialize(Graph2D<Concept> g, NodeVis<Concept> n) {
                updateNode(n);
                super.initialize(g, n);
            }

            @Override
            public void layout(Graph2D<Concept> g, int dtMS) {

                g.forEachValue(nn -> {
                    if (nn.showing())
                        updateNode(nn);
                });

                super.layout(g, dtMS);

            }
        })
//        layout(new TreeMap2D<>() {
//            @Override
//            public void layout(Graph2D<Concept> g, int dtMS) {
//
//                g.forEachValue(nn -> {
//                    if (nn.showing())
//                        updateNode(nn);
//                });
//                super.layout(g, dtMS);
//            }
//        })
                .layer(new TermlinkVis(n))
                .layer(new TasklinkVis(n))
                .layer(new StatementVis(n));
    }

    void updateNode(NodeVis<Concept> nn) {

        float pri = Math.max(nar.attn.pri(nn.id, 0f), ScalarValue.EPSILON);

        nn.color(pri, pri / 2f, 0f);

        nn.pri = pri;
    }

    /** updates the source */
    public ConceptGraph2D source(Iterable<Concept> source) {
        this.source = source;
        return this;
    }



    @Override
    public boolean start(SurfaceBase parent) {
        if (super.start(parent)) {
            on = DurService.on(nar, this::commit);
            return true;
        }
        return false;
    }

    @Override
    public boolean stop() {
        if (super.stop()) {
            on.off();
            on = null;
            return true;
        }
        return false;
    }

    protected void commit() {
        if (showing() && controls.update.get()) {
            set(source);
        }
    }

    public Surface widget() {
        Gridding cfg = configWidget();
        cfg.add(new AutoSurface(controls));

        return new Splitting(new Clipped(
                this
        ) {
            @Override
            protected void paintBelow(GL2 gl) {

                
                gl.glColor4f(0,0,0, 0.9f);
                Draw.rect(bounds, gl);

                super.paintBelow(gl);
            }
        }, cfg, 0.1f);
    }


    private static class TermlinkVis implements Graph2D.Graph2DLayer<Concept> {
        public final AtomicBoolean termlinks = new AtomicBoolean(true);
        final NAR n;

        private TermlinkVis(NAR n) {
            this.n = n;
        }

        @Override
        public void node(NodeVis<Concept> node, GraphBuilder<Concept> graph) {
            if (!termlinks.get())
                return;

            node.id.termlinks().forEach(l -> {
                Graph2D.EdgeVis<Concept> e = graph.edge(node, new ProxyTerm(l.get()));
                if (e != null) {
                    float p = l.priElseZero();
                    e.weight(p).color((0.9f * p) + 0.1f, 0, 0);
                }
            });
        }
    }

    private static class TasklinkVis implements Graph2D.Graph2DLayer<Concept> {
        public final AtomicBoolean tasklinks = new AtomicBoolean(true);
        final NAR n;

        private TasklinkVis(NAR n) {
            this.n = n;
        }

        @Override
        public void node(NodeVis<Concept> node, GraphBuilder<Concept> graph) {
            if (!tasklinks.get())
                return;
            node.id.tasklinks().forEach(l -> {

                Graph2D.EdgeVis<Concept> e = graph.edge(node, new ProxyTerm(l.term()));
                if (e != null) {
                    float p = l.priElseZero();
                    e.weight(p).color(0, (0.9f * p) + 0.1f, 0);
                }

            });

        }
    }

    

    private static class StatementVis implements Graph2D.Graph2DLayer<Concept> {
        public final AtomicBoolean statements = new AtomicBoolean(true);
        final NAR n;

        private StatementVis(NAR n) {
            this.n = n;
        }

        @Override
        public void node(NodeVis<Concept> node, Graph2D.GraphBuilder<Concept> graph) {

            if (!statements.get())
                return;

            Concept t = node.id;

            if (t!=null && t.op().statement) {

                @Nullable EdgeVis<Concept> e = graph.edge(new ProxyTerm(t.sub(0)), new ProxyTerm(t.sub(1)));
                if (e != null) {
                    float p = 1;
                    e.weight(p).color(0, 0, (0.9f * p) + 0.1f);
                }

            }

        }
    }

}
