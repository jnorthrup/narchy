package spacegraph.widget.meter;

import jcog.exe.Loop;
import jcog.math.FloatRange;
import jcog.math.random.XoRoShiRo128PlusRandom;
import jcog.math.tensor.Tensor;
import jcog.math.tensor.TensorLERP;
import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.render.Draw;
import spacegraph.widget.slider.XYSlider;
import spacegraph.widget.windo.PhyWall;
import spacegraph.widget.windo.Widget;

import java.util.Random;

public class TensorGlow  {

    static final Random rng = new XoRoShiRo128PlusRandom(1);

    public static class MyMatrix extends Widget {
        final Tensor randomVector = Tensor.randomVectorGauss(512, 0, 1, rng);
        final FloatRange lerpRate = new FloatRange(0.01f, 0, 1f);
        final TensorLERP lerpVector = new TensorLERP(randomVector, lerpRate);

        BitmapMatrixView rv = BitmapMatrixView.get(
                lerpVector, 16, Draw::colorBipolar
        );

        MyMatrix() {
            super();
            content(rv);

            Loop.of(()->{
                lerpVector.update();
                rv.update();
            }).runFPS(25);
        }
    }

    public static void main(String[] args) {



        PhyWall p = PhyWall.window(1200, 1000);
        MyMatrix m = new MyMatrix();
        PhyWall.PhyWindow w = p.addWindow(m, RectFloat2D.XYWH(0, 0, 1, 1));
        w.sprout(new XYSlider().on((x,y)->{
            m.lerpRate.set(x);
        }), 0.25f);


    }
}
