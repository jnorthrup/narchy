package org.jbox2d.dynamics.joints;

import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body2D;
import spacegraph.math.Tuple2f;

/**
 * Rope joint definition. This requires two body anchor points and a maximum lengths. Note: by
 * default the connected objects will not collide. see collideConnected in b2JointDef.
 *
 * @author Daniel Murphy
 */
public class RopeJointDef extends JointDef {

    /**
     * The local anchor point relative to bodyA's origin.
     */
    public final Tuple2f localAnchorA = new Vec2();

    /**
     * The local anchor point relative to bodyB's origin.
     */
    public final Tuple2f localAnchorB = new Vec2();

    /**
     * The maximum length of the rope. Warning: this must be larger than b2_linearSlop or the joint
     * will have no effect.
     */
    public float maxLength;


    public RopeJointDef(Body2D a, Body2D b) {
        super(JointType.ROPE);
        this.bodyA = a;
        this.bodyB = b;
        localAnchorA.set(0.0f, 0.0f);
        localAnchorB.set(0.0f, 0.0f);
    }
}
