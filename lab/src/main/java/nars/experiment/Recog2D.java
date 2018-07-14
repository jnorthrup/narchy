package nars.experiment;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.learn.MLPMap;
import nars.$;
import nars.NAR;
import nars.NAgentX;
import nars.Param;
import nars.agent.NAgent;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.gui.BeliefTableChart;
import nars.sensor.Bitmap2DSensor;
import nars.term.Term;
import nars.term.Termed;
import nars.time.Tense;
import nars.truth.Truth;
import nars.video.CameraSensorView;
import nars.video.PixelBag;
import nars.video.Scale;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.AspectAlign;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.meter.Plot2D;
import spacegraph.video.Draw;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static nars.Op.BELIEF;

/**
 * Created by me on 10/8/16.
 */
public class Recog2D extends NAgentX {


    private final Graphics2D g;
    private final int h;
    private final int w;
    private final BeliefVector outs;

    private final Training train;
    private final Bitmap2DSensor<?> sp;

    boolean mlpLearn = true, mlpSupport = true;

    BufferedImage canvas;

    public final AtomicBoolean neural = new AtomicBoolean(false);


    int image;
    final int maxImages = 2;

    int imagePeriod = 24;

    static {
        Param.DEBUG = false;
    }

    public Recog2D(NAR n) {
        super("x", n);


        w = 10;
        h = 12;
        canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_BGR);
        g = ((Graphics2D) canvas.getGraphics());

        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        















        



        
        sp = senseCamera(
                $.the("x")
                
                
                
                ,
                /*new Blink*/(new Scale(() -> canvas, w, h)/*, 0.8f*/));

        

