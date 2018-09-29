package spacegraph.space2d.widget.meter;

import com.jogamp.opengl.GL2;
import jcog.learn.gng.NeuralGasNet;
import jcog.learn.gng.impl.Centroid;
import jcog.tree.rtree.rect.MutableHyperRectDouble;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.Graph2D;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.util.math.v2;
import spacegraph.video.Draw;

import java.util.stream.Stream;

public class Cluster2DView extends Graph2D<Centroid> {

//    public final AtomicBoolean autozoom = new AtomicBoolean(true);

    public MutableHyperRectDouble netBounds = null;
    double sx, sy, minX, minY;
    float boundsRate = 0.75f;

    public Cluster2DView() {
        build(x ->
                x.set(
                        //TODO extract PolygonButton class
                        new PushButton(new VectorLabel(String.valueOf(x.id.id))) {
                            @Override
                            protected void paintBelow(GL2 gl, SurfaceRender rr) {
                                super.paintBelow(gl, rr);

                                NodeVis p = parent(NodeVis.class);
                                if (p!=null) {
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

                    float px = (float) (sx * (coords[0] - minX));
                    float py = (float) (sy * (coords.length > 1 ? (coords[1] - minY) : 0.5f));
                    node.pos(RectFloat.XYWH(
                            x() + w() * px,
                            y() + h() * py,
                            rad,
                            rad
                    ));

                    node.color(0.1f, 0.1f, 0.1f, 0.1f);
                    node.show();

            }

        });
    }


    public void update(NeuralGasNet g) {
        MutableHyperRectDouble n;
        synchronized (this) {
            n = (MutableHyperRectDouble) g.bounds();
            if (n == null) {
                sx = sy = 1;
                minX = minY = 0;
                set(Stream.empty());
            } else {

                n.growPct(0.35f);
                if (this.netBounds == null)
                    this.netBounds = n;
                else
                    n = this.netBounds.lerp(n, boundsRate);

                //float sx = w() / 2, sy = h() / 2;
                sx = (float) (1 / netBounds.range(0));
                minX = (float) netBounds.coord(0, false);
                if (netBounds.dim() > 1) {
                    sy = (float) (1 / netBounds.range(1));
                    minY = (float) netBounds.coord(1, false);
                } else {
                    minY = 0;
                    sy = 1;
                }
                if (!Double.isFinite(sx)) sx = 1;
                if (!Double.isFinite(sy)) sy = 1;

                set(g.nodeStream());
            }
        }

    }
}
