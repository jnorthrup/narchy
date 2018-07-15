package nars;

import jcog.Util;
import jcog.exe.Loop;
import jcog.signal.Bitmap2D;
import jcog.util.Int2Function;
import nars.agent.NAgent;
import nars.control.MetaGoal;
import nars.derive.Derivers;
import nars.derive.deriver.MatrixDeriver;
import nars.derive.deriver.SimpleDeriver;
import nars.exe.Attention;
import nars.exe.BufferedExec;
import nars.gui.NARui;
import nars.index.concept.HijackConceptIndex;
import nars.op.ArithmeticIntroduction;
import nars.op.mental.Inperience;
import nars.op.stm.ConjClustering;
import nars.sensor.Bitmap2DSensor;
import nars.term.Term;
import nars.time.Tense;
import nars.time.clock.RealTime;
import nars.video.*;
import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.Auvent;
import net.beadsproject.beads.core.UGen;
import net.beadsproject.beads.data.WaveFactory;
import net.beadsproject.beads.ugens.*;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.tuple.primitive.IntObjectPair;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import org.jetbrains.annotations.Nullable;
import spacegraph.SpaceGraph;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

import static nars.$.$$;
import static nars.Op.BELIEF;

/**
 * Extensions to NAgent interface:
 * <p>
 * --chart output (spacegraph)
 * --cameras (Swing and OpenGL)
 */
abstract public class NAgentX extends NAgent {

    public NAgentX(String id, NAR nar) {
        super(id, nar);

    }


    public static NAR runRT(Function<NAR, NAgent> init, float narFPS) {

        /*
        try {
            Exe.UDPeerProfiler prof = new Exe.UDPeerProfiler();

        } catch (IOException e) {
            e.printStackTrace();
        }
        */


        float clockFPS = narFPS;

        RealTime clock =
                new RealTime.MS();


        clock.durFPS(clockFPS);


        NAR n = new NARS()

                .attention(()->new Attention(256))

                //.exe(new UniExec() {
                .exe(new BufferedExec.WorkerExec(Util.concurrency(), false /* true */))

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


                        //new CaffeineIndex(80 * 1024, (x) -> 1) //, c -> (int) Math.ceil(c.voluplexity()))
                        new HijackConceptIndex(64 * 1024, 4)


                )
                .get();



        new MatrixDeriver(Derivers.nal(n, 1, 8, /*"curiosity.nal",*/ "motivation.nal"));

        n.dtDither.set(10);
        n.timeFocus.set(10);

        n.confMin.set(0.01f);
        n.freqResolution.set(0.01f);
        n.termVolumeMax.set(46);

        n.forgetRate.set(0.8f);
        n.activateConceptRate.set(0.9f);


        n.beliefConfDefault.set(0.99f);
        n.goalConfDefault.set(0.9f);

        n.beliefPriDefault.set(0.4f);
        n.goalPriDefault.set(0.8f);

        n.questionPriDefault.set(0.05f);
        n.questPriDefault.set(0.08f);

        n.emotion.want(MetaGoal.Perceive, 0f); //-0.01f); //<- dont set negative unless sure there is some positive otherwise nothing happens
        n.emotion.want(MetaGoal.Believe, +0.05f);
        n.emotion.want(MetaGoal.Answer, +0.10f);
        n.emotion.want(MetaGoal.Desire, +0.50f);
        n.emotion.want(MetaGoal.Action, +0.75f);

//        try {
//            InterNAR i = new InterNAR(n, 8, 0);
//            i.runFPS(4);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }


        ConjClustering conjClusterBinput = new ConjClustering(n, BELIEF, (Task::isInput), 8, 128);
        ConjClustering conjClusterBany = new ConjClustering(n, BELIEF, (t -> true), 4, 64);

        ArithmeticIntroduction arith = new ArithmeticIntroduction(64, n);

        Inperience inp = new Inperience(n, 64);



//        new Abbreviation(n, "z", 5, 9, 0.01f, 8);


