package nars.gui;

import jcog.pri.PriReference;
import jcog.pri.Prioritized;
import nars.NAR;
import nars.agent.NAgent;
import nars.gui.graph.run.ConceptGraph2D;
import nars.term.Termed;
import nars.util.MemorySnapshot;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.Stacking;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.grid.KeyValueModel;
import spacegraph.space2d.container.grid.ScrollGrid;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.console.ConsoleTerminal;
import spacegraph.space2d.widget.console.TextEdit;
import spacegraph.space2d.widget.meta.MetaFrame;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space2d.widget.meta.ServicesTable;
import spacegraph.space2d.widget.tab.TabPane;
import spacegraph.space2d.widget.text.LabeledPane;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.util.math.Color3f;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;
import static nars.$.$$;
import static spacegraph.SpaceGraph.window;

/**
 * SpaceGraph-based visualization utilities for NARchy
 */
public class NARui {


    public static Surface inputEditor() {
        return new TextEdit(40, 8).surface();
    }

    public static Surface beliefCharts(NAR nar, Object... x) {
        return beliefCharts(List.of(x), nar);
    }

    public static Surface beliefCharts(Iterable ii, NAR nar) {
        return new BeliefChartsGrid(ii, nar);
    }


    public static <X extends Prioritized> Surface bagHistogram(Iterable<X> bag, int bins, NAR n) {


        float[] d = new float[bins];
        return DurSurface.get(new HistogramChart(
                        () -> d,
                        new Color3f(0.5f, 0.25f, 0f), new Color3f(1f, 0.5f, 0.1f)),


                n, () -> PriReference.histogram(bag, d));


    }


    public static VectorLabel label(Object x) {
        return label(x.toString());
    }

