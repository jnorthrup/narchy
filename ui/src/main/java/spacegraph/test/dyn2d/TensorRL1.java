package spacegraph.test.dyn2d;

import jcog.learn.Agent;
import jcog.learn.Autoencoder;
import jcog.learn.ql.HaiQ;
import jcog.math.FloatRange;
import jcog.math.IntIntToObjectFunc;
import jcog.math.IntRange;
import jcog.math.tensor.Tensor;
import jcog.math.tensor.TensorFunc;
import jcog.math.tensor.TensorLERP;
import jcog.signal.Bitmap2D;
import spacegraph.ZoomOrtho;
import spacegraph.container.Gridding;
import spacegraph.container.Splitting;
import spacegraph.widget.meta.AutoSurface;
import spacegraph.widget.sketch.Sketch2DBitmap;
import spacegraph.widget.slider.XYSlider;
import spacegraph.widget.text.LabeledPane;
import spacegraph.widget.windo.PhyWall;
import spacegraph.widget.windo.Port;
import spacegraph.widget.windo.Widget;

import static spacegraph.test.dyn2d.TensorGlow.rng;

public class TensorRL1 {

    public static void main(String[] args) {

        PhyWall p = PhyWall.window(1200, 1000);
        ((ZoomOrtho) p.root()).scaleMin = 100f;
        ((ZoomOrtho) p.root()).scaleMax = 1500;

        Sketch2DBitmap bmp = new Sketch2DBitmap(4, 4);
        p.addWindow(
                new Splitting(
                        bmp.state(Widget.META),
                        new Port() {
                            float[] a = new float[16];

                            @Override
                            public void prePaint(int dtMS) {
                                super.prePaint(dtMS);

                                for (int i = 0; i < bmp.pix.length; i++) {
                                    a[i] = Bitmap2D.decodeRed(bmp.pix[i]);
                                }
                                out(a);
                            }
                        }, 0.1f),
                1, 1);


        final TensorFunc randomVector = Tensor.randomVectorGauss(16, 0, 1, rng);
        final FloatRange lerpRate = new FloatRange(0.01f, 0, 1f);
        final TensorLERP lerpVector = new TensorLERP(randomVector, lerpRate);

        p.addWindow(new Gridding(0.25f,
//                        new TensorGlow.AutoUpdateMatrixView(
//                                randomVector.data
//                        ),
                        new LabeledPane("rng", new TensorGlow.AutoUpdateMatrixView(
                                lerpVector.data
                        )),
                        new LabeledPane("lerp", new XYSlider().on((x, y) -> {
                            lerpRate.set(x);
                        })),
                        new LabeledPane("out", new Port() {
                            @Override
                            public void prePaint(int dtMS) {
                                super.prePaint(dtMS);

                                lerpVector.update();
                                out(lerpVector.data);
                            }
                        })),
                0.5f, 0.5f);

        p.addWindow(new AutoencoderChip(), 1, 1);

        //p.addWindow(new TogglePort(), 0.25f, 0.25f);

        p.addWindow(new AgentChip(HaiQ::new), 1, 1);


    }

    private static class AgentChip extends Gridding {

        float[] in = new float[1];

        Agent agent;


        private final IntIntToObjectFunc<Agent> builder;

        class Config {
            int inputs;
            public final IntRange actions = new IntRange(2, 2, 16);

            protected synchronized Agent reset() {
                inputs = Math.max(inputs, 2);
                Agent agent = builder.apply(inputs, config.actions.intValue());
                view.set(
                    new Gridding(
                        new LabeledPane(agent.getClass().getSimpleName(),
                                new AutoSurface<>(agent)),
                        new TensorGlow.AutoUpdateMatrixView(in),
                        new TensorGlow.AutoUpdateMatrixView((((HaiQ)agent).q)),
                        new TensorGlow.AutoUpdateMatrixView((((HaiQ)agent).et))
                ));
                return agent;
            }

            public void update(int inputs) {
                if (this.inputs!=inputs || config.actions.intValue()!=agent.actions) {
                    this.inputs = inputs;
                    AgentChip.this.agent = reset();
                }
            }
        }

        final Config config = new Config();

        final Gridding view = new Gridding();


        public AgentChip(IntIntToObjectFunc<Agent> builder) {
            super();
            this.builder = builder;
            this.agent = config.reset();

            Port actionOut = new Port();

            set(
                    new AutoSurface<>(config),

                    new LabeledPane("input", new Port((float[] i) -> {
                        this.in = i;
                        config.update(i.length);

                        //System.arraycopy(i, 0, in, 0, i.length);
                        int a = agent.act(rng.nextFloat(), i);

                        actionOut.out(a);
                    })),

                    view,

                    new LabeledPane("action", actionOut)

            );



        }
    }

    public static class AutoencoderChip extends Gridding {

        Autoencoder ae;

        class Config {
            public final FloatRange learningRate = new FloatRange(0.05f, 0, 1f);
            public final IntRange outputs = new IntRange(2, 2, 16);
            //sigmoid, etc..
        }

        final Config config = new Config();

        final Gridding view = new Gridding();

        protected synchronized void reset(int inputs) {
            Autoencoder ae = this.ae = new Autoencoder(inputs, config.outputs.intValue(), rng);
            view.set(
                    new TensorGlow.AutoUpdateMatrixView(ae.x),
                    new Gridding(
                            new TensorGlow.AutoUpdateMatrixView(ae.hbias),
                            new TensorGlow.AutoUpdateMatrixView(ae.vbias)
                    ),
                    new TensorGlow.AutoUpdateMatrixView(ae.W),
                    new TensorGlow.AutoUpdateMatrixView(ae.y)
            );

            //root().info(AutoencoderChip.this, 1, "reset");
        }

        public AutoencoderChip() {
            super();

            reset(1);

            set(
                    new LabeledPane("in", new Port((float[] x) -> {

                        if (ae.x.length != x.length || ae.outputs()!=config.outputs.intValue()) {
                            reset(x.length);
                        }

                        ae.put(x, config.learningRate.floatValue(), 0.005f, 0f, false);
                    })),

                    new LabeledPane("Autoencode", new AutoSurface(config)),

                    view,

                    new LabeledPane("out", new Port() {
                        @Override
                        public void prePaint(int dtMS) {
                            super.prePaint(dtMS);

                            out(ae.y);
                        }
                    })
            );

        }
    }

}
