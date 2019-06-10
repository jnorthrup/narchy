package nars.gui;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AtomicDouble;
import jcog.TODO;
import jcog.Util;
import jcog.data.iterator.ArrayIterator;
import jcog.data.list.FasterList;
import jcog.data.list.table.Table;
import jcog.event.Off;
import jcog.exe.Exe;
import jcog.learn.ql.HaiQae;
import jcog.math.Quantiler;
import jcog.pri.VLink;
import jcog.thing.Part;
import jcog.thing.Thing;
import nars.AttentionUI;
import nars.NAR;
import nars.Narsese;
import nars.Task;
import nars.agent.Game;
import nars.agent.util.RLBooster;
import nars.attention.TaskLinkWhat;
import nars.attention.What;
import nars.concept.Concept;
import nars.concept.sensor.Signal;
import nars.gui.concept.ConceptColorIcon;
import nars.gui.concept.ConceptSurface;
import nars.gui.graph.run.BagregateConceptGraph2D;
import nars.link.TaskLink;
import nars.link.TaskLinks;
import nars.op.stm.ConjClustering;
import nars.task.util.PriBuffer;
import nars.term.Termed;
import nars.time.part.DurLoop;
import nars.truth.Truth;
import nars.util.MemorySnapshot;
import org.eclipse.collections.api.block.function.primitive.IntToIntFunction;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.jetbrains.annotations.Nullable;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.ScrollXY;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.Stacking;
import spacegraph.space2d.container.graph.NodeVis;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.grid.KeyValueGrid;
import spacegraph.space2d.widget.Widget;
import spacegraph.space2d.widget.button.ButtonSet;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.button.Submitter;
import spacegraph.space2d.widget.menu.Menu;
import spacegraph.space2d.widget.menu.TabMenu;
import spacegraph.space2d.widget.meta.MetaFrame;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space2d.widget.meta.PartsTable;
import spacegraph.space2d.widget.meta.TriggeredSurface;
import spacegraph.space2d.widget.meter.PaintUpdateMatrixView;
import spacegraph.space2d.widget.meter.Plot2D;
import spacegraph.space2d.widget.meter.ScatterPlot2D;
import spacegraph.space2d.widget.meter.Spectrogram;
import spacegraph.space2d.widget.port.FloatRangePort;
import spacegraph.space2d.widget.text.LabeledPane;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.space2d.widget.textedit.TextEdit;
import spacegraph.video.Draw;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

import static com.jogamp.newt.event.KeyEvent.VK_ENTER;
import static java.util.stream.Collectors.toList;
import static nars.$.$$;
import static nars.Op.*;
import static nars.truth.func.TruthFunctions.w2cSafe;
import static spacegraph.space2d.container.grid.Gridding.VERTICAL;
import static spacegraph.space2d.container.grid.Gridding.grid;

/**
 * SpaceGraph-based visualization utilities for NARchy
 */
public class NARui {

    public static Surface beliefChart(Termed x, NAR nar) {
        return new Widget(new MetaFrame(new BeliefTableChart(x, nar)));
     }
    public static Surface beliefCharts(NAR nar, Termed... x) {
        return beliefCharts(ArrayIterator.iterable(x), nar);
    }

    public static Surface beliefCharts(Iterable<? extends Termed> ii, NAR nar) {
        return new Gridding(Lists.newArrayList(Iterables.transform(ii, i -> {
            return beliefChart(i, nar);
        })));
    }

    /**
     * ordering: first is underneath, last is above
     */
    public static Stacking stack(Surface... s) {
        return new Stacking(s);
    }


    public static Surface top(NAR n) {
        return new Bordering(
                //new Splitting(
                    new TabMenu(menu(n) /* , new WallMenuView() */ )
//                    0.5f,
//                    new TabMenu(parts(n), new GridMenuView().aspect(2))
//                ).resizeable()
            ).north(ExeCharts.runPanel(n))
            //.south(new OmniBox(new NarseseJShellModel(n))) //+50mb heap
            ;
    }

    public static HashMap<String, Supplier<Surface>> parts(Thing p) {
        HashMap<String,Supplier<Surface>> m = new HashMap<>();
        p.partStream().forEach(s -> {
            m.put( ((Part)s).toString(), ()-> new ObjectSurface(s));
        });
        return m;
    }

