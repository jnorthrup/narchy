package nars.experiment;

import com.jogamp.opengl.GL2;
import jcog.math.FloatAveragedWindow;
import jcog.math.FloatSupplier;
import jcog.signal.wave2d.ScaledBitmap2D;
import nars.$;
import nars.GameX;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.game.Game;
import nars.game.GameTime;
import nars.game.Reward;
import nars.game.action.ActionSignal;
import nars.game.action.GoalActionConcept;
import nars.gui.BeliefTableChart;
import nars.gui.sensor.VectorSensorChart;
import nars.sensor.Bitmap2DSensor;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import org.jetbrains.annotations.Nullable;
import spacegraph.SpaceGraph;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.unit.AspectAlign;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.video.Draw;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static jcog.Util.compose;
import static nars.$.*;
import static nars.Op.GOAL;

/**
 * Created by me on 10/8/16.
 */
public class Recog2D extends GameX {


    private final Graphics2D g;
    private final int h;
    private final int w;
    private final BeliefVector outs;

//    private final Training train;
    private final Bitmap2DSensor<?> sp;

//    boolean mlpLearn = true, mlpSupport = true;

    BufferedImage canvas;

//    public final AtomicBoolean neural = new AtomicBoolean(false);


    int image;
    static final int maxImages = 4;

    int imagePeriod = 64;
    static final int FPS = 16;

    public Recog2D(NAR n) {
        super(INSTANCE.$$("x"), GameTime.fps((float) FPS), n);


        w = 12; h = 14;

        canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_BGR);
        g = ((Graphics2D) canvas.getGraphics());

        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);


        int sh = 9;
        int sw = 7;
        sp = senseCamera(
                $.INSTANCE.p(id, $.INSTANCE.the("x"))


                ,
                /*new Blink*/(new ScaledBitmap2D(new Supplier<BufferedImage>() {
                    @Override
                    public BufferedImage get() {
                        return canvas;
                    }
                }, sw, sh)/*, 0.8f*/));


        outs = new BeliefVector(this, maxImages, new IntFunction<Term>() {
            @Override
            public Term apply(int ii) {
                return $.INSTANCE.inh(id, $.INSTANCE.the(ii));
            }
        }
        );
//        train = new Training(
//                sp.src instanceof PixelBag ?
//                    new FasterList(sensors).with(((PixelBag) sp.src).actions) : sensors,
//                outs, nar);
//        train = null;

        Reward r = rewardNormalized("correct", -1.0F, (float) +1, compose(new FloatSupplier() {
            @Override
            public float asFloat() {
                double error = (double) 0;
                double pcSum = (double) 0;
                for (int i = 0; i < maxImages; i++) {
                    BeliefVector.Neuron ni = Recog2D.this.outs.neurons[i];
                    ni.update();
                    float pc = ni.predictedConf;
                    pcSum = pcSum + (double) pc;
                    error = error + (double) ni.error * pc;
                }

                return (float) ((1.0 / (1.0 + (error / pcSum))) - 0.5) * 2.0F;
                //return Util.clamp(2 * -(error / maxImages - 0.5f), -1, +1);
            }
        }, new FloatAveragedWindow(16, 0.1f)));

