/**
 * Copyright (c) 2013, Daniel Murphy
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p>
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
 * <p>
 * Created at 11:34:45 AM Jan 23, 2011
 */
/**
 * Created at 11:34:45 AM Jan 23, 2011
 */
package spacegraph.space2d.phys.dynamics.joints;

import spacegraph.space2d.phys.common.Rot;
import spacegraph.space2d.phys.common.Settings;
import spacegraph.space2d.phys.common.Transform;
import spacegraph.space2d.phys.common.Vec2;
import spacegraph.space2d.phys.dynamics.Body2D;
import spacegraph.space2d.phys.dynamics.SolverData;
import spacegraph.space2d.phys.pooling.IWorldPool;
import spacegraph.util.math.Tuple2f;




















/**
 * A gear joint is used to connect two joints together. Either joint can be a revolute or prismatic
 * joint. You specify a gear ratio to bind the motions together: coordinate1 + ratio * coordinate2 =
 * constant The ratio can be negative or positive. If one joint is a revolute joint and the other
 * joint is a prismatic joint, then the ratio will have units of length or units of 1/length.
 *
 * @warning The revolute and prismatic joints must be attached to fixed bodies (which must be body1
 *          on those joints).
 * @warning You have to manually destroy the gear joint if joint1 or joint2 is destroyed.
 * @author Daniel Murphy
 */
public class GearJoint extends Joint {

    private final Joint m_joint1;
    private final Joint m_joint2;

    private final JointType m_typeA;
    private final JointType m_typeB;

    
    
    private final Body2D m_bodyC;
    private final Body2D m_bodyD;

    
    private final Tuple2f m_localAnchorA = new Vec2();
    private final Tuple2f m_localAnchorB = new Vec2();
    private final Tuple2f m_localAnchorC = new Vec2();
    private final Tuple2f m_localAnchorD = new Vec2();

    private final Tuple2f m_localAxisC = new Vec2();
    private final Tuple2f m_localAxisD = new Vec2();

    private final float m_referenceAngleA;
    private final float m_referenceAngleB;

    private final float m_constant;
    private float m_ratio;

    private float m_impulse;

    
    private int m_indexA, m_indexB, m_indexC, m_indexD;
    private final Tuple2f m_lcA = new Vec2(), m_lcB = new Vec2(), m_lcC = new Vec2(),
            m_lcD = new Vec2();
    private float m_mA, m_mB, m_mC, m_mD;
    private float m_iA, m_iB, m_iC, m_iD;
    private final Tuple2f m_JvAC = new Vec2(), m_JvBD = new Vec2();
    private float m_JwA, m_JwB, m_JwC, m_JwD;
    private float m_mass;

    GearJoint(IWorldPool argWorldPool, GearJointDef def) {
        super(argWorldPool, def);

        m_joint1 = def.joint1;
        m_joint2 = def.joint2;

        m_typeA = m_joint1.getType();
        m_typeB = m_joint2.getType();

        assert (m_typeA == JointType.REVOLUTE || m_typeA == JointType.PRISMATIC);
        assert (m_typeB == JointType.REVOLUTE || m_typeB == JointType.PRISMATIC);

        float coordinateA, coordinateB;

        

        m_bodyC = m_joint1.getBodyA();
        A = m_joint1.getBodyB();

        
        Transform xfA = A;
        float aA = A.sweep.a;
        Transform xfC = m_bodyC;
        float aC = m_bodyC.sweep.a;

        if (m_typeA == JointType.REVOLUTE) {
            RevoluteJoint revolute = (RevoluteJoint) def.joint1;
            m_localAnchorC.set(revolute.localAnchorA);
            m_localAnchorA.set(revolute.localAnchorB);
            m_referenceAngleA = revolute.m_referenceAngle;
            m_localAxisC.setZero();

            coordinateA = aA - aC - m_referenceAngleA;
        } else {
            Tuple2f pA = pool.popVec2();
            Tuple2f temp = pool.popVec2();
            PrismaticJoint prismatic = (PrismaticJoint) def.joint1;
            m_localAnchorC.set(prismatic.m_localAnchorA);
            m_localAnchorA.set(prismatic.m_localAnchorB);
            m_referenceAngleA = prismatic.m_referenceAngle;
            m_localAxisC.set(prismatic.m_localXAxisA);

            Tuple2f pC = m_localAnchorC;
            Rot.mulToOutUnsafe(xfA, m_localAnchorA, temp);
            temp.added(xfA.pos).subbed(xfC.pos);
            Rot.mulTransUnsafe(xfC, temp, pA);
            coordinateA = Tuple2f.dot(pA.subbed(pC), m_localAxisC);
            pool.pushVec2(2);
        }

        m_bodyD = m_joint2.getBodyA();
        B = m_joint2.getBodyB();

        
        Transform xfB = B;
        float aB = B.sweep.a;
        Transform xfD = m_bodyD;
        float aD = m_bodyD.sweep.a;

        if (m_typeB == JointType.REVOLUTE) {
            RevoluteJoint revolute = (RevoluteJoint) def.joint2;
            m_localAnchorD.set(revolute.localAnchorA);
            m_localAnchorB.set(revolute.localAnchorB);
            m_referenceAngleB = revolute.m_referenceAngle;
            m_localAxisD.setZero();

            coordinateB = aB - aD - m_referenceAngleB;
        } else {
            Tuple2f pB = pool.popVec2();
            Tuple2f temp = pool.popVec2();
            PrismaticJoint prismatic = (PrismaticJoint) def.joint2;
            m_localAnchorD.set(prismatic.m_localAnchorA);
            m_localAnchorB.set(prismatic.m_localAnchorB);
            m_referenceAngleB = prismatic.m_referenceAngle;
            m_localAxisD.set(prismatic.m_localXAxisA);

            Tuple2f pD = m_localAnchorD;
            Rot.mulToOutUnsafe(xfB, m_localAnchorB, temp);
            temp.added(xfB.pos).subbed(xfD.pos);
            Rot.mulTransUnsafe(xfD, temp, pB);
            coordinateB = Tuple2f.dot(pB.subbed(pD), m_localAxisD);
            pool.pushVec2(2);
        }

        m_ratio = def.ratio;

        m_constant = coordinateA + m_ratio * coordinateB;

        m_impulse = 0.0f;
    }

