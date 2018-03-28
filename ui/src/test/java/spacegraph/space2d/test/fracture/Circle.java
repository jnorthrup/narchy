package spacegraph.space2d.test.fracture;

import spacegraph.space2d.phys.collision.shapes.CircleShape;
import spacegraph.space2d.phys.dynamics.*;
import spacegraph.space2d.phys.fracture.materials.Diffusion;
import spacegraph.space2d.test.ICase;
import spacegraph.util.math.v2;

/**
 * Testovaci scenar
 *
 * @author Marek Benovic
 */
public class Circle implements ICase {
    @Override
    public void init(Dynamics2D w) {
        {
            BodyDef bodyDef2 = new BodyDef();
            bodyDef2.type = BodyType.DYNAMIC;
            bodyDef2.position = new v2(10.0f, 10.0f);
            bodyDef2.angle = -0.6f; // otocenie
            bodyDef2.linearVelocity = new v2(0.0f, 0.0f); // smer pohybu
            bodyDef2.angularVelocity = 0.0f; //rotacia (rychlost rotacie)
            Body2D newBody = w.addBody(bodyDef2);
            CircleShape shape2 = new CircleShape();
            shape2.radius = 2.5f;
            Fixture f = newBody.addFixture(shape2, 1.0f);
            f.friction = 0.5f; // trenie
            f.restitution = 0.0f; //odrazivost
            f.material = new Diffusion();
            f.material.m_rigidity = 8.0f;
        }
    }

    @Override
    public String toString() {
        return "Circle";
    }
}
