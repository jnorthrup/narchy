package nars.gui;

import com.googlecode.lanterna.input.KeyType;
import jcog.data.list.FasterList;
import jcog.event.Off;
import jcog.exe.Exe;
import jcog.math.Quantiler;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import jcog.pri.Prioritized;
import jcog.pri.bag.impl.PLinkArrayBag;
import jcog.pri.op.PriMerge;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.TextUI;
import nars.agent.NAgent;
import nars.concept.Concept;
import nars.gui.graph.run.BagregateConceptGraph2D;
import nars.term.Termed;
import nars.util.MemorySnapshot;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.*;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.console.ConsoleTerminal;
import spacegraph.space2d.widget.console.TextEdit;
import spacegraph.space2d.widget.meta.MetaFrame;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space2d.widget.meta.ServicesTable;
import spacegraph.space2d.widget.slider.FloatGuage;
import spacegraph.space2d.widget.tab.ButtonSet;
import spacegraph.space2d.widget.tab.TabPane;
import spacegraph.space2d.widget.text.LabeledPane;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.util.math.Color3f;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;
import static nars.$.$$;
import static nars.truth.TruthFunctions.w2cSafe;
import static spacegraph.SpaceGraph.window;

/**
 * SpaceGraph-based visualization utilities for NARchy
 */
public class NARui {


    public static Surface inputEditor() {
        return new TextEdit(40, 8);
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
        Map<String, Supplier<Surface>> m = Map.of(
                "shl", () -> new ConsoleTerminal(new TextUI(n).session(10f)),
                "nar", () -> new ObjectSurface<>(n),
                "exe", () -> ExeCharts.exePanel(n),
                "val", () -> ExeCharts.valuePanel(n),
                "mem", () -> MemEdit(n),
                "can", () -> ExeCharts.focusPanel(n), ///causePanel(n),
                "grp", () -> BagregateConceptGraph2D.get(n).widget(),
                "svc", () -> new ServicesTable(n.services),
                "cpt", () -> bagHistogram((Iterable) () -> n.conceptsActive().iterator(), 8, n)
        );
        HashMap<String, Supplier<Surface>> mm = new HashMap();
        mm.putAll(m);
        mm.put(
                "snp", () -> memoryView(n)
        );
        mm.put(
                "tsk", () -> taskView(n)
        );
//        mm.put("mem", () -> ScrollGrid.list(
//                (int x, int y, Term v) -> new PushButton(m.toString()).click(() ->
//                        window(
//                                ScrollGrid.list((xx, yy, zm) -> new PushButton(zm.toString()), n.memory.contents(v).collect(toList())), 800, 800, true)
//                ),
//                n.memory.roots().collect(toList())
//                )
//        );
        return
                new Bordering(
                        new TabPane().addToggles(mm)
                )
                        .north(ExeCharts.runPanel(n))
                //.south(new OmniBox(new NarseseJShellModel(n))) //+50mb heap
                ;
    }

    public static Surface MemEdit(NAR nar) {
        return new Gridding(
                MemLoad(nar),
                MemSave(nar),
                new PushButton("remove weak beliefs", () -> {
                    nar.runLater(() -> {
                        nar.logger.info("Belief prune start");
//                        final long scaleFactor = 1_000_000;
                        //Histogram i = new Histogram(1<<20, 5);
                        Quantiler q = new Quantiler(16 * 1024);
                        long now = nar.time();
                        int dur = nar.dur();
                        nar.tasks(true, false, false, false).forEach(t ->
                                {
                                    try {
                                        float c = w2cSafe(t.evi(now, dur));
                                        //i.recordValue(Math.round(c * scaleFactor));
                                        q.add(c);
                                    } catch (Throwable e) {
                                        e.printStackTrace();
                                    }
                                }
                        );
                        //System.out.println("Belief evidence Distribution:");
                        //Texts.histogramPrint(i, System.out);

                        //float confThresh = i.getValueAtPercentile(50)/ scaleFactor;
                        float confThresh = q.quantile(0.9f);
                        if (confThresh > Param.TRUTH_MIN_EVI) {
                            final int[] removed = {0};
                            nar.tasks(true, false, false, false, (c, t) -> {
                                try {
                                    if (w2cSafe(t.evi(now, dur)) < confThresh)
                                        if (c.remove(t))
                                            removed[0]++;
                                } catch (Throwable e) {
                                    e.printStackTrace();
                                }
                            });
                            nar.logger.info("Belief prune finish: {} tasks removed", removed[0]);
                        }
                    });
                })

        );

    }

