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

import org.jbox2d.common.*;
import org.jbox2d.dynamics.SolverData;
import org.jbox2d.pooling.IWorldPool;
import spacegraph.math.Tuple2f;

/**
 * A mouse joint is used to make a point on a body track a specified world point. This a soft
 * constraint with a maximum force. This allows the constraint to stretch and without applying huge
 * forces. NOTE: this joint is not documented in the manual because it was developed to be used in
 * the testbed. If you want to learn how to use the mouse joint, look at the testbed.
 *
 * @author Daniel
 */
public class MouseJoint extends Joint {

    private final Tuple2f m_localAnchorB = new Vec2();
    private final Tuple2f m_targetA = new Vec2();
    private float m_frequencyHz;
    private float m_dampingRatio;
    private float m_beta;

    // Solver shared
    private final Vec2 m_impulse = new Vec2();
    private float m_maxForce;
    private float m_gamma;

    // Solver temp
    private int m_indexB;
    private final Tuple2f m_rB = new Vec2();
    private final Tuple2f m_localCenterB = new Vec2();
    private float m_invMassB;
    private float m_invIB;
    private final Mat22 m_mass = new Mat22();
    private final Tuple2f m_C = new Vec2();

    protected MouseJoint(IWorldPool argWorld, MouseJointDef def) {
        super(argWorld, def);
        assert (def.target.isValid());
        assert (def.maxForce >= 0);
        assert (def.frequencyHz >= 0);
        assert (def.dampingRatio >= 0);

        m_targetA.set(def.target);
        Transform.mulTransToOutUnsafe(m_bodyB.getXform(), m_targetA, m_localAnchorB);

        m_maxForce = def.maxForce;
        m_impulse.setZero();

        m_frequencyHz = def.frequencyHz;
        m_dampingRatio = def.dampingRatio;

        m_beta = 0;
        m_gamma = 0;
    }

    @Override
    public void getAnchorA(Tuple2f argOut) {
        argOut.set(m_targetA);
    }

    @Override
    public void getAnchorB(Tuple2f argOut) {
        m_bodyB.getWorldPointToOut(m_localAnchorB, argOut);
    }

    @Override
    public void getReactionForce(float invDt, Tuple2f argOut) {
        argOut.set(m_impulse).scaled(invDt);
    }

    @Override
    public float getReactionTorque(float invDt) {
        return invDt * 0.0f;
    }


    public void setTarget(Tuple2f target) {
        if (m_bodyB.isAwake() == false) {
            m_bodyB.setAwake(true);
        }
        m_targetA.set(target);
    }

    public Tuple2f getTarget() {
        return m_targetA;
    }

    // / set/get the maximum force in Newtons.
    public void setMaxForce(float force) {
        m_maxForce = force;
    }

    public float getMaxForce() {
        return m_maxForce;
    }

    // / set/get the frequency in Hertz.
    public void setFrequency(float hz) {
        m_frequencyHz = hz;
    }

    public float getFrequency() {
        return m_frequencyHz;
    }

    // / set/get the damping ratio (dimensionless).
    public void setDampingRatio(float ratio) {
        m_dampingRatio = ratio;
    }

    public float getDampingRatio() {
        return m_dampingRatio;
    }

