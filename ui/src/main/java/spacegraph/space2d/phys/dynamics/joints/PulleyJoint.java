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
 * Created at 12:12:02 PM Jan 23, 2011
 */
/**
 * Created at 12:12:02 PM Jan 23, 2011
 */
package spacegraph.space2d.phys.dynamics.joints;

import spacegraph.space2d.phys.common.Rot;
import spacegraph.space2d.phys.common.Settings;
import spacegraph.space2d.phys.dynamics.SolverData;
import spacegraph.space2d.phys.pooling.IWorldPool;
import spacegraph.util.math.Tuple2f;
import spacegraph.util.math.v2;

/**
 * The pulley joint is connected to two bodies and two fixed ground points. The pulley supports a
 * ratio such that: length1 + ratio * length2 <= constant Yes, the force transmitted is scaled by
 * the ratio. Warning: the pulley joint can get a bit squirrelly by itself. They often work better
 * when combined with prismatic joints. You should also cover the the anchor points with static
 * shapes to prevent one side from going to zero length.
 *
 * @author Daniel Murphy
 */
public class PulleyJoint extends Joint {

    public static final float MIN_PULLEY_LENGTH = 2.0f;

    private final Tuple2f m_groundAnchorA = new v2();
    private final Tuple2f m_groundAnchorB = new v2();
    private final float m_lengthA;
    private final float m_lengthB;

    
    private final Tuple2f m_localAnchorA = new v2();
    private final Tuple2f m_localAnchorB = new v2();
    private final float m_constant;
    private final float m_ratio;
    private float m_impulse;

    
    private int m_indexA;
    private int m_indexB;
    private final Tuple2f m_uA = new v2();
    private final Tuple2f m_uB = new v2();
    private final Tuple2f m_rA = new v2();
    private final Tuple2f m_rB = new v2();
    private final Tuple2f m_localCenterA = new v2();
    private final Tuple2f m_localCenterB = new v2();
    private float m_invMassA;
    private float m_invMassB;
    private float m_invIA;
    private float m_invIB;
    private float m_mass;

    PulleyJoint(IWorldPool argWorldPool, PulleyJointDef def) {
        super(argWorldPool, def);
        m_groundAnchorA.set(def.groundAnchorA);
        m_groundAnchorB.set(def.groundAnchorB);
        m_localAnchorA.set(def.localAnchorA);
        m_localAnchorB.set(def.localAnchorB);

        assert (def.ratio != 0.0f);
        m_ratio = def.ratio;

        m_lengthA = def.lengthA;
        m_lengthB = def.lengthB;

        m_constant = def.lengthA + m_ratio * def.lengthB;
        m_impulse = 0.0f;
    }

    public float getLengthA() {
        return m_lengthA;
    }

    public float getLengthB() {
        return m_lengthB;
    }

    public float getCurrentLengthA() {
        final Tuple2f p = pool.popVec2();
        A.getWorldPointToOut(m_localAnchorA, p);
        p.subbed(m_groundAnchorA);
        float length = p.length();
        pool.pushVec2(1);
        return length;
    }

    public float getCurrentLengthB() {
        final Tuple2f p = pool.popVec2();
        B.getWorldPointToOut(m_localAnchorB, p);
        p.subbed(m_groundAnchorB);
        float length = p.length();
        pool.pushVec2(1);
        return length;
    }


    public Tuple2f getLocalAnchorA() {
        return m_localAnchorA;
    }

    public Tuple2f getLocalAnchorB() {
        return m_localAnchorB;
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
        argOut.set(m_uB).scaled(m_impulse).scaled(inv_dt);
    }

    @Override
    public float getReactionTorque(float inv_dt) {
        return 0f;
    }

    public Tuple2f getGroundAnchorA() {
        return m_groundAnchorA;
    }

    public Tuple2f getGroundAnchorB() {
        return m_groundAnchorB;
    }

    public float getLength1() {
        final Tuple2f p = pool.popVec2();
        A.getWorldPointToOut(m_localAnchorA, p);
        p.subbed(m_groundAnchorA);

        float len = p.length();
        pool.pushVec2(1);
        return len;
    }

    public float getLength2() {
        final Tuple2f p = pool.popVec2();
        B.getWorldPointToOut(m_localAnchorB, p);
        p.subbed(m_groundAnchorB);

        float len = p.length();
        pool.pushVec2(1);
        return len;
    }

