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
 * Created at 3:38:38 AM Jan 15, 2011
 */
/**
 * Created at 3:38:38 AM Jan 15, 2011
 */
package spacegraph.space2d.phys.dynamics.joints;

import spacegraph.space2d.phys.common.*;
import spacegraph.space2d.phys.dynamics.Dynamics2D;
import spacegraph.space2d.phys.dynamics.SolverData;
import spacegraph.space2d.phys.pooling.IWorldPool;
import spacegraph.util.math.Tuple2f;
import spacegraph.util.math.v2;















/**
 * A weld joint essentially glues two bodies together. A weld joint may distort somewhat because the
 * island constraint solver is approximate.
 *
 * @author Daniel Murphy
 */
public class WeldJoint extends Joint {

    private float m_frequencyHz;
    private float m_dampingRatio;
    private float m_bias;

    
    private final Tuple2f m_localAnchorA;
    private final Tuple2f m_localAnchorB;
    private final float m_referenceAngle;
    private float m_gamma;
    private final Vec3 m_impulse;


    
    private int m_indexA;
    private int m_indexB;
    private final Tuple2f m_rA = new v2();
    private final Tuple2f m_rB = new v2();
    private final Tuple2f m_localCenterA = new v2();
    private final Tuple2f m_localCenterB = new v2();
    private float m_invMassA;
    private float m_invMassB;
    private float m_invIA;
    private float m_invIB;
    private final Mat33 m_mass = new Mat33();

    public WeldJoint(Dynamics2D w, WeldJointDef def) {
        this(w.pool, def);
    }

    public WeldJoint(IWorldPool argWorld, WeldJointDef def) {
        super(argWorld, def);
        m_localAnchorA = new v2(def.localAnchorA);
        m_localAnchorB = new v2(def.localAnchorB);
        m_referenceAngle = def.referenceAngle;
        m_frequencyHz = def.frequencyHz;
        m_dampingRatio = def.dampingRatio;

        m_impulse = new Vec3();
        m_impulse.setZero();
    }

    public float getReferenceAngle() {
        return m_referenceAngle;
    }

    public Tuple2f getLocalAnchorA() {
        return m_localAnchorA;
    }

    public Tuple2f getLocalAnchorB() {
        return m_localAnchorB;
    }

    public float getFrequency() {
        return m_frequencyHz;
    }

    public void setFrequency(float frequencyHz) {
        this.m_frequencyHz = frequencyHz;
    }

    public float getDampingRatio() {
        return m_dampingRatio;
    }

    public void setDampingRatio(float dampingRatio) {
        this.m_dampingRatio = dampingRatio;
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
        argOut.set(m_impulse.x, m_impulse.y);
        argOut.scaled(inv_dt);
    }

    @Override
    public float getReactionTorque(float inv_dt) {
        return inv_dt * m_impulse.z;
    }

