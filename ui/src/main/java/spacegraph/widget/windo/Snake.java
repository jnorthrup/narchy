package spacegraph.widget.windo;

import jcog.list.FasterList;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.dynamics.*;
import org.jbox2d.dynamics.joints.Joint;
import org.jbox2d.dynamics.joints.RevoluteJoint;
import org.jbox2d.dynamics.joints.RevoluteJointDef;
import spacegraph.Surface;
import spacegraph.math.v2;

import java.util.List;

public class Snake {

    final List<Body2D> bodies, attachments;
    final List<Joint> joints;
    private final Surface source;
    private final Surface target;
    private final Body2D sourceBody, targetBody;

    public Snake(Surface source, Surface target, int num, float eleLen /* TODO parametric */, float thick) {

        this.source = source;
        this.target = target;
        this.sourceBody = source.parent(PhyWall.PhyWindow.class).body;
        this.targetBody = target.parent(PhyWall.PhyWindow.class).body;


        bodies = new FasterList(num);
        joints = new FasterList(num);
        attachments = new FasterList(0);

        Dynamics2D w = sourceBody.W;

        FixtureDef segment = new FixtureDef(
                PolygonShape.box(eleLen / 2, thick / 2), 0.2f, 0f);
        segment.restitution = (0f);
        segment.filter.maskBits = 0; //no collision

        final float y = 0f;

        Body2D from = null;

        for (int i = 0; i < num; ++i) {


            if (from == null) {
                from = sourceBody;
            } else {

                Body2D to;
                if (i == num - 1) {
                    to = targetBody;
                } else {

                    to = new Body2D(
                            new BodyDef(BodyType.DYNAMIC,
                                    new v2(i * eleLen, y)), w);
                    bodies.add(to);
                    to.addFixture(segment);
                    to.setGravityScale(0);
                    to.setLinearDamping(0);
                }

                RevoluteJointDef jd = new RevoluteJointDef();
                //DistanceJointDef jd = new DistanceJointDef();
                jd.collideConnected = false;

                jd.bodyA = from;
                if (from != sourceBody) {
                    jd.localAnchorA.set(eleLen / 2, 0); //right side
                } else {
                    //bind to center of the start or end
                    jd.localAnchorA.set(0, 0);
                }
                jd.bodyB = to;
                if (to != targetBody) {
                    jd.localAnchorB.set(-eleLen / 2, 0); //left side
                } else {
                    //bind to center of the start or end
                    jd.localAnchorB.set(0, 0);
                }
                jd.referenceAngle = 0;


                RevoluteJoint jj = new MyRevoluteJoint(w, jd, from, source, to, target);
                joints.add(jj);

                from = to;
            }


        }

        w.invoke(() -> {
            bodies.forEach(b -> w.addBody(b));
            joints.forEach(w::addJoint);
        });
    }

    /**
     * attach a body to center of one of the segments
     */
    public void attach(Body2D b, int segment) {
        Dynamics2D world = world();
        world.invoke(() -> {
            RevoluteJoint w = (RevoluteJoint) b.W.addJoint(new RevoluteJointDef(bodies.get(segment), b));
            attachments.add(b);
            joints.add(w);
        });
    }

    private Dynamics2D world() {
        return bodies.get(0).W;
    }

    public void remove() {

        Dynamics2D world = world();
        world.invoke(() -> {

//            joints.forEach(world::removeJoint); //joints should be removed automatically when the attached body/bodies are removed
//            joints.clear();

            attachments.forEach(Body2D::remove);
            attachments.clear();

            bodies.forEach(Body2D::remove);
            bodies.clear();
        });
    }

    private class MyRevoluteJoint extends RevoluteJoint {
        private final Body2D finalFrom;
        private final Surface source;
        private final Body2D to;
        private final Surface target;

        public MyRevoluteJoint(Dynamics2D w, RevoluteJointDef jd, Body2D finalFrom, Surface source, Body2D to, Surface target) {
            super(w, jd);
            this.finalFrom = finalFrom;
            this.source = source;
            this.to = to;
            this.target = target;
            this.positionFactor = 0.1f;
        }

        @Override
        public boolean solvePositionConstraints(SolverData data) {
            //calc relative position of the surface within the body, allowing distinct positions of multiple ports at different positions in one body
            if (finalFrom == sourceBody) {
                if (source.parent == null) {
                    remove();

                } else {
                    localAnchorA.set(source.cx(), source.cy()).subbed(
                            sourceBody.pos
                    );
                }
            } else if (to == targetBody) {
                if (target.parent == null) {
                    remove();
                } else {
                    localAnchorB.set(target.cx(), target.cy()).subbed(
                            targetBody.pos
                    );
                }
            }
            return super.solvePositionConstraints(data);
        }
    }
}
