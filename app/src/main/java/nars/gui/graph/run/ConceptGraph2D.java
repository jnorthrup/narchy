package nars.gui.graph.run;

import com.jogamp.opengl.GL2;
import jcog.bag.util.Bagregate;
import jcog.math.FloatRange;
import jcog.math.IntRange;
import jcog.pri.PLink;
import jcog.tree.rtree.rect.RectFloat2D;
import nars.NAR;
import nars.concept.Concept;
import nars.control.DurService;
import nars.exe.AbstractExec;
import nars.term.ProxyTerm;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.container.Clipped;
import spacegraph.space2d.container.ForceDirected2D;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.Graph2D;
import spacegraph.space2d.widget.meta.AutoSurface;
import spacegraph.video.Draw;

import java.util.concurrent.atomic.AtomicBoolean;

public class ConceptGraph2D extends Graph2D<Concept> {

    private final NAR nar;
    private DurService on;

    Iterable<Concept> source;

    public class Controls {
        public final FloatRange nodeScale = new FloatRange(0.2f, 0.04f, 0.5f);
        public final IntRange maxNodes = new IntRange(64, 2, 128) {
            @Override
            public void set(int value) {
                super.set(value);
                nodesMax = value;
            }
        };
        public final AtomicBoolean update = new AtomicBoolean(true);
    }

    public final Controls controls = new Controls();

    public ConceptGraph2D(NAR n) {
        this(new Bagregate<>(() -> n.conceptsActive().iterator(), 128, 0.001f).
                        iterable(PLink::get),
                n);
    }

    public ConceptGraph2D(Iterable<Concept> source, NAR n) {
        super();

        this.nar = n;
        this.source = source;

        this.layout(new ForceDirected2D<>() {
            @Override
            public void layout(Graph2D<Concept> g, int dtMS) {

                float AUTOSCALE =
                        controls.nodeScale.floatValue() *
                        (float) (Math.min(bounds.w, bounds.h)
                        / Math.sqrt(1f + nodes()));

                g.forEachValue(nn -> {
                    float pri = ((AbstractExec) n.exe).active.pri(nn.id, 0f);
                    nn.color(pri, pri / 2f, 0f);

                    float p = (float) (1f + Math.sqrt(pri)) * AUTOSCALE;
                    nn.pos(RectFloat2D.XYWH(nn.cx(), nn.cy(), p, p));
                });
                super.layout(g, dtMS);
            }
        })
                .layer(new TermlinkVis(n))
                .layer(new TasklinkVis(n))
                .layer(new StatementVis(n));
    }

    /** updates the source */
    public ConceptGraph2D source(Iterable<Concept> source) {
        this.source = source;
        return this;
    }

    public int nodes() {
        return cellMap.size();
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
        if (controls.update.get()) {
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

                //black background
                gl.glColor4f(0,0,0, 0.9f);
                Draw.rect(gl, bounds);

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

    //TODO ConjVis - creates a loop of edges for all subterms of a conjunction concept

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
            if (t.op().statement) {

                @Nullable EdgeVis<Concept> e = graph.edge(new ProxyTerm(t.sub(0)), new ProxyTerm(t.sub(1)));
                if (e != null) {
                    float p = 1;
                    e.weight(p).color(0, 0, (0.9f * p) + 0.1f);
                }

            }

        }
    }

}
