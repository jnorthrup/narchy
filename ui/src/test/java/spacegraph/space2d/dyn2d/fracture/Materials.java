package spacegraph.space2d.dyn2d.fracture;

import spacegraph.space2d.phys.collision.shapes.CircleShape;
import spacegraph.space2d.phys.collision.shapes.PolygonShape;
import spacegraph.space2d.phys.dynamics.*;
import spacegraph.space2d.phys.fracture.Material;
import spacegraph.space2d.dyn2d.ICase;
import spacegraph.util.math.v2;

/**
 * Testovaci scenar, ktory umoznuje testovanie materialov. Podla parametra sa
 * stene v scenari nadefinuje dany material, vystreli sa do nej projektyl a skuma
 * chovanie daneho materialu.
 *
 * @author Marek Benovic
 */
public class Materials implements ICase {
    private final Material material;

    /**
     * Inicializuje testovaci scenar a definuje telesu material z parametra.
     *
     * @param material
     */
    public Materials(Material material) {
        this.material = material;
    }

    @Override
    public void init(Dynamics2D w) {
        {
            BodyDef bodyDef2 = new BodyDef();
            bodyDef2.type = BodyType.DYNAMIC;
            bodyDef2.position.set(0.0f, 10.0f); //pozicia
            Body2D newBody = w.addBody(bodyDef2);
            PolygonShape shape3 = new PolygonShape();
            shape3.setAsBox(5.0f, 10.0f);
            Fixture f = newBody.addFixture(shape3, 1.0f);
            f.friction = 0.2f; // trenie
            f.restitution = 0.0f; //odrazivost

            f.material = material;

            f.material.m_rigidity = 20.0f;
        }

        {
            BodyDef bodyDefBullet = new BodyDef();
            bodyDefBullet.type = BodyType.DYNAMIC;
            bodyDefBullet.position.set(-30.0f, 12.0f); //pozicia
            bodyDefBullet.linearVelocity = new v2(100.0f, 0.0f); // smer pohybu
            Body2D bodyBullet = w.addBody(bodyDefBullet);
            CircleShape circleShape = new CircleShape();
            circleShape.radius = 1.0f;
            Fixture fixtureBullet = bodyBullet.addFixture(circleShape, 5.0f);
            fixtureBullet.friction = 0.4f; // trenie
            fixtureBullet.restitution = 0.1f; //odrazivost
        }
    }

    @Override
    public String toString() {
        return "Material: " + material;
    }
}