    @Override
    public void getAnchorA(Tuple2f argOut) {
        A.getWorldPointToOut(m_localAnchorA, argOut);
    }

    @Override
    public void getAnchorB(Tuple2f argOut) {
        B.getWorldPointToOut(m_localAnchorB, argOut);
    }

    @Override
    public void getReactionForce(float inv_dt, Tuple2f argOut) {
        argOut.set(m_JvAC).scaled(m_impulse);
        argOut.scaled(inv_dt);
    }

    @Override
    public float getReactionTorque(float inv_dt) {
        float L = m_impulse * m_JwA;
        return inv_dt * L;
    }

    public void setRatio(float argRatio) {
        m_ratio = argRatio;
    }

    public float getRatio() {
        return m_ratio;
    }

    @Override
    public void initVelocityConstraints(SolverData data) {
        m_indexA = A.island;
        m_indexB = B.island;
        m_indexC = m_bodyC.island;
        m_indexD = m_bodyD.island;
        m_lcA.set(A.sweep.localCenter);
        m_lcB.set(B.sweep.localCenter);
        m_lcC.set(m_bodyC.sweep.localCenter);
        m_lcD.set(m_bodyD.sweep.localCenter);
        m_mA = A.m_invMass;
        m_mB = B.m_invMass;
        m_mC = m_bodyC.m_invMass;
        m_mD = m_bodyD.m_invMass;
        m_iA = A.m_invI;
        m_iB = B.m_invI;
        m_iC = m_bodyC.m_invI;
        m_iD = m_bodyD.m_invI;

        
        float aA = data.positions[m_indexA].a;
        Tuple2f vA = data.velocities[m_indexA];
        float wA = data.velocities[m_indexA].w;

        
        float aB = data.positions[m_indexB].a;
        Tuple2f vB = data.velocities[m_indexB];
        float wB = data.velocities[m_indexB].w;

        
        float aC = data.positions[m_indexC].a;
        Tuple2f vC = data.velocities[m_indexC];
        float wC = data.velocities[m_indexC].w;

        
        float aD = data.positions[m_indexD].a;
        Tuple2f vD = data.velocities[m_indexD];
        float wD = data.velocities[m_indexD].w;

        Rot qA = pool.popRot(), qB = pool.popRot(), qC = pool.popRot(), qD = pool.popRot();
        qA.set(aA);
        qB.set(aB);
        qC.set(aC);
        qD.set(aD);

        m_mass = 0.0f;

        Tuple2f temp = pool.popVec2();

        if (m_typeA == JointType.REVOLUTE) {
            m_JvAC.setZero();
            m_JwA = 1.0f;
            m_JwC = 1.0f;
            m_mass += m_iA + m_iC;
        } else {
            Tuple2f rC = pool.popVec2();
            Tuple2f rA = pool.popVec2();
            Rot.mulToOutUnsafe(qC, m_localAxisC, m_JvAC);
            Rot.mulToOutUnsafe(qC, temp.set(m_localAnchorC).subbed(m_lcC), rC);
            Rot.mulToOutUnsafe(qA, temp.set(m_localAnchorA).subbed(m_lcA), rA);
            m_JwC = Tuple2f.cross(rC, m_JvAC);
            m_JwA = Tuple2f.cross(rA, m_JvAC);
            m_mass += m_mC + m_mA + m_iC * m_JwC * m_JwC + m_iA * m_JwA * m_JwA;
            pool.pushVec2(2);
        }

        if (m_typeB == JointType.REVOLUTE) {
            m_JvBD.setZero();
            m_JwB = m_ratio;
            m_JwD = m_ratio;
            m_mass += m_ratio * m_ratio * (m_iB + m_iD);
        } else {
            Tuple2f u = pool.popVec2();
            Tuple2f rD = pool.popVec2();
            Tuple2f rB = pool.popVec2();
            Rot.mulToOutUnsafe(qD, m_localAxisD, u);
            Rot.mulToOutUnsafe(qD, temp.set(m_localAnchorD).subbed(m_lcD), rD);
            Rot.mulToOutUnsafe(qB, temp.set(m_localAnchorB).subbed(m_lcB), rB);
            m_JvBD.set(u).scaled(m_ratio);
            m_JwD = m_ratio * Tuple2f.cross(rD, u);
            m_JwB = m_ratio * Tuple2f.cross(rB, u);
            m_mass += m_ratio * m_ratio * (m_mD + m_mB) + m_iD * m_JwD * m_JwD + m_iB * m_JwB * m_JwB;
            pool.pushVec2(3);
        }

        
        m_mass = m_mass > 0.0f ? 1.0f / m_mass : 0.0f;

        if (data.step.warmStarting) {
            vA.x += (m_mA * m_impulse) * m_JvAC.x;
            vA.y += (m_mA * m_impulse) * m_JvAC.y;
            wA += m_iA * m_impulse * m_JwA;

            vB.x += (m_mB * m_impulse) * m_JvBD.x;
            vB.y += (m_mB * m_impulse) * m_JvBD.y;
            wB += m_iB * m_impulse * m_JwB;

            vC.x -= (m_mC * m_impulse) * m_JvAC.x;
            vC.y -= (m_mC * m_impulse) * m_JvAC.y;
            wC -= m_iC * m_impulse * m_JwC;

            vD.x -= (m_mD * m_impulse) * m_JvBD.x;
            vD.y -= (m_mD * m_impulse) * m_JvBD.y;
            wD -= m_iD * m_impulse * m_JwD;
        } else {
            m_impulse = 0.0f;
        }
        pool.pushVec2(1);
        pool.pushRot(4);

        
        data.velocities[m_indexA].w = wA;
        
        data.velocities[m_indexB].w = wB;
        
        data.velocities[m_indexC].w = wC;
        
        data.velocities[m_indexD].w = wD;
    }

