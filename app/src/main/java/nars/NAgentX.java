package nars;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.exe.Loop;
import jcog.list.FasterList;
import jcog.math.FloatFirstOrderDifference;
import jcog.math.FloatParam;
import jcog.math.FloatPolarNormalized;
import jcog.pri.mix.control.MixContRL;
import nars.control.*;
import nars.exe.MultiExec;
import nars.gui.Vis;
import nars.gui.graph.EdgeDirected;
import nars.gui.graph.run.SimpleConceptGraph1;
import nars.index.term.map.CaffeineIndex;
import nars.op.Anoncepts;
import nars.op.mental.Inperience;
import nars.op.stm.ConjClustering;
import nars.op.video.*;
import nars.term.Term;
import nars.time.RealTime;
import nars.time.Tense;
import nars.truth.Truth;
import nars.util.signal.Bitmap2D;
import nars.util.signal.CameraSensor;
import nars.util.signal.Sensor2D;
import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.Bead;
import net.beadsproject.beads.core.UGen;
import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.ugens.*;
import org.HdrHistogram.DoubleHistogram;
import org.apache.commons.lang.StringUtils;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.tuple.primitive.IntObjectPair;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import spacegraph.AspectAlign;
import spacegraph.Ortho;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.layout.Grid;
import spacegraph.layout.VSplit;
import spacegraph.render.Draw;
import spacegraph.widget.button.CheckBox;
import spacegraph.widget.button.PushButton;
import spacegraph.widget.console.ConsoleTerminal;
import spacegraph.widget.meta.WindowToggleButton;
import spacegraph.widget.meter.BitmapMatrixView;
import spacegraph.widget.meter.Plot2D;
import spacegraph.widget.meter.TreeChart;
import spacegraph.widget.slider.BaseSlider;
import spacegraph.widget.slider.FloatSlider;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static nars.$.$;
import static nars.Op.BELIEF;
import static nars.gui.Vis.reflect;
import static nars.time.Tense.ETERNAL;
import static spacegraph.SpaceGraph.window;
import static spacegraph.layout.Grid.*;

/**
 * Extensions to NAgent interface:
 * <p>
 * --chart output (spacegraph)
 * --cameras (Swing and OpenGL)
 */
abstract public class NAgentX extends NAgent {


    public final Set<CameraSensor> cam = new LinkedHashSet<>();


    public NAgentX(String id, NAR nar) {
        super(id, nar);

        new ActionInfluencingSensorConcept(
                id != null ? $.inh($.the("joy"), id) : $.the("joy"),
                new FloatPolarNormalized(new FloatFirstOrderDifference(nar::time,
                        () -> reward)));

    }

    public static NAR runRT(Function<NAR, NAgent> init, float fps) {
        return runRT(init, fps * 2, fps);
    }


    public static NAR runRT(Function<NAR, NAgent> init, float narFPS, float agentFPS) {

        The.Subterms.the =
                The.Subterms.CaffeineSubtermBuilder.get();

        //The.Subterms.SoftSubtermBuilder.get();
//        The.Compound.the =
//            The.Compound.
//                    //SoftCompoundBuilder.get();
//                    CaffeineCompoundBuilder.get();


        float durFPS =
                agentFPS;
        //agentFPS * 2f; //nyquist
        //agentFPS * 3f;

        RealTime clock =
                durFPS >= 10 / 2f ? /* nyquist threshold between decisecond (0.1) and centisecond (0.01) clock resolution */
                        new RealTime.CS(true) :
                        new RealTime.DSHalf(true);

        clock.durFPS(durFPS);

//        Function<NAR, PrediTerm<Derivation>> deriver = Deriver.deriver(8
//                , "motivation.nal"
//                //., "relation_introduction.nal"
//        );


        int THREADS = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);

        //Predicate<Activate> randomBool = (a) -> ThreadLocalRandom.current().nextBoolean();

//        exe.add(new FocusExec(), (x) -> true);
//        exe.add(new FocusExec() {
//                    {
//                        concepts.setCapacity(32);
//                    }
//                },
//                (x) -> true);

