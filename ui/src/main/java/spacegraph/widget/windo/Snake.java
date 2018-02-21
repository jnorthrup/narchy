package spacegraph.widget.windo;

import jcog.list.FasterList;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.dynamics.*;
import org.jbox2d.dynamics.joints.Joint;
import org.jbox2d.dynamics.joints.RevoluteJoint;
import org.jbox2d.dynamics.joints.RevoluteJointDef;
import spacegraph.math.v2;

import java.util.List;

public class Snake {

    final List<Body2D> bodies;
    final List<Joint> joints;

    public Snake(Body2D start, Body2D end, int num, float eleLen /* TODO parametric */, float thick) {


        bodies = new FasterList(num);
        joints = new FasterList(num);

        Dynamics2D w = start.W;

        FixtureDef box = new FixtureDef(
                PolygonShape.box(eleLen/2, thick/2), 0.1f, 0.1f);

        final float y = 0f;

        Body2D from = null;

        for (int i = 0; i < num; ++i) {


            if (from == null) {
                from = start;
            } else {

                Body2D to;
                if (i == num - 1) {
                    to = end;
                } else {

                    to = w.addBody(
                            new BodyDef(BodyType.DYNAMIC,
                                    new v2(i * eleLen, y)),
                            box);
                }

                RevoluteJointDef jd = new RevoluteJointDef();
                //DistanceJointDef jd = new DistanceJointDef();
                jd.collideConnected = false;
                //jd.initialize(prevBody, body, /* anchor */ new v2(i * eleLen, y));

                jd.bodyA = from;
                if (from != start) {
                    jd.localAnchorA.set(eleLen / 2, 0); //right side
                } else {
                    //bind to center of the start or end
                    jd.localAnchorA.set(0, 0);
                }
                jd.bodyB = to;
                if (to!=end) {
                    jd.localAnchorB.set(-eleLen / 2, 0); //left side
                } else {
                    //bind to center of the start or end
                    jd.localAnchorB.set(0,0);
                }
                jd.referenceAngle = 0;
                Joint jj = w.addJoint(jd);
                joints.add(jj);

                ((RevoluteJoint)jj).positionFactor = 0.1f;

                from = to;

            }


            bodies.add(from);
        }

        bodies.add(from);
    }

    public void remove(boolean includeStartEnd) {
        Dynamics2D world = bodies.get(0).W;
        for (int i = (includeStartEnd ? 0 : 1); i < bodies.size()- (includeStartEnd ? 1 : 0); i++)
            bodies.get(i).remove();
        bodies.clear();
        joints.forEach(world::removeJoint);
        joints.clear();
    }
}