    @Override
    public void solveVelocityConstraints(SolverData data) {
        Tuple2f vA = data.velocities[m_indexA];
        float wA = data.velocities[m_indexA].w;
        Tuple2f vB = data.velocities[m_indexB];
        float wB = data.velocities[m_indexB].w;
        Tuple2f vC = data.velocities[m_indexC];
        float wC = data.velocities[m_indexC].w;
        Tuple2f vD = data.velocities[m_indexD];
        float wD = data.velocities[m_indexD].w;

        Tuple2f temp1 = pool.popVec2();
        Tuple2f temp2 = pool.popVec2();
        float Cdot =
                Tuple2f.dot(m_JvAC, temp1.set(vA).subbed(vC)) + Tuple2f.dot(m_JvBD, temp2.set(vB).subbed(vD));
        Cdot += (m_JwA * wA - m_JwC * wC) + (m_JwB * wB - m_JwD * wD);
        pool.pushVec2(2);

        float impulse = -m_mass * Cdot;
        m_impulse += impulse;

        vA.x += (m_mA * impulse) * m_JvAC.x;
        vA.y += (m_mA * impulse) * m_JvAC.y;
        wA += m_iA * impulse * m_JwA;

        vB.x += (m_mB * impulse) * m_JvBD.x;
        vB.y += (m_mB * impulse) * m_JvBD.y;
        wB += m_iB * impulse * m_JwB;

        vC.x -= (m_mC * impulse) * m_JvAC.x;
        vC.y -= (m_mC * impulse) * m_JvAC.y;
        wC -= m_iC * impulse * m_JwC;

        vD.x -= (m_mD * impulse) * m_JvBD.x;
        vD.y -= (m_mD * impulse) * m_JvBD.y;
        wD -= m_iD * impulse * m_JwD;


        
        data.velocities[m_indexA].w = wA;
        
        data.velocities[m_indexB].w = wB;
        
        data.velocities[m_indexC].w = wC;
        
        data.velocities[m_indexD].w = wD;
    }

    public Joint getJoint1() {
        return m_joint1;
    }

    public Joint getJoint2() {
        return m_joint2;
    }