    public static Surface MemLoad(NAR nar) {
        return new VectorLabel("Load: TODO");
    }

    public static Surface MemSave(NAR nar) {
        TextEdit path = new TextEdit(20, 1);
        try {
            path.text(Files.createTempFile(nar.self().toString(), "" + System.currentTimeMillis()).toAbsolutePath().toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        Object currentMode = null;
        ButtonSet mode = new ButtonSet(ButtonSet.Mode.One,
                new CheckBox("txt"), new CheckBox("bin")
        );
        return new Gridding(
                path,
                new Gridding(
                        mode,
                        new PushButton("save").click(() -> {
                            Exe.invokeLater(() -> {
                                try {
                                    nar.output(new File(path.text()), false);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                            });
                        })
        ));
    }

    private static Surface taskView(NAR n) {

        int cap = 32;
        float rate = 1f;

        CheckBox updating = new CheckBox("Update");
        updating.set(true);

        /** TODO make multithread better */
        PLinkArrayBag<Task> b = new PLinkArrayBag<>(PriMerge.replace, cap);
        List<Task> taskList = new FasterList();

        ScrollGrid<Task> tasks = ScrollGrid.listCached(t ->
                        new Splitting(new FloatGuage(0, 1, t::priElseZero),
                                new PushButton(new VectorLabel(t.toStringWithoutBudget())).click(() -> {
                                    conceptWindow(t, n);
                                }),
                                false, 0.1f),
                taskList, 64);

        TextEdit input = new TextEdit(16, 1);
        input.onKey((k) -> {
            if (k.getKeyType() == KeyType.Enter) {
                //input
            }
        });


        Surface s = new Splitting(
                tasks,
                new Gridding(updating, input /* ... */)
                , 0.1f);

        Off onTask = n.onTask((t) -> {
            if (updating.get()) {
                b.put(new PLink<>(t, t.pri() * rate));
            }
        });
        return DurSurface.get(s, n, (nn) -> {

        }, (nn) -> {
            if (updating.get()) {
                synchronized (tasks) {
                    taskList.clear();
                    b.commit();
                    b.forEach(x -> taskList.add(x.get()));
                    tasks.layout(); //HACK
                }
            }
        }, (nn) -> {
            onTask.off();
        });
    }

    private static Surface memoryView(NAR n) {

        return new ScrollGrid<>(new KeyValueModel(new MemorySnapshot(n).byAnon),
                (x, y, v) -> {
                    if (x == 0) {
                        return new PushButton(v.toString()).click(() -> {

                        });
                    } else {
                        return new VectorLabel(((Collection) v).size() + " concepts");
                    }
                });
    }

    public static void conceptWindow(String t, NAR n) {
        conceptWindow($$(t), n);
    }

    public static void conceptWindow(Termed t, NAR n) {
        window(new ConceptSurface(t, n), 500, 500);
    }

    public static Surface agent(NAgent a) {

        Iterable<Concept> rewards = () -> a.rewards.stream().flatMap(r -> StreamSupport.stream(r.spliterator(), false)).iterator();
        Iterable<? extends Concept> actions = a.actions;

        TabPane aa = new TabPane().addToggles(Map.of(
                a.toString(), () -> new ObjectSurface<>(a, 4),
                "emotion", () -> new EmotionPlot(128, a),
                "reward", () -> NARui.beliefCharts(rewards, a.nar()),
                "actions", () -> NARui.beliefCharts(actions, a.nar())
        ));
        return aa;
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


    /**
     * TODO make this a static utility method of Gridding that take a surface builder Function applied to an Iterable
     */
    public static class BeliefChartsGrid extends Gridding {


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