//        Param.DEBUG = true;
        nar.onTask(new Consumer<Task>() {
            @Override
            public void accept(Task t) {
                if (!t.isEternal() && t.term().equals(r.term())) {
                    System.out.println(t.proof());
                }
            }
        }, GOAL);


    }

    Surface conceptTraining(BeliefVector tv, NAR nar) {


        //Plot2D p;

//        int history = 256;

        int bound = tv.concepts.length;
        List<VectorLabel> list = new ArrayList<>();
        for (int j = 0; j < bound; j++) {
            int i = j;
            VectorLabel vectorLabel = new VectorLabel(String.valueOf(i)) {
                @Override
                protected void paintIt(GL2 gl, ReSurface r) {
                    Concept c = tv.concepts[i];
                    BeliefVector.Neuron nn = tv.neurons[i];

                    float freq;

                    Truth t = nar.beliefTruth(c, nar.time());
                    if (t != null) {
                        float conf = t.conf();
                        freq = t.freq();
                    } else {
//                            conf = nar.confMin.floatValue();
                        float defaultFreq =
                                0.5f;

                        freq = defaultFreq;
                    }


                    Draw.colorBipolar(gl,
                            2f * (freq - 0.5f)


                    );

                    //float m = 0.5f * conf;

                    Draw.rect(bounds, gl);

                    if (tv.verify) {
                        float error = nn.error;
                        if (error != error) {


                        } else {


                        }
                    }


                }
            };
            list.add(vectorLabel);
        }
        Gridding g = new Gridding(

//                p = new Plot2D(history, Plot2D.Line).addAt("Reward", () ->
//                        reward
//
//                ),


                new AspectAlign(new VectorSensorChart(sp, this), AspectAlign.Align.Center, (float) sp.width, (float) sp.height),

                new Gridding(beliefTableCharts(nar, List.of(tv.concepts), 16L)),

                new Gridding(list.toArray(new Surface[0])));

        int[] frames = {0};
        onFrame(new Runnable() {
            @Override
            public void run() {

                if (frames[0]++ % imagePeriod == 0) {
                    Recog2D.this.nextImage();
                }

                Recog2D.this.redraw();


                outs.expect(image);


//            if (neural.get()) {
//                train.update(mlpLearn, mlpSupport);
//            }

                //p.update();

            }
        });

        return g;
    }

    @Deprecated
    public List<Surface> beliefTableCharts(NAR nar, Collection<? extends Termed> terms, long window) {
        long[] btRange = new long[2];
        onFrame(new Runnable() {
            @Override
            public void run() {
                long now = nar.time();
                btRange[0] = now - window;
                btRange[1] = now + window;
            }
        });
        List<Surface> list = new ArrayList<>();
        for (Termed c : terms) {
            BeliefTableChart beliefTableChart = new BeliefTableChart(c, nar);
            list.add(beliefTableChart);
        }
        return list;
    }

    protected int nextImage() {

        image = nar.random().nextInt(maxImages);


        return image;
    }

    private void redraw() {
        g.clearRect(0, 0, w, h);
        FontMetrics fontMetrics = g.getFontMetrics();

        String s = String.valueOf((char) ((int) '0' + image));

        Rectangle2D sb = fontMetrics.getStringBounds(s, g);


        g.drawString(s, (float) Math.round((double) (w / 2f) - sb.getCenterX()), (float) Math.round((double) (h / 2f) - sb.getCenterY()));
    }

    public static void main(String[] arg) {


        GameX.Companion.initFn((float) (FPS * 2), new Function<NAR, Game>() {
            @Override
            public Game apply(NAR n) {

                Recog2D a = new Recog2D(n);
                n.add(a);
                SpaceGraph.window(a.conceptTraining(a.outs, n), 800, 600);

                return a;

            }
        });
    }

