package nars;

import com.google.common.util.concurrent.AtomicDouble;
import jcog.Util;
import jcog.exe.Exe;
import jcog.exe.Loop;
import jcog.func.IntIntToObjectFunction;
import jcog.learn.pid.MiniPID;
import jcog.learn.ql.HaiQae;
import jcog.math.FloatRange;
import jcog.signal.wave2d.Bitmap2D;
import jcog.signal.wave2d.MonoBufImgBitmap2D;
import jcog.signal.wave2d.ScaledBitmap2D;
import nars.attention.TaskLinkWhat;
import nars.attention.What;
import nars.control.MetaGoal;
import nars.control.NARPart;
import nars.control.Should;
import nars.derive.Deriver;
import nars.derive.Derivers;
import nars.derive.time.ActionTiming;
import nars.derive.time.MixedTimeFocus;
import nars.derive.time.NonEternalTaskOccurenceOrPresentDeriverTiming;
import nars.exe.impl.WorkerExec;
import nars.game.Game;
import nars.game.GameTime;
import nars.game.SimpleReward;
import nars.game.meta.GameMetaAgent;
import nars.game.meta.MetaAgent;
import nars.game.meta.SelfMetaAgent;
import nars.gui.BeliefTableChart;
import nars.gui.NARui;
import nars.memory.CaffeineMemory;
import nars.op.Arithmeticize;
import nars.op.AutoencodedBitmap;
import nars.op.Factorize;
import nars.op.mental.Inperience;
import nars.op.stm.ConjClustering;
import nars.op.stm.STMLinker;
import nars.sensor.Bitmap2DSensor;
import nars.sensor.PixelBag;
import nars.table.dynamic.SeriesBeliefTable;
import nars.task.util.PriBuffer;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.time.clock.RealTime;
import nars.video.SwingBitmap2D;
import nars.video.WaveletBag;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space2d.widget.meter.PaintUpdateMatrixView;
import spacegraph.space2d.widget.meter.Plot2D;
import spacegraph.video.OrthoSurfaceGraph;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;
import static nars.$.$$;
import static nars.Op.BELIEF;
import static spacegraph.SpaceGraph.window;

/**
 * Extensions to NAgent interface:
 * <p>
 * --chart output (spacegraph)
 * --cameras (Swing and OpenGL)
 */
public abstract class GameX extends Game {

    static final boolean initMeta = true;
    static final boolean initMetaRL = false;
    static final boolean metaAllowPause = false;
    static final Atom FRAME = Atomic.atom("frame");
    /**
     * determines memory strength
     */
    static float durationsPerFrame = 1f;

    @Deprecated
    public GameX(String id) {
        this($$(id), GameTime.durs(1), null);
    }

    public GameX(Term id, GameTime gameTime, NAR n) {
        super(id, gameTime, n);
    }

    @Deprecated
    public static NAR runRT(float narFPS, Function<NAR, Game> init) {
        return runRT(-1, narFPS, init);
    }

    public static NAR runRT(float narFPS, Consumer<NAR> init) {
        return runRT(-1, narFPS, init);
    }

    @Deprecated
    public static NAR runRT(int threads, float narFPS, Function<NAR, Game> init) {
        return runRT(threads, narFPS, (Consumer<NAR>) init::apply);
    }

    public static NAR runRT(int threads, float narFPS, Consumer<NAR> init) {

        NAR n = baseNAR(narFPS * durationsPerFrame, threads);

        init.accept(n);

        initPlugins(n);

        if (initMeta) {

            float metaFPS = narFPS / 2;

            n.parts(Game.class).filter(
                    g -> !(g instanceof MetaAgent)
            ).forEach(g -> {
                float fps = metaFPS;
                MetaAgent gm = new GameMetaAgent(g, metaAllowPause);
                n.add(gm);

            });

            SelfMetaAgent self = new SelfMetaAgent(n);
            if (initMetaRL)
                self.addRLBoost();

            final FloatRange enc = new FloatRange(0.5f, 0, 1);
            SimpleReward e = self.reward($.inh(n.self, "encouragement"), () -> {
                float v = enc.floatValue();
                enc.set(Util.lerp(0.01f, v, 0.5f)); //fade to 0.5
                return v;
            });
            n.add(self);

            window(new Gridding(new BeliefTableChart(e.concept, n), new PushButton(":)", ()->enc.set(1)), new PushButton(":(", ()->enc.set(0))), 800, 800);

            self.pri(0.1f);

        }

        Loop loop = n.startFPS(narFPS);


        n.runLater(() -> {
            n.synch();
            window(NARui.top(n), 1024, 800);
            window(new Gridding(n.parts(Game.class).map(NARui::game).collect(toList())), 1024, 768);
        });


        return n;
    }

    /**
     * agent builder should name each agent instance uniquely
     * ex: new PoleCart($.p(Atomic.the(PoleCart.class.getSimpleName()), n.self()), n);
     */
    @Deprecated
    public static NAR runRTNet(int threads, float narFPS, float netFPS, Function<NAR, Game> a) {
        return runRT(threads, narFPS, (n) -> {
            Game aa = a.apply(n);
            return aa;
        });
    }

