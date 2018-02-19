package spacegraph.widget.meter;

import jcog.exe.Loop;
import jcog.math.random.XoRoShiRo128PlusRandom;
import jcog.math.tensor.BufferedTensor;
import jcog.math.tensor.Tensor;
import jcog.math.tensor.TensorLERP;
import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.render.Draw;
import spacegraph.widget.windo.PhyWall;
import spacegraph.widget.windo.Widget;

import java.util.Random;

public class TensorGlow  {

    static final Random rng = new XoRoShiRo128PlusRandom(1);

    public static class MyMatrix extends Widget {
        final BufferedTensor randomVector =
                new TensorLERP(
                        Tensor.randomVectorGauss(512, 0, 1, rng),
                        0.01f).buffered();

        BitmapMatrixView rv = BitmapMatrixView.get(
                randomVector, 16, Draw::colorBipolar
        );

        MyMatrix() {
            super();
            children(rv);

            Loop.of(()->{
                randomVector.update();
                rv.update();
            }).runFPS(25);
        }
    }

    public static void main(String[] args) {



        PhyWall p = PhyWall.window(1200, 1000);
        p.newWindow(new MyMatrix(), RectFloat2D.XYWH(0, 0, 1, 1));
    }
}
