package spacegraph.widget.meter;

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
import org.jbox2d.common.MathUtils;
import org.jbox2d.dynamics.*;
import org.jbox2d.dynamics.joints.*;
import spacegraph.layout.Gridding;
import spacegraph.math.Tuple2f;
import spacegraph.math.v2;
import spacegraph.render.Draw;
import spacegraph.widget.meta.AutoSurface;
import spacegraph.widget.slider.XYSlider;
import spacegraph.widget.text.Label;
import spacegraph.widget.text.LabeledPane;
import spacegraph.widget.windo.PhyWall;
import spacegraph.widget.windo.Port;

import java.util.Random;
import java.util.function.Consumer;

import static java.lang.Math.abs;
import static org.jbox2d.dynamics.BodyType.DYNAMIC;
import static spacegraph.layout.Gridding.VERTICAL;

public class TensorGlow {

    static final Random rng = new XoRoShiRo128PlusRandom(1);

    private static void setBox(Dynamics2D world, float x1, float y1, float x2, float y2) {
        float thick = 0.25f;
        float cx = (x1+x2)/2f;
        float cy = (y1+y2)/2f;
        float w = x2 - x1;
        float h = y2 - y1;
        Body2D ground = world.addBody(new BodyDef(BodyType.STATIC),
                new FixtureDef(PolygonShape.box(w/2, thick/2),
                        0, 0));
        ground.setTransform(new v2(cx, y1), 0);

        Body2D top = world.addBody(new BodyDef(BodyType.STATIC),
                new FixtureDef(PolygonShape.box(w/2, thick/2),
                        0, 0));
        top.setTransform(new v2(cx, y2), 0);

        Body2D left = world.addBody(new BodyDef(BodyType.STATIC),
                new FixtureDef(PolygonShape.box(thick/2, h/2),
                        0, 0));
        left.setTransform(new v2(x1, cy), 0);

        Body2D right = world.addBody(new BodyDef(BodyType.STATIC),
                new FixtureDef(PolygonShape.box(thick/2, h/2),
                        0, 0));
        right.setTransform(new v2(x2, cy), 0);

//        Body2D wallRight = w.addBody(new BodyDef(BodyType.STATIC),
//                new FixtureDef(PolygonShape.box(0.1f, 5), 0, 0));
//        wallRight.setTransform(new v2(-41, 30.0f), 0);
//
//        Body2D wallLeft = w.addBody(new BodyDef(BodyType.STATIC),
//                new FixtureDef(PolygonShape.box(0.1f, 5), 0, 0));
//        wallLeft.setTransform(new v2(41, 30.0f), 0);
    }


    public static class TheoJansen {

        private final Dynamics2D world;
        private final v2 center;

        private final boolean m_motorOn;
        private final Body2D wheel;
        private final Body2D chassis;
        public final RevoluteJoint motorJoint;

        public TheoJansen(Dynamics2D w, v2 offset, float scale) {

            this.world = w;



            this.center = offset;
            center.set(0,0);
            m_motorOn = true;

            v2 pivot = new v2(scale * 0.0f, scale * 0.8f);


//            // Balls
//            for (int i = 0; i < 40; ++i) {
//                CircleShape shape = new CircleShape();
//                shape.m_radius = scale * 0.25f;
//
//                BodyDef bd = new BodyDef();
//                bd.type = BodyType.DYNAMIC;
//                bd.position.set(-40.0f + 2.0f * i, 0.5f);
//
//                Body2D body = world.addBody(bd);
//                body.addFixture(shape, 1.0f);
//            }

            // Chassis
            {
                PolygonShape shape = new PolygonShape();
                shape.setAsBox(scale * 2.5f, scale * 1.0f);

                FixtureDef sd = new FixtureDef();
                sd.density = 1.0f;
                sd.shape = shape;
                sd.filter.groupIndex = -1;
                BodyDef bd = new BodyDef();
                bd.type = BodyType.DYNAMIC;
                bd.position.set(pivot).added(center);
                chassis = world.addBody(bd);
                chassis.addFixture(sd);
            }

            {
                CircleShape shape = new CircleShape();
                shape.radius = scale * 1.6f;

                FixtureDef sd = new FixtureDef();
                sd.density = 1.0f;
                sd.shape = shape;
                sd.filter.groupIndex = -1;
                BodyDef bd = new BodyDef();
                bd.type = BodyType.DYNAMIC;
                bd.position.set(pivot).added(center);
                wheel = world.addBody(bd);
                wheel.addFixture(sd);
            }

            {
                RevoluteJointDef jd = new RevoluteJointDef();

                jd.initialize(wheel, chassis, pivot.add(center));
                jd.collideConnected = false;
                jd.motorSpeed = 0;
                jd.maxMotorTorque = 40.0f;
                jd.enableMotor = m_motorOn;
                motorJoint = (RevoluteJoint) world.addJoint(jd);
            }

            v2 wheelAnchor;

            wheelAnchor = pivot.add(new v2(scale*0.0f, scale * -0.8f));

            createLeg(-scale, wheelAnchor);
            createLeg(scale, wheelAnchor);

            wheel.setTransform(wheel.getPosition(), 120.0f * MathUtils.PI / 180.0f);
            createLeg(-scale, wheelAnchor);
            createLeg(scale, wheelAnchor);

            wheel.setTransform(wheel.getPosition(), -120.0f * MathUtils.PI / 180.0f);
            createLeg(-scale, wheelAnchor);
            createLeg(scale, wheelAnchor);
        }

