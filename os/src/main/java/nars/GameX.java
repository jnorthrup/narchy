package nars;

import com.google.common.util.concurrent.AtomicDouble;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.exe.Loop;
import jcog.func.IntIntToObjectFunction;
import jcog.learn.ql.HaiQae;
import jcog.learn.ql.dqn3.DQN3;
import jcog.math.FloatAveragedWindow;
import jcog.signal.wave2d.Bitmap2D;
import jcog.signal.wave2d.MonoBufImgBitmap2D;
import jcog.signal.wave2d.ScaledBitmap2D;
import jcog.util.ArrayUtil;
import nars.agent.Game;
import nars.agent.GameTime;
import nars.agent.MetaAgent;
import nars.agent.util.RLBooster;
import nars.attention.TaskLinkWhat;
import nars.concept.Concept;
import nars.control.MetaGoal;
import nars.control.NARPart;
import nars.control.Why;
import nars.derive.BatchDeriver;
import nars.derive.Derivers;
import nars.derive.rule.PremiseRuleSet;
import nars.derive.timing.ActionTiming;
import nars.exe.impl.WorkerExec;
import nars.gui.NARui;
import nars.memory.CaffeineMemory;
import nars.op.Arithmeticize;
import nars.op.AutoencodedBitmap;
import nars.op.Factorize;
import nars.op.Introduction;
import nars.op.mental.Inperience2;
import nars.op.stm.ConjClustering;
import nars.sensor.Bitmap2DSensor;
import nars.sensor.PixelBag;
import nars.task.util.PriBuffer;
import nars.term.Term;
import nars.term.Termed;
import nars.time.clock.RealTime;
import nars.video.SwingBitmap2D;
import nars.video.WaveletBag;
import org.jetbrains.annotations.Nullable;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space2d.widget.meter.PaintUpdateMatrixView;
import spacegraph.space2d.widget.meter.Plot2D;
import spacegraph.video.OrthoSurfaceGraph;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static nars.$.$$;
import static nars.Op.BELIEF;

/**
 * Extensions to NAgent interface:
 * <p>
 * --chart output (spacegraph)
 * --cameras (Swing and OpenGL)
 */
abstract public class GameX extends Game {

    /**
     * determines memory strength
     */
    static float DURATIONs = 1;

//    static {
//        try {
//            Exe.setProfiler(new Exe.UDPeerProfiler());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    @Deprecated
    public GameX(String id, NAR nar) {
        this(id, GameTime.durs(1), nar);
    }

    public GameX(String id, GameTime gameTime, NAR nar) {
        super(id, gameTime, nar);
    }

    public GameX(Term id, GameTime gameTime, NAR nar) {
        super(id, gameTime, nar);
    }

    public static NAR runRT(Function<NAR, Game> init, float narFPS) {
        return runRT(init, -1, narFPS);
    }


    public static NAR runRT(Function<NAR, Game> init, int threads, float narFPS) {

        NAR n = baseNAR(narFPS / DURATIONs, threads);

        Game g = init.apply(n);

        //n.runLater(() -> {


        //a.resume();
        //System.gc();
        //});


        initPlugins(n);
        initPlugins2(n, g);
        //initMeta(n, g, false);

        //new Gridding(n.parts(Game.class).map(NARui::agent).collect(toList())),
        n.synch();

        n.start(new SpaceGraphPart(() -> NARui.agent(g), 500, 500));
        n.start(new SpaceGraphPart(() -> NARui.attentionUI(n), 600, 600));
        n.start(new SpaceGraphPart(() -> NARui.top(n), 700, 700));


        Loop loop = n.startFPS(narFPS);

        return n;
    }

