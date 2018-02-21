package spacegraph.widget.meter;

import com.jogamp.opengl.GL2;
import jcog.exe.Loop;
import jcog.learn.ql.HaiQ;
import jcog.math.FloatRange;
import jcog.math.random.XoRoShiRo128PlusRandom;
import jcog.math.tensor.Tensor;
import jcog.math.tensor.TensorLERP;
import jcog.tree.rtree.rect.RectFloat2D;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.dynamics.*;
import spacegraph.layout.Gridding;
import spacegraph.math.Tuple2f;
import spacegraph.render.Draw;
import spacegraph.widget.meta.AutoSurface;
import spacegraph.widget.slider.XYSlider;
import spacegraph.widget.text.Label;
import spacegraph.widget.text.LabeledPane;
import spacegraph.widget.windo.PhyWall;
import spacegraph.widget.windo.Port;
import spacegraph.widget.windo.Widget;

import java.util.Random;
import java.util.function.Consumer;

public class TensorGlow {

    static final Random rng = new XoRoShiRo128PlusRandom(1);

    public static class MyMatrix extends Widget {
        final Tensor randomVector = Tensor.randomVectorGauss(8, 0, 1, rng);
        final FloatRange lerpRate = new FloatRange(0.01f, 0, 1f);
        final TensorLERP lerpVector = new TensorLERP(randomVector, lerpRate);

        BitmapMatrixView rv = BitmapMatrixView.get(
                lerpVector, 2, Draw::colorBipolar
        );

        MyMatrix() {
            super();
            content(rv);
        }
    }

    public static class Pacman extends Body2D implements Consumer<GL2> {

        public Pacman(Dynamics2D world) {
            super(new BodyDef(BodyType.DYNAMIC), world);

            addFixture(new FixtureDef(
                    PolygonShape.regular(9,0.24f),
                    0.5f, 0.2f));

            world.addBody(this);
        }

        @Override
        public void accept(GL2 gl) {
            gl.glColor3f(1, 1, 0);
            Draw.drawPoly(this, gl, (PolygonShape) fixtures.shape);


            float a = angle();
            gl.glColor3f(0, 0, 0);
            Tuple2f center = getWorldCenter();
            Draw.rect(gl,center.x + 0.01f * (float)Math.cos(a),center.y + 0.01f * (float)Math.sin(a),0.25f,0.25f);

        }

        @Override
        public void preUpdate() {
            //applyForceToCenter(new v2(rng.nextFloat()*0.01f,rng.nextFloat()*0.01f));
        }
    }

    public static void main(String[] args) {

        PhyWall p = PhyWall.window(1200, 1000);


        new Pacman(p.W);

        MyMatrix m = new MyMatrix();
        PhyWall.PhyWindow w = p.addWindow(new Gridding(0.25f,
                m,
                new LabeledPane("lerp", new XYSlider().on((x, y) -> {
                    m.lerpRate.set(x);
                })),
                new LabeledPane("out", new Port((x) -> { }) {
                    @Override
                    public void prePaint(int dtMS) {
                        super.prePaint(dtMS);
                        out(m.lerpVector.data);
                    }
                })),
                RectFloat2D.XYWH(0, 0, 0.5f, 0.5f));

        Loop.of(() -> {
            m.lerpVector.update();
            m.rv.update();
        }).runFPS(25);


        HaiQ q = new HaiQ(8, 2);
        float[] in = new float[q.inputs];
        PhyWall.PhyWindow qw = p.addWindow(
                new Gridding(
                        new Label("HaiQ"),
                        new AutoSurface<>(q),
                        new LabeledPane("input", new Port((float[] i) -> {
                            System.arraycopy(i, 0, in, 0, i.length);
                        })),
                        new AutoUpdateMatrixView(q.q),
                        new AutoUpdateMatrixView(q.et)
                ),
                RectFloat2D.XYWH(1, 1, 1, 1));

        Loop.of(() -> {
            q.act((((float) Math.random()) - 0.5f) * 2, in);
        }).runFPS(25);

    }

    private static class AutoUpdateMatrixView extends BitmapMatrixView {
        public AutoUpdateMatrixView(float[][] x) {
            super(x);
        }

        @Override
        protected void paint(GL2 gl, int dtMS) {
            update();
            super.paint(gl, dtMS);
        }
    }
}
