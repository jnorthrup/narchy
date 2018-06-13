package spacegraph.space2d.dyn2d;

import jcog.exe.Loop;
import jcog.learn.ql.HaiQae;
import jcog.math.FloatRange;
import jcog.math.random.XoRoShiRo128PlusRandom;
import jcog.math.tensor.Tensor;
import jcog.math.tensor.TensorLERP;
import spacegraph.SpaceGraph;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.phys.collision.shapes.CircleShape;
import spacegraph.space2d.phys.collision.shapes.PolygonShape;
import spacegraph.space2d.phys.dynamics.Body2D;
import spacegraph.space2d.phys.dynamics.BodyDef;
import spacegraph.space2d.phys.dynamics.BodyType;
import spacegraph.space2d.phys.dynamics.Fixture;
import spacegraph.space2d.phys.dynamics.joints.RevoluteJoint;
import spacegraph.space2d.phys.dynamics.joints.RevoluteJointDef;
import spacegraph.space2d.phys.explosive.Explosives;
import spacegraph.space2d.phys.fracture.materials.Uniform;
import spacegraph.space2d.phys.particle.ParticleColor;
import spacegraph.space2d.phys.particle.ParticleGroupDef;
import spacegraph.space2d.phys.particle.ParticleType;
import spacegraph.space2d.widget.meta.AutoSurface;
import spacegraph.space2d.widget.meter.AutoUpdateMatrixView;
import spacegraph.space2d.widget.slider.XYSlider;
import spacegraph.space2d.widget.text.Label;
import spacegraph.space2d.widget.text.LabeledPane;
import spacegraph.space2d.widget.windo.Dyn2DSurface;
import spacegraph.space2d.widget.windo.Port;
import spacegraph.space2d.widget.windo.TogglePort;
import spacegraph.util.math.v2;

import java.util.Random;

import static spacegraph.space2d.container.grid.Gridding.VERTICAL;
import static spacegraph.space2d.phys.dynamics.Dynamics2D.staticBox;

public class TensorGlow {

    static final Random rng = new XoRoShiRo128PlusRandom(1);


    public static void main(String[] args) {

        Dyn2DSurface p = SpaceGraph.wall(1200, 1000);

        p.W.setGravity(new v2(0, -2.8f));
        staticBox(p.W, -5, -8, +5, 2f, false, true, true, true);

        for (int j = 0; j < 3; j++)        {
            BodyDef bodyDef2 = new BodyDef();
            bodyDef2.type = BodyType.DYNAMIC;
            bodyDef2.angle = -0.6f; 
            bodyDef2.linearVelocity = new v2(0.0f, 0.0f); 
            bodyDef2.angularVelocity = 0.0f; 
            Body2D newBody = p.W.addBody(bodyDef2);
            PolygonShape shape2 = new PolygonShape();
            shape2.setAsBox(0.25f, 0.25f);
            Fixture f = newBody.addFixture(shape2, 1.0f);
            f.friction = 0.5f; 
            f.restitution = 0.0f; 
            f.material = new Uniform();
            f.material.m_rigidity = 1.0f;
        }



        
        {
            p.W.setContactListener(new Explosives.ExplosionContacts());

            TheoJansen t = new TheoJansen(p.W, 0.35f);
            Dyn2DSurface.PhyWindow pw = p.put(new Gridding(0.5f, new Port((float[] v) -> {
                
                t.motorJoint.setMotorSpeed(v[0]*2 - v[1]*2);
                t.motorJoint.setMaxMotorTorque(v[2]);
                t.motorJoint.enableLimit(true);
                t.motorJoint.setLimits((float) (-v[3]*Math.PI), (float) (+v[4]*Math.PI));
                if (v[5] > 0.5f) {
                    t.gun.fire();
                }
                t.turretJoint.setLimits((float) (+Math.PI/2 + v[6] * Math.PI -0.1f), (float) (+Math.PI/2 + v[6] * Math.PI +0.1f));
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
                    
                    
                    
                    
            pd.color = new ParticleColor(0.7f, 0.1f, 0.1f, 0.8f);
            pd.shape = shape;
            p.W.addParticles(pd);
        }

        HaiQae q = new HaiQae(8, 2);
        float[] in = new float[q.ae.inputs()];

        final Tensor randomVector = Tensor.randomVectorGauss(in.length, 0, 1, rng);
        final FloatRange lerpRate = new FloatRange(0.01f, 0, 1f);
        final TensorLERP lerpVector = new TensorLERP(randomVector, lerpRate);

        Dyn2DSurface.PhyWindow w = p.put(new Gridding(0.25f,
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
        Dyn2DSurface.PhyWindow qw = p.put(hw, 1, 1);

        Loop.of(() -> {
            lerpVector.update();
            q.act((((float) Math.random()) - 0.5f) * 2, in);
        }).runFPS(25);

    }


    public static Gridding haiQWindow(HaiQae q, float[] in) {
        return new Gridding(
                new Label("HaiQ"),
                new AutoSurface<>(q),
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