    /**
     * agent builder should name each agent instance uniquely
     * ex: new PoleCart($.p(Atomic.the(PoleCart.class.getSimpleName()), n.self()), n);
     */
    @Deprecated
    public static NAR runRTNet(Function<NAR, Game> a, int threads, float narFPS, float netFPS) {
        return runRT((n) -> {

            Game aa = a.apply(n);

            new InterNAR(n).fps(netFPS);

            return aa;

        }, threads, narFPS);
    }

//    public static NAR runRL(Function<NAR, Game> init, float narFPS, float clockFPS) {
//        NAR n = baseNAR(clockFPS, 1);
//
//        Game a = init.apply(n);
//        a.curiosity.enable.set(false);
//
//        n.runLater(() -> {
//
//
//
//
////            n.start(a);
//
//
//            SpaceGraph.surfaceWindow(new Gridding(NARui.agent(a), NARui.top(n)), 600, 500);
//
//
//
//
//            new RLBooster(a,
//                    //DQN2::new,
//                    HaiQae::new,
//                    //HaiQ::new,
//                    true
//            );
//        });
//
//        Loop loop = n.startFPS(narFPS);
//
//        return n;
//    }

    private static NAR baseNAR(float durFPS, int _threads) {
    /*
    try {
        Exe.UDPeerProfiler prof = new Exe.UDPeerProfiler();

    } catch (IOException e) {
        e.printStackTrace();
    }
    */


        //Param.STRONG_COMPOSITION = true;

        RealTime clock =
                new RealTime.MS();


        clock.durFPS(durFPS);

        int threads = _threads <= 0 ? Util.concurrencyExcept(1) : _threads;

        double ramGB = Runtime.getRuntime().maxMemory() / (1024 * 1024 * 1024.0);
        return new NARS()

                .what(
                        (w) -> new TaskLinkWhat(w,
                                512,
                                new PriBuffer.BagTaskBuffer(1024, 0.25f))
                )
//                .attention(() -> new ActiveConcepts(1024))
                .exe(
                        //new UniExec()

                        new WorkerExec(
                                threads,
                                false/* affinity */)

                        //new ForkJoinExec()

                        //new ForkJoinExec(Math.max(1, Runtime.getRuntime().availableProcessors() - 1))

//                new SuperExec(
//                    new Valuator.DefaultValuator(0.9f), threads <= 0 ? Util.concurrencyExcept(1) : threads
//                )
                )
//                .exe(MixMultiExec.get(
//                            1024,
//                             Util.concurrency()))
//                .exe(new WorkerMultiExec(
//
//                             new Focus.AERevaluator(new SplitMix64Random(1)),
//                             Util.concurrencyDefault(2),
//                             1024, 1024) {
//
//                         {
//                             //Exe.setExecutor(this);
//                         }
//                     }
//                )


                .time(clock)
                .index(

                        //new RadixTreeMemory(64*1024)
//
                        ramGB >= 0.5 ?
                                new CaffeineMemory(
                                        //8 * 1024
                                        //16*1024
                                        //32*1024
                                        //64 * 1024
                                        //128*1024
                                        Math.round(ramGB * 128 * 1024)
                                )
                                :
                                CaffeineMemory.soft()
//

                //, c -> (int) Math.ceil(c.term().voluplexity()))


                        //new HijackMemory((int)Math.round(ramGB * 128 * 1024), 4)
                )
                .get(GameX::config);
    }

    private static void initPlugins2(NAR n, Game a) {


        PremiseRuleSet rules = Derivers.nal(n, 6, 8,
                "motivation.nal"
                //"nal6.to.nal3.nal"
                //"induction.goal.nal"
                //"nal3.nal",
        );

        List<Concept> rewardConcepts = a.rewards.stream().flatMap(x -> stream(x.spliterator(), false)).map(n::concept).collect(toList());
        List<Termed> sensorConcepts = a.sensors.stream().flatMap(x -> stream(x.components().spliterator(), false)).collect(toList());
        List<Concept> actionConcepts = a.actions.stream().flatMap(x -> stream(x.components().spliterator(), false)).map(n::concept).collect(toList());

        // virtual tasklinks to sensors (sampler)
//        BiFunction<Concept, Derivation, BeliefSource.LinkModel> sensorLinker = ListTermLinker(sensorConcepts);
//        BiFunction<Concept, Derivation, BeliefSource.LinkModel> actionLinker = ListTermLinker(actionConcepts);
//        BiFunction<Concept, Derivation, BeliefSource.LinkModel> rewardLinker = ListTermLinker(rewardConcepts);

//        ZipperDeriver senseReward = BeliefSource.forConcepts(n, rules,
//                actionConcepts,
//                //sensorConcepts,
//                rewardLinker
//                //ConceptTermLinker
//        );
//        senseReward.timing = new ActionTiming(n);

        //Impiler.init(n);
    }

