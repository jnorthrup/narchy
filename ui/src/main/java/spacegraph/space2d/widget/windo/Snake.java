package spacegraph.space2d.widget.windo;

import jcog.list.FasterList;
import spacegraph.space2d.Surface;
import spacegraph.space2d.phys.collision.shapes.PolygonShape;
import spacegraph.space2d.phys.dynamics.*;
import spacegraph.space2d.phys.dynamics.joints.Joint;
import spacegraph.space2d.phys.dynamics.joints.RevoluteJoint;
import spacegraph.space2d.phys.dynamics.joints.RevoluteJointDef;
import spacegraph.util.math.v2;

import java.util.List;

public class Snake {

    private final List<Body2D> bodies;
    private final List<Body2D> attachments;
    final List<Joint> joints;
    private final Surface source;
    private final Surface target;
    private final Body2D sourceBody, targetBody;

    public Snake(Surface source, Surface target, int num, float eleLen /* TODO parametric */, float thick) {

        this.source = source;
        this.target = target;
        this.sourceBody = source.parent(Dyn2DSurface.PhyWindow.class).body;
        this.targetBody = target.parent(Dyn2DSurface.PhyWindow.class).body;


        bodies = new FasterList(num);
        joints = new FasterList(num);
        attachments = new FasterList(0);

        Dynamics2D w = sourceBody.W;

        FixtureDef segment = new FixtureDef(
                PolygonShape.box(eleLen / 2, thick / 2), 0.2f, 0f);
        segment.restitution = (0f);
        segment.filter.maskBits = 0; 

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
                
                jd.collideConnected = false;

                jd.bodyA = from;
                if (from != sourceBody) {
                    jd.localAnchorA.set(eleLen / 2, 0); 
                } else {
                    
                    jd.localAnchorA.set(0, 0);
                }
                jd.bodyB = to;
                if (to != targetBody) {
                    jd.localAnchorB.set(-eleLen / 2, 0); 
                } else {
                    
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




            attachments.forEach(Body2D::remove);
            attachments.clear();

            bodies.forEach(Body2D::remove);
            bodies.clear();
        });
    }

    private class MyRevoluteJoint extends RevoluteJoint {
        private final Body2D from;
        private final Surface source;
        private final Body2D to;
        private final Surface target;

        MyRevoluteJoint(Dynamics2D w, RevoluteJointDef jd, Body2D from, Surface source, Body2D to, Surface target) {
            super(w, jd);
            this.from = from;
            this.source = source;
            this.to = to;
            this.target = target;
            this.positionFactor = 0.1f;
        }

        @Override
        public boolean solvePositionConstraints(SolverData data) {
            
            if (from == sourceBody) {
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