    public static VectorLabel label(String text) {
        return new VectorLabel(text);
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


    public static Surface top(NAR n) {
        return
                new Bordering(
                        new TabPane().addToggles(Map.of(
                                                        "shl", () -> new ConsoleTerminal(new nars.TextUI(n).session(10f)),
                                                        "nar", () -> new ObjectSurface<>(n),
                                                        "exe", () -> ExeCharts.exePanel(n),
                                                        "can", () -> ExeCharts.focusPanel(n), ///causePanel(n),
                                                        "grp", () -> new ConceptGraph2D(n).widget(),
                                                        "svc", () -> new ServicesTable(n.services),
                                                        "cpt", () -> bagHistogram((Iterable) () -> n.conceptsActive().iterator(), 8, n),
                                                        "snp", () -> memoryView(n),
                                                        "mem", () -> ScrollGrid.list(
                                                                (x, y, m) -> new PushButton(m.toString()).click((mm) ->

                                                                        window(
                                                                                ScrollGrid.list((xx, yy, zm) -> new PushButton(zm.toString()), n.memory.contents(m).collect(toList())), 800, 800, true)
                                                                ),
                                                                n.memory.roots().collect(toList())
                                                        )
                                                ))
                )
                        .north(ExeCharts.runPanel(n))
                        //.south(new OmniBox(new NarseseJShellModel(n))) //+50mb heap
                ;
    }

    private static Surface memoryView(NAR n) {

        return new ScrollGrid<>(new KeyValueModel(new MemorySnapshot(n).byAnon),
                (x, y, v)-> {
                    if (x == 0) {
                        return new PushButton(v.toString()).click(() -> {

                        });
                    } else {
                        return new VectorLabel(((Collection)v).size() +  " concepts");
                    }
                });
    }

    public static void conceptWindow(String t, NAR n) {
        conceptWindow($$(t), n);
    }

    public static void conceptWindow(Termed t, NAR n) {
        window(new ConceptSurface(t, n), 500, 500);
    }

    public static ObjectSurface<NAgent> agent(NAgent a) {
        return new ObjectSurface<>(a, 4);
//            .on(Bitmap2DSensor.class, (Bitmap2DSensor b) ->
//                new PushButton(b.id.toString()).click(()-> {
//                    window(new AspectAlign(
//                        new CameraSensorView(b, a.nar()).withControls(),
//                        AspectAlign.Align.Center, b.width, b.height), 500, 500);
//                }))
//            .on(x -> x instanceof Concept,
//                    (Concept x) -> new MetaFrame(new BeliefTableChart(x.term(), a.nar())))
//            .on(x -> x instanceof LinkedHashMap, (LinkedHashMap x)->{
//                return new AutoSurface<>(x.keySet());
//            })

            //.on(Loop.class, LoopPanel::new),

    }

//    @Deprecated public static void agentOld(NAgent a) {
//        NAR nar = a.nar();
//        //nar.runLater(() -> {
//            window(
//                    grid(
//                            new ObjectSurface(a),
//
//                            beliefCharts(a.actions(), a.nar()),
//
//                            new EmotionPlot(64, a),
//                            grid(
//
//                                    new TextEdit() {
//                                        @Override
//                                        protected void onKeyEnter() {
//                                            String s = text();
//                                            text("");
//                                            try {
//                                                nar.conceptualize(s);
//                                            } catch (Narsese.NarseseException e) {
//                                                e.printStackTrace();
//                                            }
//                                            conceptWindow(s, nar);
//                                        }
//                                    }.surface(),
//
//
//                                    new PushButton("dump", () -> {
//                                        try {
//                                            nar.output(Files.createTempFile(a.toString(), "" + System.currentTimeMillis()).toFile(), false);
//                                        } catch (IOException e) {
//                                            e.printStackTrace();
//                                        }
//                                    }),
//
//                                    new PushButton("clear", () -> {
//                                        nar.runLater(NAR::clear);
//                                    }),
//
//                                    new PushButton("prune", () -> {
//                                        nar.runLater(() -> {
//                                            nar.logger.info("Belief prune start");
//                                            final long scaleFactor = 1_000_000;
//                                            //Histogram i = new Histogram(1<<20, 5);
//                                            Quantiler q = new Quantiler(16*1024);
//                                            long now = nar.time();
//                                            int dur = nar.dur();
//                                            nar.tasks(true, false, false, false).forEach(t ->
//                                                    {
//                                                        try {
//                                                            float c = w2cSafe(t.evi(now, dur));
//                                                            //i.recordValue(Math.round(c * scaleFactor));
//                                                            q.add(c);
//                                                        } catch (Throwable e) {
//                                                            e.printStackTrace();
//                                                        }
//                                                    }
//                                            );
//                                            //System.out.println("Belief evidence Distribution:");
//                                            //Texts.histogramPrint(i, System.out);
//
//                                            //float confThresh = i.getValueAtPercentile(50)/ scaleFactor;
//                                            float confThresh = q.quantile(0.9f);
//                                            if (confThresh > 0) {
//                                                nar.tasks(true, false, false, false, (c, t) -> {
//                                                    try { if (w2cSafe(t.evi(now, dur)) < confThresh)
//                                                        c.remove(t); } catch (Throwable e) { e.printStackTrace(); }
//                                                });
//                                            }
//                                            nar.logger.info("Belief prune finish");
//                                        });
//                                    }),
//
//                                    new WindowToggleButton("top", () -> new ConsoleTerminal(new nars.TextUI(nar).session(10f))),
//
//                                    new WindowToggleButton("concept graph", () -> {
//                                        DynamicConceptSpace sg;
//                                        SpaceGraphPhys3D s = new SpaceGraphPhys3D<>(
//                                                sg = new DynamicConceptSpace(nar, () -> nar.attn.active().iterator(),
//                                                        128, 16)
//                                        );
//                                        EdgeDirected3D fd = new EdgeDirected3D();
//                                        s.dyn.addBroadConstraint(fd);
//                                        fd.condense.set(fd.condense.get() * 8);
//
//                                        s.add(new SubOrtho(
//
//                                                grid(new ObjectSurface<>(fd), new ObjectSurface<>(sg.vis))) {
//
//                                        }.posWindow(0, 0, 1f, 0.2f));
//
//
//
//
//                                        s.camPos(0, 0, 90);
//                                        return s;
//                                    })
//
//
//
//
//
//
//
//
//
//
//
//
//
//
////
////                                    a instanceof NAgentX ?
////                                            new WindowToggleButton("vision", () -> grid(((NAgentX) a).sensorCam.stream().map(cs -> new AspectAlign(
////                                                    new CameraSensorView(cs, a).withControls(), AspectAlign.Align.Center, cs.width, cs.height))
////                                                    .toArray(Surface[]::new))
////                                            ) : grid()
//                            )
//                    ),
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//                    900, 600);
//        //});
//    }


    /** TODO make this a static utility method of Gridding that take a surface builder Function applied to an Iterable */
    public static class BeliefChartsGrid extends Gridding  {


        public BeliefChartsGrid(Iterable<?> ii, NAR nar) {
            super();

            List<Surface> s = StreamSupport.stream(ii.spliterator(), false)
                    .map(x -> x instanceof Termed ? (Termed) x : null).filter(Objects::nonNull)
                    .map(c -> new MetaFrame(new BeliefTableChart(c, nar))).collect(toList());

            if (!s.isEmpty()) {
                set(s);
            } else {
                set(label("(empty)"));
            }

        }


    }

//    static class NarseseJShellModel extends OmniBox.JShellModel {
//        private final NAR nar;
//
//        public NarseseJShellModel(NAR n) {
//            this.nar = n;
//        }
//
//        @Override
//        public void onTextChange(String text, int cursorPos, MutableContainer target) {
//            super.onTextChange(text, cursorPos, target);
//        }
//
//        @Override
//        public void onTextChangeControlEnter(String text, MutableContainer target) {
//            text = text.trim();
//            if (text.isEmpty())
//                return;
//            try {
//                nar.input(text);
//            } catch (Narsese.NarseseException e) {
//                super.onTextChangeControlEnter(text, target);
//            }
//        }
//
//    }
}
