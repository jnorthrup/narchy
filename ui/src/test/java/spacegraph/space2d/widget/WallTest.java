package spacegraph.space2d.widget;

import jcog.Texts;
import jcog.exe.Loop;
import jcog.learn.ql.HaiQae;
import jcog.math.FloatRange;
import jcog.random.XoRoShiRo128PlusRandom;
import jcog.signal.Tensor;
import jcog.signal.tensor.TensorLERP;
import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.Gridding;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space2d.widget.meter.AutoUpdateMatrixView;
import spacegraph.space2d.widget.slider.XYSlider;
import spacegraph.space2d.widget.text.LabeledPane;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.space2d.widget.windo.*;

import java.util.Random;

import static spacegraph.space2d.container.Gridding.HORIZONTAL;
import static spacegraph.space2d.container.Gridding.VERTICAL;

public class WallTest {

    static WiredWall newWallWindow() {
        WiredWall w = new WiredWall();
        SpaceGraph.window(
                new Bordering(w).borderSize(Bordering.S, 0.25f).south(w.debugger()), 1000, 900);
        return w;
    }

    static class TestWallDebugger1 {

        public static void main(String[] args) {

            Wall w = newWallWindow();

            w.add(new PushButton("X")).pos(RectFloat2D.XYXY(10, 10, 200, 200));
            w.add(new PushButton("Y")).pos(RectFloat2D.XYXY(50, 10, 200, 200));
            w.add(new PushButton("Z")).pos(RectFloat2D.XYXY(100, 10, 200, 200));

            //Windo ww = w.add(new PushButton("Y"), 200, 300f);
            //System.out.println(ww);

        }


    }

    static class SwitchedSignal {

        public static void main(String[] args) {

            Wall s = newWallWindow();


            Port A = new Port();
            Windo a = s.add(A).pos(RectFloat2D.Unit.transform(500, 250, 250));


            Port B = LabeledPort.generic();
            Windo b = s.add(B).pos(RectFloat2D.XYWH(+1, 0, 0.25f, 0.25f).scale(500));

            TogglePort AB = new TogglePort();
            s.add(AB).pos(RectFloat2D.XYWH(0, 0, 0.25f, 0.25f).scale(500));

            Loop.of(() -> {
                A.out(Texts.n4(Math.random()));
            }).setFPS(0.3f);
        }

    }
    public static class Box2DTest_FloatMux {

        public static void main(String[] args) {
            //WiredWall s = SpaceGraph.wall(800, 800);

            Wall s = newWallWindow();

            Surface mux = new Gridding(HORIZONTAL, new LabeledPane("->", new Gridding(VERTICAL,
                    new Port(),
                    new Port()
            )), new LabeledPane("->", new Port()));
            s.add(mux).pos(RectFloat2D.Unit.transform(250, 0, 250));

            Port A = new FloatPort(0.5f, 0, 1);
            s.add(A).pos(RectFloat2D.Unit.transform(250, 250, 250));

            Port B = new FloatPort(0.5f, 0, 1);
            s.add(B).pos(RectFloat2D.Unit.transform(250, 500, 250));

            Port Y = LabeledPort.generic();
            s.add(Y).pos(RectFloat2D.Unit.transform(250, 750, 250));

        }
    }

    static class TensorGlow {

        static final Random rng = new XoRoShiRo128PlusRandom(1);


