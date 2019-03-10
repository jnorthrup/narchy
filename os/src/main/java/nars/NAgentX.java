package nars;

import com.google.common.util.concurrent.AtomicDouble;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.exe.Loop;
import jcog.learn.ql.HaiQae;
import jcog.math.FloatRange;
import jcog.pri.bag.Bag;
import jcog.signal.tensor.ArrayTensor;
import jcog.signal.tensor.RingBufferTensor;
import jcog.signal.wave2d.Bitmap2D;
import jcog.signal.wave2d.MonoBufImgBitmap2D;
import jcog.signal.wave2d.ScaledBitmap2D;
import jcog.util.Int2Function;
import nars.agent.FrameTrigger;
import nars.agent.MetaAgent;
import nars.agent.NAgent;
import nars.agent.util.RLBooster;
import nars.concept.Concept;
import nars.control.MetaGoal;
import nars.derive.Derivers;
import nars.derive.impl.BatchDeriver;
import nars.derive.premise.PremiseDeriverRuleSet;
import nars.derive.timing.ActionTiming;
import nars.exe.MultiExec;
import nars.exe.Valuator;
import nars.gui.DurSurface;
import nars.gui.NARui;
import nars.index.concept.CaffeineIndex;
import nars.link.TaskLink;
import nars.op.Arithmeticize;
import nars.op.AutoencodedBitmap;
import nars.op.Introduction;
import nars.op.stm.ConjClustering;
import nars.sensor.Bitmap2DSensor;
import nars.sensor.PixelBag;
import nars.task.util.TaskBuffer;
import nars.term.Term;
import nars.term.Termed;
import nars.time.clock.RealTime;
import nars.video.SwingBitmap2D;
import nars.video.WaveletBag;
import org.eclipse.collections.api.block.function.primitive.IntToIntFunction;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space2d.widget.meter.PaintUpdateMatrixView;
import spacegraph.space2d.widget.meter.Plot2D;
import spacegraph.space2d.widget.meter.Spectrogram;
import spacegraph.space2d.widget.text.LabeledPane;
import spacegraph.space2d.widget.windo.GraphEdit;
import spacegraph.video.Draw;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static nars.$.$$;
import static nars.Op.*;
import static spacegraph.SpaceGraph.window;
import static spacegraph.space2d.container.grid.Gridding.VERTICAL;
import static spacegraph.space2d.container.grid.Gridding.grid;

/**
 * Extensions to NAgent interface:
 * <p>
 * --chart output (spacegraph)
 * --cameras (Swing and OpenGL)
 */
abstract public class NAgentX extends NAgent {

//    static {
//        try {
//            Exe.setProfiler(new Exe.UDPeerProfiler());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    @Deprecated
    public NAgentX(String id, NAR nar) {
        this(id, FrameTrigger.durs(1), nar);
    }

    @Deprecated
    public NAgentX(Term id, NAR nar) {
        this(id, FrameTrigger.durs(1), nar);
    }


    public NAgentX(String id, FrameTrigger frameTrigger, NAR nar) {
        super(id, frameTrigger, nar);
    }

    public NAgentX(Term id, FrameTrigger frameTrigger, NAR nar) {
        super(id, frameTrigger, nar);
    }

    public static NAR runRT(Function<NAR, NAgent> init, float clockFPS) {
        return runRT(init, -1, clockFPS*2, clockFPS);
    }

    public static NAR runRT(Function<NAR, NAgent> init, int threads, float narFPS, float durFPS) {
        NAR n = baseNAR(durFPS, threads);


        n.runLater(() -> {

            NAgent a = init.apply(n);

            n.on(a);

            initPlugins(n);
            initPlugins2(n, a);

            Loop loop = n.startFPS(narFPS);

            //System.gc();
        });

        n.synch();

        return n;
    }

    /**
     * agent builder should name each agent instance uniquely
     * ex: new PoleCart($.p(Atomic.the(PoleCart.class.getSimpleName()), n.self()), n);
     */
    public static NAR runRTNet(Function<NAR, NAgent> a, int threads, float narFPS, float durFPS, float netFPS) {
        return runRT((n) -> {

            NAgent aa = a.apply(n);

            new InterNAR(n).runFPS(netFPS);

            return aa;

        }, threads, narFPS, durFPS);
    }

    public static NAR runRL(Function<NAR, NAgent> init, float narFPS, float clockFPS) {
        NAR n = baseNAR(clockFPS, 1);


        n.runLater(() -> {

            NAgent a = init.apply(n);

            a.curiosity.enable.set(false);

            n.on(a);


            window(new Gridding(NARui.agent(a), NARui.top(n)), 600, 500);




            new RLBooster(a,
                    //DQN2::new,
                    HaiQae::new,
                    //HaiQ::new,
                    true
            );
        });

        Loop loop = n.startFPS(narFPS);

        return n;
    }

