package spacegraph.space2d.test.fracture;

import spacegraph.space2d.phys.collision.shapes.CircleShape;
import spacegraph.space2d.phys.collision.shapes.PolygonShape;
import spacegraph.space2d.phys.dynamics.*;
import spacegraph.space2d.phys.fracture.Material;
import spacegraph.space2d.test.ICase;
import spacegraph.util.math.v2;

/**
 * Testovaci scenar.
 *
 * @author Marek Benovic
 */
public class MainScene implements ICase {
    @Override
    public void init(Dynamics2D w) {
        {
            BodyDef bodyDef2 = new BodyDef();
            bodyDef2.type = BodyType.DYNAMIC;
            bodyDef2.position.set(0.0f, 5.0f); //pozicia
            Body2D newBody = w.addBody(bodyDef2);
            PolygonShape shape3 = new PolygonShape();
            shape3.setAsBox(5.0f, 5.0f);
            Fixture f = newBody.addFixture(shape3, 1.0f);
            f.friction = 0.2f; // trenie
            f.restitution = 0.0f; //odrazivost

            f.material = Material.UNIFORM;
            f.material.m_rigidity = 80.0f;
            f.material.m_shattering = 10.0f;
        }

        {
            BodyDef ball = new BodyDef();
            ball.type = BodyType.DYNAMIC;
            ball.position.set(-30.0f, 5.3f); //pozicia
            ball.linearVelocity = new v2(100.0f, 0.0f); // smer pohybu
            Body2D ballBody = w.addBody(ball);
            CircleShape circleShape = new CircleShape();
            circleShape.radius = 1.0f;
            Fixture fb = ballBody.addFixture(circleShape, 5.0f);
            fb.friction = 0.4f; // trenie
            fb.restitution = 0.1f; //odrazivost
        }
    }

    @Override
    public String toString() {
        return "Main scene";
    }
}
