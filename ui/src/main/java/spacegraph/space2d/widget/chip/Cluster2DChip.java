package spacegraph.space2d.widget.chip;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.data.graph.Node;
import jcog.event.Off;
import jcog.learn.Autoencoder;
import jcog.learn.gng.NeuralGasNet;
import jcog.learn.gng.impl.Centroid;
import jcog.math.IntRange;
import jcog.random.XoRoShiRo128PlusRandom;
import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.*;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space2d.widget.port.Port;
import spacegraph.video.Draw;

import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Cluster2DChip extends Bordering {

    private final Port in;
    private final Graph2D<Centroid> centroids;
    Autoencoder ae;
    NeuralGasNet g;
    private Off update;

    class Config {
        public final IntRange clusters = new IntRange(4, 2, 32);

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

        config.reset(8);

        in = new Port().on((float[] x)->{
            synchronized (g) {
                config.reset(x.length);
                g.put(Util.toDouble(x));
            }
        });

//        display = new Surface() {
//
//            @Override
//            protected void paint(GL2 gl, SurfaceRender surfaceRender) {
//                Draw.bounds(bounds, gl, this::paint);
//            }
//
//            void paint(GL2 gl) {
//                synchronized (g) {
//                    NeuralGasNet g = Cluster2DChip.this.g;
//
//
//
//
//
//
//                    float cw = 0.1f;
//                    float ch = 0.1f;
//                    for (Centroid c : g.centroids) {
//                        float a = (float) (1.0 / (1 + c.localError()));
//                        ae.put(Util.toFloat(c.getDataRef()), a * 0.05f, 0.001f, 0, false);
//                        float x =
//                                0.5f*(1+ae.y[0]);
//
//
//                        float y =
//                                0.5f*(1+ae.y[1]);
//
//
//
//
//
//
//
//                        Draw.colorHash(gl, c.id, a);
//                        Draw.rect(x-cw/2, y-ch/2, cw, ch, gl);
//                    }
//                }
//            }
//
//        };
        centroids = new Graph2D<Centroid>()
                .render((node, graph)->{

                    node.color(1f, 0.5f, 0f, 1f);

                    double[] coords = node.id.getDataRef();
                    float sx = 100f;
                    float sy = 100f;
                    node.pos(RectFloat2D.XYWH(
                            x() + w()/2+sx * (float)coords[0],
                            y() + h()/2+sy * (float)coords[1], 10, 10));//TODO abstract to provided dimensionalization model

                })


        ;


//        Graph2D<Object> data = new Graph2D<>()
//                .render(new Graph2D.NodeGraphRenderer())
//                .set(Stream.of(g.centroids));
        Surface data = new EmptySurface(); //TODO

        //set(C, new Stacking(centroids, data ));
        set(C, centroids );
        set(E, in, 0.1f);
        set(S, new ObjectSurface(config), 0.1f);
    }

    @Override
    protected void starting() {
        super.starting();
        update = root().animate((dt)->{
           assert(visible());
           centroids.set(Stream.of(g.centroids));
//            for (Centroid c : g.centroids) {
//                float a = (float) (1.0 / (1 + c.localError()));
//                ae.put(Util.toFloat(c.getDataRef()), a * 0.05f, 0.001f, 0, false);
//            }
            return true;
        });
    }

    @Override
    protected void stopping() {
        update.off();
        super.stopping();
    }

}
