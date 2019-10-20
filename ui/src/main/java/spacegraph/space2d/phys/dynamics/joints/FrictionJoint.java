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
 * Created at 7:27:32 AM Jan 20, 2011
 */
/**
 * Created at 7:27:32 AM Jan 20, 2011
 */
package spacegraph.space2d.phys.dynamics.joints;

import jcog.math.v2;
import spacegraph.space2d.phys.common.Mat22;
import spacegraph.space2d.phys.common.MathUtils;
import spacegraph.space2d.phys.common.Rot;
import spacegraph.space2d.phys.dynamics.SolverData;
import spacegraph.space2d.phys.pooling.IWorldPool;

/**
 * @author Daniel Murphy
 */
public class FrictionJoint extends Joint {

    private final v2 m_localAnchorA;
    private final v2 m_localAnchorB;

    
    private final v2 m_linearImpulse;
    private float m_angularImpulse;
    private float m_maxForce;
    private float m_maxTorque;

    
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
    private final Mat22 m_linearMass = new Mat22();
    private float m_angularMass;

    FrictionJoint(IWorldPool argWorldPool, FrictionJointDef def) {
        super(argWorldPool, def);
        m_localAnchorA = new v2(def.localAnchorA);
        m_localAnchorB = new v2(def.localAnchorB);

        m_linearImpulse = new v2();
        m_angularImpulse = 0.0f;

        m_maxForce = def.maxForce;
        m_maxTorque = def.maxTorque;
    }

    public v2 getLocalAnchorA() {
        return m_localAnchorA;
    }

    public v2 getLocalAnchorB() {
        return m_localAnchorB;
    }

    @Override
    public void getAnchorA(v2 argOut) {
        A.getWorldPointToOut(m_localAnchorA, argOut);
    }

    @Override
    public void getAnchorB(v2 argOut) {
        B.getWorldPointToOut(m_localAnchorB, argOut);
    }

    @Override
    public void getReactionForce(float inv_dt, v2 argOut) {
        argOut.set(m_linearImpulse).scaled(inv_dt);
    }

    @Override
    public float getReactionTorque(float inv_dt) {
        return inv_dt * m_angularImpulse;
    }

    public void setMaxForce(float force) {
        assert (force >= 0.0f);
        m_maxForce = force;
    }

    public float getMaxForce() {
        return m_maxForce;
    }

    public void setMaxTorque(float torque) {
        assert (torque >= 0.0f);
        m_maxTorque = torque;
    }

    public float getMaxTorque() {
        return m_maxTorque;
    }

    /**
     * @see Joint#initVelocityConstraints(org.jbox2d.dynamics.TimeStep)
     */
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


        var temp = pool.popVec2();
        var qA = pool.popRot();
        var qB = pool.popRot();

        qA.set(aA);
        qB.set(aB);

        
        Rot.mulToOutUnsafe(qA, temp.set(m_localAnchorA).subbed(m_localCenterA), m_rA);
        Rot.mulToOutUnsafe(qB, temp.set(m_localAnchorB).subbed(m_localCenterB), m_rB);

        
        
        

        
        
        
        

        float mA = m_invMassA, mB = m_invMassB;
        float iA = m_invIA, iB = m_invIB;

        var K = pool.popMat22();
        K.ex.x = mA + mB + iA * m_rA.y * m_rA.y + iB * m_rB.y * m_rB.y;
        K.ex.y = -iA * m_rA.x * m_rA.y - iB * m_rB.x * m_rB.y;
        K.ey.x = K.ex.y;
        K.ey.y = mA + mB + iA * m_rA.x * m_rA.x + iB * m_rB.x * m_rB.x;

        K.invertToOut(m_linearMass);

        m_angularMass = iA + iB;
        if (m_angularMass > 0.0f) {
            m_angularMass = 1.0f / m_angularMass;
        }

        if (data.step.warmStarting) {
            
            m_linearImpulse.scaled(data.step.dtRatio);
            m_angularImpulse *= data.step.dtRatio;

            var P = pool.popVec2();
            P.set(m_linearImpulse);

            temp.set(P).scaled(mA);
            vA.subbed(temp);
            wA -= iA * (v2.cross(m_rA, P) + m_angularImpulse);

            temp.set(P).scaled(mB);
            vB.added(temp);
            wB += iB * (v2.cross(m_rB, P) + m_angularImpulse);

            pool.pushVec2(1);
        } else {
            m_linearImpulse.setZero();
            m_angularImpulse = 0.0f;
        }

        assert !(data.velocities[m_indexA].w != wA) || (data.velocities[m_indexA].w != wA);
        data.velocities[m_indexA].w = wA;

        data.velocities[m_indexB].w = wB;

        pool.pushRot(2);
        pool.pushVec2(1);
        pool.pushMat22(1);
    }

    @Override
    public void solveVelocityConstraints(SolverData data) {
        v2 vA = data.velocities[m_indexA];
        var wA = data.velocities[m_indexA].w;
        v2 vB = data.velocities[m_indexB];
        var wB = data.velocities[m_indexB].w;

        float mA = m_invMassA, mB = m_invMassB;
        float iA = m_invIA, iB = m_invIB;

        var h = data.step.dt;

        
        {
            var Cdot = wB - wA;
            var impulse = -m_angularMass * Cdot;

            var oldImpulse = m_angularImpulse;
            var maxImpulse = h * m_maxTorque;
            m_angularImpulse = MathUtils.clamp(m_angularImpulse + impulse, -maxImpulse, maxImpulse);
            impulse = m_angularImpulse - oldImpulse;

            wA -= iA * impulse;
            wB += iB * impulse;
        }

        
        {
            var Cdot = pool.popVec2();
            var temp = pool.popVec2();

            v2.crossToOutUnsafe(wA, m_rA, temp);
            v2.crossToOutUnsafe(wB, m_rB, Cdot);
            Cdot.added(vB).subbed(vA).subbed(temp);

            var impulse = pool.popVec2();
            Mat22.mulToOutUnsafe(m_linearMass, Cdot, impulse);
            impulse.negated();


            var oldImpulse = pool.popVec2();
            oldImpulse.set(m_linearImpulse);
            m_linearImpulse.added(impulse);

            var maxImpulse = h * m_maxForce;

            if (m_linearImpulse.lengthSquared() > maxImpulse * maxImpulse) {
                m_linearImpulse.normalize();
                m_linearImpulse.scaled(maxImpulse);
            }

            impulse.set(m_linearImpulse).subbed(oldImpulse);

            temp.set(impulse).scaled(mA);
            vA.subbed(temp);
            wA -= iA * v2.cross(m_rA, impulse);

            temp.set(impulse).scaled(mB);
            vB.added(temp);
            wB += iB * v2.cross(m_rB, impulse);

        }


        assert !(data.velocities[m_indexA].w != wA) || (data.velocities[m_indexA].w != wA);
        data.velocities[m_indexA].w = wA;


        data.velocities[m_indexB].w = wB;

        pool.pushVec2(4);
    }

    @Override
    public boolean solvePositionConstraints(SolverData data) {
        return true;
    }
}
