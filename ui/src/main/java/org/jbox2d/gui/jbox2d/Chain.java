package org.jbox2d.gui.jbox2d;

import org.jbox2d.collision.shapes.EdgeShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.dynamics.*;
import org.jbox2d.dynamics.joints.RevoluteJointDef;
import org.jbox2d.gui.ICase;
import spacegraph.math.v2;

/**
 * @author Daniel Murphy
 */
public class Chain implements ICase {

    @Override
    public void init(Dynamics2D w) {


        Body2D ground = null;
        {
            ground = w.addBody(new BodyDef());

            EdgeShape shape = new EdgeShape();
            shape.set(new v2(-40.0f, 0.0f), new v2(40.0f, 0.0f));
            ground.addFixture(shape, 0.0f);
        }

        {

            FixtureDef box = new FixtureDef(
                    PolygonShape.box(0.6f, 0.125f), 20f, 0.2f);

            RevoluteJointDef jd = new RevoluteJointDef();
            jd.collideConnected = false;

            final float y = 25.0f;

            Body2D prevBody = ground;

            for (int i = 0; i < 30; ++i) {

                Body2D body = w.addBody(
                        new BodyDef(BodyType.DYNAMIC,
                                    new v2(0.5f + i, y) ),
                        box);

                w.addJoint(jd.initialize(prevBody, body, /* anchor */ new v2(i, y)));

                prevBody = body;
            }
        }
    }


}