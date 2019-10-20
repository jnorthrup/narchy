package spacegraph.test;

import com.jogamp.opengl.GL2;
import com.jujutsu.tsne.SimpleTSne;
import com.jujutsu.tsne.TSneConfig;
import com.jujutsu.tsne.matrix.MatrixOps;
import jcog.Util;
import jcog.table.DataTable;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.SpaceGraph;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.PaintSurface;
import spacegraph.space2d.container.graph.Graph2D;
import spacegraph.space2d.container.graph.NodeVis;
import spacegraph.space2d.container.graph.TsneModel;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.video.Draw;
import tech.tablesaw.api.DoubleColumn;

public class TsneTest {


    public static Surface testTsneModel() {

//        try {
        DataTable data = //new ARFF(new File("/tmp/x.arff"));
                new DataTable();
        data.addColumns(DoubleColumn.create("a"), DoubleColumn.create("b"), DoubleColumn.create("c"));
        float n = 20.0F;
        for (int j = 0; (float) j < n; j++) {
            for (int i = 0; (float) i < n; i++) {
                data.add(
                        //Math.cos((i ^ (i-5))/n), Math.sin(i/10f), -0.5f + Math.random()
                        (float) (i ^ j) /n, Math.sqrt((double) (i / n)), Math.sin((double) (j / (n / 4f)))
                );
            }
        }

        TsneModel m = new TsneModel(0, data.columnCount());
        return
                new Graph2D<DataTable.Instance>().
                        update(m).
                        render(new TsneRenderer() {
                            @Override
                            protected void paintNode(GL2 gl, Surface surface, DataTable.Instance id) {
                                float score = ((Double) (id.data.get(0))).floatValue();
                                Draw.colorGrays(gl, 0.5f + 0.5f * Util.unitize((score - 1400.0F) / 200.0F));
                                Draw.rect(surface.bounds, gl);
                            }
                        }).
                        set(data.stream().map(data::instance)).widget();

        //Util.sleepMS(1000000);

//        } catch (Throwable e) {
//            e.printStackTrace();
//        }

    }

    public void test1() {


        int DIM = 4;
        int N = 128;
        double[][] x = new double[N][DIM];
        int j = 0;
        for (int i = 0; i < N / 2; i++) {
            x[j++] = new double[]{(double) 0, (double) 0, 1.0 + Math.random() / 2, 1.0 + Math.random() / 2};
        }
        for (int i = 0; i < N / 2; i++) {
            x[j++] = new double[]{1.0, (double) 0, -1.0 + Math.random() / 2, -1.0 + Math.random() / 2};
        }


        SimpleTSne t = new SimpleTSne();

        double[][] y = t.reset(x, new TSneConfig(
                2,
                false, true
        ));
        System.out.println(MatrixOps.doubleArrayToPrintString(y));

        Surface plot = new PaintSurface() {

            @Override
            protected void paint(GL2 gl, ReSurface surfaceRender) {
                Draw.bounds(bounds, gl, this::paint);
            }

            protected void paint(GL2 gl) {
                double[][] vv = t.Y;
                if (vv == null)
                    return;
                vv = vv.clone();


                float h;
                float w = h = 1f / (float) vv.length;
                float scale = 0.1f;
                for (int i = 0, yLength = vv.length; i < yLength; i++) {
                    double[] v = vv[i];

                    float x = (float) (v[0]);
                    float y = (float) (((v.length > 1) ? v[1] : (double) 0));

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

//        Util.sleepMS(10000);
    }

    public static class TsneRenderer implements Graph2D.Graph2DRenderer<DataTable.Instance> {

        @Override
        public void node(NodeVis<DataTable.Instance> node, Graph2D.GraphEditing<DataTable.Instance> graph) {
            node.set(new PushButton() {
                @Override
                protected void paintWidget(RectFloat bounds, GL2 gl) {

                }

                @Override
                protected void paintIt(GL2 gl, ReSurface r) {
                    paintNode(gl, this, node.id);
                }
            }.clicked(() -> System.out.println(node.id)));
        }

        protected void paintNode(GL2 gl, Surface surface, DataTable.Instance id) {
            Draw.colorHash(gl, id.hashCode(), 0.8f);
            Draw.rect(surface.bounds, gl);
        }
    }
}
