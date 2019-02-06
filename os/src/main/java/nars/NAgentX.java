package nars;

import jcog.Util;
import jcog.exe.Loop;
import jcog.learn.ql.HaiQae;
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
import nars.gui.BagSpectrogram;
import nars.gui.NARui;
import nars.index.concept.AbstractConceptIndex;
import nars.index.concept.CaffeineIndex;
import nars.op.Arithmeticize;
import nars.op.AutoencodedBitmap;
import nars.op.Introduction;
import nars.op.mental.Inperience;
import nars.op.stm.ConjClustering;
import nars.op.stm.STMLinkage;
import nars.sensor.Bitmap2DSensor;
import nars.sensor.PixelBag;
import nars.term.Term;
import nars.term.Termed;
import nars.time.clock.RealTime;
import nars.video.SwingBitmap2D;
import nars.video.WaveletBag;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.container.grid.Gridding;
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
//        Param.ETERNALIZE_BELIEF_PROJECTED_IN_DERIVATION = true;


        RealTime clock =
                new RealTime.MS();


        clock.durFPS(durFPS);


        NAR n = new NARS()

//                .attention(() -> new ActiveConcepts(1024))

                //.exe(new UniExec() {
                .exe(new MultiExec.WorkerExec(
                        new Valuator.DefaultValuator(0.85f),
                        //new Valuator.AERevaluator(new XoRoShiRo128PlusRandom()),

                        threads <= 0 ? Util.concurrencyExcept(1) : threads, false/* affinity */))

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
//                                128 * 1024
//                                //96 * 1024
                                64 * 1024
                                //32 * 1024
////                                //16 * 1024
                                , c -> 1) //, c -> (int) Math.ceil(c.voluplexity()))

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

//        ZipperDeriver senseActions = BeliefSource.forConcepts(n, rules,
//                sensorConcepts,
//                actionLinker
//        );
//        senseActions.timing = new ActionTiming(n);


//        ZipperDeriver motorInference = BeliefSource.forConcepts(n, Derivers.files(n,
//                "nal6.nal", "motivation.nal"),
//                a.actions.stream().collect(Collectors.toList())
//        );
//        motorInference.timing = new ActionTiming(n);


        //sd.timing = new ActionTiming(n);
////                MatrixDeriver motivation = new MatrixDeriver(a.sampleActions(),
////                        rules) {
//////                    @Override
//////                    public float puncFactor(byte conclusion) {
//////                        return conclusion == GOAL ? 1 : 0.5f;
//////                    }
////                };


        MetaAgent meta = new MetaAgent(a);

        //window(AttentionUI.attentionGraph(n, a), 600, 600);

        window(new Gridding(NARui.agent(a), NARui.top(n)), 600, 500);

        window(new BagSpectrogram<>(((AbstractConceptIndex)n.concepts).active, 128, n)
                .color(x -> {
                    float h;
                    switch (x.puncMax()) {
                        case BELIEF: h = 0; break;
                        case QUESTION: h = 0.25f; break;
                        case GOAL: h = 0.5f; break;
                        case QUEST: h = 0.75f; break;
                        default:
                            return Draw.rgbInt(0.5f, 0.5f, 0.5f);
                    }

                    return Draw.colorHSB(h, 0.75f, 0.25f + 0.75f * x.priElseZero());

                }), 500, 500);


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
        n.timeResolution.set(
                //10
                20
                //40
        );

        n.confMin.set(0.01f);
        //n.freqResolution.setAt(0.03f);
        n.termVolumeMax.set(28);

        ((AbstractConceptIndex)n.concepts).activeCapacity.set(512);
        ((AbstractConceptIndex)n.concepts).activationRate.set(1); //HACK TODO based on active bag capacity

        float p =
                //1;
                0.01f;
        
        n.beliefPriDefault.set(0.25f * p);
        n.goalPriDefault.set(8f * p);
        n.questionPriDefault.set(0.05f * p);
        n.questPriDefault.set(0.05f * p);

        n.beliefConfDefault.set(0.9f);
        n.goalConfDefault.set(0.9f);


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

        n.emotion.want(MetaGoal.PerceiveCmplx, 0f); //-0.01f); //<- dont set negative unless sure there is some positive otherwise nothing happens

        //n.emotion.want(MetaGoal.Believe, 0.01f);
        n.emotion.want(MetaGoal.Desire, 0.1f);

        n.emotion.want(MetaGoal.Action, +1f);

        //n.emotion.want(MetaGoal.Answer, 0f);
    }

    public static void initPlugins(NAR n) {


        BatchDeriver bd = new BatchDeriver(Derivers.nal(n, 1, 8,
                //"nal6.to.nal1.nal"
                "motivation.nal"
                //"equivalence.nal"
                //  "induction.goal.nal"
        ));
        bd.timing = new ActionTiming(n);


        BatchDeriver bd2 = new BatchDeriver(Derivers.nal(n, 1, 8,
                "relation_introduction.nal"
        ));


        new STMLinkage(n, 1);

        ConjClustering conjClusterBinput = new ConjClustering(n, BELIEF,
                Task::isInput,
                //t->true,
                32, 256);
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


        new Inperience.Believe(16, n);
        new Inperience.Want(16, n);
//        new Inperience.Wonder(8, n);
//        new Inperience.Plan(8, n);



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

        //new Abbreviation("z", 5, 9, n);

//        Impiler.ImpilerTracker t = new Impiler.ImpilerTracker(8, 16, n);
//        Impiler.ImpilerDeduction d = new Impiler.ImpilerDeduction(8, 8, n);


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