//    public static class Training {
//        private final List<Sensor> ins;
//        private final BeliefVector outs;
//        private final MLPMap trainer;
//        private final NAR nar;
//
//        private final float learningRate = 0.3f;
//
//        /**
//         * Introduction of the momentum rate allows the attenuation of oscillations in the gradient descent. The geometric idea behind this idea can probably best be understood in terms of an eigenspace analysis in the linear case. If the ratio between lowest and largest eigenvalue is large then performing a gradient descent is slow even if the learning rate large due to the conditioning of the matrix. The momentum introduces some balancing in the update between the eigenvectors associated to lower and larger eigenvalues.
//         * <p>
//         * For more detail I refer to
//         * <p>
//         * http:
//         */
//        private final float momentum = 0.6f;
//
//        public Training(java.util.List<Sensor> ins, BeliefVector outs, NAR nar) {
//
//            this.nar = nar;
//            this.ins = ins;
//            this.outs = outs;
//
//
//            this.trainer = new MLPMap(ins.size(), new int[]{(ins.size() + outs.states) / 2, outs.states}, nar.random(), true);
//            trainer.layers[1].setIsSigmoid(false);
//
//        }
//
//
//        float[] in(float[] i, long when) {
//            int s = ins.size();
//
//            if (i == null || i.length != s)
//                i = new float[s];
//            for (int j = 0, insSize = ins.size(); j < insSize; j++) {
//                float b = nar.beliefTruth(ins.get(j), when).freq();
//                if (b != b)
//                    b = 0.5f;
//                i[j] = b;
//            }
//
//            return i;
//        }
//
//        protected void update(boolean train, boolean apply) {
//            float[] i = in(null, nar.time());
//
//            float errSum;
//            if (train) {
//                float[] err = trainer.put(i, outs.expected(null), learningRate);
//
//                errSum = Util.sumAbs(err) / err.length;
//                System.err.println("  error sum=" + errSum);
//            } else {
//                errSum = 0f;
//            }
//
//            if (apply/* && errSum < 0.25f*/) {
//                float[] o = trainer.get(i);
//                for (int j = 0, oLength = o.length; j < oLength; j++) {
//                    float y = o[j];
//
//                    float c = nar.confDefault(BELIEF) * (1f - errSum);
//                    if (c > 0) {
//                        nar.believe(
//                                outs.concepts[j].target(),
//                                Tense.Present, y, c);
//                    }
//
//                }
//
//            }
//        }
//    }
//

    /**
     * Created by me on 10/15/16.
     */
    public class BeliefVector {


        class Neuron {

            public float predictedFreq = 0.5f;
            public float predictedConf = (float) 0;

            public float expectedFreq;

            public float error;

            public Neuron() {
                expectedFreq = Float.NaN;
                error = (float) 0;
            }

            public void expect(float expected) {
                this.expectedFreq = expected;
                update();
            }

            public void actual(float f, float c) {
                this.predictedFreq = f;
                this.predictedConf = c;
                update();
            }

            protected void update() {
                float a = this.predictedFreq;
                float e = this.expectedFreq;
                if (e != e) {
                    this.error = (float) 0;
                } else if (a != a) {
                    this.error = 0.5f;
                } else {
                    this.error = (Math.abs(a - e));
                }
            }
        }

        public float[] expected(float[] output) {
            output = sized(output);
            for (int i = 0; i < concepts.length; i++)
                output[i] = expected(i);
            return output;
        }

        public float[] actual(float[] output) {
            output = sized(output);
            for (int i = 0; i < concepts.length; i++)
                output[i] = actual(i);
            return output;
        }

        float[] sized(float[] output) {
            if (output == null || output.length != states) {
                output = new float[states];
            }
            return output;
        }


        Neuron[] neurons;
        TaskConcept[] concepts;

        final int states;


        boolean verify;


        public BeliefVector(Game a, int maxStates, IntFunction<Term> namer) {

            this.states = maxStates;
            this.neurons = new Neuron[maxStates];
            this.concepts = IntStream.range(0, maxStates).mapToObj(new IntFunction<GoalActionConcept>() {
                                                                       @Override
                                                                       public GoalActionConcept apply(int i) {
                                                                           Term tt = namer.apply(i);

                                                                           Neuron n = neurons[i] = new Neuron();

                                                                           return a.action(tt, new GoalActionConcept.MotorFunction() {
                                                                               @Override
                                                                               public @Nullable Truth apply(@Nullable Truth bb, @Nullable Truth x) {


                                                                                   float predictedFreq = x != null ? x.freq() : 0.5f;


                                                                                   n.actual(predictedFreq, x != null ? x.conf() : (float) 0);


                                                                                   //return x;
                                                                                   return x != null ? $.INSTANCE.t(x.freq(), nar.beliefConfDefault.floatValue()) : null;
                                                                               }
                                                                           });


                                                                       }
                                                                   }


            ).toArray(TaskConcept[]::new);


        }

        public float expected(int i) {
            return neurons[i].expectedFreq;
        }


        public float actual(int state) {
            return neurons[state].predictedFreq;
        }


        void expect(IntToFloatFunction stateValue) {

            for (int i = 0; i < states; i++)
                neurons[i].expect(stateValue.valueOf(i));
        }

        public void expect(int onlyStateToBeOn) {
            float offValue =
                    0f;


            expect(new IntToFloatFunction() {
                @Override
                public float valueOf(int ii) {
                    return ii == onlyStateToBeOn ? 1f : offValue;
                }
            });
        }


    }
}