        void createLeg(float s, v2 wheelAnchor) {
            v2 p1 = new v2(5.4f * s, -6.1f * abs(s));
            v2 p2 = new v2(7.2f * s, -1.2f * abs(s));
            v2 p3 = new v2(4.3f * s, -1.9f * abs(s));
            v2 p4 = new v2(3.1f * s, 0.8f * abs(s));
            v2 p5 = new v2(6.0f * s, 1.5f * abs(s));
            v2 p6 = new v2(2.5f * s, 3.7f * abs(s));

            FixtureDef fd1 = new FixtureDef();
            FixtureDef fd2 = new FixtureDef();
            fd1.filter.groupIndex = -1;
            fd2.filter.groupIndex = -1;
            fd1.density = 1.0f;
            fd2.density = 1.0f;

            PolygonShape poly1 = new PolygonShape();
            PolygonShape poly2 = new PolygonShape();

            if (s > 0.0f) {
                v2[] vertices = new v2[3];

                vertices[0] = p1;
                vertices[1] = p2;
                vertices[2] = p3;
                poly1.set(vertices, 3);

                vertices[0] = new v2();
                vertices[1] = p5.sub(p4);
                vertices[2] = p6.sub(p4);
                poly2.set(vertices, 3);
            } else {
                v2[] vertices = new v2[3];

                vertices[0] = p1;
                vertices[1] = p3;
                vertices[2] = p2;
                poly1.set(vertices, 3);

                vertices[0] = new v2();
                vertices[1] = p6.sub(p4);
                vertices[2] = p5.sub(p4);
                poly2.set(vertices, 3);
            }

            fd1.shape = poly1;
            fd2.shape = poly2;

            BodyDef bd1 = new BodyDef(), bd2 = new BodyDef();
            bd1.type = BodyType.DYNAMIC;
            bd2.type = BodyType.DYNAMIC;
            bd1.position = center;
            bd2.position = p4.add(center);

            bd1.angularDamping = 10.0f;
            bd2.angularDamping = 10.0f;

            Body2D body1 = world.addBody(bd1);
            Body2D body2 = world.addBody(bd2);

            body1.addFixture(fd1);
            body2.addFixture(fd2);

            DistanceJointDef djd = new DistanceJointDef();

            // Using a soft distance constraint can reduce some jitter.
            // It also makes the structure seem a bit more fluid by
            // acting like a suspension system.
            djd.dampingRatio = 0.5f;
            djd.frequencyHz = 10.0f;

            djd.initialize(body1, body2, p2.add(center), p5.add(center));
            world.addJoint(djd);

            djd.initialize(body1, body2, p3.add(center), p4.add(center));
            world.addJoint(djd);

            djd.initialize(body1, wheel, p3.add(center), wheelAnchor.add(center));
            world.addJoint(djd);

            djd.initialize(body2, wheel, p6.add(center), wheelAnchor.add(center));
            world.addJoint(djd);

            RevoluteJointDef rjd = new RevoluteJointDef();

            rjd.initialize(body2, chassis, p4.add(center));
            world.addJoint(rjd);
        }
    }
    public static class Pacman extends Body2D implements Consumer<GL2> {

        public Pacman(Dynamics2D world) {
            super(new BodyDef(DYNAMIC), world);

            addFixture(new FixtureDef(
                    PolygonShape.regular(9, 0.24f),
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
            Draw.rect(gl, center.x + 0.01f * (float) Math.cos(a), center.y + 0.01f * (float) Math.sin(a), 0.25f, 0.25f);

        }

        @Override
        public void preUpdate() {
            //applyForceToCenter(new v2(rng.nextFloat()*0.01f,rng.nextFloat()*0.01f));
        }
    }

    public static void main(String[] args) {

        PhyWall p = PhyWall.window(1200, 1000);

        p.W.setGravity(new v2(0, -2.8f));
        setBox(p.W, -5, -1, +5, 10f);

        //new Pacman(p.W);
        {
            TheoJansen t = new TheoJansen(p.W, new v2(0, 4), 0.1f);
            PhyWall.PhyWindow pw = p.addWindow(new Gridding(0.25f,new Port((float[] v)->{
                //System.out.println(v);
                t.motorJoint.setMotorSpeed(v[0]);
            })), RectFloat2D.XYWH(0, 0, 0.2f, 0.1f));
            p.W.addJoint(new WeldJoint(p.W, new WeldJointDef(pw.body, t.chassis)));
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
