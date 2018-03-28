package spacegraph.space2d.test.fracture;

import spacegraph.space2d.phys.collision.shapes.CircleShape;
import spacegraph.space2d.phys.collision.shapes.PolygonShape;
import spacegraph.space2d.phys.dynamics.*;
import spacegraph.space2d.phys.fracture.PolygonFixture;
import spacegraph.space2d.phys.fracture.materials.Diffusion;
import spacegraph.space2d.phys.particle.ParticleGroupDef;
import spacegraph.space2d.test.ICase;
import spacegraph.util.math.Tuple2f;
import spacegraph.util.math.v2;

/**
 * Testovaci scenar
 *
 * @author Marek Benovic
 */
public class Fluid implements ICase {
    @Override
    public void init(Dynamics2D w) {
        {
            BodyDef bodyDef2 = new BodyDef();
            bodyDef2.type = BodyType.DYNAMIC;
            bodyDef2.position.set(0.0f, 0.0f); //pozicia
            bodyDef2.angle = 0.0f; //otocenie
            Body2D newBody = w.addBody(bodyDef2);

            FixtureDef fd = new FixtureDef();
            fd.friction = 0.5f; // trenie
            fd.restitution = 0.4f; //odrazivost
            fd.density = 1.0f;
            fd.material = new Diffusion();
            fd.material.m_rigidity = 30.0f;

            PolygonFixture polygon = new PolygonFixture(
                    new Tuple2f[]{
                            new v2(0.0f, 10.0f),
                            new v2(1.0f, 10.0f),
                            new v2(1.0f, 5.0f),
                            new v2(9.0f, 5.0f),
                            new v2(9.0f, 10.0f),
                            new v2(10.0f, 10.0f),
                            new v2(10.0f, 0.0f),
                            new v2(0.0f, 0.0f)
                    }
            );
            newBody.addFixture(polygon, fd);
        }

        {
            w.setParticleRadius(0.08f);

            ParticleGroupDef pgf = new ParticleGroupDef();
            pgf.position.x = 5.0f;
            pgf.position.y = 7.6f;

            PolygonShape shapeParticles = new PolygonShape();
            shapeParticles.setAsBox(4.0f, 2.5f);
            pgf.shape = shapeParticles;
            pgf.linearVelocity.y = 2.0f;

            w.addParticles(pgf);
        }

        {
            BodyDef bodyDefBullet = new BodyDef();
            bodyDefBullet.type = BodyType.DYNAMIC;
            bodyDefBullet.position.set(30.0f, 7.0f); //pozicia
            bodyDefBullet.linearVelocity = new v2(-30.0f, 0.0f); // smer pohybu
            bodyDefBullet.angularVelocity = 0.0f; //rotacia (rychlost rotacie)
            bodyDefBullet.bullet = true;
            Body2D bodyBullet = w.addBody(bodyDefBullet);
            CircleShape circleShape = new CircleShape();
            circleShape.radius = 0.5f;
            Fixture fixtureBullet = bodyBullet.addFixture(circleShape, 10.0f);
            fixtureBullet.friction = 0.4f; // trenie
            fixtureBullet.restitution = 0.1f; //odrazivost
        }
    }

    @Override
    public String toString() {
        return "Fluid";
    }
}