    private static void initMeta(NAR n, Game a, boolean rl) {

        Gridding g = new Gridding();
        MetaAgent meta = new MetaAgent(true, 8f, a);
        g.add(NARui.agent(meta));
        meta.what().pri(0.05f);

        if (rl) {
            meta.what().pri(0.05f);
            RLBooster metaBoost = new RLBooster(meta, (i, o) ->
                    //new HaiQae(i, 12, o).alpha(0.01f).gamma(0.9f).lambda(0.9f),
                    new DQN3(i, o, Map.of(

                    )),
                    2, 3, false);
            meta.curiosity.rate.set(0);
            g.add(NARui.rlbooster(metaBoost));
        }

        n.start(new SpaceGraphPart(() -> g, 500, 500));


//        window(AttentionUI.attentionGraph(n), 600, 600);

        //d.durs(0.25f);

//                if (a instanceof NAgentX) {
//                    NAgent m = metavisor(a);
//                    m.pri.setAt(0.1f);
//                    window(NARui.agent(m), 400, 400);
//                }
        //new AgentControlFeedback(a);

        //new Spider(n, Iterables.concat(Iterables.concat(java.util.List.of(a.id, n.self()), a.actions), a.sensors));

        //new NARSpeak.VocalCommentary(n);

//        AudioContext cc = new AudioContext();
//        Clock c = cc.clock(200f);
//        new Metronome(a.id.target(), c, n);
//        cc.printCallChain();


//        String experiencePath = System.getProperty("java.io.tmpdir") + "/" + a.getClass().getSimpleName() + ".nal";
//        File f = new File(experiencePath);
//        if (f.exists()) {
//            n.runLater(()->{
//                try {
//                    n.inputBinary(f);
//                } catch (IOException e) {
//                    //e.getCause().printStackTrace();
//                    e.printStackTrace();
//                }
//            });
//        }


//        Runtime.getRuntime().addShutdownHook(new Thread(()->{
//            //n.pause();
//            //a.off();
//
//            try {
//                n.outputBinary(new File(experiencePath), false,
//                        (Task t) -> !t.isGoal() ?
//                                Task.eternalized(t,1, c2wSafe(n.confMin.floatValue()), n) : null
//                );
//
//                n.logger.info("eternalized memory saved to: " + experiencePath);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }));
    }


    public static void config(NAR n) {
        n.dtDither.set(
                //1
                10
                //20
                //40
        );

        n.confMin.set(0.01f);
        n.termVolMax.set(36);


        n.beliefPriDefault.amp(0.1f);
        n.goalPriDefault.amp(0.1f);
        n.questionPriDefault.amp(0.04f);
        n.questPriDefault.amp(0.05f);

        n.beliefConfDefault.set(0.75f);
        n.goalConfDefault.set(0.75f);

        n.emotion.want(MetaGoal.Futile, -0.001f);
        n.emotion.want(MetaGoal.Perceive, -0.002f);

        n.emotion.want(MetaGoal.Believe, 0.01f);
        n.emotion.want(MetaGoal.Desire, 0.15f);

        n.emotion.want(MetaGoal.Action, +1f);


//
//        n.attn.forgetting = new Forgetting.AsyncForgetting() {
//            @Override
//            protected Consumer<TaskLink> forgetTasklinks(Concept cc, Bag<Tasklike, TaskLink> tasklinks) {
//                Consumer<TaskLink> c = super.forgetTasklinks(cc, tasklinks);
//
//                if (c==null) return null;
//
//                Term cct = cc.target();
//
//                Consumer<TaskLink> cSpecial = new PriForget<>(  Util.lerp(0.25f, 0, 1-((PriForget)c).mult));
//                return (tl)->{
//                      if (tl.punc()==GOAL || (tl.target().op()==IMPL && tl.target().sub(1).equals(cct)))
//                          cSpecial.accept(tl);
//                      else
//                          c.accept(tl);
//                };
//            }
//        };

        //n.emotion.want(MetaGoal.Answer, 0f);
    }

