package spacegraph.space2d.widget.chip;

import jcog.event.Off;
import jcog.learn.gng.NeuralGasNet;
import jcog.learn.gng.impl.Centroid;
import jcog.math.IntRange;
import jcog.signal.Tensor;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space2d.widget.meter.ScatterPlot2D;
import spacegraph.space2d.widget.port.Port;
import spacegraph.space2d.widget.port.TypedPort;

public class Cluster2DChip extends Bordering {

    private final Port<Tensor> in;
    private final ScatterPlot2D centroids;

    //TODO allow choice or more abstract mapping from certain dimensions
    final IntRange xDimension = null;
    final IntRange yDimension = null;

    //Autoencoder ae;
    NeuralGasNet g;
    private Off update;

    class Config {
        public final IntRange clusters = new IntRange(4, 2, 32);

        //TODO configure
        public Centroid.DistanceFunction distanceCartesianManhattan = Centroid.DistanceFunction::distanceCartesianManhattan;

        void update(int dim) {
            if (g == null || g.dimension != dim || clusters.intValue() != g.centroids.length) {

                g = new NeuralGasNet(dim, clusters.intValue(), distanceCartesianManhattan);
                //ae = new Autoencoder(dim, 2, new XoRoShiRo128PlusRandom(1));
            }
        }
    }

    final Config config = new Config();

    public Cluster2DChip() {
        super();

        config.update(1);

        in = new TypedPort<>(Tensor.class).on((Tensor t) -> {
            synchronized (g) {
                int volume = t.volume();
                config.update(volume);
                if (volume >= 2) {
                    g.put(t.toDoubleArray());
                } else if (volume == 1) {
                    g.put(t.getAt(0));
                }
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
        centroids = new ScatterPlot2D<Centroid>(null /* TODO */);
        //(c)->new v2((float)c.getEntry(0), (float)c.getEntry(1)));


//        Graph2D<Object> data = new Graph2D<>()
//                .render(new Graph2D.NodeGraphRenderer())
//                .setAt(Stream.of(g.centroids));
//        Surface data = new EmptySurface(); //TODO

        //setAt(C, new Stacking(centroids, data ));
        set(C, centroids);
        set(W, in, 0.15f);
        set(S, new Gridding(new ObjectSurface(g), new ObjectSurface(config)), 0.15f);
    }

    @Override
    protected void starting() {
        super.starting();
//        update = root().animate((dt) -> {
//            //if (visible()) {
//            //}
////            for (Centroid c : g.centroids) {
////                float a = (float) (1.0 / (1 + c.localError()));
////                ae.put(Util.toFloat(c.getDataRef()), a * 0.05f, 0.001f, 0, false);
////            }
//            return true;
//        });
    }

    @Override
    protected void renderContent(ReSurface r) {
        centroids.set(g.nodeStream());
        super.renderContent(r);
    }

    @Override
    protected void stopping() {
        update.pause();
        super.stopping();
    }

}
