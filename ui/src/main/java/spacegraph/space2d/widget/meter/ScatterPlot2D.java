package spacegraph.space2d.widget.meter;

import com.jogamp.opengl.GL2;
import jcog.math.v2;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.container.graph.Graph2D;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.video.Draw;

/** 2d scatter ("bubble") plot */
public class ScatterPlot2D<X> extends Graph2D<X> {

    public interface ScatterPlotModel<X> {
        v2 coord(X x);

        /** set size, color, etc */
        default void style(X x, NodeVis<X> v) {

        }

        /** in proportion of the entire visible area's radius */
        float radius(X x);

        /** priority in 0..1.0 */
        default float pri(X x) {
            return 1;
        }

        default void colorize(X x, NodeVis<X> node) {
            node.color(0.5f, 0.5f, 0.5f, 0.5f);
        }

        default String label(X id) {
            return id.toString();
        }
    }

    final ScatterPlotModel<X> model;

    private transient RectFloat extent;

    public ScatterPlot2D(ScatterPlotModel<X> model) {
        this.model = model;
        build(x ->
                x.set(
                        //TODO extract PolygonButton class
                        new PushButton(new VectorLabel(model.label(x.id))) {
                            @Override
                            protected void paintWidget(RectFloat bounds, GL2 gl) {


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

            //synchronized (ScatterPlot2D.this) {

                X x = node.id;

                v2 coords = model.coord(x);
                double rad = w() /* Math.max(w,h)? */ * model.radius(x);

                RectFloat b = bounds(coords, rad);
                node.pos(b);

                node.pri = model.pri(x);

                model.colorize(x, node);

                node.show();

            //}

        });
    }

    /** maps the coordinates to a 2D boundary for display */
    protected RectFloat bounds(v2 p, double rad) {
        float px = ((p.x - extent.x)/ extent.w);
        float py = ((p.y - extent.y)/ extent.h);
        return RectFloat.XYWH(
                x() + w() * px,
                y() + h() * py,
                rad,
                rad
        );
    }

    @Override
    protected void onUpdateEnd() {
        extent = RectFloat.XYXY(-1, -1, +1, +1);
        //TODO abstract compute extent
    }

}
//public class ConjClusterView extends ScatterPlot2D {
//
//    private final ConjClustering conj;
//
//    public ConjClusterView(ConjClustering c) {
//        this.conj = c;
//        DurService.on(c.nar(), () -> update(c.data.net));
//    }
//
//    @Override
//    protected void paintIt(GL2 gl, SurfaceRender r) {
//        conj.data.bag.forEach(l -> {
//            Task t = l.get();
//            RectFloat b = bounds(new double[]{t.mid(), t.priElseZero()}, 35);
//            gl.glColor3f(0.5f, 0.25f, 0f);
//            Draw.rect(b, gl);
//        });
//        super.paintIt(gl, r);
//    }
//}