    private static void initPlugins(NAR n) {

//        Consumer<Why[]> governor = (cc) -> {
////           final Random rng = n.random();
//           for (Why c : cc) {
//               if (c==null) continue;
//               float v = c.amp();
//               c.pri(v + rng.nextFloat()*0.1f);
//           }
//        };


        addGovernor(n);
        Loop.of(()->{
            n.control.printPerf(System.out);
            System.out.println();
        }).setFPS(0.25f);

//        BatchDeriver bd = new BatchDeriver(Derivers.nal(n, 1, 8,
//                "motivation.nal"
//                //"nal6.to.nal1.nal"
//                //"equivalence.nal"
//                //  "induction.goal.nal"
//        ));
//        bd.timing = new ActionTiming(n);
//        bd.tasklinksPerIteration.set(8);


        BatchDeriver bd1 = new BatchDeriver(Derivers.nal(n, 1, 1)
        );
        BatchDeriver bd2_4 = new BatchDeriver(Derivers.nal(n, 2, 4)
        );

        BatchDeriver bd6 = new BatchDeriver(Derivers.nal(n, 6, 8,
                "motivation.nal"));

        BatchDeriver bd6_act = new BatchDeriver(Derivers.nal(n, 6, 8,
                "motivation.nal"));
        bd6_act.timing = new ActionTiming();

//        BatchDeriver bdExtra = new BatchDeriver(Derivers.files(n,
//                "motivation.nal"
//                "nal4.sect.nal",
//                //, "nal6.to.nal3.nal"
//        ));


//        inputInjectionPID(injection, n);

        //bd2.timing = new ActionTiming(n);
//        bd.tasklinksPerIteration.set(8);
        //bd.timing = bd.timing; //default


        //new StatementLinker(n);
        //new PuncNoise(n);
        //n.start(new Eternalizer(n));

//        new STMLinkage(n, 1);

//        ConjClustering conjClusterBinput = new ConjClustering(n, BELIEF,
//                t->t.isInput(),
//                32, 128);


        List<ConjClustering> conjClusters = List.of(
                new ConjClustering(n, BELIEF, 32, 512)
                //, new ConjClustering(n, GOAL, 16, 64)
        );
        conjClusters.forEach(c -> n.start(c));

//        SpaceGraph.window(grid(conjClusters, c -> NARui.clusterView(c, n)), 700, 700);


//        ConjClustering conjClusterBderived = new ConjClustering(n, BELIEF,
//                t->!t.isInput(),
//                4, 16);
//        {
//
//            SpaceGraph.window(
//                    new ConjClusterView(conjClusterBinput),
//                    500, 500);
//
//        }


        Introduction arith = new Arithmeticize.ArithmeticIntroduction(n);

        Introduction factorizer = new Factorize.FactorIntroduction(n);


        new Inperience2(n);

        //new Abbreviation("z", 2, 5, n);


//        try {
//            InterNAR i = new InterNAR(n, 0) {
//                @Override
//                protected void starting(NAR nar) {
//                    super.starting(nar);
//                    runFPS(4);
//                }
//            };
//
//
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }


    }

    /**
     * governor
     * TODO extract to class
     */
    private static void addGovernor(NAR n) {
        int gHist = 4;
        float momentum = 0.75f;
        float explorationRate = 0.1f;
        n.onDur(new Consumer<>() {

            final Consumer<FasterList<Why>> reval = new Consumer<FasterList<Why>>() {

                float[] f = ArrayUtil.EMPTY_FLOAT_ARRAY;

                @Override
                public void accept(FasterList<Why> whys) {
                    int ww = whys.size();
                    if (f.length != ww)
                        f = new float[ww];
                    int i = 0;
                    for (Why w : whys) {
                        float r = w.valueRaw();
                        float v;
                        if (r == r)
                            f[i] = v = Util.lerp(momentum, r, f[i]);
                        else
                            v = f[i];

                        w.setPri(valueToPri(v));
                        i++;
                    }

                }

                float valueToPri(float v) {
                    return v;
                    //return v * v * v;
                }
            };


            @Override
            public void accept(NAR nn) {
                MetaGoal.value(nn, reval);

                int numHow = nn.how.size();
                nn.how.forEach(h -> {

                    float v = h.valueRateNormalized;
                    if (v != v) v = 0;




                    FloatAveragedWindow g = (FloatAveragedWindow) h.governor;
                    if (g == null)
                        h.governor = g = new FloatAveragedWindow(gHist, 1 - momentum, 0).mode(
                                FloatAveragedWindow.Mode.Exponential
                                //FloatAveragedWindow.Mode.Mean
                        );


                    float pp = g.valueOf(v);
                    //float pp = v;
                    h.pri(Util.lerp(pp, explorationRate, 1));
                });
//                nn.how.forEach(h -> System.out.println(n4(h.pri()) + " " + n4(h.valueRateNormalized) + "\t" + h));
//                System.out.println();
            }
        });
    }

    @Override
    protected void starting(NAR nar) {
        super.starting(nar);

//        ((TaskLinkWhat) what()).links.pri(new Predicate<TaskLink>() {
//
//            //behavior overdrive
//            ImmutableSet<Term> stimSet = Sets.immutable.ofAll(Streams.concat(
//                    rewards.stream().flatMap(r -> Streams.stream(r.components())),
//                    actions().stream().flatMap(x -> Streams.stream(x.components())))
//                    .map(Termed::term).collect(toSet()));
//
//            float drive = 0.5f;
//            float happiness;
//
//            {
//                happiness = 0.5f; //initial neutral
//                onFrame((a) -> {
//                    happiness = happiness();
////                     System.out.println(happiness);
//                });
//            }
//
//            @Override
//            public boolean test(TaskLink tl) {
//                float d = (1 - happiness) * drive;
//
//                if (!stimSet.contains(tl.to())) //&& !stimSet.contains(tl.from()))
//                    tl.priMult(1 / (1 + d/2));
////              else
////                    tl.priMult(1/(1 - d/2));
//
//                return true;
//            }
//        });
    }

    //    static void inputInjectionQ(NAR n) {