    @Override
    public void initVelocityConstraints(final SolverData data) {
        m_indexB = m_bodyB.island;
        m_localCenterB.set(m_bodyB.sweep.localCenter);
        m_invMassB = m_bodyB.m_invMass;
        m_invIB = m_bodyB.m_invI;

        Tuple2f cB = data.positions[m_indexB].c;
        float aB = data.positions[m_indexB].a;
        Tuple2f vB = data.velocities[m_indexB].v;
        float wB = data.velocities[m_indexB].w;

        final Rot qB = pool.popRot();

        qB.set(aB);

        float mass = m_bodyB.getMass();

        // Frequency
        float omega = 2.0f * MathUtils.PI * m_frequencyHz;

        // Damping coefficient
        float d = 2.0f * mass * m_dampingRatio * omega;

        // Spring stiffness
        float k = mass * (omega * omega);

        // magic formulas
        // gamma has units of inverse mass.
        // beta has units of inverse time.
        float h = data.step.dt;
        assert (d + h * k > Settings.EPSILON);
        m_gamma = h * (d + h * k);
        if (m_gamma != 0.0f) {
            m_gamma = 1.0f / m_gamma;
        }
        m_beta = h * k * m_gamma;

        Tuple2f temp = pool.popVec2();

        // Compute the effective mass matrix.
        Rot.mulToOutUnsafe(qB, temp.set(m_localAnchorB).subbed(m_localCenterB), m_rB);

        // K = [(1/m1 + 1/m2) * eye(2) - skew(r1) * invI1 * skew(r1) - skew(r2) * invI2 * skew(r2)]
        // = [1/m1+1/m2 0 ] + invI1 * [r1.y*r1.y -r1.x*r1.y] + invI2 * [r1.y*r1.y -r1.x*r1.y]
        // [ 0 1/m1+1/m2] [-r1.x*r1.y r1.x*r1.x] [-r1.x*r1.y r1.x*r1.x]
        final Mat22 K = pool.popMat22();
        K.ex.x = m_invMassB + m_invIB * m_rB.y * m_rB.y + m_gamma;
        K.ex.y = -m_invIB * m_rB.x * m_rB.y;
        K.ey.x = K.ex.y;
        K.ey.y = m_invMassB + m_invIB * m_rB.x * m_rB.x + m_gamma;

        K.invertToOut(m_mass);

        m_C.set(cB).added(m_rB).subbed(m_targetA);
        m_C.scaled(m_beta);

        // Cheat with some damping
        wB *= 0.98f;

        if (data.step.warmStarting) {
            m_impulse.scaled(data.step.dtRatio);
            vB.x += m_invMassB * m_impulse.x;
            vB.y += m_invMassB * m_impulse.y;
            wB += m_invIB * Tuple2f.cross(m_rB, m_impulse);
        } else {
            m_impulse.setZero();
        }

//    data.velocities[m_indexB].v.set(vB);
        data.velocities[m_indexB].w = wB;

        pool.pushVec2(1);
        pool.pushMat22(1);
        pool.pushRot(1);
    }

    @Override
    public boolean solvePositionConstraints(final SolverData data) {
        return true;
    }

    @Override
    public void solveVelocityConstraints(final SolverData data) {

        Tuple2f vB = data.velocities[m_indexB].v;
        float wB = data.velocities[m_indexB].w;

        // Cdot = v + cross(w, r)
        final Tuple2f Cdot = pool.popVec2();
        Tuple2f.crossToOutUnsafe(wB, m_rB, Cdot);
        Cdot.added(vB);

        final Tuple2f impulse = pool.popVec2();
        final Tuple2f temp = pool.popVec2();

        temp.set(m_impulse).scaled(m_gamma).added(m_C).added(Cdot).negated();
        Mat22.mulToOutUnsafe(m_mass, temp, impulse);

        Tuple2f oldImpulse = temp;
        oldImpulse.set(m_impulse);
        m_impulse.addLocal(impulse);
        float maxImpulse = data.step.dt * m_maxForce;
        if (m_impulse.lengthSquared() > maxImpulse * maxImpulse) {
            m_impulse.scaled(maxImpulse / m_impulse.length());
        }
        impulse.set(m_impulse).subbed(oldImpulse);

        vB.x += m_invMassB * impulse.x;
        vB.y += m_invMassB * impulse.y;
        wB += m_invIB * Tuple2f.cross(m_rB, impulse);

//    data.velocities[m_indexB].v.set(vB);
        data.velocities[m_indexB].w = wB;

        pool.pushVec2(3);
    }

}
