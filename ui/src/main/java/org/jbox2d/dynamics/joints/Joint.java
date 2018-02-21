/*******************************************************************************
 * Copyright (c) 2013, Daniel Murphy
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 	* Redistributions of source code must retain the above copyright notice,
 * 	  this list of conditions and the following disclaimer.
 * 	* Redistributions in binary form must reproduce the above copyright notice,
 * 	  this list of conditions and the following disclaimer in the documentation
 * 	  and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package org.jbox2d.dynamics.joints;

import org.jbox2d.dynamics.Body2D;
import org.jbox2d.dynamics.Dynamics2D;
import org.jbox2d.dynamics.SolverData;
import org.jbox2d.pooling.IWorldPool;
import spacegraph.math.Tuple2f;

// updated to rev 100

/**
 * The base joint class. Joints are used to constrain two bodies together in various fashions. Some
 * joints also feature limits and motors.
 *
 * @author Daniel Murphy
 */
public abstract class Joint {

    public static Joint build(Dynamics2D world, JointDef def) {
        // Joint joint = null;
        switch (def.type) {
            case MOUSE:
                return new MouseJoint(world.pool, (MouseJointDef) def);
            case DISTANCE:
                return new DistanceJoint(world.pool, (DistanceJointDef) def);
            case PRISMATIC:
                return new PrismaticJoint(world.pool, (PrismaticJointDef) def);
            case REVOLUTE:
                return new RevoluteJoint(world.pool, (RevoluteJointDef) def);
            case WELD:
                return new WeldJoint(world.pool, (WeldJointDef) def);
            case FRICTION:
                return new FrictionJoint(world.pool, (FrictionJointDef) def);
            case WHEEL:
                return new WheelJoint(world.pool, (WheelJointDef) def);
            case GEAR:
                return new GearJoint(world.pool, (GearJointDef) def);
            case PULLEY:
                return new PulleyJoint(world.pool, (PulleyJointDef) def);
            case CONSTANT_VOLUME:
                return new ConstantVolumeJoint(world, (ConstantVolumeJointDef) def);
            case ROPE:
                return new RopeJoint(world.pool, (RopeJointDef) def);
            case MOTOR:
                return new MotorJoint(world.pool, (MotorJointDef) def);
            case UNKNOWN:
            default:
                return null;
        }
    }

    public static void destroy(Joint joint) {
        joint.destructor();
    }

    private final JointType type;
    public Joint prev;
    public Joint next;
    public JointEdge edgeA;
    public JointEdge edgeB;
    protected Body2D A;
    protected Body2D B;

    public boolean islandFlag;
    private final boolean collideConnected;

    public Object data;

    protected IWorldPool pool;

    // Cache here per time step to reduce cache misses.
    // final Vec2 m_localCenterA, m_localCenterB;
    // float m_invMassA, m_invIA;
    // float m_invMassB, m_invIB;

    protected Joint(IWorldPool worldPool, JointDef def) {
        assert (def.bodyA != def.bodyB);

        pool = worldPool;
        type = def.type;
        prev = null;
        next = null;
        A = def.bodyA;
        B = def.bodyB;
        collideConnected = def.collideConnected;
        islandFlag = false;
        data = def.userData;

        edgeA = new JointEdge();
        edgeA.joint = null;
        edgeA.other = null;
        edgeA.prev = null;
        edgeA.next = null;

        edgeB = new JointEdge();
        edgeB.joint = null;
        edgeB.other = null;
        edgeB.prev = null;
        edgeB.next = null;

        // m_localCenterA = new Vec2();
        // m_localCenterB = new Vec2();
    }

    /**
     * get the type of the concrete joint.
     *
     * @return
     */
    public JointType getType() {
        return type;
    }

    /**
     * get the first body attached to this joint.
     */
    public final Body2D getBodyA() {
        return A;
    }

    /**
     * get the second body attached to this joint.
     *
     * @return
     */
    public final Body2D getBodyB() {
        return B;
    }

    /**
     * get the anchor point on bodyA in world coordinates.
     *
     * @return
     */
    public abstract void getAnchorA(Tuple2f out);

    /**
     * get the anchor point on bodyB in world coordinates.
     *
     * @return
     */
    public abstract void getAnchorB(Tuple2f out);

    /**
     * get the reaction force on body2 at the joint anchor in Newtons.
     *
     * @param inv_dt
     * @return
     */
    public abstract void getReactionForce(float inv_dt, Tuple2f out);

    /**
     * get the reaction torque on body2 in N*m.
     *
     * @param inv_dt
     * @return
     */
    public abstract float getReactionTorque(float inv_dt);

    /**
     * get the next joint the world joint list.
     */
    public Joint getNext() {
        return next;
    }

    /**
     * get the user data pointer.
     */
    public Object data() {
        return data;
    }

    /**
     * Set the user data pointer.
     */
    public void setData(Object data) {
        this.data = data;
    }

    /**
     * Get collide connected. Note: modifying the collide connect flag won't work correctly because
     * the flag is only checked when fixture AABBs begin to overlap.
     */
    public final boolean getCollideConnected() {
        return collideConnected;
    }

    /**
     * Short-cut function to determine if either body is inactive.
     *
     * @return
     */
    public boolean isActive() {
        return A.isActive() && B.isActive();
    }

    /**
     * Internal
     */
    public abstract void initVelocityConstraints(SolverData data);

    /**
     * Internal
     */
    public abstract void solveVelocityConstraints(SolverData data);

    /**
     * This returns true if the position errors are within tolerance. Internal.
     */
    public abstract boolean solvePositionConstraints(SolverData data);

    /**
     * Override to handle destruction of joint
     */
    public void destructor() {
    }



}
