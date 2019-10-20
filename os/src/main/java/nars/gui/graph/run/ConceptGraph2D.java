package nars.gui.graph.run;

import com.google.common.collect.Iterables;
import jcog.data.map.CellMap;
import jcog.pri.ScalarValue;
import nars.NAR;
import nars.gui.NARui;
import nars.link.TaskLink;
import nars.term.Term;
import nars.term.Termed;
import nars.time.part.DurLoop;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.container.graph.EdgeVis;
import spacegraph.space2d.container.graph.Graph2D;
import spacegraph.space2d.container.graph.NodeVis;
import spacegraph.space2d.container.layout.ForceDirected2D;
import spacegraph.space2d.container.unit.Scale;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.text.VectorLabel;

import java.util.concurrent.atomic.AtomicBoolean;

import static nars.Op.*;

/**
 * TODO edge capacity limiting
 */
public class ConceptGraph2D extends Graph2D<Term> {

    private final NAR nar;
    private DurLoop on;

    Iterable<Term> source;

//    public class Controls {
    public final AtomicBoolean update = new AtomicBoolean(true);
//    }

//    public final Controls controls = new Controls();

    public ConceptGraph2D(Iterable<? extends Termed> source, Iterable<TaskLink> links, NAR n) {
        super();

        this.nar = n;
        this.source = Iterables.transform(source, Termed::term);

        build(nn -> nn.set(
                new Scale(
                        new PushButton(new VectorLabel(nn.id.toString())).clicked(() -> {
                            Term t = nn.id;
                            if (t != null)
                                NARui.conceptWindow(t, nar);
                        }),
                        0.8f
                )
        ));

        this.update(getLayout())
//        layout(new TreeMap2D<>() {
//            @Override
//            public void layout(Graph2D<Term> g, int dtMS) {
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
                        //                      new TermlinkVis(n),
//                        new SubtermVis(n),
                        new TasklinkVis(links),
                        new StatementVis(n)
                );
    }


    public static Graph2DUpdater<Term> getLayout() {
        return new ForceDirected2D<>() {

            @Override
            public void init(Graph2D<Term> g, NodeVis<Term> newNode) {
                updateNode(newNode);
                super.init(g, newNode);
            }

            @Override
            public void update(Graph2D<Term> g, float dtS) {
                g.forEachValue(ConceptGraph2D::updateNode);
                super.update(g, dtS);
            }
        };
    }

    static void updateNode(NodeVis<Term> nn) {
        Term i = nn.id;
        if (i != null && nn.visible()) {

            float pri =
                    //Math.max(nar.concepts.pri(i, 0f), ScalarValue.EPSILON);
                    0.5f;

            nn.color(pri, pri / 2f, 0f);

            nn.pri = pri;
        }
    }

    /**
     * updates the source
     */
    public ConceptGraph2D source(Iterable<Term> source) {
        this.source = source;
        return this;
    }



    @Override
    protected void starting() {
        super.starting();
        on = nar.onDur(this::commit);
    }

    @Override
    protected void stopping() {
        on.close();
        on = null;
        super.stopping();
    }


    protected void commit() {
        if (showing() && update.get()) {
            set(source);
        }
    }


    static final float WEIGHT_UPDATE_RATE = 0.5f;
    static final float COLOR_UPDATE_RATE = 0.5f;
    static final float COLOR_FADE_RATE = 0.05f;

//    private static class TermlinkVis implements Graph2DRenderer<Term> {
//        public final AtomicBoolean termlinks = new AtomicBoolean(true);
//        final NAR n;
//
//        private TermlinkVis(NAR n) {
//            this.n = n;
//        }
//
//        @Override
//        public void node(NodeVis<Term> node, GraphEditing<Term> graph) {
//            if (!termlinks.get())
//                return;
//
//            Concept id = node.id;
//            if (id!=null) {
//                id.termlinks().forEach(l -> {
//                    Graph2D.EdgeVis<Term> e = graph.edge(node, wrap(l.get()));
//                    if (e != null) {
//                        float p = l.priElseZero();
//                        e.weightLerp(p, WEIGHT_UPDATE_RATE)
//                                .colorLerp((0.9f * p) + 0.1f, Float.NaN, Float.NaN, COLOR_UPDATE_RATE)
//                                .colorLerp(Float.NaN,0,0,COLOR_FADE_RATE)
//                        ;
//
//                    }
//                });
//            }
//        }
//    }