    public float getRatio() {
        return m_ratio;
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

        Tuple2f cA = data.positions[m_indexA];
        float aA = data.positions[m_indexA].a;
        Tuple2f vA = data.velocities[m_indexA];
        float wA = data.velocities[m_indexA].w;

        Tuple2f cB = data.positions[m_indexB];
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

        m_uA.set(cA).added(m_rA).subbed(m_groundAnchorA);
        m_uB.set(cB).added(m_rB).subbed(m_groundAnchorB);

        float lengthA = m_uA.length();
        float lengthB = m_uB.length();

        if (lengthA > 10f * Settings.linearSlop) {
            m_uA.scaled(1.0f / lengthA);
        } else {
            m_uA.setZero();
        }

        if (lengthB > 10f * Settings.linearSlop) {
            m_uB.scaled(1.0f / lengthB);
        } else {
            m_uB.setZero();
        }

        
        float ruA = Tuple2f.cross(m_rA, m_uA);
        float ruB = Tuple2f.cross(m_rB, m_uB);

        float mA = m_invMassA + m_invIA * ruA * ruA;
        float mB = m_invMassB + m_invIB * ruB * ruB;

        m_mass = mA + m_ratio * m_ratio * mB;

        if (m_mass > 0.0f) {
            m_mass = 1.0f / m_mass;
        }

        if (data.step.warmStarting) {

            
            m_impulse *= data.step.dtRatio;

            
            final Tuple2f PA = pool.popVec2();
            final Tuple2f PB = pool.popVec2();

            PA.set(m_uA).scaled(-m_impulse);
            PB.set(m_uB).scaled(-m_ratio * m_impulse);

            vA.x += m_invMassA * PA.x;
            vA.y += m_invMassA * PA.y;
            wA += m_invIA * Tuple2f.cross(m_rA, PA);
            vB.x += m_invMassB * PB.x;
            vB.y += m_invMassB * PB.y;
            wB += m_invIB * Tuple2f.cross(m_rB, PB);

            pool.pushVec2(2);
        } else {
            m_impulse = 0.0f;
        }

        data.velocities[m_indexA].w = wA;

        data.velocities[m_indexB].w = wB;

        pool.pushVec2(1);
        pool.pushRot(2);
    }

    @Override
    public void solveVelocityConstraints(final SolverData data) {
        Tuple2f vA = data.velocities[m_indexA];
        float wA = data.velocities[m_indexA].w;
        Tuple2f vB = data.velocities[m_indexB];
        float wB = data.velocities[m_indexB].w;

        final Tuple2f vpA = pool.popVec2();
        final Tuple2f vpB = pool.popVec2();
        final Tuple2f PA = pool.popVec2();
        final Tuple2f PB = pool.popVec2();

        Tuple2f.crossToOutUnsafe(wA, m_rA, vpA);
        vpA.added(vA);
        Tuple2f.crossToOutUnsafe(wB, m_rB, vpB);
        vpB.added(vB);

        float Cdot = -Tuple2f.dot(m_uA, vpA) - m_ratio * Tuple2f.dot(m_uB, vpB);
        float impulse = -m_mass * Cdot;
        m_impulse += impulse;

        PA.set(m_uA).scaled(-impulse);
        PB.set(m_uB).scaled(-m_ratio * impulse);
        vA.x += m_invMassA * PA.x;
        vA.y += m_invMassA * PA.y;
        wA += m_invIA * Tuple2f.cross(m_rA, PA);
        vB.x += m_invMassB * PB.x;
        vB.y += m_invMassB * PB.y;
        wB += m_invIB * Tuple2f.cross(m_rB, PB);


        data.velocities[m_indexA].w = wA;

        data.velocities[m_indexB].w = wB;

        pool.pushVec2(4);
    }

    @Override
    public boolean solvePositionConstraints(final SolverData data) {
        final Rot qA = pool.popRot();
        final Rot qB = pool.popRot();
        final Tuple2f rA = pool.popVec2();
        final Tuple2f rB = pool.popVec2();
        final Tuple2f uA = pool.popVec2();
        final Tuple2f uB = pool.popVec2();
        final Tuple2f temp = pool.popVec2();
        final Tuple2f PA = pool.popVec2();
        final Tuple2f PB = pool.popVec2();

        Tuple2f cA = data.positions[m_indexA];
        float aA = data.positions[m_indexA].a;
        Tuple2f cB = data.positions[m_indexB];
        float aB = data.positions[m_indexB].a;

        qA.set(aA);
        qB.set(aB);

        Rot.mulToOutUnsafe(qA, temp.set(m_localAnchorA).subbed(m_localCenterA), rA);
        Rot.mulToOutUnsafe(qB, temp.set(m_localAnchorB).subbed(m_localCenterB), rB);

        uA.set(cA).added(rA).subbed(m_groundAnchorA);
        uB.set(cB).added(rB).subbed(m_groundAnchorB);

        float lengthA = uA.length();
        float lengthB = uB.length();

        if (lengthA > 10.0f * Settings.linearSlop) {
            uA.scaled(1.0f / lengthA);
        } else {
            uA.setZero();
        }

        if (lengthB > 10.0f * Settings.linearSlop) {
            uB.scaled(1.0f / lengthB);
        } else {
            uB.setZero();
        }

        
        float ruA = Tuple2f.cross(rA, uA);
        float ruB = Tuple2f.cross(rB, uB);

        float mA = m_invMassA + m_invIA * ruA * ruA;
        float mB = m_invMassB + m_invIB * ruB * ruB;

        float mass = mA + m_ratio * m_ratio * mB;

        if (mass > 0.0f) {
            mass = 1.0f / mass;
        }

        float C = m_constant - lengthA - m_ratio * lengthB;
        float linearError = Math.abs(C);

        float impulse = -mass * C;

        PA.set(uA).scaled(-impulse);
        PB.set(uB).scaled(-m_ratio * impulse);

        cA.x += m_invMassA * PA.x;
        cA.y += m_invMassA * PA.y;
        aA += m_invIA * Tuple2f.cross(rA, PA);
        cB.x += m_invMassB * PB.x;
        cB.y += m_invMassB * PB.y;
        aB += m_invIB * Tuple2f.cross(rB, PB);


        data.positions[m_indexA].a = aA;

        data.positions[m_indexB].a = aB;

        pool.pushRot(2);
        pool.pushVec2(7);

        return linearError < Settings.linearSlop;
    }
}
