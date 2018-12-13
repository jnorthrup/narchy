package spacegraph.space2d.widget.meter;

import com.jogamp.opengl.GL2;
import jcog.learn.gng.NeuralGasNet;
import jcog.learn.gng.impl.Centroid;
import jcog.tree.rtree.rect.MutableHyperRectDouble;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.graph.Graph2D;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.text.VectorLabel;
import jcog.math.v2;
import spacegraph.video.Draw;

import java.util.stream.Stream;

public class Cluster2DView extends Graph2D<Centroid> {

//    public final AtomicBoolean autozoom = new AtomicBoolean(true);

    double maxX, maxY, minX, minY;

    public Cluster2DView() {
        build(x ->
                x.set(
                        //TODO extract PolygonButton class
                        new PushButton(new VectorLabel(String.valueOf(x.id.id))) {
                            @Override
                            protected void paintIt(GL2 gl, SurfaceRender rr) {
                                super.paintIt(gl, rr);

                                NodeVis p = parent(NodeVis.class);
                                if (p != null) {
                                    float alpha = 0.25f + 0.75f * p.pri;
                                    gl.glColor4f(1f, 0.5f, 0, alpha);
                                    //Draw.colorHash(gl, x.id, alpha);
                                    Draw.circle(gl, new v2(cx(), cy()), false, Math.max(w(), h()) / 2, 6);
                                }
                            }
                        }
                )
        );
        render((node, graph) -> {

            synchronized (Cluster2DView.this) {
                Centroid centroid = node.id;

                double[] coords = centroid.getDataRef();

                double rad = //centroid.localDistance() / 2; //?
                        w() / 10;
                //double err = node.id.localError();

                float localError = (float) centroid.localError();
                node.pri = (float) (1 / (1 + Math.log(1 + localError)));

                RectFloat b = bounds(coords, rad);
                node.pos(b);

                node.color(0.1f, 0.1f, 0.1f, 0.1f);
                node.show();

            }

        });
    }

    /** maps the coordinates to a 2D boundary for display */
    protected RectFloat bounds(double[] coords, double rad) {
        float px = (float) ((coords[0] - minX)/(maxX-minX));
        float py = (float) ((coords.length > 1 ? (coords[1] - minY)/(maxY-minY) : 0.5f));
        return RectFloat.XYWH(
                x() + w() * px,
                y() + h() * py,
                rad,
                rad
        );
    }


    public void update(NeuralGasNet g) {
        MutableHyperRectDouble n;
        synchronized (this) {
            n = (MutableHyperRectDouble) g.bounds();
            if (n == null) {
                maxX = maxY = Double.NEGATIVE_INFINITY;
                minX = minY = Double.POSITIVE_INFINITY;
                set(Stream.empty());
            } else {

                n.growPct(0.5f);

                minX = n.min.coord(0);
                minY = n.min.coord(1);
                maxX = n.max.coord(0);
                maxY = n.max.coord(1);

                set(g.nodeStream());
            }
        }

    }
}
