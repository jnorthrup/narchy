package nars.gui;

import jcog.Service;
import jcog.pri.PriReference;
import nars.NAR;
import nars.control.DurService;
import nars.term.Termed;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.Stacking;
import spacegraph.space2d.widget.console.ConsoleTerminal;
import spacegraph.space2d.widget.console.TextEdit;
import spacegraph.space2d.widget.meta.AutoSurface;
import spacegraph.space2d.widget.tab.TabPane;
import spacegraph.space2d.widget.text.Label;
import spacegraph.space2d.widget.text.LabeledPane;
import spacegraph.util.math.Color3f;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;
import static nars.$.$$;

/**
 * SpaceGraph-based visualization utilities for NAR analysis
 */
public class Vis {


    public static Surface inputEditor() {
        return new TextEdit(40, 8).surface();
    }

    public static Surface beliefCharts(int window, NAR nar, Object... x) {
        return beliefCharts(window, List.of(x), nar);
    }

    public static Surface beliefCharts(int window, Iterable ii, NAR nar) {
        BeliefChartsGrid g = new BeliefChartsGrid(ii, nar, window);
        return DurSurface.get(g, nar);
    }


    //    public static <X extends Termed> BagChart<X> items(Bag<X,PLink<X>> bag, final Cycles d, final int count) {
//        BagChart tc = new BagChart(bag, count) {
//            @Override
//            public void accept(PLink x, ItemVis y) {
//                float p = x.pri();
//
//                float[] f = Draw.hsb(
//                        (0.3f * x.get().hashCode() / (float) Integer.MAX_VALUE),
//                        .5f + 0.25f * p, 0.5f + 0.25f * p, 1f, null);
//                y.update(p, f[0], f[1], f[2]);
//
//            }
//        };
//
//        d.onCycle(xx -> {
//
//            //if (s.window.isVisible()) {
//            tc.update();
//            //}
//        });
//
//        return tc;
//    }

//    public static Surface budgetHistogram(NAR nar, int bins) {
//        if (nar instanceof Default) {
//            return budgetHistogram((Iterable) nar.focus().concepts(), bins);
//        } else { //if (nar instance)
//            //return budgetHistogram(((Default2)nar).active, bins);
//            return grid(); //TODO
//        }
//    }