    @Override
    public void initVelocityConstraints(final SolverData data) {
        m_indexA = A.island;
        m_indexB = B.island;
        m_localCenterA.set(A.sweep.localCenter);
        m_localCenterB.set(B.sweep.localCenter);
        m_invMassA = A.m_invMass;
        m_invMassB = B.m_invMass;
        m_invIA = A.m_invI;
        m_invIB = B.m_invI;

        
        float aA = data.positions[m_indexA].a;
        Tuple2f vA = data.velocities[m_indexA];
        float wA = data.velocities[m_indexA].w;

        
        float aB = data.positions[m_indexB].a;
        Tuple2f vB = data.velocities[m_indexB];
        float wB = data.velocities[m_indexB].w;

        final Rot qA = pool.popRot();
        final Rot qB = pool.popRot();
        final Tuple2f temp = pool.popVec2();

        qA.set(aA);
        qB.set(aB);

        
        Rot.mulToOutUnsafe(qA, temp.set(m_localAnchorA).subbed(m_localCenterA), m_rA);
        Rot.mulToOutUnsafe(qB, temp.set(m_localAnchorB).subbed(m_localCenterB), m_rB);

        
        
        

        
        
        
        

        float mA = m_invMassA, mB = m_invMassB;
        float iA = m_invIA, iB = m_invIB;

        final Mat33 K = pool.popMat33();

        K.ex.x = mA + mB + m_rA.y * m_rA.y * iA + m_rB.y * m_rB.y * iB;
        K.ey.x = -m_rA.y * m_rA.x * iA - m_rB.y * m_rB.x * iB;
        K.ez.x = -m_rA.y * iA - m_rB.y * iB;
        K.ex.y = K.ey.x;
        K.ey.y = mA + mB + m_rA.x * m_rA.x * iA + m_rB.x * m_rB.x * iB;
        K.ez.y = m_rA.x * iA + m_rB.x * iB;
        K.ex.z = K.ez.x;
        K.ey.z = K.ez.y;
        K.ez.z = iA + iB;

        if (m_frequencyHz > 0.0f) {
            K.getInverse22(m_mass);

            float invM = iA + iB;
            float m = invM > 0.0f ? 1.0f / invM : 0.0f;

            float C = aB - aA - m_referenceAngle;

            
            float omega = 2.0f * MathUtils.PI * m_frequencyHz;

            
            float d = 2.0f * m * m_dampingRatio * omega;

            
            float k = m * omega * omega;

            
            float h = data.step.dt;
            m_gamma = h * (d + h * k);
            m_gamma = m_gamma != 0.0f ? 1.0f / m_gamma : 0.0f;
            m_bias = C * h * k * m_gamma;

            invM += m_gamma;
            m_mass.ez.z = invM != 0.0f ? 1.0f / invM : 0.0f;
        } else {
            K.getSymInverse33(m_mass);
            m_gamma = 0.0f;
            m_bias = 0.0f;
        }

        if (data.step.warmStarting) {
            final Tuple2f P = pool.popVec2();
            
            m_impulse.mulLocal(data.step.dtRatio);

            P.set(m_impulse.x, m_impulse.y);

            vA.x -= mA * P.x;
            vA.y -= mA * P.y;
            wA -= iA * (Tuple2f.cross(m_rA, P) + m_impulse.z);

            vB.x += mB * P.x;
            vB.y += mB * P.y;
            wB += iB * (Tuple2f.cross(m_rB, P) + m_impulse.z);
            pool.pushVec2(1);
        } else {
            m_impulse.setZero();
        }


        data.velocities[m_indexA].w = wA;

        data.velocities[m_indexB].w = wB;

        pool.pushVec2(1);
        pool.pushRot(2);
        pool.pushMat33(1);
    }

    @Override
    public void solveVelocityConstraints(final SolverData data) {
        Tuple2f vA = data.velocities[m_indexA];
        float wA = data.velocities[m_indexA].w;
        Tuple2f vB = data.velocities[m_indexB];
        float wB = data.velocities[m_indexB].w;

        float mA = m_invMassA, mB = m_invMassB;
        float iA = m_invIA, iB = m_invIB;

        final Tuple2f Cdot1 = pool.popVec2();
        final Tuple2f P = pool.popVec2();
        final Tuple2f temp = pool.popVec2();
        if (m_frequencyHz > 0.0f) {
            float Cdot2 = wB - wA;

            float impulse2 = -m_mass.ez.z * (Cdot2 + m_bias + m_gamma * m_impulse.z);
            m_impulse.z += impulse2;

            wA -= iA * impulse2;
            wB += iB * impulse2;

            Tuple2f.crossToOutUnsafe(wB, m_rB, Cdot1);
            Tuple2f.crossToOutUnsafe(wA, m_rA, temp);
            Cdot1.added(vB).subbed(vA).subbed(temp);

            final Tuple2f impulse1 = P;
            Mat33.mul22ToOutUnsafe(m_mass, Cdot1, impulse1);
            impulse1.negated();

            m_impulse.x += impulse1.x;
            m_impulse.y += impulse1.y;

            vA.x -= mA * P.x;
            vA.y -= mA * P.y;
            wA -= iA * Tuple2f.cross(m_rA, P);

            vB.x += mB * P.x;
            vB.y += mB * P.y;
            wB += iB * Tuple2f.cross(m_rB, P);
        } else {
            Tuple2f.crossToOutUnsafe(wA, m_rA, temp);
            Tuple2f.crossToOutUnsafe(wB, m_rB, Cdot1);
            Cdot1.added(vB).subbed(vA).subbed(temp);
            float Cdot2 = wB - wA;

            final Vec3 Cdot = pool.popVec3();
            Cdot.set(Cdot1.x, Cdot1.y, Cdot2);

            final Vec3 impulse = pool.popVec3();
            Mat33.mulToOutUnsafe(m_mass, Cdot, impulse);
            impulse.negateLocal();
            m_impulse.addLocal(impulse);

            P.set(impulse.x, impulse.y);

            vA.x -= mA * P.x;
            vA.y -= mA * P.y;
            wA -= iA * (Tuple2f.cross(m_rA, P) + impulse.z);

            vB.x += mB * P.x;
            vB.y += mB * P.y;
            wB += iB * (Tuple2f.cross(m_rB, P) + impulse.z);

            pool.pushVec3(2);
        }


        data.velocities[m_indexA].w = wA;

        data.velocities[m_indexB].w = wB;

        pool.pushVec2(3);
    }