    static NAR baseNAR(float durFPS, int threads) {
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


        NAR n = new NARS()

//                .attention(() -> new ActiveConcepts(1024))

                //.exe(new UniExec() {
                .exe(new MultiExec.WorkerExec(
                        new Valuator.DefaultValuator(0.9f),
                        //new Valuator.AEValuator(new XoRoShiRo128PlusRandom()),

                        threads <= 0 ? Util.concurrencyExcept(1) : threads,

                        false/* affinity */))

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

                        //CaffeineIndex.soft()

                        new CaffeineIndex(
                            64 * 1024
                    //96 * 1024
//                                64 * 1024
////                                //32 * 1024
////////                                //16 * 1024
                                 //, c -> (int) Math.ceil(c.term().voluplexity()))
                        )

//                        new HijackConceptIndex(
//
//                                //192 * 1024,
//                                128 * 1024,
//                                //64 * 1024,
//                                //32 * 1024,
//                                //16 * 1024,
//                                //8 * 1024,
//                                4)


                )
                .get();


        config(n);
        return n;
    }

    static void initPlugins2(NAR n, NAgent a) {


        PremiseDeriverRuleSet rules = Derivers.nal(n, 6, 8,
                "motivation.nal"
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


        MetaAgent meta = new MetaAgent(a, false);

        window(AttentionUI.attentionGraph(n, a), 600, 600);

        window(new Gridding(NARui.agent(a), NARui.top(n)), 800, 500);

        final Bag<?,TaskLink> active = n.attn.active;
        int c = active.capacity();
        int history = 64;
        int width = c;
        Spectrogram s = new Spectrogram(false, history, width);

        DurSurface d = DurSurface.get(s, n, new Runnable() {

            final FasterList<TaskLink> snapshot = new FasterList();

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

        window(d, 500, 500);

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
                10
                //20
                //40
        );

        n.confMin.set(0.01f);
        n.termVolumeMax.set(28);


        n.attn.activeCapacity.set(1024);


        n.beliefPriDefault.set(0.1f);
        n.goalPriDefault.set(0.25f);
        n.questionPriDefault.set(0.05f);
        n.questPriDefault.set(0.05f);

        n.beliefConfDefault.set(0.75f);
        n.goalConfDefault.set(0.75f);

        //n.emotion.want(MetaGoal.PerceiveCmplx, -0.01f); //<- dont set negative unless sure there is some positive otherwise nothing happens

        n.emotion.want(MetaGoal.Believe, 0.01f);
        n.emotion.want(MetaGoal.Desire, 0.2f);

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

    public static void initPlugins(NAR n) {


//        BatchDeriver bd = new BatchDeriver(Derivers.nal(n, 1, 8,
//                "motivation.nal"
//                //"nal6.to.nal1.nal"
//                //"equivalence.nal"
//                //  "induction.goal.nal"
//        ));
//        bd.timing = new ActionTiming(n);
//        bd.tasklinksPerIteration.set(8);


        TaskBuffer.BagTaskBuffer injection = new TaskBuffer.BagTaskBuffer(512, 0.5f);

        BatchDeriver bd1 = new BatchDeriver(Derivers.nal(n, 6, 6,
                "motivation.nal"), injection);
        bd1.timing = new ActionTiming(n);

        BatchDeriver bdLow = new BatchDeriver(Derivers.nal(n,
                1, 4,
                "nal4.sect.nal"),
                injection);

        BatchDeriver bdHigh = new BatchDeriver(Derivers.nal(n,
                6, 8,
                "relation_introduction.nal", "motivation.nal"),
                injection);

//        inputInjectionPID(injection, n);

        //bd2.timing = new ActionTiming(n);
//        bd.tasklinksPerIteration.set(8);
        //bd.timing = bd.timing; //default



        //new StatementLinker(n);
        //new PuncNoise(n);

//        new STMLinkage(n, 1);

//        ConjClustering conjClusterBinput = new ConjClustering(n, BELIEF,
//                t->t.isInput(),
//                32, 128);


        List<ConjClustering> conjClusters = List.of(
            new ConjClustering(n, BELIEF, 32, 256)
            //new ConjClustering(n, GOAL, 4, 16)
        );

        window(grid(conjClusters, c->NARui.clusterView(c, n)), 700, 700);



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

        //ConjClustering conjClusterBany = new ConjClustering(n, BELIEF, (t -> true), 2, 32);

//        ConjClustering conjClusterGany = new ConjClustering(n, GOAL, (t -> !(t instanceof CuriosityTask) ),
//                8, 96);

        Introduction arith = new Arithmeticize.ArithmeticIntroduction(n,64);
        //Introduction factorizer = new Factorize.FactorIntroduction( n, 16);


//        new Inperience.Believe(8, n);
//        new Inperience.Want(8, n);
//        new Inperience.Wonder(8, n);
//        new Inperience.Plan(8, n);

        //new Abbreviation("z", 5, 9, n);



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


//        Impiler.ImpilerTracker t = new Impiler.ImpilerTracker(8, 16, n);
//        Impiler.ImpilerDeduction d = new Impiler.ImpilerDeduction(8, 8, n);


    }

//    static void inputInjectionQ(NAR n) {
//        //TODO
//    }
    /** https://www.controlglobal.com/blogs/controltalkblog/how-to-avoid-a-common-pid-tuning-mistake-tips/ */
    static void inputInjectionPID(TaskBuffer b, NAR n) {
        //perception injection control
//        MiniPID pid = new MiniPID(0.01, 0.01, 0.002);
//        pid.outLimit(-1, +1);
//        pid.setSetpointRange(+1);
//        pid.f(100);

        //pid.setOutRampRate(0.5);

        FloatRange valve = ((TaskBuffer.BagTaskBuffer) b).valve;
        //DurService.on(n,
//        n.onCycle(
//        ()->{
//            double vol = b.volume();
//            double nextV = pid.out(vol,0.5);
////                System.out.println(nextV);
//            valve.set(Util.unitize(nextV ));
//        });

        GraphEdit<Surface> g = GraphEdit.window(800, 800);
        g.add(NARui.taskBufferView(b, n)).sizeRel(0.75f,0.25f);
        //g.add(new PIDChip(pid)).sizeRel(0.2f,0.2f);

        RingBufferTensor history = new RingBufferTensor(3, 8);
        HaiQae q = new HaiQae(history.volume(), 32,5);
        float[] in = new float[q.ae.inputs()];
        Plot2D plot;
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
                plot = new Plot2D(100, Plot2D.Line)
        );
//        hw.add(LabeledPane.the("input", new TypedPort<>(float[].class, (i) -> {
//            System.arraycopy(i, 0, in, 0, i.length);
//        })));
        //hw.add(LabeledPane.the("act", new IntPort(q.actions)));



        g.add(LabeledPane.the("Q", inner)).sizeRel(0.2f, 0.2f);


        AtomicDouble rewardSum = new AtomicDouble();
        plot.add("Reward", ()->{
            return rewardSum.getAndSet(0); //clear
        });


        n.onCycle(
        //DurService.on(n,
                new Runnable() {

            private float[] sense;
            float dv = 0.1f;

            @Override
            public void run() {

                float v = b.volume();
                float reward =
                        //-((2 * Math.abs(v - 0.5f))-0.5f)*2;
                        (float) (Math.log(n.emotion.busyVol.floatValue())/5f);
                rewardSum.addAndGet(reward);
                plot.commit();

                float x = ((TaskBuffer.BagTaskBuffer) b).valve.floatValue();
                sense = history.commit(new ArrayTensor(new float[]{
                        x, v, 0.5f + 0.5f * Util.tanhFast((float) -Math.log(dv))
                })).snapshot(sense);


                int decision = q.act(reward, sense);
                float w = x;
                switch (decision) {
                    case 0: //nothing
                        break;
                    case 1:
                        w = Math.max(0, x - dv);
                        break;
                    case 2:
                        w = Math.min(1, x + dv);
                        break;
                    case 3:
                        dv = Math.max(0.001f, dv - dv*dv);
                        break;
                    case 4:
                        dv = Math.min(0.2f, dv + dv*dv);
                        break;
//                case 0: valve.set(0); break;
//                case 1: valve.set(0.5); break;
//                case 2: valve.set(1); break;
                }
                valve.set(w);
            }
        });

        //Loop.of(() -> {

            //int a = q.act(new float[] { (((float) Math.random()) - 0.5f) * 2, in);
            //outs.out(a);
//            int n = outs.size();
//            for (int i = 0; i < n; i++) {
//                outs.out(i, (i == a));
//            }
        //}).setFPS(25);

//        SwitchChip outDemultiplexer = new SwitchChip (4);
//        p.addAt(outDemultiplexer).pos(450, 450, 510, 510);

    }


    /**
     * pixelTruth defaults to linear monochrome brightness -> frequency
     */
    protected Bitmap2DSensor senseCamera(String id, java.awt.Container w, int pw, int ph) {
        return senseCamera(id, new SwingBitmap2D(w), pw, ph);
    }


    protected Bitmap2DSensor<ScaledBitmap2D> senseCamera(String id, Supplier<BufferedImage> w, int pw, int ph) {
        return senseCamera(id, new ScaledBitmap2D(w, pw, ph));
    }

    protected Bitmap2DSensor<PixelBag> senseCameraRetina(String id, Component w, int pw, int ph) {
        return senseCameraRetina(id, new SwingBitmap2D(w), pw, ph);
    }


    protected Bitmap2DSensor<PixelBag> senseCameraRetina(String id, Supplier<BufferedImage> w, int pw, int ph) {
        return senseCameraRetina($$(id), w, pw, ph);
    }


    protected Bitmap2DSensor<PixelBag> senseCameraRetina(Term id, Supplier<BufferedImage> w, int pw, int ph) {
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

    protected <C extends Bitmap2D> Bitmap2DSensor<C> senseCamera(@Nullable Int2Function<Term> id, C bc) {
        Bitmap2DSensor c = new Bitmap2DSensor(id, bc, nar());
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


}