    public static Surface bagHistogram(Iterable<? extends PriReference> bag, int bins, NAR n) {
        //new SpaceGraph().add(new Facial(


        float[] d = new float[bins];
        return DurSurface.get(new HistogramChart(
                () -> d,
                new Color3f(0.5f, 0.25f, 0f), new Color3f(1f, 0.5f, 0.1f)),

//                Vis.pane("Concept Volume",
//                        new HistogramChart(
//                                () -> Bag.priHistogram(bag, d),
//                                new Color3f(0.5f, 0.25f, 0f), new Color3f(1f, 0.5f, 0.1f))
//                )
                n, () -> PriReference.histogram(bag, d));

//                PanelSurface.of("Concept Durability Distribution (0..1)", new HistogramChart(nar, c -> {
//                    if (c != null)
//                        return c.dur();
//                    return 0;
//                }, bins, new Color3f(0f, 0.25f, 0.5f), new Color3f(0.1f, 0.5f, 1f)))

    }

//    public static Grid conceptLinePlot(NAR nar, Iterable<? extends Termed> concepts, int plotHistory, FloatFunction<Termed> value) {
//
//        //TODO make a lambda Grid constructor
//        Grid grid = new Grid();
//        List<Plot2D> plots = $.newArrayList();
//        for (Termed t : concepts) {
//            Plot2D p = new Plot2D(plotHistory, Plot2D.Line /*BarWave*/);
//            p.add(t.toString(), () -> value.floatValueOf(t), 0f, 1f);
//            grid.children.add(p);
//            plots.add(p);
//        }
//        grid.layout();
//
//        nar.onCycle(f -> {
//            plots.forEach(Plot2D::update);
//        });
//
//        return grid;
//    }

//    public static Gridding conceptBeliefPlots(NAgent a, Iterable<? extends Termed> concepts, int plotHistory) {
//
//        //TODO make a lambda Grid constructor
//        Gridding grid = new Gridding();
//        List<Plot2D> plots = $.newArrayList();
//        for (Termed t : concepts) {
//            final Truth[] bb = {$.t(0.5f, 0.5f)};
//            Plot2D p = new Plot2D(plotHistory, Plot2D.Line /*BarWave*/) {
//
//                @Override
//                protected void paintWidget(GL2 gl, RectFloat2D bounds) {
//                    Concept concept = a.nar.concept(t);
//
//                    bb[0] = a.nar.beliefTruth(concept, a.nar.time());
//                    float b;
//                    b = bb[0] == null ? 0f : 2f * (bb[0].freq()) - 1f;
//
//                    backgroundColor[0] = b < 0 ? -b / 4f : 0;
//                    backgroundColor[1] = 0;
//                    backgroundColor[2] = b >= 0 ? b / 4f : 0;
//                    backgroundColor[3] = 0.9f;
//                }
//            };
//            p.setTitle(t.toString());
////            p.add("P", () -> a.nar.pri(t, Float.NaN), 0f, 1f);
////            p.add("G", () -> a.nar.concept(t).goalFreq(nar.time(), nar.dur()), 0f, 1f);
//            p.add("B", () -> {
//                return bb[0] != null ? bb[0].freq() : Float.NaN;
//            }, 0f, 1f);
//            p.add("G", () -> {
//                Truth b = a.nar.goalTruth(t, a.nar.time());
//                return b != null ? b.freq() : Float.NaN;
//            }, 0f, 1f);
//
//            plots.add(p);
//        }
//        grid.set(plots);
//
//        a.onFrame(f -> {
//            plots.forEach(Plot2D::update);
//        });
//
//        return grid;
//    }


//    public static Grid agentBudgetPlot(NAgent t, int history) {
//        return conceptLinePlot(t.nar,
//                Iterables.concat(t.actions, Lists.newArrayList(t.happy, t.joy)), history);
//    }

    public static Label label(Object x) {
        return label(x.toString());
    }

    public static Label label(String text) {
        return new Label(text);
    }

    /**
     * ordering: first is underneath, last is above
     */
    public static Stacking stack(Surface... s) {
        return new Stacking(s);
    }

