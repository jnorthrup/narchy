package spacegraph.space2d.widget.chip;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.learn.Autoencoder;
import jcog.learn.gng.NeuralGasNet;
import jcog.learn.gng.impl.Centroid;
import jcog.math.IntRange;
import jcog.random.XoRoShiRo128PlusRandom;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.Gridding;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space2d.widget.port.Port;
import spacegraph.video.Draw;

public class Cluster2DChip extends Gridding {

    private final Port in;
    private final Surface display;

    Autoencoder ae;
    NeuralGasNet g;
    class Config {
        final IntRange clusters = new IntRange(16, 2, 32);

        synchronized void reset(int dim) {
            if (ae == null || ae.inputs()!=dim) {
                g = new NeuralGasNet(dim, clusters.intValue(), Centroid.DistanceFunction::distanceCartesianManhattan);
                ae = new Autoencoder(dim, 2, new XoRoShiRo128PlusRandom(1));
            }
        }
    }

    final Config config = new Config();

    public Cluster2DChip() {
        super();

        config.reset(2);

        in = new Port().on((float[] x)->{
            synchronized (g) {
                config.reset(x.length);
                g.put(Util.toDouble(x));
            }
        });
        display = new Surface() {

            @Override
            protected void paint(GL2 gl, SurfaceRender surfaceRender) {
                Draw.bounds(bounds, gl, this::paint);
            }

            void paint(GL2 gl) {
                synchronized (g) {
                    NeuralGasNet g = Cluster2DChip.this.g;






                    float cw = 0.1f;
                    float ch = 0.1f;
                    for (Centroid c : g.centroids) {
                        float a = (float) (1.0 / (1 + c.localError()));
                        ae.put(Util.toFloat(c.getDataRef()), a * 0.05f, 0.001f, 0, false);
                        float x =
                                0.5f*(1+ae.y[0]);


                        float y =
                                0.5f*(1+ae.y[1]);







                        Draw.colorHash(gl, c.id, a);
                        Draw.rect(x-cw/2, y-ch/2, cw, ch, gl);
                    }
                }
            }

        };
        set(in, new ObjectSurface(config), display);
    }
}