    @Override
    public boolean solvePositionConstraints(final SolverData data) {
        Tuple2f cA = data.positions[m_indexA];
        float aA = data.positions[m_indexA].a;
        Tuple2f cB = data.positions[m_indexB];
        float aB = data.positions[m_indexB].a;
        final Rot qA = pool.popRot();
        final Rot qB = pool.popRot();
        final Tuple2f temp = pool.popVec2();
        final Tuple2f rA = pool.popVec2();
        final Tuple2f rB = pool.popVec2();

        qA.set(aA);
        qB.set(aB);

        float mA = m_invMassA, mB = m_invMassB;
        float iA = m_invIA, iB = m_invIB;

        Rot.mulToOutUnsafe(qA, temp.set(m_localAnchorA).subbed(m_localCenterA), rA);
        Rot.mulToOutUnsafe(qB, temp.set(m_localAnchorB).subbed(m_localCenterB), rB);
        float positionError, angularError;

        final Mat33 K = pool.popMat33();
        final Tuple2f C1 = pool.popVec2();
        final Tuple2f P = pool.popVec2();

        K.ex.x = mA + mB + rA.y * rA.y * iA + rB.y * rB.y * iB;
        K.ey.x = -rA.y * rA.x * iA - rB.y * rB.x * iB;
        K.ez.x = -rA.y * iA - rB.y * iB;
        K.ex.y = K.ey.x;
        K.ey.y = mA + mB + rA.x * rA.x * iA + rB.x * rB.x * iB;
        K.ez.y = rA.x * iA + rB.x * iB;
        K.ex.z = K.ez.x;
        K.ey.z = K.ez.y;
        K.ez.z = iA + iB;
        if (m_frequencyHz > 0.0f) {
            C1.set(cB).added(rB).subbed(cA).subbed(rA);

            positionError = C1.length();
            angularError = 0.0f;

            K.solve22ToOut(C1, P);
            P.negated();

            cA.x -= mA * P.x;
            cA.y -= mA * P.y;
            aA -= iA * Tuple2f.cross(rA, P);

            cB.x += mB * P.x;
            cB.y += mB * P.y;
            aB += iB * Tuple2f.cross(rB, P);
        } else {
            C1.set(cB).added(rB).subbed(cA).subbed(rA);
            float C2 = aB - aA - m_referenceAngle;

            positionError = C1.length();
            angularError = Math.abs(C2);

            final Vec3 C = pool.popVec3();
            final Vec3 impulse = pool.popVec3();
            C.set(C1.x, C1.y, C2);

            K.solve33ToOut(C, impulse);
            impulse.negateLocal();
            P.set(impulse.x, impulse.y);

            cA.x -= mA * P.x;
            cA.y -= mA * P.y;
            aA -= iA * (Tuple2f.cross(rA, P) + impulse.z);

            cB.x += mB * P.x;
            cB.y += mB * P.y;
            aB += iB * (Tuple2f.cross(rB, P) + impulse.z);
            pool.pushVec3(2);
        }


        data.positions[m_indexA].a = aA;

        data.positions[m_indexB].a = aB;

        pool.pushVec2(5);
        pool.pushRot(2);
        pool.pushMat33(1);

        return positionError <= Settings.linearSlop && angularError <= Settings.angularSlop;
    }
}
