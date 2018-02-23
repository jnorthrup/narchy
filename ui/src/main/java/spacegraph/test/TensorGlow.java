package spacegraph.test;

import com.jogamp.opengl.GL2;
import jcog.exe.Loop;
import jcog.learn.ql.HaiQae;
import jcog.math.FloatRange;
import jcog.math.random.XoRoShiRo128PlusRandom;
import jcog.math.tensor.Tensor;
import jcog.math.tensor.TensorLERP;
import jcog.tree.rtree.rect.RectFloat2D;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.dynamics.*;
import org.jbox2d.dynamics.joints.RevoluteJoint;
import org.jbox2d.dynamics.joints.RevoluteJointDef;
import org.jbox2d.fracture.materials.Uniform;
import org.jbox2d.particle.ParticleColor;
import org.jbox2d.particle.ParticleGroupDef;
import org.jbox2d.particle.ParticleType;
import spacegraph.layout.Gridding;
import spacegraph.math.v2;
import spacegraph.widget.meta.AutoSurface;
import spacegraph.widget.meter.BitmapMatrixView;
import spacegraph.widget.slider.XYSlider;
import spacegraph.widget.text.Label;
import spacegraph.widget.text.LabeledPane;
import spacegraph.widget.windo.PhyWall;
import spacegraph.widget.windo.Port;
import spacegraph.widget.windo.TogglePort;

import java.util.Random;

import static spacegraph.layout.Gridding.VERTICAL;

public class TensorGlow {

    static final Random rng = new XoRoShiRo128PlusRandom(1);

    private static void addBox(Dynamics2D world, float x1, float y1, float x2, float y2) {
        addBox(world, x1, y1, x2, y2, true, true, true, true);
    }

    private static void addBox(Dynamics2D world, float x1, float y1, float x2, float y2, boolean top, boolean right, boolean bottom, boolean left) {

        float cx = (x1 + x2) / 2f;
        float cy = (y1 + y2) / 2f;
        float w = x2 - x1;
        float h = y2 - y1;

        float thick = Math.min(w, h) / 20f;

        if (bottom) {
            Body2D _bottom = world.addBody(new BodyDef(BodyType.STATIC),
                    new FixtureDef(PolygonShape.box(w / 2, thick / 2),
                            0, 0));
            _bottom.setTransform(new v2(cx, y1), 0);
        }

        if (top) {
            Body2D _top = world.addBody(new BodyDef(BodyType.STATIC),
                    new FixtureDef(PolygonShape.box(w / 2, thick / 2),
                            0, 0));
            _top.setTransform(new v2(cx, y2), 0);
        }

        if (left) {
            Body2D _left = world.addBody(new BodyDef(BodyType.STATIC),
                    new FixtureDef(PolygonShape.box(thick / 2, h / 2 - thick / 2),
                            0, 0));
            _left.setTransform(new v2(x1, cy), 0);

        }

        if (right) {
            Body2D _right = world.addBody(new BodyDef(BodyType.STATIC),
                    new FixtureDef(PolygonShape.box(thick / 2, h / 2 - thick / 2),
                            0, 0));
            _right.setTransform(new v2(x2, cy), 0);
        }


//        Body2D wallRight = w.addBody(new BodyDef(BodyType.STATIC),
//                new FixtureDef(PolygonShape.box(0.1f, 5), 0, 0));
//        wallRight.setTransform(new v2(-41, 30.0f), 0);
//
//        Body2D wallLeft = w.addBody(new BodyDef(BodyType.STATIC),
//                new FixtureDef(PolygonShape.box(0.1f, 5), 0, 0));
//        wallLeft.setTransform(new v2(41, 30.0f), 0);
    }


