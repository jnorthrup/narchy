package nars;

import jcog.Util;
import jcog.exe.Exe;
import jcog.exe.Loop;
import jcog.math.random.SplitMix64Random;
import jcog.signal.Bitmap2D;
import jcog.util.Int2Function;
import nars.agent.NAgent;
import nars.derive.Derivers;
import nars.derive.deriver.MatrixDeriver;
import nars.exe.Focus;
import nars.exe.WorkerMultiExec;
import nars.gui.EmotionPlot;
import nars.gui.NARui;
import nars.gui.graph.DynamicConceptSpace;
import nars.index.concept.CaffeineIndex;
import nars.index.concept.HijackConceptIndex;
import nars.op.ArithmeticIntroduction;
import nars.op.mental.Inperience;
import nars.op.stm.ConjClustering;
import nars.sensor.Bitmap2DSensor;
import nars.term.Term;
import nars.time.Tense;
import nars.time.clock.RealTime;
import nars.util.TimeAware;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.AspectAlign;
import spacegraph.space2d.container.EdgeDirected3D;
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
import static spacegraph.space2d.container.grid.Gridding.grid;

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

    public static TimeAware runRT(Function<NAR, NAgent> init, float fps) {
        return runRT(init,
                fps * 2, 
                
                fps);
    }

    public static TimeAware runRT(Function<NAR, NAgent> init, float narFPS, float agentFPS) {

        try {
            Exe.UDPeerProfiler prof = new Exe.UDPeerProfiler();

        } catch (IOException e) {
            e.printStackTrace();
        }

        
        
        

        






        float clockFPS =
                
                narFPS;

        RealTime clock =
                new RealTime.MS(false);




        clock.durFPS(clockFPS);







        

        









        NAR n = new NARS()













//                .exe(new MixMultiExec.WorkerMultiExec(
//                            1024,
//                             Util.concurrencyDefault(2)) {
//
//                         {
//                             Exe.setExecutor(this);
//                         }
//
//
//                     }
//                )
                .exe(new WorkerMultiExec(

                             new Focus.AERevaluator(new SplitMix64Random(1)),
                             Util.concurrencyDefault(2),
                             1024, 2048) {

                        {
                            Exe.setExecutor(this);
                        }


                     }
                )



                .time(clock)
                .index(

                        

                        
                        
                        //newCaffeineIndex()
                        new HijackConceptIndex(64 * 1024, 4)
                        
                        
                )
                .get();

        new MatrixDeriver(Derivers.nal(1, 8, n));

        

        n.dtMergeOrChoose.set(true);
        
        n.dtDither.set(10); //100fps base
        n.timeFocus.set(5);


        n.confMin.set(0.01f);
        n.freqResolution.set(0.01f);
        n.termVolumeMax.set(30);

        n.beliefConfDefault.set(0.98f);
        n.goalConfDefault.set(0.98f);



        n.beliefPriDefault.set(0.1f);
        n.goalPriDefault.set(1f);
        n.questionPriDefault.set(0.1f);
        n.questPriDefault.set(0.25f);

        n.forgetRate.set(0.9f);

//        try {
//            InterNAR i = new InterNAR(n, 8, 0);
//            i.runFPS(4);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }



























































        

        













        ConjClustering conjClusterBinput = new ConjClustering(n, BELIEF, (Task::isInput), 8, 64);
        ConjClustering conjClusterBany = new ConjClustering(n, BELIEF, (t -> true), 2, 16);

        

        ArithmeticIntroduction arith = new ArithmeticIntroduction(32, n);





        











        
        Inperience inp = new Inperience(n, 32);


        

        
        


        














        









        

        













        
        System.gc();


        NAgent a = init.apply(n);
        a.motivation.set(0.5f);

        n.on(a);
        n.synch();







        Loop loop = n.startFPS(narFPS);

        n.runLater(() -> {

            chart(a);

            SpaceGraph.window(NARui.top(n), 800, 800);


            
            Loop aLoop = a.startFPS(agentFPS);

        });

        return n;
    }

    @NotNull
    public static CaffeineIndex newCaffeineIndex() {
        return new CaffeineIndex(
                
                1500 * 1024,
                
                
                
                
                
                c -> {
















                    return (int) Math.ceil(c.voluplexity());





                }
        );
    }

    public static void chart(NAgent a) {
        NAR nar = a.nar();
        nar.runLater(() -> {
            SpaceGraph.window(
                    grid(
                            new AutoSurface(a),

                            NARui.beliefCharts(nar.dur() * 64, a.actions.keySet(), a.nar()),

                            new EmotionPlot(64, a),
                            grid(
                                    
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
                                            NARui.conceptWindow(s, nar);
                                        }
                                    }.surface(),

                                    
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
                                        EdgeDirected3D fd = new EdgeDirected3D();
                                        s.dyn.addBroadConstraint(fd);
                                        fd.attraction.set(fd.attraction.get() * 8);

                                        s.add(new SubOrtho(
                                                
                                                grid(new AutoSurface<>(fd), new AutoSurface<>(sg.vis))) {

                                        }.posWindow(0, 0, 1f, 0.2f));

                                        
                                        

                                        s.camPos(0, 0, 90);
                                        return s;
                                    }),


                                    

                                    






                                    
                                    
                                    

                                    a instanceof NAgentX ?
                                            new WindowToggleButton("vision", () -> grid(((NAgentX) a).sensorCam.stream().map(cs -> new AspectAlign(
                                                    new CameraSensorView(cs, a).withControls(), AspectAlign.Align.Center, cs.width, cs.height))
                                                    .toArray(Surface[]::new))
                                            ) : grid()
                            )
                    ),









































                    900, 600);
        });
    }




















    





















































    

















    /**
     * pixelTruth defaults to linear monochrome brightness -> frequency
     */
    protected Bitmap2DSensor senseCamera(String id, Container w, int pw, int ph) {
        return senseCamera(id, new SwingBitmap2D(w), pw, ph);
    }


































    protected Bitmap2DSensor<Scale> senseCamera(String id, Supplier<BufferedImage> w, int pw, int ph) {
        return senseCamera(id, new Scale(w, pw, ph));
    }

    protected Bitmap2DSensor<PixelBag> senseCameraRetina(String id, Container w, int pw, int ph) throws
            Narsese.NarseseException {
        return senseCameraRetina(id, new SwingBitmap2D(w), pw, ph);
    }





    protected Bitmap2DSensor<PixelBag> senseCameraRetina(String id, Supplier<BufferedImage> w, int pw, int ph) throws
            Narsese.NarseseException {
        return senseCameraRetina($(id), w, pw, ph);
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

