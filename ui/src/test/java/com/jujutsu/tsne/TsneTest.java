package com.jujutsu.tsne;

import com.jogamp.opengl.GL2;
import com.jujutsu.tsne.matrix.MatrixOps;
import jcog.Util;
import jcog.data.set.ArrayHashSet;
import jcog.io.Schema;
import jcog.io.arff.ARFF;
import jcog.math.FloatRange;
import jcog.tree.rtree.rect.RectFloat;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.Graph2D;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.video.Draw;

import java.io.File;

public class TsneTest {


    //TODO maxGain parameter for SimpleTsne
    //TODO dynamic perplexity control
    public static class TsneModel implements Graph2D.Graph2DUpdater<Schema.Instance> {

        public final FloatRange spaceScale = new FloatRange(0.2f, 0.001f, 100);
        public final FloatRange nodeScale = new FloatRange(1f, 0, 10);

        final int firstCol;
        final int lastCol;

        // = i.id.data.size()
        public TsneModel(int firstCol, int lastCol) {
            this.firstCol =firstCol;
            this.lastCol = lastCol;
        }

        final TSneConfig config = new TSneConfig(
                 2, 10,
                false, true
        );

        final SimpleTSne s = new SimpleTSne();
        final ArrayHashSet<Graph2D.NodeVis<Schema.Instance>> xx = new ArrayHashSet<>();
        final ArrayHashSet<Graph2D.NodeVis<Schema.Instance>> nn = new ArrayHashSet<>();
        double[][] X = new double[0][0];

        @Override public void update(Graph2D<Schema.Instance> g, int dtMS) {
            g.forEachValue(nn::add);
            if (!nn.equals(xx)) {
                xx.clear();
                xx.addAll(nn);


                int rows = xx.size();
                if (rows > 0) {
                    //TODO write an overridable extractor method
//                    int cols = xx.get(0).id.data.size()-1;
                    if (X.length != rows /*|| X[0].length != cols*/) {
                        X = new double[rows][];
                    }
                    int j = 0;
                    for (Graph2D.NodeVis<Schema.Instance> i : xx) {
                        X[j++] = i.id.toDoubleArray(firstCol, lastCol);
                    }
                } else {
                    X = new double[0][0];
                }

                s.reset(X, config);
            }
            nn.clear();


            double[][] Y = s.next(1);
            int j = 0;

            int n = X.length;
            float space = 1; //(float) Math.sqrt(n+1)*100f;
            double cx = g.cx(), cy = g.cy();
            float scale = spaceScale.floatValue() * Math.max(g.w(), g.h()) / space;
            float nodeScale = this.nodeScale.floatValue();
            for (Graph2D.NodeVis<Schema.Instance> i : xx) {
                double[] Yj = Y[j];
                double x = (Yj[0] =
                                //Util.clamp(Yj[0], -space /2, space /2)
                                Yj[0]
                        )*scale;
                double y = (Yj[1] =
                                //Util.clamp(Yj[1], -space /2, space /2)
                                Yj[1]
                        )*scale;

//                double z = (Yj[2] = Util.clamp(Yj[2], -0.5, +0.5)); //narrow but room to maneuver
//                s.gains[j][2] = 0; //clear z gains
//                //Arrays.fill(s.gains[j], 0.0); //reset gains


                float w = nodeScale *
                        (((Number)xx.get(j).id.data.get(0)).floatValue() -400)*0.01f; //customized: first column as size TODO normalize

                i.posXYWH((float)(x+cx), (float)(y+cy), w, w);
//                i.fence(g.bounds);
//                Yj[0] = i.left()/scale;
//                Yj[1] = i.top()/scale;
                j++;
            }

        }

    }

    public static class TsneRenderer implements Graph2D.Graph2DRenderer<Schema.Instance> {

        @Override
        public void node(Graph2D.NodeVis<Schema.Instance> node, Graph2D.GraphEditing<Schema.Instance> graph) {
            node.set(new PushButton() {
                @Override
                protected void paintWidget(GL2 gl, RectFloat bounds) {

                }

                @Override
                protected void paintBelow(GL2 gl, SurfaceRender r) {
                    paintNode(gl, this, node.id);
                }
            }.click(()->{
                System.out.println(node.id);
            }));
        }

        protected void paintNode(GL2 gl, Surface surface, Schema.Instance id) {
            Draw.colorHash(gl, id.hashCode(), 0.8f);
            Draw.rect(surface.bounds, gl);
        }
    }

    @Test public void testTsneModel() {

        try {
            ARFF data = new ARFF(new File("/tmp/x.arff"));


            SpaceGraph.window(
                new Graph2D<Schema.Instance>().
                    update(new TsneModel(1, 7)).
                    render(new TsneRenderer() {
                        @Override protected void paintNode(GL2 gl, Surface surface, Schema.Instance id) {
                            float score = ((Double)(id.data.get(0))).floatValue();
                            Draw.colorGrays(gl, 0.25f + 0.75f * Util.unitize((score - 1400)/200));
                            Draw.rect(surface.bounds, gl);
                        }
                    }).
                    set(data.stream()),
                800, 800);

            Util.sleepMS(1000000);

        } catch (Throwable e) {
            e.printStackTrace();
        }

    }

    @Disabled
    @Test
    public void test1() {


        int DIM = 4;
        int N = 128;
        double[][] x = new double[N][DIM];
        int j = 0;
        for (int i = 0; i < N / 2; i++) {
            x[j++] = new double[]{0, 0, 1 + Math.random() / 2f, 1 + Math.random() / 2f};
        }
        for (int i = 0; i < N / 2; i++) {
            x[j++] = new double[]{1, 0, -1 + Math.random() / 2f, -1 + Math.random() / 2f};
        }


        SimpleTSne t = new SimpleTSne() {
            Surface plot = new Surface() {

                @Override
                protected void paint(GL2 gl, SurfaceRender surfaceRender) {
                    Draw.bounds(bounds, gl, this::paint);
                }

                protected void paint(GL2 gl) {
                    double[][] vv = Y;
                    if (vv == null)
                        return;
                    vv = vv.clone();


                    float scale = 0.1f;
                    float w, h;
                    w = h = 1f / vv.length;
                    for (int i = 0, yLength = vv.length; i < yLength; i++) {
                        double[] v = vv[i];

                        float x = (float) (v[0]);
                        float y = (float) (((v.length > 1) ? v[1] : 0));

                        x *= scale;
                        y *= scale;

                        Draw.colorHash(gl, i, 0.75f);
                        Draw.rect(x, y, w, h, gl);
                    }
                }
            };

            {
                SpaceGraph.window(plot, 800, 800);
            }

//            @Override
//            public double[][] next(int iter) {
//                Util.sleep(50);
//
//                return super.next(iter);
//            }
        };

        double[][] y = t.reset(x, new TSneConfig(
                 2, 5f,
                false, true
        ));
        System.out.println(MatrixOps.doubleArrayToPrintString(y));


    }
}