    public static HashMap<String, Supplier<Surface>> menu(NAR n) {
        Map<String, Supplier<Surface>> m = Map.of(
                //"shl", () -> new ConsoleTerminal(new TextUI(n).session(10f)),
                "nar", () -> new ObjectSurface<>(n, 1),
                "on", () -> new ObjectSurface(n.atMap().entrySet(), 2),
                "exe", () -> ExeCharts.exePanel(n),
                "val", () -> ExeCharts.valuePanel(n),
                "mem", () -> MemEdit(n),
                "can", () -> ExeCharts.causeProfiler(n),
                //ExeCharts.focusPanel(n),
                ///causePanel(n),
                "svc", () -> new PartsTable(n),
                "cpt", () -> new ConceptBrowser(n)
        );
        HashMap<String, Supplier<Surface>> mm = new HashMap<>()
        {{
            putAll(m);
            put("snp", () -> memoryView(n));
            put("tsk", () -> taskView(n));
//            put("mem", () -> ScrollGrid.list(
//                (int x, int y, Term v) -> new PushButton(m.toString()).click(() ->
//                        window(
//                                ScrollGrid.list((xx, yy, zm) -> new PushButton(zm.toString()), n.memory.contents(v).collect(toList())), 800, 800, true)
//                ),
//                n.memory.roots().collect(toList())
//                )
//        );
        }};

        return mm;
    }

//    private static Surface priView(NAR n) {
//        TaskLinks cc = n.attn;
//
//        return Splitting.row(
//                new BagView<>(cc.links, n),
//                0.2f,
//                new Gridding(
//                     new ObjectSurface(
////                        new XYSlider(
////                                cc.activationRate
//                                cc.decay
//                                //.subRange(1/1000f, 1/2f)
//                        ) {
////                            @Override
////                            public String summaryX(float x) {
////                                return "forget=" + n4(x);
////                            }
////
////                            @Override
////                            public String summaryY(float y) {
////                                return "activate=" + n4(y);
////                            }
//                        },
//
//                        new PushButton("Print", () -> {
//                            Appendable a = null;
//                            try {
//                                a = TextEdit.out().append(
//                                        Joiner.on('\n').join(cc.links)
//                                );
//                                window(a, 800, 500);
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }),
//                        new PushButton("Clear", () -> cc.links.clear())
//                )
//        );
//
//    }

    public static Surface MemEdit(NAR nar) {
        return new Gridding(
                memLoad(nar),
                memSave(nar),
                new PushButton("Prune Beliefs", () -> {
                    nar.runLater(() -> {
                        //nar.logger.info("Belief prune start");
//                        final long scaleFactor = 1_000_000;
                        //Histogram i = new Histogram(1<<20, 5);
                        Quantiler q = new Quantiler(128 * 1024);
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
                        float confThresh = q.quantile(0.5f);
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
                        //nar.logger.info("Belief prune finish: {} tasks removed", removed[0]);
                    });
                })

        );

    }

    public static Surface memLoad(NAR nar) {
        return new VectorLabel("Load: TODO");
    }

    public static Surface memSave(NAR nar) {
        TextEdit path = new TextEdit(40);
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
                        new PushButton("save").clicked(() -> {
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

    public static Surface taskView(NAR n) {
        List<Predicate<Task>> filter = new CopyOnWriteArrayList<>();
        Consumer<Task> printer = (Task t) -> {
            if (Util.and(t, (Iterable) filter))
                System.out.println(t);
        };

        return LabeledPane.the("Trace",
                grid(
                        grid(
                                new CheckBox("Belief").on(taskTrace(n, BELIEF, printer)),
                                new CheckBox("Goal").on(taskTrace(n, GOAL, printer)),
                                new CheckBox("Question").on(taskTrace(n, QUESTION, printer)),
                                new CheckBox("Quest").on(taskTrace(n, QUEST, printer))
                        ),
                        grid(
                                new CheckBox("Not Eternal").on(taskFilter(filter, (x) -> !x.isEternal())),
                                new CheckBox("Not Signal").on(taskFilter(filter, (x) -> !(x instanceof Signal))),
                                new CheckBox("Not Input").on(taskFilter(filter, (x) -> x.stamp().length > 1))
                                //TODO priority and complexity sliders
                        )
                )
        );
    }

    static BooleanProcedure taskFilter(List<Predicate<Task>> ff, Predicate<Task> f) {
        return new BooleanProcedure() {
            @Override
            public synchronized void value(boolean on) {
                if (on) {
                    ff.add(f);
                } else {
                    boolean rem = ff.remove(f);
                    assert (rem);
                }
            }
        };
    }


    static BooleanProcedure taskTrace(NAR n, byte punc, Consumer<Task> printer) {
        return new BooleanProcedure() {

            private Off off;

            @Override
            public synchronized void value(boolean b) {
                if (b) {
                    assert (off == null);
                    off = n.onTask(printer, punc);
                } else {
                    assert (off != null);
                    off.close();
                    off = null;
                }
            }
        };
    }

//    public static Surface taskTable(NAR n) {
//
//        int cap = 32;
//        float rate = 1f;
//
//        CheckBox updating = new CheckBox("Update");
//        updating.on(true);
//
//        /** TODO make multithread better */
//        PLinkArrayBag<Task> b = new PLinkArrayBag<>(PriMerge.replace, cap);
//        List<Task> taskList = new FasterList();
//
//        ScrollXY tasks = ScrollXY.listCached(t ->
//                        new Splitting<>(new FloatGuage(0, 1, t::priElseZero),
//                                new PushButton(new VectorLabel(t.toStringWithoutBudget())).click(() -> {
//                                    conceptWindow(t, n);
//                                }),
//                                false, 0.1f),
//                taskList, 64);
//        tasks.view(1, cap);
//
//        TextEdit0 input = new TextEdit0(16, 1);
//        input.onKey((k) -> {
//            if (k.getKeyType() == KeyType.Enter) {
//                //input
//            }
//        });
//
//
//        Surface s = new Splitting(
//                tasks,
//                new Gridding(updating, input /* ... */)
//                , 0.1f);
//
//        Off onTask = n.onTask((t) -> {
//            if (updating.on()) {
//                b.put(new PLinkHashCached<>(t, t.priElseZero() * rate));
//            }
//        });
//        return DurSurface.get(s, n, (nn) -> {
//
//        }, (nn) -> {
//            if (updating.on()) {
//                synchronized (tasks) {
//                    taskList.clear();
//                    b.commit();
//                    b.forEach(x -> taskList.add(x.get()));
//                    tasks.update();
//                }
//            }
//        }, (nn) -> {
//            onTask.off();
//        });
//    }

    private static Surface memoryView(NAR n) {

        return new ScrollXY<>(new KeyValueGrid(new MemorySnapshot(n).byAnon),
                (x, y, v) -> {
                    if (x == 0) {
                        return new PushButton(v.toString()).clicked(() -> {

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
        SpaceGraph.window(new ConceptSurface(t, n), 500, 500);
    }

    public static Surface agent(Game a) {

        Iterable<? extends Concept> rewards = () -> a.rewards.stream().flatMap(r -> StreamSupport.stream(r.spliterator(), false)).iterator();
        Iterable<? extends Concept> actions = a.actions;

        Menu aa = new TabMenu(Map.of(
                a.toString(), () -> new ObjectSurface<>(a, 4),

                "stat", () -> new Gridding(
                    new TriggeredSurface<>(
                            new Plot2D(512, Plot2D.Line)
                                    .add("Happy", a::happiness),
                            a::onFrame, Plot2D::commit),
                    new TriggeredSurface<>(
                        new Plot2D(512, Plot2D.Line)
                                .add("Dex+0", a::dexterity),
                            a::onFrame, Plot2D::commit),
                    new TriggeredSurface<>(
                            new Plot2D(512, Plot2D.Line)
                                    .add("Coh", ()->a.coherency()),
                            a::onFrame, Plot2D::commit)
                    ),

//                        .addAt("Dex+2", () -> a.dexterity(a.now() + 2 * a.nar().dur()))
//                        .addAt("Dex+4", () -> a.dexterity(a.now() + 4 * a.nar().dur())), a),
                "reward", () -> NARui.beliefCharts(rewards, a.nar()),
                "actions", () -> NARui.beliefCharts(actions, a.nar())
        ));
        return LabeledPane.the(a.id.toString(), aa);
//            .on(Bitmap2DSensor.class, (Bitmap2DSensor b) ->
//                new PushButton(b.id.toString()).click(()-> {
//                    window(new AspectAlign(
//                        new CameraSensorView(b, a.nar()).withControls(),
//                        AspectAlign.Align.Center, b.width, b.height), 500, 500);
//                }))
//            .on(x -> x instanceof Concept,
//                    (Concept x) -> new MetaFrame(new BeliefTableChart(x.target(), a.nar())))
//            .on(x -> x instanceof LinkedHashMap, (LinkedHashMap x)->{
//                return new AutoSurface<>(x.keySet());
//            })

        //.on(Loop.class, LoopPanel::new),

    }

    public static Gridding beliefIcons(List<? extends Termed> c, NAR nar) {

        BiConsumer<Concept, spacegraph.space2d.phys.common.Color3f> colorize = (concept, color) -> {
            if (concept != null) {

                @Nullable Truth b = nar.beliefTruth(concept, nar.time());
                if (b != null) {
                    float f = b.freq();
                    float conf = b.conf();
                    float a = 0.25f + conf * 0.75f;
                    color.set((1 - f) * a, f * a, 0);
                    return;
                }
            }
            color.set(0.5f, 0.5f, 0.5f);
        };
        List<ConceptColorIcon> d = c.stream().map(x -> new ConceptColorIcon(x.term(), nar, colorize)).collect(toList());
        return grid(d);
    }

    public static TextEdit newNarseseInput(NAR n, Consumer<Task> onTask, Consumer<Exception> onException) {
        TextEdit input = new TextEdit(16, 1);
        input.onKey((k) -> {
            if (k.getKeyCode() == VK_ENTER) {
                String s = input.text();
                input.text("");
                try {
                    List<Task> t = n.input(s);
                    t.forEach(onTask);
                } catch (Narsese.NarseseException e) {
                    onException.accept(e);
                }
            }
        });
        return input;
    }

    public static Surface clusterView(ConjClustering c, NAR n) {

        ScatterPlot2D.ScatterPlotModel<VLink<Task>> model = new ScatterPlot2D.SimpleXYScatterPlotModel<VLink<Task>>() {


            private long now = n.time();

            @Override
            public void start() {
                now = n.time();
            }

            @Override
            public void coord(VLink<Task> v, float[] target) {
                Task t = v.get();
                target[0] = t.mid() - now; //to be certain of accuracy with 32-bit reduced precision assigned from long
                target[1] = t.priElseZero();
            }


            @Override
            public String label(VLink<Task> id) {
                return id.get()
                        .term().toString();
                        //toStringWithoutBudget();
            }


            @Override
            public float pri(VLink<Task> v) {
                return v.priElseZero();
            }

            final float[] c = new float[4];

            @Override
            public void colorize(VLink<Task> v, NodeVis<VLink<Task>> node) {
                int centroid = v.centroid;

                float a = 0.8f;//v.priElseZero() * 0.5f + 0.5f;
                if (centroid >= 0) {
                    Draw.colorHash(centroid, c, 1, 0.75f + 0.25f * v.priElseZero(), a);
                    node.color(c[0], c[1], c[2], c[3]);
                } else {
                    node.color(0.5f,0.5f,0.5f, a); //unassigned
                }
            }

            @Override
            public float width(VLink<Task> v, int population) {
                Task t = v.get();
                return (t.term().eventRange() + t.range())/(population * 50f);
                //return (0.5f + v.get().priElseZero()) * 1/20f;
            }
            @Override
            public float height(VLink<Task> v, int population) {
                return 1/(population * 1f);
            }
        };

        ScatterPlot2D<VLink<Task>> s = new ScatterPlot2D<VLink<Task>>(model);
        return DurSurface.get(s, n, () -> {

            s.set(c.data.bag); //Iterable Concat the Centroids as dynamic VLink's

        }).live();
    }

    public static Surface taskBufferView(PriBuffer b, NAR n) {
        Plot2D plot = new Plot2D(256, Plot2D.Line).add("load", b::load, 0, 1);
        DurSurface plotSurface = DurSurface.get(plot, n, plot::commit);
        Gridding g = new Gridding(
                plotSurface,
                new MetaFrame(b),
                new Gridding(
                        new FloatRangePort(
                                DurLoop.cache(b::load, 0, 1, 1, n).getOne(),
                                "load"
                        )
                )
        );
        if (b instanceof PriBuffer.BagTaskBuffer)
            g.add(new BagView(((PriBuffer.BagTaskBuffer)b).tasks, n));

        return g;
    }

    public static Surface tasklinkSpectrogram(Table<?, TaskLink> b, int history, NAR n) {
        return tasklinkSpectrogram(n, b, history, b.capacity());
    }

    public static Surface attentionUI_2(NAR n) {
        //TODO
        return new BagView(n.what, n);
    }

    public static Surface attentionUI(NAR n) {
        //TODO watch for added and removed What's for live update

        Map<String,Supplier<Surface>> global = new HashMap();
        global.put("Attention", ()-> AttentionUI.attentionGraph(n));
        global.put("What", ()-> AttentionUI.whatMixer(n));


        Map<String,Supplier<Surface>> attentions = new HashMap();
        n.what.forEach((v)->{
           attentions.put(v.id.toString(), ()->attentionUI((What)v));
        });
        TabMenu atMenu = new TabMenu(attentions);
        return new Splitting(new TabMenu(global), 0.25f, atMenu).horizontal(true).resizeable();
    }

    public static Surface attentionUI(What w) {
        final Bordering m = new Bordering();
        NAR n = w.nar;
        TaskLinks attn = ((TaskLinkWhat)w).links;
        m.center(new TabMenu(Map.of(
                "Input", () -> taskBufferView(w.in, n),
                "Spectrum", ()->tasklinkSpectrogram(attn.links, 300, n),
                "Histogram", ()->new BagView(attn.links, n),
                "Concepts", ()->BagregateConceptGraph2D.get(attn, n)
        )));
        m.south(new ObjectSurface(attn));
        m.west(new Gridding(
            new PushButton("Clear").clicked(w::clear), //TODO n::clear "Clear All"
            Submitter.text("Load", t->{
                throw new TODO();
            }),
            Submitter.text("Save", t->{
                throw new TODO(); //tagging
            }),
            new PushButton("List").clicked(()->attn.links.bag.print()) //TODO better

        ));
//        m.east(new Gridding(
//                //TODO interactive filter widgets
//        ));

        return m;
    }

    public static Surface tasklinkSpectrogram(NAR n, Table<?,nars.link.TaskLink> active, int history, int width) {

        Spectrogram s = new Spectrogram(true, history, width);

        return DurSurface.get(s, n, new Runnable() {

            final FasterList<TaskLink> snapshot = new FasterList(active.capacity());

            @Override
            public void run() {
                active.forEach(snapshot::add);
                s.next(color);
                snapshot.clear();
            }

            final IntToIntFunction color = _x -> {
                TaskLink x = snapshot.getSafe(_x);
                if (x == null)
                    return 0;

//                float[] bgqq = x.priPuncSnapshot();
                float r = x.priPunc(BELIEF);
                float g = x.priPunc(GOAL);
                float b = (x.priPunc(QUESTION) + x.priPunc(QUEST)) / 2;
                return Draw.rgbInt(r, g, b);

//                    float h;
//                    switch (x.puncMax()) {
//                        case BELIEF: h = 0; break;
//                        case QUESTION: h = 0.25f; break;
//                        case GOAL: h = 0.5f; break;
//                        case QUEST: h = 0.75f; break;
//                        default:
//                            return Draw.rgbInt(0.5f, 0.5f, 0.5f);
//                    }
//
//                    return Draw.colorHSB(h, 0.75f, 0.25f + 0.75f * x.priElseZero());

            };

        });
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
//                                        fd.condense.setAt(fd.condense.get() * 8);
//
//                                        s.addAt(new SubOrtho(
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

    public static Surface rlbooster(RLBooster rlb) {

//            return new Gridding(
//                Stream.of(((HaiQ) (rlb.agent)).q,((HaiQ) (rlb.agent)).et).map(
//                        l -> {
//
//                            BitmapMatrixView i = new BitmapMatrixView(l);
//                            rlb.env.onFrame(i::update);
//                            return i;
//                        }
//                ).collect(toList()));

        Plot2D plot = new Plot2D(200, Plot2D.Line);
        Gridding charts = new Gridding();
        if (rlb.agent instanceof HaiQae) {
            HaiQae q = (HaiQae) rlb.agent;
            charts.add(
                    new ObjectSurface(q),
                    new Gridding(VERTICAL,
                            new PaintUpdateMatrixView(rlb.input),
                            new PaintUpdateMatrixView(q.ae.W),
                            new PaintUpdateMatrixView(q.ae.y),
                            new PaintUpdateMatrixView(rlb.actionFeedback)
                    ),
                    new Gridding(VERTICAL,
                            new PaintUpdateMatrixView(q.q),
                            new PaintUpdateMatrixView(q.et)
                    )
            );
        }
        AtomicDouble rewardSum = new AtomicDouble();
        plot.add("Reward", ()->{
            return rewardSum.getAndSet(0); //clear
        }, 0, +1);

        rlb.env.onFrame(()->{
            rewardSum.addAndGet(rlb.lastReward);
            plot.commit();
        });

        charts.add(plot);
        return charts;


//            window(
//                    new LSTMView(
//                            ((LivePredictor.LSTMPredictor) ((DQN2) rlb.agent).valuePredict).lstm.agent
//                    ), 800, 800
//            );
//
////            window(new Gridding(
////                Stream.of(((DQN2) (rlb.agent)).valuePredict.layers).map(
////                        l -> {
////
////                            BitmapMatrixView i = new BitmapMatrixView(l.input);
////                            BitmapMatrixView w = new BitmapMatrixView(l.weights);
////                            BitmapMatrixView o = new BitmapMatrixView(l.output);
////
////                            a.onFrame(i::update);
////                            a.onFrame(w::update);
////                            a.onFrame(o::update);
////
////                            return new Gridding(i, w, o);
////                        }
////                ).collect(toList()))
////            , 800, 800);

    }

}