        outs = new BeliefVector(ii ->
                //$.inst($.the( ii), $.the("x"))
                $.p(ii)
                , maxImages, this);
        train = new Training(
                
                Lists.newArrayList(
                        sp.src instanceof PixelBag ? Iterables.concat(sensors.keySet(), ((PixelBag) sp.src).actions ) :
                                sensors.keySet()
                ),
                outs, nar);

        
        SpaceGraph.window(conceptTraining(outs, nar), 800, 600);
        

    }

    Surface conceptTraining(BeliefVector tv, NAR nar) {

        

        Plot2D p;

        int history = 256;

        Gridding g = new Gridding(

                p = new Plot2D(history, Plot2D.Line).add("Reward", () ->
                                reward
                        
                ),
                

                new AspectAlign(new CameraSensorView(sp, this), AspectAlign.Align.Center, sp.width, sp.height),

                new Gridding(beliefTableCharts(nar, List.of(tv.concepts), 16)),

                new Gridding(IntStream.range(0, tv.concepts.length).mapToObj(i-> new spacegraph.space2d.widget.text.Label(String.valueOf(i)) {
                    @Override
                    protected void paintBelow(GL2 gl) {
                        Concept c = tv.concepts[i];
                        BeliefVector.Neuron nn = tv.neurons[i];

                        float freq, conf;

                        Truth t = nar.beliefTruth(c, nar.time());
                        if (t != null) {
                            conf = t.conf();
                            freq = t.freq();
                        } else {
                            conf = nar.confMin.floatValue();
                            float defaultFreq =
                                    0.5f; 
                            
                            freq = defaultFreq;
                        }


                        Draw.colorBipolar(gl,
                                2f * (freq - 0.5f)
                                
                                
                        );

                        float m = 0.5f * conf;

                        Draw.rect(gl, bounds);

                        if (tv.verify) {
                            float error = nn.error;
                            if (error != error) {

                                
                                
                            } else {

                                

                                
                                





                            }
                        }


                    }
                }).toArray(Surface[]::new)));

        final int[] frames = {0};
        onFrame(() -> {

            if (frames[0]++ % imagePeriod == 0) {
                nextImage();
            }

            redraw();

            
                
                outs.expect(image);
            
            
            
            
            


            if (neural.get()) {
                train.update(mlpLearn, mlpSupport);
            }

            p.update();
            
        });

        return g;
    }

    @Deprecated
    public List<Surface> beliefTableCharts(NAR nar, Collection<? extends Termed> terms, long window) {
        long[] btRange = new long[2];
        onFrame(() -> {
            long now = nar.time();
            btRange[0] = now - window;
            btRange[1] = now + window;
        });
        return terms.stream().map(c -> new BeliefTableChart(nar, c, btRange)).collect(toList());
    }


    @Override
    protected float act() {

        float error = 0;
        for (int i = 0; i < maxImages; i++) {

            this.outs.neurons[i].update();
            error += this.outs.neurons[i].error;










        }

        return Util.clamp( 2 * -(error/maxImages - 0.5f), -1, +1);






        


    }


    protected int nextImage() {

        image = nar.random().nextInt(maxImages);


        return image;
    }

    private void redraw() {
        g.clearRect(0, 0, w, h);
        FontMetrics fontMetrics = g.getFontMetrics();

        String s = String.valueOf((char) ('0' + image));
        
        Rectangle2D sb = fontMetrics.getStringBounds(s, g);

        

        

        g.drawString(s, Math.round(w / 2f - sb.getCenterX()), Math.round(h / 2f - sb.getCenterY()));
    }

    public static void main(String[] arg) {

        NAgentX.runRT((n) -> {

            Recog2D a = new Recog2D(n);

            
            

            
            

            return a;

        }, 15);
    }

    public static class Training {
        private final List<Concept> ins;
        private final BeliefVector outs;
        private final MLPMap trainer;
        private final NAR nar;

        private final float learningRate = 0.3f;

        /**
         * Introduction of the momentum rate allows the attenuation of oscillations in the gradient descent. The geometric idea behind this idea can probably best be understood in terms of an eigenspace analysis in the linear case. If the ratio between lowest and largest eigenvalue is large then performing a gradient descent is slow even if the learning rate large due to the conditioning of the matrix. The momentum introduces some balancing in the update between the eigenvectors associated to lower and larger eigenvalues.
         * <p>
         * For more detail I refer to
         * <p>
         * http:
         */
        private final float momentum = 0.6f;

        public Training(java.util.List<Concept> ins, BeliefVector outs, NAR nar) {

            this.nar = nar;
            this.ins = ins;
            this.outs = outs;


            this.trainer = new MLPMap(ins.size(), new int[]{(ins.size() + outs.states) / 2, outs.states}, nar.random(), true);
            trainer.layers[1].setIsSigmoid(false);

        }


        float[] in(float[] i, long when) {
            int s = ins.size();

            if (i == null || i.length != s)
                i = new float[s];
            for (int j = 0, insSize = ins.size(); j < insSize; j++) {
                float b = nar.beliefTruth(ins.get(j), when).freq();
                if (b != b) 
                    b = 0.5f;
                i[j] = b;
            }

            return i;
        }

        protected void update(boolean train, boolean apply) {
            float[] i = in(null, nar.time());

            float errSum;
            if (train) {
                float[] err = trainer.put(i, outs.expected(null), learningRate, momentum);
                
                errSum = Util.sumAbs(err) / err.length;
                System.err.println("  error sum=" + errSum);
            } else {
                errSum = 0f;
            }

            if (apply/* && errSum < 0.25f*/) {
                float[] o = trainer.get(i);
                for (int j = 0, oLength = o.length; j < oLength; j++) {
                    float y = o[j];
                    
                    float c = nar.confDefault(BELIEF) * (1f - errSum);
                    if (c > 0) {
                        nar.believe(
                                outs.concepts[j].term(),
                                Tense.Present, y, c);
                    }

                }
                
            }
        }
    }


    /**
     * Created by me on 10/15/16.
     */
    public static class BeliefVector {





        static class Neuron {

            public float predictedFreq = 0.5f, predictedConf = 0;

            public float expectedFreq = 0.5f;

            public float error;

            public Neuron() {
                expectedFreq = Float.NaN;
                error = 0;
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
                    this.error = 0;
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


        public BeliefVector(IntFunction<Term> namer, int maxStates, NAgent a) {

            this.states = maxStates;
            this.neurons = new Neuron[maxStates];
            this.concepts = IntStream.range(0, maxStates).mapToObj((int i) -> {
                        Term tt = namer.apply(i);

                        Neuron n = neurons[i] = new Neuron();

                        return a.action(tt, (bb, x)-> {
                            

                            float predictedFreq = x!=null ? x.expectation() : 0.5f;

                                














                            
                            n.actual(predictedFreq, x!=null ? x.conf() : 0);

                            
                            return x;
                        });



























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
            
            
            

            expect(ii -> ii == onlyStateToBeOn ? 1f : offValue);
        }

    
    
    
    

    
    
    
    




    }
}
