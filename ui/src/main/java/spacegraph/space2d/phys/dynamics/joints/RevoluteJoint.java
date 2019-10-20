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
package spacegraph.space2d.phys.dynamics.joints;

import jcog.math.v2;
import jcog.math.v3;
import spacegraph.space2d.phys.common.*;
import spacegraph.space2d.phys.dynamics.Body2D;
import spacegraph.space2d.phys.dynamics.Dynamics2D;
import spacegraph.space2d.phys.dynamics.SolverData;
import spacegraph.space2d.phys.pooling.IWorldPool;


/**
 * A revolute joint constrains two bodies to share a common point while they are free to rotate
 * about the point. The relative rotation about the shared point is the joint angle. You can limit
 * the relative rotation with a joint limit that specifies a lower and upper angle. You can use a
 * motor to drive the relative rotation about the shared point. A maximum motor torque is provided
 * so that infinite forces are not generated.
 *
 * @author Daniel Murphy
 */
public class RevoluteJoint extends Joint {

    
    protected final v2 localAnchorA = new v2();
    protected final v2 localAnchorB = new v2();
    private final Vec3 m_impulse = new Vec3();
    private float m_motorImpulse;

    private boolean m_enableMotor;
    private float m_maxMotorTorque;
    private float m_motorSpeed;

    private boolean m_enableLimit;
    final float m_referenceAngle;
    private float m_lowerAngle;
    private float m_upperAngle;

    
    private int m_indexA;
    private int m_indexB;
    private final v2 m_rA = new v2();
    private final v2 m_rB = new v2();
    private final v2 m_localCenterA = new v2();
    private final v2 m_localCenterB = new v2();
    private float m_invMassA;
    private float m_invMassB;
    private float m_invIA;
    private float m_invIB;
    private final Mat33 m_mass = new Mat33(); 
    private float m_motorMass; 
    private LimitState m_limitState;

    /** how important it is to resolve position 'error' (distance from point-point).
     * 1 = normal revolute joint behavior
     * ~ = somewhat solve it
     * 0 = does not resolve point-to-point distance 'error'
     */
    public float positionFactor = 1f;

    public RevoluteJoint(Dynamics2D world, RevoluteJointDef def) {
        this(world.pool, def);
    }

    public RevoluteJoint(IWorldPool argWorld, RevoluteJointDef def) {
        super(argWorld, def);
        localAnchorA.set(def.localAnchorA);
        localAnchorB.set(def.localAnchorB);
        m_referenceAngle = def.referenceAngle;

        m_motorImpulse = 0;

        m_lowerAngle = def.lowerAngle;
        m_upperAngle = def.upperAngle;
        m_maxMotorTorque = def.maxMotorTorque;
        m_motorSpeed = def.motorSpeed;
        m_enableLimit = def.enableLimit;
        m_enableMotor = def.enableMotor;
        m_limitState = LimitState.INACTIVE;
    }