        NAR n = new NARS()
//                .exe(new UniExec(64) {
//                    @Override
//                    public boolean concurrent() {
//                        return true;
//                    }
//                })
                .exe(new MultiExec
                        //Intense
                        //CoolNQuiet
                        (512, THREADS, 128))

                .time(clock)
                .deriverAdd(1, 1)
                .deriverAdd(2, 5)
                .deriverAdd(6, 6)
                //.deriverAdd(6,6) //extra NAL6
                .deriverAdd(7, 8)
                //.deriverAdd("goal_analogy.nal")
                .deriverAdd("motivation.nal")
                .deriverAdd("list.nal")
                .index(
                        new CaffeineIndex(200 * 1024)
                        // new PriMapTermIndex()
                        //new CaffeineIndex2(64 * 1024)
                        //new CaffeineIndex2(-1)
                        //new HijackTermIndex(Primes.nextPrime( 64 * 1024 + 1),  3)
                        //new MapTermIndex(new CustomConcurrentHashMap<>(STRONG, EQUALS, SOFT, EQUALS, 128*1024))
                )
                .get();

        n.defaultWants();

        n.conceptActivation.set(0.1f);

        n.dtMergeOrChoose.set(true);
        n.dtDither.set(0.5f);

        n.confMin.set(0.01f);
        n.freqResolution.set(0.01f);
        n.termVolumeMax.set(32);

        n.beliefConfidence(0.9f);
        n.goalConfidence(0.9f);


        float priFactor = 0.3f;
        n.DEFAULT_BELIEF_PRIORITY = 1f * priFactor;
        n.DEFAULT_GOAL_PRIORITY = 1f * priFactor;
        n.DEFAULT_QUESTION_PRIORITY = 1f * priFactor;
        n.DEFAULT_QUEST_PRIORITY = 1f * priFactor;

        NAgent a = init.apply(n);

        new Deriver(a.fire(), Deriver.deriver(6, 7,
                "motivation.nal", "goal_analogy.nal").apply(n).deriver, n) {
            @Override
            protected long matchTime(Task task) {

                if (task.isEternal()) {
                    return ETERNAL;
                } else {
                    return this.now +
                            Util.sqr(n.random().nextInt(4)) * n.dur(); //forward
                }

            }
        };

        Loop aLoop = a.runFPS(agentFPS);


        //n.dtDither.setValue(0.25f);
        //n.dtMergeOrChoose.setValue(true);

        //STMLinkage stmLink = new STMLinkage(n, 1, false);

//        LinkClustering linkClusterPri = new LinkClustering(n, Prioritized::priElseZero /* anything temporal */,
//                32, 128);

//        LinkClustering linkClusterConf = new LinkClustering(n, (t) -> t.isBeliefOrGoal() ? t.conf() : Float.NaN,
//                4, 16);

//        SpaceGraph.window(col(
//                new STMView.BagClusterVis(n, linkClusterPri.bag),
//                new STMView.BagClusterVis(n, linkClusterConf.bag)
//        ), 800, 600);


        ConjClustering conjClusterBinput = new ConjClustering(n, BELIEF, (t->t.isInput()), 32, 128);
        ConjClustering conjClusterBnonInput = new ConjClustering(n, BELIEF, (t->!t.isInput()), 4, 8);

        //ConjClustering conjClusterG = new ConjClustering(n, 3, GOAL, true, false, 16, 64);

//        n.runLater(() -> {
////            AudioContext ac = new AudioContext();
////            ac.start();
////            Clock aclock = new Clock(ac, 1000f / (agentFPS * 0.5f));
////            new Metronome(aclock, n);
//            new VocalCommentary(null, a);
//            //ac.out.dependsOn(aclock);
//        });


        Inperience inp = new Inperience(n, 32);
//

//        Abbreviation abb = new Abbreviation(n, "z", 3, 6, 10f, 32);

        //reflect.ReflectSimilarToTaskTerm refSim = new reflect.ReflectSimilarToTaskTerm(16, n);
        //reflect.ReflectClonedTask refTask = new reflect.ReflectClonedTask(16, n);


