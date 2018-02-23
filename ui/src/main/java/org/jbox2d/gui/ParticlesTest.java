package org.jbox2d.gui;

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.dynamics.Body2D;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.Dynamics2D;
import org.jbox2d.particle.ParticleGroupDef;
import org.jbox2d.particle.ParticleType;
import spacegraph.math.v2;

public class ParticlesTest implements ICase {
    @Override
    public void init(Dynamics2D m_world) {

        Body2D ground = m_world.bodies().iterator().next(); //first one
        
        {
            PolygonShape shape = new PolygonShape();
            v2[] vertices =
                    new v2[]{new v2(-40, -10), new v2(40, -10), new v2(40, 0), new v2(-40, 0)};
            shape.set(vertices, 4);
            ground.addFixture(shape, 0.0f);
        }

        {
            PolygonShape shape = new PolygonShape();
            v2[] vertices =
                    {new v2(-40, -1), new v2(-20, -1), new v2(-20, 20), new v2(-40, 30)};
            shape.set(vertices, 4);
            ground.addFixture(shape, 0.0f);
        }

        {
            PolygonShape shape = new PolygonShape();
            v2[] vertices = {new v2(20, -1), new v2(40, -1), new v2(40, 30), new v2(20, 20)};
            shape.set(vertices, 4);
            ground.addFixture(shape, 0.0f);
        }

        m_world.setParticleRadius(0.35f);
        m_world.setParticleDamping(0.2f);

        {
            CircleShape shape = new CircleShape();
            shape.center.set(0, 30);
            shape.radius = 20;
            ParticleGroupDef pd = new ParticleGroupDef();
            pd.flags = ParticleType.b2_waterParticle;
            pd.shape = shape;
            m_world.addParticles(pd);
        }

        {
            BodyDef bd = new BodyDef();
            bd.type = BodyType.DYNAMIC;
            Body2D body = m_world.addBody(bd);
            CircleShape shape = new CircleShape();
            shape.center.set(0, 80);
            shape.radius = 5;
            body.addFixture(shape, 0.5f);
        }

    }

}
