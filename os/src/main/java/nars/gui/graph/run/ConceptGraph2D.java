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
import nars.term.Term;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.container.Clipped;
import spacegraph.space2d.container.ForceDirected2D;
import spacegraph.space2d.container.Scale;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.Graph2D;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.video.Draw;

import java.util.concurrent.atomic.AtomicBoolean;

/** TODO edge capacity limiting */
public class ConceptGraph2D extends Graph2D<Concept> {

    private final NAR nar;
    private DurService on;

    Iterable<Concept> source;


    public class Controls {
        public final IntRange maxNodes = new IntRange(256, 1, 512) {
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
        this(new Bagregate<>(() -> n.conceptsActive().iterator(), 256, 0.01f).
                        iterable(activate -> activate.id),
                n);
    }

    public ConceptGraph2D(Iterable<Concept> source, NAR n) {
        super();

        this.nar = n;
        this.source = source;

        nodesMax(128);

        build((nn)->{
           nn.set(
               new Scale(
                   new PushButton(new VectorLabel(nn.id.toString()))
                       .click(()-> {
                           Concept t = nn.id;
                           if (t!=null)
                            NARui.conceptWindow(t, nar);
                       }),
                0.8f
               )
           );
        });

        this.update(getLayout())
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
                .render(
                        Graph2D.InvalidateEdges,
                        new TermlinkVis(n),
                        new TasklinkVis(n),
                        new StatementVis(n)
                );
    }


    public Graph2DUpdater<Concept> getLayout() {
        return new ForceDirected2D<>() {

            @Override
            public void init(Graph2D<Concept> g, NodeVis<Concept> newNode) {
                updateNode(newNode);
                super.init(g, newNode);
            }

            @Override
            public void update(Graph2D<Concept> g, int dtMS) {

                g.forEachValue(nn -> {
                    if (nn.showing())
                        updateNode(nn);
                });

                super.update(g, dtMS);

            }
        };
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
        cfg.add(new ObjectSurface(controls));

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


    final static float WEIGHT_UPDATE_RATE = 0.1f;
    final static float COLOR_UPDATE_RATE = 0.5f;
    final static float COLOR_FADE_RATE = 0.05f;

    private static class TermlinkVis implements Graph2DRenderer<Concept> {
        public final AtomicBoolean termlinks = new AtomicBoolean(true);
        final NAR n;

        private TermlinkVis(NAR n) {
            this.n = n;
        }

        @Override
        public void node(NodeVis<Concept> node, GraphEditing<Concept> graph) {
            if (!termlinks.get())
                return;

            Concept id = node.id;
            if (id!=null) {
                id.termlinks().forEach(l -> {
                    Graph2D.EdgeVis<Concept> e = graph.edge(node, wrap(l.get()));
                    if (e != null) {
                        float p = l.priElseZero();
                        e.weightLerp(p, WEIGHT_UPDATE_RATE)
                                .colorLerp((0.9f * p) + 0.1f, Float.NaN, Float.NaN, COLOR_UPDATE_RATE)
                                .colorLerp(Float.NaN,0,0,COLOR_FADE_RATE)
                        ;

                    }
                });
            }
        }
    }

    /** bad HACK to avoid a term/concept equality issue */
    @Deprecated public static ProxyTerm wrap(Term l) {
        return new ProxyTerm(l);
    }

    private static class TasklinkVis implements Graph2DRenderer<Concept> {
        public final AtomicBoolean tasklinks = new AtomicBoolean(true);
        final NAR n;

        private TasklinkVis(NAR n) {
            this.n = n;
        }

        @Override
        public void node(NodeVis<Concept> node, GraphEditing<Concept> graph) {
            if (!tasklinks.get())
                return;
            Concept n = node.id;
            if (n==null)
                return;

            Term sourceTerm = n.term();
            n.tasklinks().forEach(l -> {

                Term targetTerm = l.term();
//                if (targetTerm.equals(sourceTerm.term()))
//                    return; //ignore

                Graph2D.EdgeVis<Concept> e = graph.edge(node, wrap(targetTerm));
                if (e != null) {
                    float p = l.priElseZero();
                    e.weightLerp(p, WEIGHT_UPDATE_RATE)
                            .colorLerp(Float.NaN, (0.9f * p) + 0.1f, Float.NaN, COLOR_UPDATE_RATE)
                            .colorLerp(0,Float.NaN,0,COLOR_FADE_RATE)
                    ;
                }

            });

        }
    }



    private static class StatementVis implements Graph2DRenderer<Concept> {
        public final AtomicBoolean statements = new AtomicBoolean(true);
        final NAR n;

        private StatementVis(NAR n) {
            this.n = n;
        }

        @Override
        public void node(NodeVis<Concept> node, GraphEditing<Concept> graph) {

            if (!statements.get())
                return;

            Concept t = node.id;

            if (t!=null && t.op().statement) {

                @Nullable EdgeVis<Concept> e = graph.edge((t.sub(0)), (t.sub(1)));
                if (e != null) {
                    float p = 0.5f;
                    e.weightLerp(p, WEIGHT_UPDATE_RATE)
                            .colorLerp(Float.NaN, Float.NaN, (0.9f * p) + 0.1f, COLOR_UPDATE_RATE)
                            .colorLerp(0,0,Float.NaN,COLOR_FADE_RATE)
                    ;
                }

            }

        }
    }

}
