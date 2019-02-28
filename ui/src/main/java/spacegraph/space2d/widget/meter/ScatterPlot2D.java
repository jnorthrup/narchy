package spacegraph.space2d.widget.meter;

import jcog.data.map.CellMap;
import jcog.math.v2;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.container.graph.Graph2D;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.text.VectorLabel;

/** 2d scatter ("bubble") plot */
public class ScatterPlot2D<X> extends Graph2D<X> {

    public interface ScatterPlotModel<X> {

        /** internal (model) dimension */
        int dimensionInternal();

        /** external (visualized) dimension */
        default int dimensionExternal() {
            return 2;
        }

        void coord(X x, float[] target);

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

        void layout(float[][] in, float[][] out);


    }
    public static abstract class SimpleXYScatterPlotModel<X> implements ScatterPlotModel<X> {
        @Override
        public int dimensionInternal() {
            return 2;
        }

        @Override
        public void layout(float[][] in, float[][] out) {
            int dim = dimensionExternal();
            for (int i = 0; i < in.length; i++)
                System.arraycopy(in[i], 0, out[i], 0, dim);
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
//                            @Override
//                            protected void paintWidget(RectFloat bounds, GL2 gl) {
//                                NodeVis p = parent(NodeVis.class);
//                                if (p != null) {
//                                    float alpha = 0.25f + 0.75f * p.pri;
//                                    gl.glColor4f(1f, 0.5f, 0, alpha);
//                                    //Draw.colorHash(gl, x.id, alpha);
//                                    Draw.circle(gl, new v2(cx(), cy()), false, Math.max(w(), h()) / 2, 6);
//                                }
//                            }
                        }
                )
        );
        render(
            new Graph2DRenderer<X>() {

                float[][] coord = new float[0][0], coordOut = null;
                int currentCoord = 0;

                @Override
                public void nodes(CellMap<X, NodeVis<X>> cells, GraphEditing<X> edit) {

                    int n = cells.size();
                    if (coord.length < n || coord.length > n*2) {
                        coord = new float[n][model.dimensionInternal()];
                        coordOut = new float[n][model.dimensionExternal()];
                    }

                    currentCoord = 0;
                    Graph2DRenderer.super.nodes(cells, edit);

                    model.layout(coord, coordOut);

                    cells.forEachValue(node -> {
                        int c = node.i;
                        if (c >= 0 && node.visible()) {
                            post(node, c);
                        }
                    });

                }

                void post(NodeVis<X> node, int c) {
                    X x = node.id;

                    float[] xy = coordOut[c];
                    double rad = ScatterPlot2D.this.w() /* Math.max(w,h)? */ * model.radius(x);
                    RectFloat b = ScatterPlot2D.this.bounds(new v2(xy[0], xy[1]), rad);
                    node.pos(b);

                    node.pri = model.pri(x);

                    model.colorize(x, node);

                    node.show();
                }

                /** pre */
                @Override public void node(NodeVis<X> node, GraphEditing<X> graph) {
                    int c = currentCoord++;
                    if(c < coord.length) {
                        model.coord(node.id, coord[c]);
                        node.i = c;
                    } else {
                        node.i = Integer.MIN_VALUE;
                    }
                }

            }
        );
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