        n.runLater(() -> {

            NAgent a = init.apply(n);
            //a.durs(2f); //nyquist?

            n.on(a);

            n.runLater(() -> {
                MatrixDeriver motivation = new MatrixDeriver(a.fire(),
                        Derivers.nal(n, 1, 6, "motivation.nal"));


                SimpleDeriver curiosityDeriver = new SimpleDeriver(a.fireActions(),
                        Derivers.nal(n, 0, 0, "curiosity.nal"),
                        SimpleDeriver.GlobalTermLinker) {

                    @Override
                    public boolean singleton() {
                        return true;
                    }


                    long last;

                    @Override
                    protected void starting(NAR nar) {
                        last = nar.time();
                        super.starting(nar);
                    }

                    @Override
                    protected int next(NAR n, int iterations) {
                        long now = n.time();
                        if (last + a.dur() > now)
                            return 0; //already called for this duration
                        last = now;

                        int numActions = a.actions.size();

                        iterations = numActions; //override
//                        if (iterations == 1) {
//                            if (n.random().nextFloat() >= a.curiosity.floatValue())
//                                return 0;
//
//                        } else {
                            iterations = Math.round(a.curiosity.floatValue() * iterations);
                            iterations = Math.min(iterations, numActions);
                            if (iterations < 1)
                                return 0;
//                        }

                        return super.next(n, iterations);
                    }
                };

                a.curiosity.set(0.5f);


                //new MatrixDeriver(a.fire(), n::input, Derivers.nal(n, 1, 8, "curiosity.nal"), n);

                NARui.agentWindow(a);

                SpaceGraph.window(NARui.top(n), 800, 800);

                //new Spider(n, Iterables.concat(java.util.List.of(a.id, n.self(), a.happy.id), Iterables.transform(a.always, Task::term)));

                //System.gc();
            });
        });

        Loop loop = n.startFPS(narFPS);

        return n;
    }


    /**
     * pixelTruth defaults to linear monochrome brightness -> frequency
     */
    protected Bitmap2DSensor senseCamera(String id, java.awt.Container w, int pw, int ph) {
        return senseCamera(id, new SwingBitmap2D(w), pw, ph);
    }


    protected Bitmap2DSensor<Scale> senseCamera(String id, Supplier<BufferedImage> w, int pw, int ph) {
        return senseCamera(id, new Scale(w, pw, ph));
    }

    protected Bitmap2DSensor<PixelBag> senseCameraRetina(String id, Component w, int pw, int ph) throws
            Narsese.NarseseException {
        return senseCameraRetina(id, new SwingBitmap2D(w), pw, ph);
    }


    protected Bitmap2DSensor<PixelBag> senseCameraRetina(String id, Supplier<BufferedImage> w, int pw, int ph) throws
            Narsese.NarseseException {
        return senseCameraRetina($$(id), w, pw, ph);
    }


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
        return senseCamera((Term) (id != null ? $$(id) : null), bc);
    }

    protected <C extends Bitmap2D> Bitmap2DSensor<C> senseCamera(@Nullable Term id, C bc) {
        return addCamera(new Bitmap2DSensor(id, bc, nar()));
    }

    protected <C extends Bitmap2D> Bitmap2DSensor<C> senseCamera(@Nullable Int2Function<Term> id, C bc) {
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
        nar().runLater(() -> {
            c.readAdaptively(this);
        });
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

        }

        private void set(int i) {
            if (i < 0) i = 0;
            if (i >= v.length) i = v.length - 1;

            update.value(v[x = i]);

        }

        @Override
        public void accept(int aa) {


            switch (aa) {
                case 0:
                    set(x - 1);
                    break;
                case 1:
                    set(x + 1);
                    break;
                default:
                    throw new RuntimeException("OOB");


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


    private static class Metronome {
        public Metronome(Clock cc, NAR n) {
            cc.on(new Auvent<Clock>() {

                public final Envelope kickEnv, snareEnv;
                AudioContext ac = cc.getContext();

                {
                    kickEnv = new Envelope(ac, 0.0f);

                    UGen kickGain = new Gain(ac, 1, kickEnv).in(
                            new BiquadFilter(ac, BiquadFilter.BESSEL_LP, 500.0f, 1.0f).in(
                                    new WavePlayer(ac, 100.0f, WaveFactory.SINE)));

                    ac.out.in(kickGain);

                }

                {
                    snareEnv = new Envelope(ac, 0.0f);

                    WavePlayer snareNoise = new WavePlayer(ac, 1.0f, WaveFactory.NOISE);
                    WavePlayer snareTone = new WavePlayer(ac, 200.0f, WaveFactory.SINE);

                    IIRFilter snareFilter = new BiquadFilter(ac, BiquadFilter.BP_SKIRT, 2500.0f, 1.0f);
                    snareFilter.in(snareNoise);
                    snareFilter.in(snareTone);

                    Gain snareGain = new Gain(ac, 1, snareEnv);
                    snareGain.in(snareFilter);


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

                        kickEnv.add(0.5f, 2.0f);
                        kickEnv.add(0.2f, 5.0f);
                        kickEnv.add(0.0f, 50.0f);
                        n.believe($.the("kick"), Tense.Present);


                    }
                }
            });
        }
    }


}

