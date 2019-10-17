package spacegraph.space2d.widget.meter;

import jcog.Util;
import jcog.data.map.CellMap;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.container.graph.Graph2D;
import spacegraph.space2d.container.graph.NodeVis;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.util.MutableRectFloat;
import spacegraph.util.RectAnimator;

/** 2d scatter ("bubble") plot */
public class ScatterPlot2D<X> extends Graph2D<X> {

    /** controls camera speed */
    private final float extentUpdatePeriodS = 0.5f;
    private final float minVisPct = 0.01f;

//    private float marginPctW = 0.01f, marginPctH = 0.1f;

    public interface ScatterPlotModel<X> {

        /** internal (model) dimension */
        int dimensionInternal();

        /** external (visualized) dimension */
        default int dimensionExternal() {
            return 2;
        }

        void coord(X x, float[] target);

//        /** set size, color, etc */
//        default void style(X x, NodeVis<X> v) {
//
//        }

        /** in proportion of the entire visible area's radius. provided population estimate for relative sizing heuristics */
        float width(X x, int population);
        float height(X x, int n);

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

        /** returns visible rect extent */
        MutableRectFloat layout(float[][] in, float[][] out);


        /** called before an update */
        default void start()  { }
    }

    public static abstract class SimpleXYScatterPlotModel<X> implements ScatterPlotModel<X> {
        @Override
        public int dimensionInternal() {
            return 2;
        }

        @Override
        public MutableRectFloat layout(float[][] in, float[][] out) {
            if (in.length == 0) return new MutableRectFloat().setX0Y0WH(0,0, 1,1);

            int dim = dimensionExternal();
            for (int i = 0; i < in.length; i++) {
                System.arraycopy(in[i], 0, out[i], 0, dim); //TODO make this unnecessary by making in==out
            }

            MutableRectFloat m = new MutableRectFloat().setX0Y0WH(out[0][0], out[0][1], 0, 0);
            for (int i = 1; i < in.length; i++) {
                m.mbr(out[i][0], out[i][1]);
            }
            return m;
        }
    }

    final ScatterPlotModel<X> model;



    final RectAnimator extent =
            new RectAnimator.ExponentialRectAnimator(new MutableRectFloat());

    float[][] coord = new float[0][0], coordOut = null;


    public ScatterPlot2D(ScatterPlotModel<X> model) {
        this.model = model;
        build(x ->
                x.set(
                        //TODO extract PolygonButton class
                        new PushButton(new VectorLabel(model.label(x.id)))
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
//                        }
                )
        );


        update((g, dtS)->{
            float minVis = this.minVisPct;
            int n = g.nodes();
            float w = w(), h = h();
            MutableRectFloat e = extent.animated();
            g.forEachValue(node->{
                float[][] cc = coordOut;
                int c = node.i;
                if (c < 0 || c >= cc.length) {
                    node.hide();
                } else {

                    float[] xy = cc[c];

                    X id = node.id;

                    node.pos(e.normalizeScale(
                            xy[0], xy[1],
                            model.width(id, n), model.height(id, n),
                            minVis,
                            w,h
                    ));

                    //node.move(w/2, h/2); //HACK wtf why is this necessary

                    node.pri = model.pri(id);

                    model.colorize(id, node);

                    node.show();
                }

            });
        });
        render(
                new Graph2DRenderer<>() {


                    int currentCoord = 0;

                    @Override
                    public void nodes(CellMap<X, NodeVis<X>> cells, GraphEditing<X> edit) {

                        int n = cells.size();

                        model.start();

                        if (coord.length < n || coord.length > n * 2 /* TODO || coord[0].length!=dimensionInternal ... */) {
                            coord = new float[n][model.dimensionInternal()];
                            coordOut = new float[n][model.dimensionExternal()];
                        }

                        currentCoord = 0;
                        Graph2DRenderer.super.nodes(cells, edit);

                        MutableRectFloat nextExtent = model.layout(coord, coordOut);
                        if (Util.equals(0, nextExtent.w))
                            nextExtent.w = 1;
                        if (Util.equals(0, nextExtent.h))
                            nextExtent.h = 1;

                        extent.set(nextExtent, extentUpdatePeriodS);
                    }


                    /**
                     * pre
                     */
                    @Override
                    public void node(NodeVis<X> node, GraphEditing<X> graph) {
                        int c = currentCoord++;
                        if (c < coord.length) {
                            model.coord(node.id, coord[c]);
                            node.i = c;
                        } else {
                            node.i = Integer.MIN_VALUE;
                        }
                    }

                }
        );
    }

    @Override
    protected boolean canRender(ReSurface r) {
        if (super.canRender(r)) {
            extent.animate(r.dtS());
            return true;
        }
        return false;
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
