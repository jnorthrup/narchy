package nars;

import jcog.Util;
import jcog.exe.Loop;
import jcog.math.random.XoRoShiRo128PlusRandom;
import jcog.signal.Bitmap2D;
import nars.exe.Focus;
import nars.exe.WorkerMultiExec;
import nars.gui.EmotionPlot;
import nars.gui.Vis;
import nars.gui.graph.DynamicConceptSpace;
import nars.index.term.map.CaffeineIndex;
import nars.op.ArithmeticIntroduction;
import nars.op.mental.Inperience;
import nars.op.stm.ConjClustering;
import nars.term.Term;
import nars.time.RealTime;
import nars.time.Tense;
import nars.util.signal.Bitmap2DSensor;
import nars.video.*;
import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.Auvent;
import net.beadsproject.beads.core.UGen;
import net.beadsproject.beads.data.WaveFactory;
import net.beadsproject.beads.ugens.*;
import org.HdrHistogram.DoubleHistogram;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.tuple.primitive.IntObjectPair;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import org.jetbrains.annotations.Nullable;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.AspectAlign;
import spacegraph.space2d.container.EdgeDirected;
import spacegraph.space2d.hud.SubOrtho;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.console.ConsoleTerminal;
import spacegraph.space2d.widget.console.TextEdit;
import spacegraph.space2d.widget.meta.AutoSurface;
import spacegraph.space2d.widget.meta.WindowToggleButton;
import spacegraph.space3d.SpaceGraphPhys3D;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

import static nars.$.$;
import static nars.$.$$;
import static nars.Op.BELIEF;
import static spacegraph.space2d.container.Gridding.grid;

/**
 * Extensions to NAgent interface:
 * <p>
 * --chart output (spacegraph)
 * --cameras (Swing and OpenGL)
 */
abstract public class NAgentX extends NAgent {

    public NAgentX(String id, NAR nar) {
        super(id, nar);


        if (Param.DEBUG) {
//            nar.onTask(x -> {
//                if (x.isBeliefOrGoal() && x.isEternal()) {
//                    //if (x.isInput())
//                    if (!always.contains(x)) {
//                        System.err.println(x.proof());
//                        System.err.println();
//                    }
//                }
//            });

            nar.onTask(t -> {
                if (t.isGoal() && t.isNegative() && t.term().equals(happy.term())) {
                    System.err.println("MASOCHISM DETECTED:\n" + t.proof());
                }
            });

        }
    }

    public static NAR runRT(Function<NAR, NAgent> init, float fps) {
        return runRT(init,
                //fps * 2, //NYQUIST
                fps * 1, //1:1
                fps);
    }

