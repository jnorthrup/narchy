package spacegraph.test.dyn2d;

import jcog.learn.Autoencoder;
import jcog.learn.ql.HaiQ;
import jcog.math.FloatRange;
import jcog.math.IntRange;
import jcog.math.tensor.Tensor;
import jcog.math.tensor.TensorLERP;
import jcog.signal.Bitmap2D;
import spacegraph.ZoomOrtho;
import spacegraph.container.Gridding;
import spacegraph.container.Splitting;
import spacegraph.widget.meta.AutoSurface;
import spacegraph.widget.sketch.Sketch2DBitmap;
import spacegraph.widget.slider.XYSlider;
import spacegraph.widget.text.Label;
import spacegraph.widget.text.LabeledPane;
import spacegraph.widget.windo.PhyWall;
import spacegraph.widget.windo.Port;
import spacegraph.widget.windo.TogglePort;
import spacegraph.widget.windo.Widget;

import static spacegraph.container.Gridding.VERTICAL;
import static spacegraph.test.dyn2d.TensorGlow.rng;

public class TensorRL1 {

    public static class AutoencoderChip extends Gridding {

        Autoencoder ae;

        class Config {
            public final FloatRange learningRate = new FloatRange(0.05f, 0, 1f);
            public final IntRange outputs = new IntRange(2, 2, 16);
            //sigmoid, etc..
        }

        final Config config = new Config();

        Gridding view = new Gridding();

        protected void reset(int inputs) {
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


    public static void main(String[] args) {

        PhyWall p = PhyWall.window(1200, 1000);
        ((ZoomOrtho) p.root()).scaleMin = 100f;
        ((ZoomOrtho) p.root()).scaleMax = 500;

        final Tensor randomVector = Tensor.randomVectorGauss(16, 0, 1, rng);
        final FloatRange lerpRate = new FloatRange(0.01f, 0, 1f);
        final TensorLERP lerpVector = new TensorLERP(randomVector, lerpRate);

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

        PhyWall.PhyWindow lew = p.addWindow(new Gridding(0.25f,
                        new TensorGlow.AutoUpdateMatrixView(
                                lerpVector.data
                        ),
                        new LabeledPane("lerp", new XYSlider().on((x, y) -> {
                            lerpRate.set(x);
                        })),
                        new LabeledPane("out", new Port((x) -> {
                        }) {
                            @Override
                            public void prePaint(int dtMS) {
                                super.prePaint(dtMS);

                                lerpVector.update();
                                out(lerpVector.data);
                            }
                        })),
                0.5f, 0.5f);

        PhyWall.PhyWindow aew = p.addWindow(new AutoencoderChip(), 1, 1);

        HaiQ q = new HaiQ(8, 2);
        float[] in = new float[q.inputs];


        p.addWindow(new TogglePort(), 0.25f, 0.25f);

        PhyWall.PhyWindow qw = p.addWindow(
                new Gridding(
                        new Label("HaiQ"),
                        new AutoSurface<>(q),
                        new LabeledPane("input", new Port((float[] i) -> {
                            System.arraycopy(i, 0, in, 0, i.length);
                            q.act(rng.nextFloat(), i);
                        })),
                        new Gridding(VERTICAL,
                                new TensorGlow.AutoUpdateMatrixView(in)
                        ),
                        new Gridding(VERTICAL,
                                new TensorGlow.AutoUpdateMatrixView(q.q),
                                new TensorGlow.AutoUpdateMatrixView(q.et)
                        )

                ),
                1, 1);

    }
}
