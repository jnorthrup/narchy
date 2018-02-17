package spacegraph.widget.meter;

import jcog.math.random.XoRoShiRo128PlusRandom;
import jcog.math.tensor.BufferedTensor;
import jcog.math.tensor.Tensor;
import jcog.math.tensor.TensorLERP;
import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.SurfaceBase;
import spacegraph.render.Draw;
import spacegraph.render.SpaceGraphFlat;
import spacegraph.widget.windo.PhyWall;

import java.util.Random;

public class TensorGlow extends PhyWall {

    final Random rng = new XoRoShiRo128PlusRandom(1);

    final BufferedTensor randomVector =
            new TensorLERP(
                Tensor.randomVectorGauss(512, 0, 1, rng),
            0.01f).buffered();
    private BitmapMatrixView rngView;

    public TensorGlow() {

    }

    @Override
    public void start(SurfaceBase parent) {
        super.start(parent);


        addWindo(rngView = BitmapMatrixView.get(
            randomVector, 16, Draw::colorBipolar
        ), RectFloat2D.XYWH(250, 250, 400, 400));

        //addWindo(WidgetTest.widgetDemo(), RectFloat2D.XYWH(+100, 0, 100, 100));

    }

    @Override
    protected void prePaint(int dtMS) {
        super.prePaint(dtMS);

        randomVector.update();
        if (rngView!=null)
            rngView.update();
    }

    public static void main(String[] args) {
        SpaceGraphFlat.window(new TensorGlow(), 1200, 1000);
    }
}