    public static void main(String[] args) {

        PhyWall p = PhyWall.window(1200, 1000);

        p.W.setGravity(new v2(0, -2.8f));
        addBox(p.W, -10, -8, +10, 2f, false, true, true, true);

        for (int j = 0; j < 3; j++)        {
            BodyDef bodyDef2 = new BodyDef();
            bodyDef2.type = BodyType.DYNAMIC;
            bodyDef2.angle = -0.6f; // otocenie
            bodyDef2.linearVelocity = new v2(0.0f, 0.0f); // smer pohybu
            bodyDef2.angularVelocity = 0.0f; //rotacia (rychlost rotacie)
            Body2D newBody = p.W.addBody(bodyDef2);
            PolygonShape shape2 = new PolygonShape();
            shape2.setAsBox(0.25f, 0.25f);
            Fixture f = newBody.addFixture(shape2, 1.0f);
            f.friction = 0.5f; // trenie
            f.restitution = 0.0f; //odrazivost
            f.material = new Uniform();
            f.material.m_rigidity = 1.0f;
        }
//        //ceiling rack
//        addBox(p.W, -1, +0.4f, 0, +0.65f, false, true, true, true);

        //new Pacman(p.W);
        {
            TheoJansen t = new TheoJansen(p.W, 0.25f);
            PhyWall.PhyWindow pw = p.addWindow(new Gridding(0.5f, new Port((float[] v) -> {
                //System.out.println(v);
                t.motorJoint.setMotorSpeed(v[0]*2 - v[1]*2);
                t.motorJoint.setMaxMotorTorque(v[2]);
                t.motorJoint.enableLimit(true);
                t.motorJoint.setLimits((float) (-v[3]*Math.PI), (float) (+v[4]*Math.PI));
            })), 0.8f, 0.4f);
            p.W.addJoint(new RevoluteJoint(p.W, new RevoluteJointDef(pw.body, t.chassis)));
        }

        {
            p.W.setParticleRadius(0.05f);
            p.W.setParticleDamping(0.1f);

            CircleShape shape = new CircleShape();
            shape.center.set(0, 10);
            shape.radius = 2f;
            ParticleGroupDef pd = new ParticleGroupDef();
            pd.flags = ParticleType.
                    b2_waterParticle;
                    //b2_viscousParticle;
                    //b2_elasticParticle;
                    //b2_springParticle;
                    //b2_powderParticle;
            pd.color = new ParticleColor(0.7f, 0.1f, 0.1f, 0.8f);
            pd.shape = shape;
            p.W.addParticles(pd);
        }

        HaiQae q = new HaiQae(8, 2);
        float[] in = new float[q.ae.inputs()];

        final Tensor randomVector = Tensor.randomVectorGauss(in.length, 0, 1, rng);
        final FloatRange lerpRate = new FloatRange(0.01f, 0, 1f);
        final TensorLERP lerpVector = new TensorLERP(randomVector, lerpRate);

        PhyWall.PhyWindow w = p.addWindow(new Gridding(0.25f,
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
                RectFloat2D.XYWH(0, 0, 0.5f, 0.5f));

        p.addWindow(new TogglePort(), 0.25f, 0.25f);

        PhyWall.PhyWindow qw = p.addWindow(
                new Gridding(
                        new Label("HaiQ"),
                        new AutoSurface<>(q),
                        new LabeledPane("input", new Port((float[] i) -> {
                            System.arraycopy(i, 0, in, 0, i.length);
                        })),
                        new Gridding(VERTICAL,
                                new AutoUpdateMatrixView(in),
                                new AutoUpdateMatrixView(q.ae.xx),
                                new AutoUpdateMatrixView(q.ae.W),
                                new AutoUpdateMatrixView(q.ae.y)
                        ),
                        new Gridding(VERTICAL,
                                new AutoUpdateMatrixView(q.q),
                                new AutoUpdateMatrixView(q.et)
                        )

                ),
                RectFloat2D.XYWH(1, 1, 1, 1));

        Loop.of(() -> {
            lerpVector.update();
            q.act((((float) Math.random()) - 0.5f) * 2, in);
        }).runFPS(25);

    }

    private static class AutoUpdateMatrixView extends BitmapMatrixView {
        public AutoUpdateMatrixView(float[] x) {
            super(x);
        }

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