    public static NAR baseNAR(float durFPS, int _threads) {

        RealTime clock =
                new RealTime.MS();

        clock.durFPS(durFPS);

        int threads = _threads <= 0 ? Util.concurrencyExcept(1) : _threads;

        double ramGB = Runtime.getRuntime().maxMemory() / (1024 * 1024 * 1024.0);
        return new NARS()                  .what(
                        w -> new TaskLinkWhat(w,
                                512,
                                new PriBuffer.DirectTaskBuffer()
                        )
                ).exe(


                        new WorkerExec(
                                threads,
                                false/* affinity */)


                )


                .time(clock)
                .index(


                        ramGB >= 0.5 ?
                                new CaffeineMemory(


                                        Math.round(ramGB * 96 * 1024)
                                )
                                :
                                CaffeineMemory.soft()


                )
                .get(GameX::config);
    }

    private static void initPlugins2(NAR n, Game a) {


    }

    public static void config(NAR n) {
        n.main.pri(0);
        n.what.remove(n.main.id);

        n.dtDither.set(

                10


        );

        n.causeCapacity.set(16);
        n.termVolMax.set(
                64
        );


        n.beliefPriDefault.pri(0.5f);
        n.goalPriDefault.pri(0.5f);
        n.questionPriDefault.pri(0.5f);
        n.questPriDefault.pri(0.5f);

        n.beliefConfDefault.set(0.75f);
        n.goalConfDefault.set(0.75f);


        n.emotion.want(MetaGoal.Perceive, 0 /*-0.005f*/);
        n.emotion.want(MetaGoal.Believe, 0.01f);
        n.emotion.want(MetaGoal.Desire, 0.05f);


    }

    private static void initPlugins(NAR n) {


        n.exe.governor = Should.predictMLP;

        Loop.of(() -> {

            n.stats(false, true, System.out);

            System.out.println();
        }).setFPS(0.25f);


        ConjClustering cc;

        Deriver bd6_act = new Deriver(
                Derivers.nal(n, 1, 8, "motivation.nal")
                        .add(new STMLinker(1))
                        .add(new Arithmeticize.ArithmeticIntroduction())
                        .add(new Factorize.FactorIntroduction())
                        .add(new Inperience())
                        .add(cc = new ConjClustering(n, BELIEF, BELIEF, 6, 32, t -> true))


                ,
                new MixedTimeFocus(
                        new NonEternalTaskOccurenceOrPresentDeriverTiming(),

                        new ActionTiming()
                )
        );


    }

    private static void addClock(NAR n) {
        n.parts(Game.class).forEach(g -> g.onFrame(() -> {
            long now = n.time();
            int X = g.iterationCount();
            int radix = 16;
            Term x = $.pRecurse(false, $.radixArray(X, radix, Integer.MAX_VALUE));

            Term f = $.funcImg(FRAME, g.id, x);
            Task t = new SeriesBeliefTable.SeriesTask(f, BELIEF, $.t(1f, n.confDefault(BELIEF)),
                    now, Math.round(now + g.durLoop()),
                    new long[]{n.time.nextStamp()});
            t.pri(n.priDefault(BELIEF) * g.what().pri());

            g.what().accept(t);
        }));
    }

    private static void addFuelInjection(NAR n) {
        n.parts(What.class).filter(w -> w.inBuffer instanceof PriBuffer.BagTaskBuffer).map(w -> (PriBuffer.BagTaskBuffer) w.inBuffer).forEach(b -> {
            MiniPID pid = new MiniPID(0.007f, 0.005, 0.0025, 0);
            pid.outLimit(0, 1);
            pid.setOutMomentum(0.1);
            float ideal = 0.5f;
            n.onDur(() -> b.valve.set(pid.out(ideal - b.load(), 0)));
        });

    }

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
        Bitmap2DSensor c = new Bitmap2DSensor(nar(), bc, id);
        addSensor(c);
        return c;
    }

    protected <C extends Bitmap2D> Bitmap2DSensor<C> senseCamera(@Nullable IntIntToObjectFunction<nars.term.Term> id, C bc, float defaultFreq) {
        Bitmap2DSensor c = new Bitmap2DSensor(nar(), defaultFreq, bc, id);
        addSensor(c);
        return c;
    }

    protected <C extends Bitmap2D> Bitmap2DSensor<C> addCameraCoded(@Nullable Term id, Supplier<BufferedImage> bc, int sx, int sy, int ox, int oy) {
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


            rewardSum = new AtomicDouble();


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
        private final int w;
        private final int h;
        private OrthoSurfaceGraph win;

        SpaceGraphPart(Supplier<Surface> surface, int w, int h) {
            this.w = w;
            this.h = h;
            this.surface = surface;
        }

        @Override
        protected void starting(NAR nar) {
            win = window(surface.get(), w, h);
        }

        @Override
        protected void stopping(NAR nar) {
            win.delete();
            win = null;
        }
    }
}