    @Override
    public void initVelocityConstraints(SolverData data) {
        m_indexA = A.island;
        m_indexB = B.island;
        m_localCenterA.set(A.sweep.localCenter);
        m_localCenterB.set(B.sweep.localCenter);
        m_invMassA = A.m_invMass;
        m_invMassB = B.m_invMass;
        m_invIA = A.m_invI;
        m_invIB = B.m_invI;


        var aA = data.positions[m_indexA].a;
        v2 vA = data.velocities[m_indexA];
        var wA = data.velocities[m_indexA].w;


        var aB = data.positions[m_indexB].a;
        v2 vB = data.velocities[m_indexB];
        var wB = data.velocities[m_indexB].w;
        var qA = pool.popRot();
        var qB = pool.popRot();
        var temp = new v2();

        qA.set(aA);
        qB.set(aB);

        
        Rot.mulToOutUnsafe(qA, temp.set(localAnchorA).subbed(m_localCenterA), m_rA);
        Rot.mulToOutUnsafe(qB, temp.set(localAnchorB).subbed(m_localCenterB), m_rB);

        
        
        

        
        
        
        

        float mA = m_invMassA, mB = m_invMassB;
        float iA = m_invIA, iB = m_invIB;

        m_mass.ex.x = mA + mB + m_rA.y * m_rA.y * iA + m_rB.y * m_rB.y * iB;
        m_mass.ey.x = -m_rA.y * m_rA.x * iA - m_rB.y * m_rB.x * iB;
        m_mass.ez.x = -m_rA.y * iA - m_rB.y * iB;
        m_mass.ex.y = m_mass.ey.x;
        m_mass.ey.y = mA + mB + m_rA.x * m_rA.x * iA + m_rB.x * m_rB.x * iB;
        m_mass.ez.y = m_rA.x * iA + m_rB.x * iB;
        m_mass.ex.z = m_mass.ez.x;
        m_mass.ey.z = m_mass.ez.y;
        m_mass.ez.z = iA + iB;

        m_motorMass = iA + iB;
        if (m_motorMass > 0.0f) {
            m_motorMass = 1.0f / m_motorMass;
        }

        var fixedRotation = (iA + iB == 0.0f);
        if (!m_enableMotor || fixedRotation) {
            m_motorImpulse = 0.0f;
        }

        if (m_enableLimit && !fixedRotation) {
            var jointAngle = aB - aA - m_referenceAngle;
            if (Math.abs(m_upperAngle - m_lowerAngle) < 2.0f * Settings.angularSlop) {
                m_limitState = LimitState.EQUAL;
            } else if (jointAngle <= m_lowerAngle) {
                if (m_limitState != LimitState.AT_LOWER) {
                    m_impulse.z = 0.0f;
                }
                m_limitState = LimitState.AT_LOWER;
            } else if (jointAngle >= m_upperAngle) {
                if (m_limitState != LimitState.AT_UPPER) {
                    m_impulse.z = 0.0f;
                }
                m_limitState = LimitState.AT_UPPER;
            } else {
                m_limitState = LimitState.INACTIVE;
                m_impulse.z = 0.0f;
            }
        } else {
            m_limitState = LimitState.INACTIVE;
        }

        if (data.step.warmStarting) {
            var P = new v2();
            
            m_impulse.x *= data.step.dtRatio;
            m_impulse.y *= data.step.dtRatio;
            m_motorImpulse *= data.step.dtRatio;

            P.x = m_impulse.x;
            P.y = m_impulse.y;

            vA.x -= mA * P.x;
            vA.y -= mA * P.y;
            wA -= iA * (v2.cross(m_rA, P) + m_motorImpulse + m_impulse.z);

            vB.x += mB * P.x;
            vB.y += mB * P.y;
            wB += iB * (v2.cross(m_rB, P) + m_motorImpulse + m_impulse.z);
        } else {
            m_impulse.zero();
            m_motorImpulse = 0.0f;
        }
        
        data.velocities[m_indexA].w = wA;
        
        data.velocities[m_indexB].w = wB;


        pool.pushRot(2);
    }