        //a.trace = true;


//        n.onTask(t -> {
//            if (t instanceof DerivedTask)
//                System.out.println(t);
//        });


//        NInner nin = new NInner(n);
//        nin.start();


//        AgentService mc = MetaGoal.newController(a);

        //init();


//        n.onCycle(nn -> {
//            float lag = narLoop.lagSumThenClear() + a.running().lagSumThenClear();
//            //n.emotion.happy(-lag);
//            //n.emotion.happy(n.emotion.busyPri.getSum()/50000f);
//        });


        new Anoncepts(8, n);

//        new Implier(2f, a,
//                1
//                //0,1,4
//        );

//        AgentService p = new AgentService.AgentBuilder(
//                //DQN::new,
//                HaiQAgent::new,
//                //() -> Util.tanhFast(a.dexterity())) //reward function
//                () -> a.dexterity() * Util.tanhFast( a.reward) /* - lag */ ) //reward function
//
//                .in(a::dexterity)
//                .in(new FloatNormalized(()->a.reward).decay(0.9f))
//                .in(new FloatNormalized(
//                        ((Emotivation) n.emotion).cycleDTRealMean::getValue)
//                            .decay(0.9f)
//                )
//                .in(new FloatNormalized(
//                        //TODO use a Long-specific impl of this:
//                        new FirstOrderDifferenceFloat(n::time, () -> n.emotion.taskDerived.getValue().longValue())
//                ).decay(0.9f))
//                .in(new FloatNormalized(
//                        //TODO use a Long-specific impl of this:
//                        new FirstOrderDifferenceFloat(n::time, () -> n.emotion.conceptFirePremises.getValue().longValue())
//                    ).decay(0.9f)
//                ).in(new FloatNormalized(
//                        () -> n.emotion.busyVol.getSum()
//                    ).decay(0.9f)
//                ).out(
//                        new StepController((x) -> n.time.dur(Math.round(x)), 1, n.dur(), n.dur()*2)
//                ).out(
//                        StepController.harmonic(n.confMin::setValue, 0.01f, 0.08f)
//                ).out(
//                        StepController.harmonic(n.truthResolution::setValue, 0.01f, 0.08f)
//                ).out(
//                        StepController.harmonic(a.curiosity::setValue, 0.01f, 0.16f)
//                ).get(n);

//
//        window(new MatrixView(p.in, (x, gl) -> {
//            Draw.colorBipolar(gl, x);
//            return 0;
//        }), 100, 100);


        //get ready
        System.gc();

        Loop loop = a.nar.startFPS(narFPS);


        a.nar.runLater(() ->

        {

            chart(a);

//            window(new ConceptView(a.happy,n), 800, 600);

            window(new Vis.EmotionPlot(64, a), 500, 500);

            window(grid(Vis.reflect(n)), 700, 600);

            Surface exePanel = row(
                    Vis.reflect(n.loop)
            );


            window(
                    new VSplit(exePanel,
                            col(
                                    //metaGoalChart(a),
                                    row(
                                            metaGoalPlot(a),
                                            metaGoalControls(n)
                                    )),
                            0.9f), 800, 800);

        });