//        //TODO
//    }

//    /** https://www.controlglobal.com/blogs/controltalkblog/how-to-avoid-a-common-pid-tuning-mistake-tips/ */
//    static void inputInjectionPID(PriBuffer b, NAR n) {
//        //perception injection control
////        MiniPID pid = new MiniPID(0.01, 0.01, 0.002);
////        pid.outLimit(-1, +1);
////        pid.setSetpointRange(+1);
////        pid.f(100);
//
//        //pid.setOutRampRate(0.5);
//
//        FloatRange valve = ((PriBuffer.BagTaskBuffer) b).valve;
//        //DurService.on(n,
////        n.onCycle(
////        ()->{
////            double vol = b.volume();
////            double nextV = pid.out(vol,0.5);
//////                System.out.println(nextV);
////            valve.set(Util.unitize(nextV ));
////        });
//
//        EditGraph2D<Surface> g = EditGraph2D.window(800, 800);
//        g.add(NARui.taskBufferView(b, n)).sizeRel(0.75f,0.25f);
//        //g.add(new PIDChip(pid)).sizeRel(0.2f,0.2f);
//
//        TensorRing history = new TensorRing(3, 8);
//        HaiQae q = new HaiQae(history.volume(), 32,5);
//
//        HaiQChip haiQChip = new HaiQChip(q);
//
//        g.add(LabeledPane.the("Q", haiQChip)).sizeRel(0.2f, 0.2f);
//
////        n.onCycle(()->haiQChip.next(reward));
//        //n.onCycle(
//        //-((2 * Math.abs(v - 0.5f))-0.5f)*2;
//        //nothing
//        //                case 0: valve.set(0); break;
//        //                case 1: valve.set(0.5); break;
//        //                case 2: valve.set(1); break;
//        n.onDur(new Runnable() {
//
//            private float[] sense;
//            float dv = 0.1f;
//
//            @Override
//            public void run() {
//
//                float v = b.load();
//                float reward =
//                        //-((2 * Math.abs(v - 0.5f))-0.5f)*2;
//                        (float) (Math.log(n.feel.busyVol.asFloat())/5f);
//
//                haiQChip.rewardSum.addAndGet(reward);
//                haiQChip.next();
//
//                float x = ((PriBuffer.BagTaskBuffer) b).valve.floatValue();
//                sense = history.set(new float[]{
//                        x, v, 0.5f + 0.5f * Util.tanhFast((float) -Math.log(dv))
//                }).snapshot(sense);
//
//
//                int decision = q.act(reward, sense);
//                float w = x;
//                switch (decision) {
//                    case 0: //nothing
//                        break;
//                    case 1:
//                        w = Math.max(0, x - dv);
//                        break;
//                    case 2:
//                        w = Math.min(1, x + dv);
//                        break;
//                    case 3:
//                        dv = Math.max(0.001f, dv - dv*dv);
//                        break;
//                    case 4:
//                        dv = Math.min(0.2f, dv + dv*dv);
//                        break;
////                case 0: valve.set(0); break;
////                case 1: valve.set(0.5); break;
////                case 2: valve.set(1); break;
//                }
//                valve.set(w);
//            }
//        });
//
//        //Loop.of(() -> {
//
//            //int a = q.act(new float[] { (((float) Math.random()) - 0.5f) * 2, in);
//            //outs.out(a);
////            int n = outs.size();
////            for (int i = 0; i < n; i++) {
////                outs.out(i, (i == a));
////            }
//        //}).setFPS(25);
//
////        SwitchChip outDemultiplexer = new SwitchChip (4);
////        p.addAt(outDemultiplexer).pos(450, 450, 510, 510);
//
//    }

    /**
     * pixelTruth defaults to linear monochrome brightness -> frequency
     */
    protected Bitmap2DSensor senseCamera(String id, java.awt.Container w, int pw, int ph) {
        return senseCamera(id, new SwingBitmap2D(w), pw, ph);
    }


    private Bitmap2DSensor<ScaledBitmap2D> senseCamera(String id, Supplier<BufferedImage> w, int pw, int ph) {
        return senseCamera(id, new ScaledBitmap2D(w, pw, ph));
    }

    protected Bitmap2DSensor<PixelBag> senseCameraRetina(String id, Component w, int pw, int ph) {
        return senseCameraRetina(id, new SwingBitmap2D(w), pw, ph);
    }


    private Bitmap2DSensor<PixelBag> senseCameraRetina(String id, Supplier<BufferedImage> w, int pw, int ph) {
        return senseCameraRetina($$(id), w, pw, ph);
    }


    private Bitmap2DSensor<PixelBag> senseCameraRetina(Term id, Supplier<BufferedImage> w, int pw, int ph) {
        return senseCamera(id, new PixelBag(new MonoBufImgBitmap2D(w), pw, ph));
    }

    protected Bitmap2DSensor<PixelBag> senseCameraRetina(Term id, Bitmap2D w, int pw, int ph) {
        return senseCamera(id, new PixelBag(w, pw, ph));
    }

    protected Bitmap2DSensor<WaveletBag> senseCameraFreq(String id, Supplier<BufferedImage> w, int pw, int ph) {
        return senseCamera(id, new WaveletBag(w, pw, ph));
    }

    protected <C extends Bitmap2D> Bitmap2DSensor<C> senseCamera(@Nullable String id, C bc) {
        return senseCamera((Term) (id != null ? $$(id) : null), bc);
    }

    protected <C extends Bitmap2D> Bitmap2DSensor<C> senseCamera(@Nullable Term id, C bc) {
        Bitmap2DSensor c = new Bitmap2DSensor(id, bc, nar());
        addSensor(c);
        return c;
    }

    protected <C extends Bitmap2D> Bitmap2DSensor<C> senseCamera(@Nullable IntIntToObjectFunction<nars.term.Term> id, C bc) {
        Bitmap2DSensor c = new Bitmap2DSensor(id, bc, nar());
        addSensor(c);
        return c;
    }
    protected <C extends Bitmap2D> Bitmap2DSensor<C> senseCamera(@Nullable IntIntToObjectFunction<nars.term.Term> id, C bc, float defaultFreq) {
        Bitmap2DSensor c = new Bitmap2DSensor(id, bc, defaultFreq, nar());
        addSensor(c);
        return c;
    }

    protected <C extends Bitmap2D> Bitmap2DSensor<C> addCameraCoded(@Nullable Term
                                                                            id, Supplier<BufferedImage> bc, int sx, int sy, int ox, int oy) {
        Bitmap2DSensor c = new Bitmap2DSensor(id, new AutoencodedBitmap(new MonoBufImgBitmap2D(bc), sx, sy, ox, oy), nar());
        addSensor(c);
        return c;
    }

    protected <C extends Bitmap2D> Bitmap2DSensor<C> addCameraCoded(@Nullable Term id, C bc, int sx, int sy,
                                                                    int ox, int oy) {
        Bitmap2DSensor c = new Bitmap2DSensor(id, new AutoencodedBitmap(bc, sx, sy, ox, oy), nar());
        addSensor(c);
        return c;
    }


    public static class HaiQChip extends Gridding {
        private final HaiQae q;
        private Plot2D plot;
        private AtomicDouble rewardSum;

        public HaiQChip(HaiQae q) {
            super();
            this.q = q;


        }

        @Override
        protected void starting() {
            super.starting();
            float[] in = new float[q.ae.inputs()];
            Gridding inner = new Gridding(
                    new ObjectSurface(q),
                    new Gridding(VERTICAL,
                            new PaintUpdateMatrixView(in),
                            new PaintUpdateMatrixView(q.ae.x),
                            new PaintUpdateMatrixView(q.ae.W),
                            new PaintUpdateMatrixView(q.ae.y)
                    ),
                    new Gridding(VERTICAL,
                            new PaintUpdateMatrixView(q.q),
                            new PaintUpdateMatrixView(q.et)
                    ),
                    plot = new Plot2D(1024, Plot2D.Line)
            );
//        hw.add(LabeledPane.the("input", new TypedPort<>(float[].class, (i) -> {
//            System.arraycopy(i, 0, in, 0, i.length);
//        })));
            //hw.add(LabeledPane.the("act", new IntPort(q.actions)));


            rewardSum = new AtomicDouble();
//            plot.add("Reward", ()->{
//                return rewardSum.getAndSet(0); //clear
//            });

            set(inner);
        }

        public Plot2D getPlot() {
            return plot;
        }

        public AtomicDouble getRewardSum() {
            return rewardSum;
        }

        public void next() {

            plot.commit();
        }
    }


    protected static class SpaceGraphPart extends NARPart {
        private final Supplier<Surface> surface;
        private final int w, h;
        private OrthoSurfaceGraph win;

        SpaceGraphPart(Supplier<Surface> surface, int w, int h) {
            this.w = w;
            this.h = h;
            this.surface = surface;
        }

        @Override
        protected void starting(NAR nar) {
            win = SpaceGraph.window(surface.get(), w, h);
        }

        @Override
        protected void stopping(NAR nar) {
            win.delete();
            win = null;
        }
    }
}