//    /**
//     * bad HACK to avoid a target/concept equality issue
//     */
//    @Deprecated
//    public static ProxyTerm wrap(Term l) {
//        return new ProxyTerm(l);
//    }

    private static class TasklinkVis implements Graph2D.Graph2DRenderer<Term> {

        public final AtomicBoolean belief = new AtomicBoolean(true);
        public final AtomicBoolean goal = new AtomicBoolean(true);
        public final AtomicBoolean question = new AtomicBoolean(true);
        public final AtomicBoolean quest = new AtomicBoolean(true);

        final Iterable<TaskLink> links;

        /** non-volatile cached is this helpful? */
        private transient boolean _belief;
        private transient boolean _goal;
        private transient boolean _question;
        private transient boolean _quest;


        private TasklinkVis(Iterable<TaskLink> links) {
            this.links = links;
        }

        @Override
        public void nodes(CellMap<Term, NodeVis<Term>> cells, GraphEditing<Term> edit) {

            _belief = this.belief.getOpaque();
            _goal = this.goal.getOpaque();
            _question = this.question.getOpaque();
            _quest = this.quest.getOpaque();
            if (!_belief && !_goal && !_question && !_quest)
                return;


            for (TaskLink l : links) {
                add(edit, l);
            }


        }

        @Override
        public void node(NodeVis<Term> node, GraphEditing<Term> graph) {
            //N/A
        }

        public void add(GraphEditing<Term> graph, TaskLink l) {

            Term targetTerm = l.to();//.concept();
            if (targetTerm instanceof nars.term.Variable)
                return; //ignore variables

            float[] pp = new float[4];
            pp[0] = _belief ? l.priPunc(BELIEF) : (float) 0;
            pp[1] = _goal ? l.priPunc(GOAL) : (float) 0;
            pp[2] = _question ? l.priPunc(QUESTION) : (float) 0;
            pp[3] = _quest ? l.priPunc(QUEST) : (float) 0;
            float pSum = (pp[0] + pp[1] + pp[2] + pp[3]);
            if (pSum < ScalarValue.EPSILON)
                return;


            Term from = l.from().concept();
            EdgeVis<Term> e = graph.edge(from, targetTerm);
            if (e != null) {
                /*
                https://www.colourlovers.com/palette/848743/(_%E2%80%9D_)
                BELIEF   Red     189,21,80
                QUESTION Orange  233,127,2
                GOAL     Green   138,155,15
                QUEST    Yellow  248,202,0
                */

                for (int i = 0; i < 4; i++) {
                    float ppi = pp[i];
                    if (ppi < ScalarValue.EPSILON)
                        continue;

                    int b;
                    int g;
                    int r;
                    switch (i) {
                        case 0:
                            r = 189;
                            g = 21;
                            b = 80;
                            break;
                        case 1:
                            r = 138;
                            g = 155;
                            b = 15;
                            break;
                        case 2:
                            r = 233;
                            g = 127;
                            b = 2;
                            break;
                        case 3:
                            r = 248;
                            g = 202;
                            b = 0;
                            break;
                        default:
                            throw new UnsupportedOperationException();
                    }


                    e//.colorLerp(0,0,0,COLOR_FADE_RATE).colorAdd
                       .colorLerp
                            ((float) r / 256f, (float) g / 256f, (float) b / 256f, COLOR_UPDATE_RATE);
                }
                e.weightLerp(0.5f + 0.5f * (pSum/ 4.0F), WEIGHT_UPDATE_RATE);
            }
        }
    }

//    private class SubtermVis implements Graph2D.Graph2DRenderer<Term> {
//
//        public final AtomicBoolean subterms = new AtomicBoolean(false);
//
//        public final AtomicBoolean visible = new AtomicBoolean(true);
//
//        public final FloatRange strength = new FloatRange(0.1f, 0, 1f);
//
//        final NAR n;
//
//        private SubtermVis(NAR n) {
//            this.n = n;
//        }
//
//        @Override
//        public void node(NodeVis<Term> node, GraphEditing<Term> graph) {
//            if (!subterms.getOpaque())
//                return;
//
//            boolean visible = this.visible.getOpaque();
//
//            float p = strength.floatValue();
//
//            Term t = node.id;
//            if (t == null) return;
//            Concept c = n.concept(t);
//            if (c != null) {
//
//                c.linker().targets().forEach(s -> {
//                    if (s.op().conceptualizable) {
//                        if (t != null) {
//                            int v = t.volume();
//                            @Nullable EdgeVis<Term> e = graph.edge(node, s.term().concept());
//                            if (e != null) {
//                                e.weightLerp(p, WEIGHT_UPDATE_RATE);
//                                if (visible)
//                                    e.colorAdd(p, p, p, COLOR_UPDATE_RATE / v);
//                            }
//                        }
//                    }
//                });
//
//
//            }
//        }
//    }

    private static class StatementVis implements Graph2DRenderer<Term> {
        public final AtomicBoolean statements = new AtomicBoolean(true);
        final NAR n;

        private StatementVis(NAR n) {
            this.n = n;
        }

        @Override
        public void node(NodeVis<Term> node, GraphEditing<Term> graph) {

            if (!statements.get())
                return;

            Term t = node.id;
            if (t.op().statement) {
                Term subj = t.sub(0);
                if (!(subj instanceof nars.term.Variable)) {
                    Term pred = t.sub(1);
                    if (!(pred instanceof nars.term.Variable)) {
                        @Nullable EdgeVis<Term> e = graph.edge(subj, pred);
                        if (e != null) {
                            float p = 0.5f;
                            e.weightLerp(p, WEIGHT_UPDATE_RATE)
                                .colorLerp(Float.NaN, Float.NaN, (0.9f * p) + 0.1f, COLOR_UPDATE_RATE)
                            ;
                        }
                    }
                }

//                }
            }
        }

    }


}