        return n;
    }

    private static Surface metaGoalControls(NAR n) {
        CheckBox auto = new CheckBox("Auto");
        //auto.set(true);

        Grid g = grid(
                Stream.concat(
                        Stream.of(auto),
                        IntStream.range(0, n.want.length).mapToObj(
                                w -> new FloatSlider(n.want[w], -2f, +2f) {

                                    @Override
                                    protected void paint(GL2 gl) {
                                        if (auto.on()) {
                                            value(n.want[w]);
                                        }
                                        super.paint(gl);
                                    }
                                }
                                        .label(MetaGoal.values()[w].name())
                                        .draw(BaseSlider.Knob)
                                        .on((s, v) -> {
                                            if (!auto.on())
                                                n.want[w] = v;
                                        })
                        )).toArray(Surface[]::new));

        return g;
    }

    /**
     * increments/decrements within a finite set of powers-of-two so that harmonics
     * wont interfere as the resolution changes
     * <p>
     * TODO allow powers other than 2, ex: 1.618
     */
    public static class StepController implements IntConsumer, IntObjectPair<StepController> {

        private final FloatProcedure update;
        final float[] v;
        int x;

        public StepController(FloatProcedure update, float... steps) {
            v = steps;
            this.update = update;
        }

        public static StepController harmonic(FloatProcedure update, float min, float max) {

            FloatArrayList f = new FloatArrayList();
            float x = min;
            while (x <= max) {
                f.add(x);
                x *= 2;
            }
            assert (f.size() > 1);
            return new StepController(update, f.toArray());
            //set(0);
        }

        private void set(int i) {
            if (i < 0) i = 0;
            if (i >= v.length) i = v.length - 1;
            //if (this.x != i) {
            update.value(v[x = i]);
            //}
        }

        @Override
        public void accept(int aa) {
            //System.out.println(aa);

            switch (aa) {
                case 0:
                    set(x - 1);
                    break;
                case 1:
                    set(x + 1);
                    break;
                default:
                    throw new RuntimeException("OOB");
//                case 1:
//                    break; //nothing
            }
        }

        /**
         * number actions
         */
        @Override
        public int getOne() {
            return 2;
        }

        @Override
        public StepController getTwo() {
            return this;
        }

        @Override
        public int compareTo(@NotNull IntObjectPair<StepController> o) {
            throw new UnsupportedOperationException();
        }
    }


    private static Surface metaGoalChart(NAgent a) {

        return new TreeChart<Cause>() {
            final DurService on;

            final FasterList<ItemVis<Cause>> cache = new FasterList();

            final Function<Cause, TreeChart.ItemVis<Cause>> builder = ((i) -> {
                short id = i.id;
                ItemVis<Cause> item;
                if (cache.capacity() - 1 < id)
                    cache.ensureCapacity(id + 16);
                else {
                    item = cache.get(id);
                    if (item != null)
                        return item;
                }


                String str = i.toString();
                if (str.startsWith("class nars."))
                    str = str.substring("class nars.".length()); //skip default toString

                if (str.startsWith("class "))
                    str = str.substring(5); //skip default toString

                item = new CauseVis(i, str);
                cache.set(id, item);
                return item;
            });

            {

                on = a.onFrame(() -> {
                    update(a.nar.causes, (c, i) -> {
                        float v = c.value();
                        float r, g, b;
                        if (v < 0) {
                            r = 0.75f * Math.max(0.1f, Math.min(1f, -v));
                            g = 0;
                        } else {
                            g = 0.75f * Math.max(0.1f, Math.min(1f, +v));
                            r = 0;
                        }

                        float t = Util.sum(((FloatFunction<Traffic>) (p -> Math.abs(p.current + p.prev))), c.goalValue) / 2f;

                        b = Math.max(r, g) / 2f * Util.unitize(t);

                        i.update(v, r, g, b);
//                        i.updateMomentum(
//                                //0.01f + Util.sqr(Util.tanhFast(v)+1),
//                                //Math.signum(v) *(1+Math.abs(v))*(t),
//                                //Math.signum(v) * t,
//                                v,
//                                0.25f,
//                                r, g, b);

                    }, builder);
                });
            }

            @Override
            public void stop() {
                super.stop();
                on.stop();
            }
        };
    }

    private static Surface metaGoalPlot(NAgent a) {
        NAR nar = a.nar;

        int s = nar.causes.size();

        FloatParam gain = new FloatParam(20f, 0f, 20f);

        BitmapMatrixView bmp = new BitmapMatrixView((i) ->
                Util.tanhFast(
                        gain.floatValue() * nar.causes.get(i).value()
                ),
                //Util.tanhFast(nar.causes.get(i).value()),
                s, Math.max(1, (int) Math.ceil(Math.sqrt(s))),
                Draw::colorBipolar) {

            final DurService on;

            {
                on = a.onFrame(this::update);
            }

            @Override
            public void stop() {
                super.stop();
                on.stop();
            }
        };

        return new VSplit(bmp, Vis.reflect(gain), 0.1f);
    }

