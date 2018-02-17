package org.jbox2d.gui.fracture;

import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.dynamics.*;
import org.jbox2d.fracture.materials.Diffusion;
import org.jbox2d.gui.ICase;
import spacegraph.math.v2;

/**
 * Testovaci scenar
 *
 * @author Marek Benovic
 */
public class RotatedBody implements ICase {
    @Override
    public void init(Dynamics2D w) {
        {
            BodyDef bodyDef2 = new BodyDef();
            bodyDef2.type = BodyType.DYNAMIC;
            bodyDef2.position.set(10.0f, 20.0f); //pozicia
            bodyDef2.linearVelocity = new v2(0.0f, 0.0f); // smer pohybu
            bodyDef2.angularVelocity = 10.0f; //rotacia (rychlost rotacie)
            Body newBody = w.addBody(bodyDef2);
            PolygonShape shape3 = new PolygonShape();
            shape3.setAsBox(1.0f, 10.0f);

            Fixture f = newBody.addFixture(shape3, 1.0f);
            f.m_friction = 0.2f; // trenie
            f.m_restitution = 0.0f; //odrazivost
            f.m_material = new Diffusion();
            f.m_material.m_rigidity = 32.0f;
        }
    }

    @Override
    public String toString() {
        return "Rotated body";
    }
}
