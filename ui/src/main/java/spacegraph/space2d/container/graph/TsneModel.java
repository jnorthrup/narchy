package spacegraph.space2d.container.graph;

import com.jujutsu.tsne.SimpleTSne;
import com.jujutsu.tsne.TSneConfig;
import jcog.data.set.ArrayHashSet;
import jcog.data.set.ArraySet;
import jcog.math.FloatRange;
import jcog.math.IntRange;
import jcog.table.DataTable;

//TODO maxGain parameter for SimpleTsne
//TODO dynamic perplexity control
public class TsneModel implements Graph2D.Graph2DUpdater<DataTable.Instance> {

    final SimpleTSne s = new SimpleTSne();

    public final IntRange iters = new IntRange(1, 0, 6);

    /** TODO autonormalize to visible range, or atleast an option for this */
    @Deprecated public final FloatRange spaceScale = new FloatRange(0.05f, 0.001f, 0.5f);
    public final FloatRange nodeScale = new FloatRange(1f, 0, 10);
    public final FloatRange perplexity = s.perplexity;
    public final FloatRange momentum = s.momentum;

    final int firstCol, lastCol;

    // = i.id.data.size()
    public TsneModel(int firstCol, int lastCol) {
        this.firstCol =firstCol;
        this.lastCol = lastCol;
    }

    final TSneConfig config = new TSneConfig(
             2,
            false, true
    );

    final ArraySet<NodeVis<DataTable.Instance>> xx = new ArrayHashSet<>();
    final ArrayHashSet<NodeVis<DataTable.Instance>> nn = new ArrayHashSet<>();
    double[][] X = new double[0][0];

    @Override public void update(Graph2D<DataTable.Instance> g, float dtS) {

        int iters = this.iters.getAsInt();
        if (iters < 1)
            return;


        nn.clear();
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
                for (NodeVis<DataTable.Instance> i : xx) {
                    X[j++] = i.id.toDoubleArray(firstCol, lastCol);
                }
            } else {
                X = new double[0][0];
            }

            s.reset(X, config);
        }


        double gcx = g.cx(), gcy = g.cy();

        double[][] Y = s.next(iters);
        int j = 0;

        int n = X.length;
        double magnify = spaceScale.floatValue() * g.radius(); ///Math.sqrt(n+1);// * g.radius();// / (float) Math.sqrt(n+1);
        float nodeScale = this.nodeScale.floatValue();
        for (NodeVis<DataTable.Instance> i : xx) {
            double[] Yj = Y[j];
            double x = (
                            Yj[0]
                    )*magnify;
            double y = (
                            Yj[1]
                    )*magnify;

//                double z = (Yj[2] = Util.clamp(Yj[2], -0.5, +0.5)); //narrow but room to maneuver
//                s.gains[j][2] = 0; //clear z gains
//                //Arrays.fill(s.gains[j], 0.0); //reset gains


            float w = 10+nodeScale *
                    (((Number)xx.get(j).id.data.get(0)).floatValue() )*0.01f; //customized: first column as size TODO normalize

            i.posXYWH((float)(gcx+x), (float)(gcy+y), w, w);
            i.pos(i.bounds.clamp(g.bounds));
//                Yj[0] = i.left()/scale;
//                Yj[1] = i.top()/scale;
            j++;
        }

    }

}