//    public static class NARSView extends Grid {
//
//
//        public NARSView(NAR n, NAgent a) {
//            super(
//                    //new MixBoard(n, n.in),
//                    //new MixBoard(n, n.nalMix), //<- currently dont use this it will itnerfere with the stat collection
//
//
//
//
//
//                    //row(n.sub.stream().map(c -> Vis.reflect(n)).collect(toList()))
//            );
////                (n.sub.stream().map(c -> {
////                int capacity = 128;
////                return new BagChart<ITask>(
////                        //new Bagregate<>(
////                                ((BufferedSynchronousExecutorHijack) c.exe).active
////                          //      ,capacity*2,
////                            //    0.9f
////                        //)
////                        ,capacity) {
////
////                    @Override
////                    public void accept(ITask x, ItemVis<ITask> y) {
////                        float p = Math.max(x.priElseZero(), Pri.EPSILON);
////                        float r = 0, g = 0, b = 0;
////                        int hash = x.hashCode();
////                        switch (Math.abs(hash) % 3) {
////                            case 0: r = p/2f; break;
////                            case 1: g = p/2f; break;
////                            case 2: b = p/2f; break;
////                        }
////                        switch (Math.abs(2837493 ^ hash) % 3) {
////                            case 0: r += p/2f; break;
////                            case 1: g += p/2f; break;
////                            case 2: b += p/2f; break;
////                        }
////
////                        y.update(p, r, g, b);
////                    }
////                };
////            }).collect(toList()))); //, 0.5f);
//            a.onFrame(x -> update());
//        }
//
//        protected void update() {
////            /*bottom().*/forEach(x -> {
////                x.update();
////            });
//        }
//    }

    //    public static NAR newAlann(int dur) {