    @Override
    public void solveVelocityConstraints(SolverData data) {
        v2 vA = data.velocities[m_indexA];
        var wA = data.velocities[m_indexA].w;
        v2 vB = data.velocities[m_indexB];
        var wB = data.velocities[m_indexB].w;

        float mA = m_invMassA, mB = m_invMassB;
        float iA = m_invIA, iB = m_invIB;

        var fixedRotation = (iA + iB == 0.0f);

        
        if (m_enableMotor && m_limitState != LimitState.EQUAL && !fixedRotation) {
            var Cdot = wB - wA - m_motorSpeed;
            var impulse = -m_motorMass * Cdot;
            var oldImpulse = m_motorImpulse;
            var maxImpulse = data.step.dt * m_maxMotorTorque;
            m_motorImpulse = MathUtils.clamp(m_motorImpulse + impulse, -maxImpulse, maxImpulse);
            impulse = m_motorImpulse - oldImpulse;

            wA -= iA * impulse;
            wB += iB * impulse;
        }
        var temp = new v2();

        
        if (m_enableLimit && m_limitState != LimitState.INACTIVE && !fixedRotation) {

            var Cdot1 = new v2();
            v3 Cdot = new Vec3();

            
            v2.crossToOutUnsafe(wA, m_rA, temp);
            v2.crossToOutUnsafe(wB, m_rB, Cdot1);
            Cdot1.added(vB).subbed(vA).subbed(temp).scaled(positionFactor);
            var Cdot2 = wB - wA;
            Cdot.set(Cdot1.x, Cdot1.y, Cdot2);

            var impulse = new Vec3();
            m_mass.solve33ToOut(Cdot, impulse);
            impulse.negated();

            switch (m_limitState) {
                case EQUAL:
                    m_impulse.addLocal(impulse);
                    break;
                case AT_LOWER: {
                    var newImpulse = m_impulse.z + impulse.z;
                    if (newImpulse < 0.0f) {
                        var rhs = new v2();
                        rhs.set(m_mass.ez.x, m_mass.ez.y).scaled(m_impulse.z).subbed(Cdot1);
                        m_mass.solve22ToOut(rhs, temp);
                        impulse.x = temp.x;
                        impulse.y = temp.y;
                        impulse.z = -m_impulse.z;
                        m_impulse.x += temp.x;
                        m_impulse.y += temp.y;
                        m_impulse.z = 0.0f;
                    } else {
                        m_impulse.addLocal(impulse);
                    }
                    break;
                }
                case AT_UPPER: {
                    var newImpulse = m_impulse.z + impulse.z;
                    if (newImpulse > 0.0f) {
                        var rhs = new v2();
                        rhs.set(m_mass.ez.x, m_mass.ez.y).scaled(m_impulse.z).subbed(Cdot1);
                        m_mass.solve22ToOut(rhs, temp);
                        impulse.x = temp.x;
                        impulse.y = temp.y;
                        impulse.z = -m_impulse.z;
                        m_impulse.x += temp.x;
                        m_impulse.y += temp.y;
                        m_impulse.z = 0.0f;
                    } else {
                        m_impulse.addLocal(impulse);
                    }
                    break;
                }
            }
            var P = new v2();

            P.set(impulse.x, impulse.y);

            vA.x -= mA * P.x;
            vA.y -= mA * P.y;
            wA -= iA * (v2.cross(m_rA, P) + impulse.z);

            vB.x += mB * P.x;
            vB.y += mB * P.y;
            wB += iB * (v2.cross(m_rB, P) + impulse.z);

        } else {


            var Cdot = new v2();
            var impulse = new v2();

            v2.crossToOutUnsafe(wA, m_rA, temp);
            v2.crossToOutUnsafe(wB, m_rB, Cdot);
            Cdot.added(vB).subbed(vA).subbed(temp).scaled(positionFactor);
            m_mass.solve22ToOut(Cdot.negated(), impulse); 

            m_impulse.x += impulse.x;
            m_impulse.y += impulse.y;

            vA.x -= mA * impulse.x;
            vA.y -= mA * impulse.y;
            wA -= iA * v2.cross(m_rA, impulse);

            vB.x += mB * impulse.x;
            vB.y += mB * impulse.y;
            wB += iB * v2.cross(m_rB, impulse);

        }

        
        data.velocities[m_indexA].w = wA;
        
        data.velocities[m_indexB].w = wB;


    }