        public static void main(String[] args) {

            WiredWall p = SpaceGraph.wall(1200, 1000);

//        p.W.setGravity(new v2(0, -2.8f));
//        staticBox(p.W, -5, -8, +5, 2f, false, true, true, true);
//
//        for (int j = 0; j < 3; j++)        {
//            BodyDef bodyDef2 = new BodyDef();
//            bodyDef2.type = BodyType.DYNAMIC;
//            bodyDef2.angle = -0.6f;
//            bodyDef2.linearVelocity = new v2(0.0f, 0.0f);
//            bodyDef2.angularVelocity = 0.0f;
//            Body2D newBody = p.W.addBody(bodyDef2);
//            PolygonShape shape2 = new PolygonShape();
//            shape2.setAsBox(0.25f, 0.25f);
//            Fixture f = newBody.addFixture(shape2, 1.0f);
//            f.friction = 0.5f;
//            f.restitution = 0.0f;
//            f.material = new Uniform();
//            f.material.m_rigidity = 1.0f;
//        }




//        {
//            p.W.setContactListener(new Explosives.ExplosionContacts());
//
//            TheoJansen t = new TheoJansen(p.W, 0.35f);
//            Dyn2DSurface.PhyWindow pw = p.put(new Gridding(0.5f, new Port((float[] v) -> {
//
//                t.motorJoint.setMotorSpeed(v[0]*2 - v[1]*2);
//                t.motorJoint.setMaxMotorTorque(v[2]);
//                t.motorJoint.enableLimit(true);
//                t.motorJoint.setLimits((float) (-v[3]*Math.PI), (float) (+v[4]*Math.PI));
//                if (v[5] > 0.5f) {
//                    t.gun.fire();
//                }
//                t.turretJoint.setLimits((float) (+Math.PI/2 + v[6] * Math.PI -0.1f), (float) (+Math.PI/2 + v[6] * Math.PI +0.1f));
//            })), 0.8f, 0.4f);
//            p.W.addJoint(new RevoluteJoint(p.W, new RevoluteJointDef(pw.body, t.chassis)));
//        }
//
//        {
//            p.W.setParticleRadius(0.05f);
//            p.W.setParticleDamping(0.1f);
//
//            CircleShape shape = new CircleShape();
//            shape.center.set(0, 10);
//            shape.radius = 2f;
//            ParticleGroupDef pd = new ParticleGroupDef();
//            pd.flags = ParticleType.
//                    b2_waterParticle;
//
//
//
//
//            pd.color = new ParticleColor(0.7f, 0.1f, 0.1f, 0.8f);
//            pd.shape = shape;
//            p.W.addParticles(pd);
//        }

            HaiQae q = new HaiQae(8, 2);
            float[] in = new float[q.ae.inputs()];

            final Tensor randomVector = Tensor.randomVectorGauss(in.length, 0, 1, rng);
            final FloatRange lerpRate = new FloatRange(0.01f, 0, 1f);
            final TensorLERP lerpVector = new TensorLERP(randomVector, lerpRate);

            WiredWall.PhyWindow w = p.put(new Gridding(0.25f,
                            new AutoUpdateMatrixView(
                                    lerpVector.data
                            ),
                            new LabeledPane("lerp", new XYSlider().on((x, y) -> {
                                lerpRate.set(x);
                            })),
                            new LabeledPane("out", new Port((x) -> {
                            }) {
                                @Override
                                public void prePaint(int dtMS) {
                                    super.prePaint(dtMS);
                                    out(lerpVector.data);
                                }
                            })),
                    0.5f, 0.5f);

            p.put(new TogglePort(), 0.25f, 0.25f);

            Gridding hw = haiQWindow(q, in);
            hw.add(new LabeledPane("input", new Port((float[] i) -> {
                System.arraycopy(i, 0, in, 0, i.length);
            })));
            WiredWall.PhyWindow qw = p.put(hw, 1, 1);

            Loop.of(() -> {
                lerpVector.update();
                q.act((((float) Math.random()) - 0.5f) * 2, in);
            }).setFPS(25);

        }


        public static Gridding haiQWindow(HaiQae q, float[] in) {
            return new Gridding(
                    new VectorLabel("HaiQ"),
                    new ObjectSurface(q),
                    new Gridding(VERTICAL,
                            new AutoUpdateMatrixView(in),
                            new AutoUpdateMatrixView(q.ae.x),
                            new AutoUpdateMatrixView(q.ae.W),
                            new AutoUpdateMatrixView(q.ae.y)
                    ),
                    new Gridding(VERTICAL,
                            new AutoUpdateMatrixView(q.q),
                            new AutoUpdateMatrixView(q.et)
                    )

            );
        }

    }

}