//
//        NAR nar = NARBuilder.newALANN(new RealTime.CS(true).dur( dur ), 3, 512, 3, 3, 2 );
//
//        nar.termVolumeMax.set(32);
//
//        MySTMClustered stm = new MySTMClustered(nar, 64, '.', 8, true, 3);
//        MySTMClustered stmGoal = new MySTMClustered(nar, 32, '!', 8, true, 3);
//
////        Abbreviation abbr = new Abbreviation(nar, "the",
////                4, 16,
////                0.05f, 32);
//
//        new Inperience(nar, 0.05f, 16);
//
//        /*SpaceGraph.window(grid(nar.cores.stream().map(c ->
//                Vis.items(c.activeBag(), nar, 16)).toArray(Surface[]::new)), 900, 700);*/
//
//        return nar;
//    }


    public static void chart(NAgent a) {
        NAR nar = a.nar;
        a.nar.runLater(() -> {
            window(grid(

                    //new WindowButton("log", () -> Vis.logConsole(nar, 80, 25, new FloatParam(0f))),
                    new PushButton("dump", () -> {
                        try {
                            nar.output(Files.createTempFile(a.toString(), "" + System.currentTimeMillis()).toFile(), false);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }),
                    new PushButton("prune", () -> {
                        nar.runLater(() -> {
                            DoubleHistogram i = new DoubleHistogram(2);
                            nar.tasks(true, false, false, false).forEach(t ->
                                    i.recordValue(t.conf())
                            );
                            float confThresh = (float) i.getValueAtPercentile(25);
                            nar.tasks(true, false, false, false).filter(t ->
                                    t.conf() < confThresh
                            ).forEach(Task::delete);
                        });
                    }),
                    new WindowToggleButton("top", () -> (new ConsoleTerminal(new nars.TextUI(nar).session(20f)))),
                    new WindowToggleButton("concept graph", () -> {
                        SimpleConceptGraph1 sg;
                        SpaceGraph s = new SpaceGraph<>(
                                sg = new SimpleConceptGraph1(nar,
                                        64, 7)
                        );
                        EdgeDirected fd = new EdgeDirected();
                        s.dyn.addBroadConstraint(fd);
                        fd.attraction.set(fd.attraction.get() * 4);

                        s.add(new Ortho(
                                //window(
                                grid(reflect(fd), reflect(sg.vis))) {
                            @Override
                            protected void resized() {

                                surface.pos(0, 0, W / 3, H / 4);

                                scale.set(1, 1);
                                cam.set(W / 2, H / 2);

                                layout();
                            }
                        });

                        //,  400, 400);
                        //.pos(0, 0, 0.5f, 0.5f)

                        s.camPos(0, 0, 90);
                        return s;
                    }),


                    //new WindowButton("prompt", () -> Vis.newInputEditor(), 300, 60)

                    //Vis.beliefCharts(16, nar, a.reward),
                    new WindowToggleButton("agent", () -> (a)),
                    col(
                            new WindowToggleButton("actionShort", () -> Vis.beliefCharts(a.nar.dur() * 16, a.actions.keySet(), a.nar)),
                            new WindowToggleButton("actionMed", () -> Vis.beliefCharts(a.nar.dur() * 64, a.actions.keySet(), a.nar)),
                            new WindowToggleButton("actionLong", () -> Vis.beliefCharts(a.nar.dur() * 256, a.actions.keySet(), a.nar))
                    ),
                    //new WindowButton("predict", () -> Vis.beliefCharts(200, a.predictors, a.nar)),
                    //"agentActions",
                    //"agentPredict",

                    a instanceof NAgentX ?
                            new WindowToggleButton("vision", () -> grid(((NAgentX) a).cam.stream().map(cs ->
                                    new AspectAlign(new CameraSensorView(cs, a), AspectAlign.Align.Center, cs.height, cs.width))
                                    .toArray(Surface[]::new))
                            ) : grid()

//                    grid(
////                    new WindowButton( "conceptBudget",
////                            ()->{
////
////                                double[] d = new double[32];
////                                return new HistogramChart(
////                                        ()->d,
////                                        //()->h.uniformProb(32, 0, 1.0)
////                                        new Color3f(0.5f, 0.25f, 0f), new Color3f(1f, 0.5f, 0.25f)) {
////
////                                    On on = a.onFrame((r) -> {
////                                        Bag.priHistogram(r.nar.focus().concepts(), d);
////                                    });
////
////                                    @Override
////                                    public Surface hide() {
////                                        on.off();
////                                        return this;
////                                    }
////                                };
////                            }
////                        //Vis.budgetHistogram(nar, 64)
////                    ),
//
////                    new WindowButton( "conceptTreeMap", () -> {
////
////                        BagChart tc = new Vis.ConceptBagChart(new Bagregate(
////                                ((NARS)a.nar).sub.stream().flatMap(x ->
////                                        (((BufferedSynchronousExecutorHijack)(x.exe)).active.stream().map(
////                                    y -> (y instanceof ConceptFire) ? ((ConceptFire)y) : null
////                                ).filter(Objects::nonNull)), 128, 0.5f), 128, nar);
////
////                        return tc;
////                    })
//
//                            //"tasks", ()-> taskChart,
//
//                            new WindowButton("conceptGraph", () ->
//                                    Vis.conceptsWindow3D(nar, 128, 4))
//
//                    )
            ), 900, 600);
        });
    }

//    public static void chart(NAgent a) {
//
//        a.nar.runLater(() -> {
//
//            //Vis.conceptsWindow3D(a.nar, 64, 12).show(1000, 800);
////
////            BagChart<Concept> tc = new Vis.ConceptBagChart(new Bagregate(a.nar.focus().concepts(), 32, 0.5f), 32, a.nar);
////
//
//            window(
//                    grid(
//                            new ReflectionSurface<>(a),
//                            Vis.beliefCharts(100, a.actions, a.nar ),
//
//                            Vis.emotionPlots(a, 256),
//
//                            //tc,
//
//
//                            //budgetHistogram(d, 16),
//
//                            //Vis.agentActions(a, 50),
//                            //Vis.beliefCharts(400, a.predictors, a.nar),
//                            new ReflectionSurface<>(a.nar),
//
//                            Vis.budgetHistogram(a.nar, 24)
//                            /*Vis.conceptLinePlot(nar,
//                                    Iterables.concat(a.actions, Lists.newArrayList(a.happy, a.joy)),
//                                    2000)*/
//                    ), 1200, 900);
//        });
//    }

    /**
     * pixelTruth defaults to linear monochrome brightness -> frequency
     */
    protected CameraSensor senseCamera(String id, Container w, int pw, int ph) throws Narsese.NarseseException {
        return senseCamera(id, new SwingBitmap2D(w), pw, ph);
    }

    protected CameraSensor<Scale> senseCamera(String id, Supplier<BufferedImage> w, int pw, int ph) throws
            Narsese.NarseseException {
        return senseCamera(id, new Scale(w, pw, ph));
    }

//    protected CameraSensor<Scale> senseCamera(String id, Container w, int pw, int ph) throws Narsese.NarseseException {
//        return senseCamera(id, new Scale(new SwingBitmap2D(w), pw, ph));
//    }

    protected Sensor2D<PixelBag> senseCameraRetina(String id, Container w, int pw, int ph) throws
            Narsese.NarseseException {
        return senseCameraRetina(id, new SwingBitmap2D(w), pw, ph);
    }

    protected Sensor2D<PixelBag> senseCameraRetina(String id, Container w, int pw, int ph, FloatToObjectFunction<
            Truth> pixelTruth) throws Narsese.NarseseException {
        return senseCameraRetina(id, new SwingBitmap2D(w), pw, ph);
    }

    protected CameraSensor<PixelBag> senseCameraRetina(String id, Supplier<BufferedImage> w, int pw, int ph) throws
            Narsese.NarseseException {
        return senseCameraRetina($(id), w, pw, ph);
    }

    protected CameraSensor<PixelBag> senseCameraRetina(Term id, Supplier<BufferedImage> w, int pw, int ph) {
        PixelBag pb = PixelBag.of(w, pw, ph);
        pb.addActions(id, this);
        return senseCamera(id, pb);
    }

    protected Sensor2D<WaveletBag> senseCameraFreq(String id, Supplier<BufferedImage> w, int pw, int ph) throws
            Narsese.NarseseException {
        WaveletBag pb = new WaveletBag(w, pw, ph);
        return senseCamera(id, pb);
    }

    protected <C extends Bitmap2D> CameraSensor<C> senseCamera(@Nullable String id, C bc) throws
            Narsese.NarseseException {
        return senseCamera(id != null ? $(id) : null, bc);
    }

    protected <C extends Bitmap2D> CameraSensor<C> senseCamera(@Nullable Term id, C bc) {
        return addCamera(new CameraSensor(id, bc, this));
    }

    protected <C extends Bitmap2D> CameraSensor<C> senseCameraReduced(@Nullable Term
                                                                              id, Supplier<BufferedImage> bc, int sx, int sy, int ox, int oy) {
        return addCamera(new CameraSensor(id, new AutoencodedBitmap(new BufferedImageBitmap2D(bc), sx, sy, ox, oy), this));
    }

    protected <C extends Bitmap2D> CameraSensor<C> senseCameraReduced(@Nullable Term id, C bc, int sx, int sy,
                                                                      int ox, int oy) {
        return addCamera(new CameraSensor(id, new AutoencodedBitmap(bc, sx, sy, ox, oy), this));
    }

    protected <C extends Bitmap2D> CameraSensor<C> addCamera(CameraSensor<C> c) {
        cam.add(c);
        return c;
    }

    static Surface mixPlot(NAgent a, MixContRL m, int history) {
        return Grid.grid(m.dim, i -> col(
                new MixGainPlot(a, m, history, i),
                new MixTrafficPlot(a, m, history, i)
        ));
    }

    private static class MixGainPlot extends Plot2D {
        public MixGainPlot(NAgent a, MixContRL m, int history, int i) {
            super(history, BarWave);

            add(m.id(i), () -> m.gain(i), -1f, +1f);
            a.onFrame(this::update);
        }
    }

    private static class MixTrafficPlot extends Plot2D {
        public MixTrafficPlot(NAgent a, MixContRL m, int history, int i) {
            super(history, Line);
            add(m.id(i) + "_in", () -> m.trafficInput(i), 0f, 1f);
            add(m.id(i), () -> m.trafficActive(i), 0f, 1f);
            a.onFrame(this::update);
        }
    }

    private static class CauseVis extends TreeChart.ItemVis<Cause> {
        public CauseVis(Cause i, String str) {
            super(i, StringUtils.abbreviate(str, 26));
        }

        @Override
        public float requestedArea() {
            return 0.01f + super.requestedArea();
        }
    }

    private static class Metronome {
        public Metronome(Clock cc, NAR n) {
            cc.on(new Bead<Clock>() {

                AudioContext ac = cc.getContext();
                public final Envelope kickEnv, snareEnv;

                {
                    kickEnv = new Envelope(ac, 0.0f); //gain of kick drum

                    UGen kickGain = new Gain(ac, 1, kickEnv).in(
                            new BiquadFilter(ac, BiquadFilter.BESSEL_LP, 500.0f, 1.0f).in(
                                    new WavePlayer(ac, 100.0f, Buffer.SINE)));

                    ac.out.in(kickGain);

                }

                {
                    snareEnv = new Envelope(ac, 0.0f);
                    // set up the snare WavePlayers
                    WavePlayer snareNoise = new WavePlayer(ac, 1.0f, Buffer.NOISE);
                    WavePlayer snareTone = new WavePlayer(ac, 200.0f, Buffer.SINE);
                    // set up the filters
                    IIRFilter snareFilter = new BiquadFilter(ac, BiquadFilter.BP_SKIRT, 2500.0f, 1.0f);
                    snareFilter.in(snareNoise);
                    snareFilter.in(snareTone);
                    // set up the Gain
                    Gain snareGain = new Gain(ac, 1, snareEnv);
                    snareGain.in(snareFilter);

                    // connect the gain to the main out
                    ac.out.in(snareGain);
                }

                @Override
                protected void messageReceived(Clock c) {
                    if (c.isBeat(16)) {
                        snareEnv.add(0.5f, 2.00f);
                        snareEnv.add(0.2f, 8.0f);
                        snareEnv.add(0.0f, 80.0f);
                        n.believe($.the("snare"), Tense.Present);
                    }
                    if (c.isBeat(4)) {

                        kickEnv.add(0.5f, 2.0f); // attack segment
                        kickEnv.add(0.2f, 5.0f); // decay segment
                        kickEnv.add(0.0f, 50.0f);  // release segment
                        n.believe($.the("kick"), Tense.Present);

//                        //choose some nice frequencies
//                        //if (random(1) < 0.5) return;
//                        float pitch = Pitch.forceToScale((int) random(12), Pitch.dorian);
//                        float freq = Pitch.mtof(pitch + (int) random(5) * 12 + 32);
//                        WavePlayer wp = new WavePlayer(ac, freq, Buffer.SINE);
//                        Gain g = new Gain(ac, 1, new Envelope(ac, 0));
//                        g.addInput(wp);
//                        ac.out.addInput(g);
//                        ((Envelope) g.getGainUGen()).add(0.1f, random(200));
//                        ((Envelope) g.getGainUGen()).add(0, random(200), g.die());
                    }
                }
            });
        }
    }

    private static class VocalCommentary {
        public VocalCommentary(Clock ac, NAgent a) {

            NARchy.installSpeech(a.nar);
            try {
                a.nar.goal($("speak(ready)"), Tense.Present, 1f, 0.9f);
//                a.nar.believe($("(" + a.sad + " =|> speak(sad))."));
//                a.nar.goal($("(" + a.sad + " &| speak(sad))"));
                a.nar.believe($("(" + a.happy + " =|> speak(happy))."));
                a.nar.goal($("(" + a.happy + " &| speak(happy))"));
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
            }

        }
    }


    //    private static class CorePanel extends Surface{
//
//        public CorePanel(Default2.GraphPremiseBuilder c, NAR nar) {
//            super();
//            grid(Vis.items(c.terms, nar, 10))
//        }
//    }

//    protected <C extends PixelCamera> MatrixSensor addMatrixAutoEncoder(String id, C bc, FloatToObjectFunction<Truth> pixelTruth) {
//        CameraSensor c = new CameraSensor<>($.the(id), bc, this, pixelTruth);
//        cam.put(id, c);
//        return c;
//    }

}