    public static LabeledPane pane(String k, Surface s) {
        return new LabeledPane(k, s);
    }


//    public static SpaceGraph<Term> conceptsWindow2D(NAR nar, int maxNodes, int maxEdges) {
//        return conceptsWindow(new ConceptsSpace(nar, maxNodes, 1, maxEdges));
//    }
//
//    public static SpaceGraph<Term> conceptsWindow2D(NAR nar, Iterable<? extends Termed> terms, int max, int maxEdges) {
//        List<ConceptWidget> termWidgets = StreamSupport.stream(terms.spliterator(), false).map(x -> new ConceptWidget(x.term())).collect(toList());
//
//        NARSpace active = new NARSpace(nar) {
//
//            final ObjectFloatHashMap<Term> priCache = new ObjectFloatHashMap<>();
//            final FloatFunction<Term> termFloatFunction = k -> nar.pri(k);
//
//            @Override
//            protected void get(Collection displayNext) {
//                Collections.sort(termWidgets, (a, b) -> {
//                    return Float.compare(
//                            priCache.getIfAbsentPutWithKey(b.key, termFloatFunction),
//                            priCache.getIfAbsentPutWithKey(a.key, termFloatFunction)
//                    );
//                });
//                priCache.clear();
//
//                for (int i = 0; i < max; i++) {
//                    ConceptWidget w = termWidgets.get(i);
//                    displayNext.add(w);
//                }
//            }
//        };
//
//        return conceptsWindow(
//                active
//        );
//    }

//    public static SpaceGraph<Term> conceptsWindow(AbstractSpace nn) {
//        Surface controls = col(new PushButton("x"), row(new FloatSlider("z", 0, 0, 4)), new CheckBox("?"))
//                .hide();
//
//
//        ForceDirected fd;
//        SpaceGraph<Term> s = new SpaceGraph2D<>()
////                .add(
////                        new Ortho(
//////                                new FloatSlider("~", 0, 0, 1f).on((slider, v) -> {
//////
//////                                }).scale(100, 100).pos(0f, 0f)
////
////                                new ConsoleTerminal(new ConsoleSurface.EditTerminal(40,20))
////
//////                                new CheckBox("").on((cb, v) -> {
//////                                    if (!v)
//////                                        controls.hide();
//////                                    else
//////                                        controls.scale(200,200f).pos(300f,300f);
//////                                }).scale(100, 100).pos(0f, 0f)
////
////                        ).scale(500,500))
//                .add(new Ortho(controls))
//                .add(nn.with(
//                        new Flatten()
//                        //new Spiral()
//                        //new FastOrganicLayout()
//                )).with(fd = new EdgeDirected());
//
//        s.add(new Ortho(new CrosshairSurface(s)));
//
//        window(new ReflectionSurface(fd), 500, 500);
//
//        return s;
//    }

//    public static ConsoleSurface logConsole(NAR nar, int cols, int rows, FloatParam priMin) {
//        ConsoleSurface term = new ConsoleTerminal(cols, rows);
//        new TaskLeak(4, 0.25f, nar) {
//
//            @Override
//            public float value() {
//                return 1;
//            }
//
//            @Override
//            public boolean preFilter(@NotNull Task next) {
//                if (next.pri() >= priMin.floatValue()) {
//                    return super.preFilter(next);
//                }
//                return false;
//            }
//
//            @Override
//            protected float leak(Task next) {
//                if (next.pri() >= priMin.floatValue()) {
//                    try {
//                        next.appendTo(term);
//                        term.append('\n');
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    return 1;
//                }
//                return 0;
//            }
//        };
//        return term;
//    }

    public static Surface top(NAR n) {
        return
                new Splitting(
                        ExeCharts.runPanel(n),
                        new TabPane(Map.of(
                                "shl", () -> new ConsoleTerminal(new nars.TextUI(n).session(10f)),
                                "nar", () -> new AutoSurface<>(n),
                                "exe", () -> ExeCharts.exePanel(n),
                                "can", () -> ExeCharts.causePanel(n),
                                "svc", () -> new AutoSurface(n.services) {
                                    @Override
                                    protected boolean addService(Service x) {
                                        return !(x instanceof DurService) && super.addService(x);
                                    }
                                },
                                "cpt", () -> bagHistogram((Iterable)()->n.conceptsActive().iterator(), 8, n)
                        )), 0.9f);
    }

    public static void conceptWindow(String t, NAR n) {
        conceptWindow($$(t), n);
    }

    public static void conceptWindow(Termed t, NAR n) {
        SpaceGraph.window(new ConceptSurface(t, n), 500, 500);
    }


    public static class BeliefChartsGrid extends Gridding implements Consumer<NAR> {

        private final int window;
        //        private final DurService on;
        long[] btRange;

        public BeliefChartsGrid(Iterable<?> ii, NAR nar, int window) {
            super();

            btRange = new long[2];
            this.window = window;

            List<Surface> s = StreamSupport.stream(ii.spliterator(), false)
                    .map(x -> x instanceof Termed ? (Termed) x : null).filter(Objects::nonNull)
                    .map(c -> new BeliefTableChart(nar, c, btRange)).collect(toList());

            if (!s.isEmpty()) {
                set(s);
//                on = DurService.on(nar, this);
            } else {
//                on = null;
                set(label("(empty)"));
            }

        }


        @Override
        public void accept(NAR nar) {
            long now = nar.time();
            int dur = nar.dur();
            btRange[0] = now - (window * dur);
            btRange[1] = now + (window * dur);
        }
    }
}