    @Override
    public boolean solvePositionConstraints(SolverData data) {
        var qA = pool.popRot();
        var qB = pool.popRot();
        v2 cA = data.positions[m_indexA];
        var aA = data.positions[m_indexA].a;
        v2 cB = data.positions[m_indexB];
        var aB = data.positions[m_indexB].a;

        qA.set(aA);
        qB.set(aB);

        var angularError = 0.0f;

        var fixedRotation = (m_invIA + m_invIB == 0.0f);

        
        if (m_enableLimit && m_limitState != LimitState.INACTIVE && !fixedRotation) {
            var angle = aB - aA - m_referenceAngle;
            var limitImpulse = 0.0f;

            switch (m_limitState) {
                case EQUAL: {

                    var C =
                            MathUtils.clamp(angle - m_lowerAngle, -Settings.maxAngularCorrection,
                                    Settings.maxAngularCorrection);
                    limitImpulse = -m_motorMass * C;
                    angularError = Math.abs(C);
                    break;
                }
                case AT_LOWER: {
                    var C = angle - m_lowerAngle;
                    angularError = -C;

                    
                    C = MathUtils.clamp(C + Settings.angularSlop, -Settings.maxAngularCorrection, 0.0f);
                    limitImpulse = -m_motorMass * C;
                    break;
                }
                case AT_UPPER: {
                    var C = angle - m_upperAngle;
                    angularError = C;

                    
                    C = MathUtils.clamp(C - Settings.angularSlop, 0.0f, Settings.maxAngularCorrection);
                    limitImpulse = -m_motorMass * C;
                    break;
                }
            }

            aA -= m_invIA * limitImpulse;
            aB += m_invIB * limitImpulse;
        }

        var positionError = 0.0f;
        {
            qA.set(aA);
            qB.set(aB);

            var rA = new v2();
            var rB = new v2();
            var C = new v2();
            var impulse = new v2();

            Rot.mulToOutUnsafe(qA, C.set(localAnchorA).subbed(m_localCenterA), rA);
            Rot.mulToOutUnsafe(qB, C.set(localAnchorB).subbed(m_localCenterB), rB);
            C.set(cB).added(rB).subbed(cA).subbed(rA).scaled(positionFactor);
            positionError = C.length();


            float mA = m_invMassA, mB = m_invMassB;
            float iA = m_invIA, iB = m_invIB;

            var K = pool.popMat22();
            K.ex.x = mA + mB + iA * rA.y * rA.y + iB * rB.y * rB.y;
            K.ex.y = -iA * rA.x * rA.y - iB * rB.x * rB.y;
            K.ey.x = K.ex.y;
            K.ey.y = mA + mB + iA * rA.x * rA.x + iB * rB.x * rB.x;
            K.solveToOut(C, impulse);
            impulse.negated();

            cA.x -= mA * impulse.x;
            cA.y -= mA * impulse.y;
            aA -= iA * v2.cross(rA, impulse);

            cB.x += mB * impulse.x;
            cB.y += mB * impulse.y;
            aB += iB * v2.cross(rB, impulse);

            pool.pushMat22(1);
        }
        
        data.positions[m_indexA].a = aA;
        
        data.positions[m_indexB].a = aB;

        pool.pushRot(2);

        return positionError <= Settings.linearSlop && angularError <= Settings.angularSlop;
    }

    public v2 getLocalAnchorA() {
        return localAnchorA;
    }

    public v2 getLocalAnchorB() {
        return localAnchorB;
    }

    public float getReferenceAngle() {
        return m_referenceAngle;
    }

    @Override
    public void getAnchorA(v2 argOut) {
        A.getWorldPointToOut(localAnchorA, argOut);
    }

    @Override
    public void getAnchorB(v2 argOut) {
        B.getWorldPointToOut(localAnchorB, argOut);
    }

    @Override
    public void getReactionForce(float inv_dt, v2 argOut) {
        argOut.set(m_impulse.x, m_impulse.y).scaled(inv_dt);
    }

    @Override
    public float getReactionTorque(float inv_dt) {
        return inv_dt * m_impulse.z;
    }

    public float getJointAngle() {
        var b1 = A;
        var b2 = B;
        return b2.sweep.a - b1.sweep.a - m_referenceAngle;
    }

    public float getJointSpeed() {
        var b1 = A;
        var b2 = B;
        return b2.velAngular - b1.velAngular;
    }

    public boolean isMotorEnabled() {
        return m_enableMotor;
    }

    public void enableMotor(boolean flag) {
        A.setAwake(true);
        B.setAwake(true);
        m_enableMotor = flag;
    }

    public float getMotorTorque(float inv_dt) {
        return m_motorImpulse * inv_dt;
    }

    public void setMotorSpeed(float speed) {
        A.setAwake(true);
        B.setAwake(true);
        m_motorSpeed = speed;
    }

    public void setMaxMotorTorque(float torque) {
        A.setAwake(true);
        B.setAwake(true);
        m_maxMotorTorque = torque;
    }

    public float getMotorSpeed() {
        return m_motorSpeed;
    }

    public float getMaxMotorTorque() {
        return m_maxMotorTorque;
    }

    public boolean isLimitEnabled() {
        return m_enableLimit;
    }

    public void enableLimit(boolean flag) {
        if (flag != m_enableLimit) {
            A.setAwake(true);
            B.setAwake(true);
            m_enableLimit = flag;
            m_impulse.z = 0.0f;
        }
    }

    public float getLowerLimit() {
        return m_lowerAngle;
    }

    public float getUpperLimit() {
        return m_upperAngle;
    }

    public void setLimits(float lower, float upper) {
        assert (lower <= upper);
        if (lower != m_lowerAngle || upper != m_upperAngle) {
            A.setAwake(true);
            B.setAwake(true);
            m_impulse.z = 0.0f;
            m_lowerAngle = lower;
            m_upperAngle = upper;
        }
    }
}
