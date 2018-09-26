package spacegraph.space2d.widget;

import com.jogamp.opengl.GL2;
import jcog.exe.Loop;
import jcog.experiment.TrackXY;
import jcog.learn.ql.HaiQae;
import jcog.math.FloatRange;
import jcog.random.XoRoShiRo128PlusRandom;
import jcog.signal.Tensor;
import jcog.signal.tensor.TensorLERP;
import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.Gridding;
import spacegraph.space2d.widget.chip.SwitchChip;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space2d.widget.meter.AutoUpdateMatrixView;
import spacegraph.space2d.widget.meter.BitmapMatrixView;
import spacegraph.space2d.widget.port.Port;
import spacegraph.space2d.widget.slider.XYSlider;
import spacegraph.space2d.widget.text.LabeledPane;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.space2d.widget.windo.GraphEdit;
import spacegraph.space2d.widget.windo.Windo;
import spacegraph.video.Draw;

import java.util.Random;

import static spacegraph.space2d.container.Gridding.VERTICAL;

public class TensorGlow {

    static final Random rng = new XoRoShiRo128PlusRandom(1);


    public static void main(String[] args) {

        GraphEdit p = WallTest.newWallWindow();

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

        HaiQae q = new HaiQae(8, 4);
        float[] in = new float[q.ae.inputs()];

        final Tensor randomVector = Tensor.randomVectorGauss(in.length, 0, 1, rng);
        final FloatRange lerpRate = new FloatRange(0.01f, 0, 1f);
        final TensorLERP lerpVector = new TensorLERP(randomVector, lerpRate);

        {
            TrackXY track = new TrackXY(4, 4);
            BitmapMatrixView trackView = new BitmapMatrixView(track.W, track.H, (x, y) -> Draw.rgbInt(track.grid.brightness(x, y), 0, 0)) {
                @Override
                protected void paint(GL2 gl, SurfaceRender surfaceRender) {
                    super.paint(gl, surfaceRender);

                    RectFloat2D at = cellRect(track.cx, track.cy, 0.5f, 0.5f);
                    gl.glColor4f(0, 0, 1, 0.9f);
                    Draw.rect(at.move(x(), y(), 0.01f), gl);
                }
            };
            Loop.of(() -> {
                track.act();
                trackView.update();
            }).setFPS(10f);

            Port state = new Port() {

            };
            Port reward = new Port() {

            };
            Windo trackWin = p.add(new Bordering(trackView).set(Bordering.S, state, 0.05f).set(Bordering.E, reward, 0.05f));
            trackWin.pos(500, 500, 600, 600);

            p.sprout(trackWin, new Port((z)->{ if ((Boolean)z) track.control(-1, 0); }), 0.25f);
            p.sprout(trackWin, new Port((z)->{ if ((Boolean)z) track.control(0, -1); }), 0.25f);
            p.sprout(trackWin, new Port((z)->{ if ((Boolean)z) track.control(+1, 0); }), 0.25f);
            p.sprout(trackWin, new Port((z)->{ if ((Boolean)z) track.control(0, +1); }), 0.25f);
        }

        p.add(new Gridding(0.25f,
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
                        }))
                ).pos(100, 100, 200, 200);

        //p.add(new TogglePort()).pos(200, 200, 300, 300);

        Port outs;
        Gridding hw = new Gridding(
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
        hw.add(new LabeledPane("input", new Port((float[] i) -> {
            System.arraycopy(i, 0, in, 0, i.length);
        })));
        hw.add(new LabeledPane("act", outs = new Port()));

        p.add(hw).pos(350, 350, 500, 500);

        Loop.of(() -> {
            lerpVector.update();
            int a = q.act((((float) Math.random()) - 0.5f) * 2, in);
            outs.out(a);
//            int n = outs.size();
//            for (int i = 0; i < n; i++) {
//                outs.out(i, (i == a));
//            }
        }).setFPS(25);

        SwitchChip outDemultiplexer = new SwitchChip (4);
        p.add(outDemultiplexer).pos(450, 450, 510, 510);
    }


}