    public static NAR runRT(Function<NAR, NAgent> init, float narFPS, float agentFPS) {

        //The.Subterms.the =
        //The.Subterms.CaffeineSubtermBuilder.get();
        //The.Subterms.HijackSubtermBuilder.get();

        //The.Subterms.SoftSubtermBuilder.get();
//        The.Compound.the =
//            The.Compound.
//                    //SoftCompoundBuilder.get();
//                    CaffeineCompoundBuilder.get();


        float clockFPS =
                //agentFPS;
                narFPS;

        RealTime clock =
                clockFPS >= 10 / 2f ? /* nyquist threshold between decisecond (0.1) and centisecond (0.01) clock resolution */
                        new RealTime.CS(true) :
                        new RealTime.DSHalf(true);

        clock.durFPS(clockFPS);

//        Function<NAR, PrediTerm<Derivation>> deriver = Deriver.deriver(8
//                , "motivation.nal"
//                //., "relation_introduction.nal"
//        );


        //int THREADS = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);

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

//                .exe(new PoolMultiExec(
//                        128,
//                        //new Focus.DefaultRevaluator()
//                        new Focus.AERevaluator(new XoRoShiRo128PlusRandom(1))
//                    )
//                )

                .exe(new WorkerMultiExec(
                        //new Focus.DefaultRevaluator(),
                        new Focus.AERevaluator(new XoRoShiRo128PlusRandom(1)),
                        256, 8192) {
                        {
                            Util.setExecutor(this);
                        }
                     }
                )

                .time(clock)
                .deriverAdd(1, 1)
                .deriverAdd(2, 2)
                .deriverAdd(3, 3)
                .deriverAdd(5, 5)
                .deriverAdd(6, 8)
                .deriverAdd("motivation.nal")
                //.deriverAdd("goal_analogy.nal")
                //.deriverAdd(6,6) //extra NAL6
                //.deriverAdd("list.nal")

                .index(
                        new CaffeineIndex(
                                //800 * 1024,
                                2500 * 1024,
                                //1200 * 1024,
                                //50 * 1024
                                //20 * 1024,
                                //4096
                                //Integer.MAX_VALUE,
                                c -> {

//                            int HISTORY = 1000;
//                            AtomicHistogram a = c.meta("%");
//                            int score;
//                            if (a == null)
//                                score = 0; //new
//                            else {
//
//                                long now = clock.now();
//                                long count = a.getCountBetweenValues(Math.max(0, now - HISTORY), now);
//                                if (count >= Integer.MAX_VALUE)
//                                    return 0;
//                                score = (int)count;
//                            }
//                            return (Integer.MAX_VALUE - score) / (Integer.MAX_VALUE / (16*1024));

                                    return (int) Math.ceil(c.voluplexity());
//                            return Math.round(
//                                    ((float)c.voluplexity())
//                                        / (1 + 100 * (c.termlinks().priSum() + c.tasklinks().priSum()))
//                                            //(c.beliefs().size() + c.goals().size()))
//                            );
                                }
                        ) /*{
                            @Override
                            public Termed get(Term x, boolean createIfMissing) {

                                Termed t = super.get(x, createIfMissing);

                                if (createIfMissing) {
                                    AtomicHistogram a = ((Concept)t).meta("%", (z)->new AtomicHistogram(Long.MAX_VALUE, 0));
//                                    int a1 = a.getEstimatedFootprintInBytes();
//                                    int a2 = a.getNeededByteBufferCapacity();
                                    a.recordValueWithCount(clock.now(), 1);
                                }

                                return t;
                            }
                        }*/

                        // new PriMapTermIndex()
                        //new CaffeineIndex2(64 * 1024)
                        //new CaffeineIndex2(-1)
                        //new HijackConceptIndex(Primes.nextPrime( 64 * 1024 + 1),  3)
                        //new MapTermIndex(new CustomConcurrentHashMap<>(STRONG, EQUALS, SOFT, EQUALS, 128*1024))
                )
                .get();

        //n.defaultWants();


        n.dtMergeOrChoose.set(true);
        //0.5f //nyquist
        n.dtDither.set(1f);
        //n.timeFocus.set(4);

        n.confMin.set(0.01f);
        n.freqResolution.set(0.01f);
        n.termVolumeMax.set(40);

        n.beliefConfDefault.set(0.9f);
        n.goalConfDefault.set(0.9f);


        float priFactor = 0.2f;
        n.beliefPriDefault.set(1f * priFactor);
        n.goalPriDefault.set(1f * priFactor);
        n.questionPriDefault.set(1f * priFactor);
        n.questPriDefault.set(1f * priFactor);

        n.activationRate.set(0.5f);

        NAgent a = init.apply(n);

//        new RLBooster(a, HaiQAgent::new, 1);


////            @Override
////            protected long matchTime(Task task) {
////
////                //future lookahead to catalyze prediction
////                return n.time() +
////                        Util.sqr(n.random().nextInt(3)) * n.dur();
////
////            }
//        };


//        {
//          AgentBuilder b = MetaGoal.newController(a);
////                .in(a::dexterity)
////                .in(new FloatNormalized(()->a.reward).decay(0.9f))
////                .in(new FloatNormalized(
////                        ((Emotivation) n.emotion).cycleDTRealMean::getValue)
////                            .decay(0.9f)
////                )
//                b.in(new FloatNormalized(
//                        //TODO use a Long-specific impl of this:
//                        new FloatFirstOrderDifference(n::time, () -> n.emotion.deriveTask.getValue().longValue())
//                ).relax(0.99f))
////                .in(new FloatNormalized(
////                        //TODO use a Long-specific impl of this:
////                        new FirstOrderDifferenceFloat(n::time, () -> n.emotion.conceptFirePremises.getValue().longValue())
////                    ).decay(0.9f)
//                .in(new FloatNormalized(
//                        () -> n.emotion.busyVol.getSum()
//                    ).relax(0.99f))
//                .out(2, (onOff)->{
//                    switch(onOff) {
//                        case 0:
//                            a.enabled.set(false); //pause
//                            break;
//                        case 1:
//                            a.enabled.set(true); //un-pause
//                            break;
//                    }
//                })
////                ).out(
////                        new StepController((x) -> n.time.dur(Math.round(x)), 1, n.dur(), n.dur()*2)
////                .out(
////                        StepController.harmonic(n.confMin::set, 0.01f, 0.5f)
////                )//.out(
////                        StepController.harmonic(n.truthResolution::setValue, 0.01f, 0.08f)
////                ).out(
////                        StepController.harmonic(a.curiosity::setValue, 0.01f, 0.16f)
////                ).get(n);
//
//                ;
//            new AgentService(new MutableFloat(1), n, b.get());
//        }


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


        //ConjClustering conjClusterBinput = new ConjClustering(n, BELIEF, (Task::isInput), 8, 32);
        ConjClustering conjClusterBany = new ConjClustering(n, BELIEF, (t->true), 8, 64);

        //ConjClustering conjClusterG = new ConjClustering(n, GOAL, (t -> true), 4, 16);

        ArithmeticIntroduction arith = new ArithmeticIntroduction(4, n);

//        RelationClustering relCluster = new RelationClustering(n,
//                (t)->t.isBelief() && !t.isEternal() && !t.term().isTemporal() ? t.conf() : Float.NaN,
//                8, 32);

        //ConjClustering conjClusterG = new ConjClustering(n, GOAL, (t->true),8, 32);

//        n.runLater(() -> {
////            AudioContext ac = new AudioContext();
////            ac.start();
////            Clock aclock = new Clock(ac, 1000f / (agentFPS * 0.5f));
////            new Metronome(aclock, n);
//            new VocalCommentary(null, a);
//            //ac.out.dependsOn(aclock);
//        });


        ///needs tryContent before its safe
        Inperience inp = new Inperience(n, 12);
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


        //new Anoncepts(8, n);

//        new Implier(2f, a,
//                1
//                //0,1,4
//        );

//
//        window(new MatrixView(p.in, (x, gl) -> {
//            Draw.colorBipolar(gl, x);
//            return 0;
//        }), 100, 100);


        //get ready
        System.gc();



        n.runLater(() -> {

            chart(a);

            SpaceGraph.window(Vis.top(a.nar()), 800, 800);

//            window(new ConceptView(a.happy,n), 800, 600);


            n.on(a);
            //START AGENT
            Loop aLoop = a.runFPS(agentFPS);

//            n.runLater(() -> {
//                new Deriver(a.fire(), Derivers.deriver(6, 8,
//                        "motivation.nal"
//                        //, "goal_analogy.nal"
//                ).apply(n).deriver, n); //{
//            });
        });
        Loop loop = n.startFPS(narFPS);

        return n;
    }

