package org.jbox2d.gui.fracture;

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.dynamics.*;
import org.jbox2d.fracture.PolygonFixture;
import org.jbox2d.fracture.materials.Diffusion;
import org.jbox2d.gui.ICase;
import spacegraph.math.Tuple2f;
import spacegraph.math.v2;

/**
 * Testovaci scenar
 *
 * @author Marek Benovic
 */
public class StaticBody implements ICase {
    @Override
    public void init(Dynamics2D w) {
        {
            BodyDef bodyDef2 = new BodyDef();
            bodyDef2.type = BodyType.STATIC;
            bodyDef2.position.set(10.0f, 0.0f); //pozicia
            bodyDef2.linearVelocity = new v2(0.0f, 0.0f); // smer pohybu
            bodyDef2.angularVelocity = 0.0f; //rotacia (rychlost rotacie)
            Body2D newBody = w.newBody(bodyDef2);

            PolygonFixture pf = new PolygonFixture(new Tuple2f[]{
                    new v2(0.0f, 3.7f),
                    new v2(6.3f, 3.7f),
                    new v2(6.3f, 16.0f),
                    new v2(3.8f, 16.0f),
                    new v2(3.7f, 20.0f),
                    new v2(8.2f, 20.0f),
                    new v2(8.2f, 0.0f),
                    new v2(0.0f, 0.0f)
            });
            FixtureDef fd = new FixtureDef();
            fd.friction = 0.2f; // trenie
            fd.restitution = 0.0f; //odrazivost
            fd.density = 1.0f;
            fd.material = new Diffusion();
            fd.material.m_rigidity = 32.0f;
            newBody.addFixture(pf, fd);
        }

        {
            BodyDef bodyDefBullet = new BodyDef();
            bodyDefBullet.type = BodyType.DYNAMIC;
            bodyDefBullet.position.set(-20.0f, 18.0f); //pozicia
            bodyDefBullet.linearVelocity = new v2(100.0f, 0.0f); // smer pohybu
            bodyDefBullet.angularVelocity = 0.0f; //rotacia (rychlost rotacie)
            Body2D bodyBullet = w.newBody(bodyDefBullet);

            CircleShape circleShape = new CircleShape();
            circleShape.m_radius = 1.0f;
            Fixture fixtureBullet = bodyBullet.addFixture(circleShape, 2.0f);
            fixtureBullet.friction = 0.4f; // trenie
            fixtureBullet.restitution = 0.1f; //odrazivost
        }
    }

    @Override
    public String toString() {
        return "Static body";
    }
}