    @Override
    public boolean solvePositionConstraints(SolverData data) {
        Tuple2f cA = data.positions[m_indexA];
        float aA = data.positions[m_indexA].a;
        Tuple2f cB = data.positions[m_indexB];
        float aB = data.positions[m_indexB].a;
        Tuple2f cC = data.positions[m_indexC];
        float aC = data.positions[m_indexC].a;
        Tuple2f cD = data.positions[m_indexD];
        float aD = data.positions[m_indexD].a;

        Rot qA = pool.popRot(), qB = pool.popRot(), qC = pool.popRot(), qD = pool.popRot();
        qA.set(aA);
        qB.set(aB);
        qC.set(aC);
        qD.set(aD);

        float linearError = 0.0f;

        float coordinateA, coordinateB;

        Tuple2f temp = pool.popVec2();
        Tuple2f JvAC = pool.popVec2();
        Tuple2f JvBD = pool.popVec2();
        float JwA, JwB, JwC, JwD;
        float mass = 0.0f;

        if (m_typeA == JointType.REVOLUTE) {
            JvAC.setZero();
            JwA = 1.0f;
            JwC = 1.0f;
            mass += m_iA + m_iC;

            coordinateA = aA - aC - m_referenceAngleA;
        } else {
            Tuple2f rC = pool.popVec2();
            Tuple2f rA = pool.popVec2();
            Tuple2f pC = pool.popVec2();
            Tuple2f pA = pool.popVec2();
            Rot.mulToOutUnsafe(qC, m_localAxisC, JvAC);
            Rot.mulToOutUnsafe(qC, temp.set(m_localAnchorC).subbed(m_lcC), rC);
            Rot.mulToOutUnsafe(qA, temp.set(m_localAnchorA).subbed(m_lcA), rA);
            JwC = Tuple2f.cross(rC, JvAC);
            JwA = Tuple2f.cross(rA, JvAC);
            mass += m_mC + m_mA + m_iC * JwC * JwC + m_iA * JwA * JwA;

            pC.set(m_localAnchorC).subbed(m_lcC);
            Rot.mulTransUnsafe(qC, temp.set(rA).added(cA).subbed(cC), pA);
            coordinateA = Tuple2f.dot(pA.subbed(pC), m_localAxisC);
            pool.pushVec2(4);
        }

        if (m_typeB == JointType.REVOLUTE) {
            JvBD.setZero();
            JwB = m_ratio;
            JwD = m_ratio;
            mass += m_ratio * m_ratio * (m_iB + m_iD);

            coordinateB = aB - aD - m_referenceAngleB;
        } else {
            Tuple2f u = pool.popVec2();
            Tuple2f rD = pool.popVec2();
            Tuple2f rB = pool.popVec2();
            Tuple2f pD = pool.popVec2();
            Tuple2f pB = pool.popVec2();
            Rot.mulToOutUnsafe(qD, m_localAxisD, u);
            Rot.mulToOutUnsafe(qD, temp.set(m_localAnchorD).subbed(m_lcD), rD);
            Rot.mulToOutUnsafe(qB, temp.set(m_localAnchorB).subbed(m_lcB), rB);
            JvBD.set(u).scaled(m_ratio);
            JwD = Tuple2f.cross(rD, u);
            JwB = Tuple2f.cross(rB, u);
            mass += m_ratio * m_ratio * (m_mD + m_mB) + m_iD * JwD * JwD + m_iB * JwB * JwB;

            pD.set(m_localAnchorD).subbed(m_lcD);
            Rot.mulTransUnsafe(qD, temp.set(rB).added(cB).subbed(cD), pB);
            coordinateB = Tuple2f.dot(pB.subbed(pD), m_localAxisD);
            pool.pushVec2(5);
        }

        float C = (coordinateA + m_ratio * coordinateB) - m_constant;

        float impulse = 0.0f;
        if (mass > 0.0f) {
            impulse = -C / mass;
        }
        pool.pushVec2(3);
        pool.pushRot(4);

        cA.x += (m_mA * impulse) * JvAC.x;
        cA.y += (m_mA * impulse) * JvAC.y;
        aA += m_iA * impulse * JwA;

        cB.x += (m_mB * impulse) * JvBD.x;
        cB.y += (m_mB * impulse) * JvBD.y;
        aB += m_iB * impulse * JwB;

        cC.x -= (m_mC * impulse) * JvAC.x;
        cC.y -= (m_mC * impulse) * JvAC.y;
        aC -= m_iC * impulse * JwC;

        cD.x -= (m_mD * impulse) * JvBD.x;
        cD.y -= (m_mD * impulse) * JvBD.y;
        aD -= m_iD * impulse * JwD;

        
        data.positions[m_indexA].a = aA;
        
        data.positions[m_indexB].a = aB;
        
        data.positions[m_indexC].a = aC;
        
        data.positions[m_indexD].a = aD;

        
        return linearError < Settings.linearSlop;
    }
}