    public static void chart(NAgent a) {
        NAR nar = a.nar();
        nar.runLater(() -> {
            SpaceGraph.window(
                    grid(
                            new AutoSurface(a),

                            Vis.beliefCharts(nar.dur() * 64, a.actions.keySet(), a.nar()),

                            new EmotionPlot(64, a),
                            grid(
                                    //concept query box
                                    new TextEdit() {
                                        @Override
                                        protected void onKeyEnter() {
                                            String s = text();
                                            text("");
                                            try {
                                                nar.conceptualize(s);
                                            } catch (Narsese.NarseseException e) {
                                                e.printStackTrace();
                                            }
                                            Vis.conceptWindow(s, nar);
                                        }
                                    }.surface(),

                                    //new WindowButton("log", () -> Vis.logConsole(nar, 80, 25, new FloatParam(0f))),
                                    new PushButton("dump", () -> {
                                        try {
                                            nar.output(Files.createTempFile(a.toString(), "" + System.currentTimeMillis()).toFile(), false);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }),

                                    new PushButton("clear", () -> {
                                        nar.runLater(NAR::clear);
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

                                    new WindowToggleButton("top", () -> new ConsoleTerminal(new nars.TextUI(nar).session(10f))),

                                    new WindowToggleButton("concept graph", () -> {
                                        DynamicConceptSpace sg;
                                        SpaceGraphPhys3D s = new SpaceGraphPhys3D<>(
                                                sg = new DynamicConceptSpace(nar, () -> nar.exe.active().iterator(),
                                                        128, 16)
                                        );
                                        EdgeDirected fd = new EdgeDirected();
                                        s.dyn.addBroadConstraint(fd);
                                        fd.attraction.set(fd.attraction.get() * 8);

                                        s.add(new SubOrtho(
                                                //window(
                                                grid(new AutoSurface<>(fd), new AutoSurface<>(sg.vis))) {

                                        }.posWindow(0, 0, 1f, 0.2f));

                                        //,  400, 400);
                                        //.pos(0, 0, 0.5f, 0.5f)

                                        s.camPos(0, 0, 90);
                                        return s;
                                    }),


                                    //new WindowButton("prompt", () -> Vis.newInputEditor(), 300, 60)

                                    //Vis.beliefCharts(16, nar, a.reward),
//                                    new WindowToggleButton("agent", () -> (a)),
//                                    col(
//                                            new WindowToggleButton("actionShort", () -> Vis.beliefCharts(a.nar.dur() * 16, a.actions.keySet(), a.nar)),
//                                            new WindowToggleButton("actionMed", () -> Vis.beliefCharts(a.nar.dur() * 64, a.actions.keySet(), a.nar)),
//                                            new WindowToggleButton("actionLong", () -> Vis.beliefCharts(a.nar.dur() * 256, a.actions.keySet(), a.nar))
//                                    ),
                                    //new WindowButton("predict", () -> Vis.beliefCharts(200, a.predictors, a.nar)),
                                    //"agentActions",
                                    //"agentPredict",

                                    a instanceof NAgentX ?
                                            new WindowToggleButton("vision", () -> grid(((NAgentX) a).sensorCam.stream().map(cs -> new AspectAlign(
                                                    new CameraSensorView(cs, a).withControls(), AspectAlign.Align.Center, cs.width, cs.height))
                                                    .toArray(Surface[]::new))
                                            ) : grid()
                            )
                    ),

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
                    900, 600);
        });
    }

//    @Override
//    protected void start(NAR nar) {
//        super.start(nar);
//
////        ActionInfluencingScalar joy = new ActionInfluencingScalar(
////                id != null ?
////                        //$.prop($.the(id), $.the("joy"))
////                        $.inh($.the(id), $.the("joy"))
////                        :
////                        $.the("joy"),
////                new FloatPolarNormalized(new FloatFirstOrderDifference(nar::time,
////                        () -> reward)).relax(0.01f));
//        //dont be too strong because we want to be happy primarily, not to seek increasing joy at some cost of stable happiness (ie. it will allow sadness to get future joy)
////        alwaysWant(joy, nar.confDefault(GOAL)*0.25f);
//
//
//    }


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

    /**
     * pixelTruth defaults to linear monochrome brightness -> frequency
     */
    protected Bitmap2DSensor senseCamera(String id, Container w, int pw, int ph) {
        return senseCamera(id, new SwingBitmap2D(w), pw, ph);
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

    protected Bitmap2DSensor<Scale> senseCamera(String id, Supplier<BufferedImage> w, int pw, int ph) {
        return senseCamera(id, new Scale(w, pw, ph));
    }

    protected Bitmap2DSensor<PixelBag> senseCameraRetina(String id, Container w, int pw, int ph) throws
            Narsese.NarseseException {
        return senseCameraRetina(id, new SwingBitmap2D(w), pw, ph);
    }

//    protected CameraSensor<Scale> senseCamera(String id, Container w, int pw, int ph) throws Narsese.NarseseException {
//        return senseCamera(id, new Scale(new SwingBitmap2D(w), pw, ph));
//    }

    protected Bitmap2DSensor<PixelBag> senseCameraRetina(String id, Supplier<BufferedImage> w, int pw, int ph) throws
            Narsese.NarseseException {
        return senseCameraRetina($(id), w, pw, ph);
    }

//    protected Bitmap2DSensor<PixelBag> senseCameraRetina(String id, Container w, int pw, int ph, FloatToObjectFunction<
//            Truth> pixelTruth) throws Narsese.NarseseException {
//        return senseCameraRetina(id, new SwingBitmap2D(w), pw, ph);
//    }

    protected Bitmap2DSensor<PixelBag> senseCameraRetina(Term id, Supplier<BufferedImage> w, int pw, int ph) {
        PixelBag pb = PixelBag.of(w, pw, ph);
        pb.addActions(id, this);
        return senseCamera(id, pb);
    }

    protected Bitmap2DSensor<WaveletBag> senseCameraFreq(String id, Supplier<BufferedImage> w, int pw, int ph) {
        WaveletBag pb = new WaveletBag(w, pw, ph);
        return senseCamera(id, pb);
    }

    protected <C extends Bitmap2D> Bitmap2DSensor<C> senseCamera(@Nullable String id, C bc) {
        return senseCamera(id != null ? $$(id) : null, bc);
    }

    protected <C extends Bitmap2D> Bitmap2DSensor<C> senseCamera(@Nullable Term id, C bc) {
        return addCamera(new Bitmap2DSensor(id, bc, nar()));
    }

    protected <C extends Bitmap2D> Bitmap2DSensor<C> senseCameraReduced(@Nullable Term
                                                                                id, Supplier<BufferedImage> bc, int sx, int sy, int ox, int oy) {
        return addCamera(new Bitmap2DSensor(id, new AutoencodedBitmap(new BufferedImageBitmap2D(bc), sx, sy, ox, oy), nar()));
    }

    protected <C extends Bitmap2D> Bitmap2DSensor<C> senseCameraReduced(@Nullable Term id, C bc, int sx, int sy,
                                                                        int ox, int oy) {
        return addCamera(new Bitmap2DSensor(id, new AutoencodedBitmap(bc, sx, sy, ox, oy), nar()));
    }

    protected <C extends Bitmap2D> Bitmap2DSensor<C> addCamera(Bitmap2DSensor<C> c) {
        sensorCam.add(c);
        c.readAdaptively();
        return c;
    }

    /**
     * increments/decrements within a finite set of powers-of-two so that harmonics
     * wont interfere as the resolution changes
     * <p>
     * TODO allow powers other than 2, ex: 1.618
     */
    public static class StepController implements IntConsumer, IntObjectPair<StepController> {

        final float[] v;
        private final FloatProcedure update;
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
        public int compareTo(IntObjectPair<StepController> o) {
            throw new UnsupportedOperationException();
        }
    }

//    static Surface mixPlot(NAgent a, MixContRL m, int history) {
//        return Grid.grid(m.dim, i -> col(
//                new MixGainPlot(a, m, history, i),
//                new MixTrafficPlot(a, m, history, i)
//        ));
//    }
//
//    private static class MixGainPlot extends Plot2D {
//        public MixGainPlot(NAgent a, MixContRL m, int history, int i) {
//            super(history, BarWave);
//
//            add(m.id(i), () -> m.gain(i), -1f, +1f);
//            a.onFrame(this::update);
//        }
//    }
//
//    private static class MixTrafficPlot extends Plot2D {
//        public MixTrafficPlot(NAgent a, MixContRL m, int history, int i) {
//            super(history, Line);
//            add(m.id(i) + "_in", () -> m.trafficInput(i), 0f, 1f);
//            add(m.id(i), () -> m.trafficActive(i), 0f, 1f);
//            a.onFrame(this::update);
//        }
//    }

    private static class Metronome {
        public Metronome(Clock cc, NAR n) {
            cc.on(new Auvent<Clock>() {

                public final Envelope kickEnv, snareEnv;
                AudioContext ac = cc.getContext();

                {
                    kickEnv = new Envelope(ac, 0.0f); //gain of kick drum

                    UGen kickGain = new Gain(ac, 1, kickEnv).in(
                            new BiquadFilter(ac, BiquadFilter.BESSEL_LP, 500.0f, 1.0f).in(
                                    new WavePlayer(ac, 100.0f, WaveFactory.SINE)));

                    ac.out.in(kickGain);

                }

                {
                    snareEnv = new Envelope(ac, 0.0f);
                    // set up the snare WavePlayers
                    WavePlayer snareNoise = new WavePlayer(ac, 1.0f, WaveFactory.NOISE);
                    WavePlayer snareTone = new WavePlayer(ac, 200.0f, WaveFactory.SINE);
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
                protected void on(Clock c) {
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

