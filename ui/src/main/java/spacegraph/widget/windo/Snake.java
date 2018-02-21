package spacegraph.widget.windo;

import jcog.list.FasterList;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.dynamics.*;
import org.jbox2d.dynamics.joints.*;
import spacegraph.math.v2;

import java.util.List;

public class Snake {

    final List<Body2D> bodies, attachments;
    final List<Joint> joints;

    public Snake(Body2D start, Body2D end, int num, float eleLen /* TODO parametric */, float thick) {


        bodies = new FasterList(num);
        joints = new FasterList(num);
        attachments = new FasterList(0);

        Dynamics2D w = start.W;

        FixtureDef segment = new FixtureDef(
                PolygonShape.box(eleLen/2, thick/2), 0.1f, 0f);
        segment.filter.maskBits = 0; //no collision

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
                            segment);
                }

                RevoluteJointDef jd = new RevoluteJointDef();
                //DistanceJointDef jd = new DistanceJointDef();
                jd.collideConnected = false;

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

                if (to!=end)
                    bodies.add(to);

                from = to;
            }


        }

    }

    /** attach a body to center of one of the segments */
    public void attach(Body2D b, int segment) {
        synchronized (this) {
            Joint w = b.W.addJoint(new RevoluteJointDef(bodies.get(segment), b));
            attachments.add(b);
            joints.add(w);
        }
    }

    public void remove() {
        synchronized (this) {
            Dynamics2D world = bodies.get(0).W;

            joints.forEach(world::removeJoint);
            joints.clear();

            bodies.forEach(world::removeBody);

            attachments.forEach(world::removeBody);
            attachments.clear();

            bodies.clear();

        }
    }
}
