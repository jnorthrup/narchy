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
package spacegraph.space2d.phys.dynamics.contacts;

import jcog.math.v2;
import spacegraph.space2d.phys.collision.Manifold;
import spacegraph.space2d.phys.collision.ManifoldPoint;
import spacegraph.space2d.phys.collision.WorldManifold;
import spacegraph.space2d.phys.collision.shapes.Shape;
import spacegraph.space2d.phys.common.*;
import spacegraph.space2d.phys.dynamics.Body2D;
import spacegraph.space2d.phys.dynamics.Fixture;
import spacegraph.space2d.phys.dynamics.TimeStep;
import spacegraph.space2d.phys.dynamics.contacts.ContactVelocityConstraint.VelocityConstraintPoint;

/**
 * @author Daniel
 */
public class ContactSolver {

    private static final boolean DEBUG_SOLVER = false;
    private static final float k_errorTol = 1.0e-3f;
    /**
     * For each solver, this is the initial number of constraints in the array, which expands as
     * needed.
     */
    private static final int INITIAL_NUM_CONSTRAINTS = 256;

    /**
     * Ensure a reasonable condition number. for the block solver
     */
    private static final float k_maxConditionNumber = 100.0f;

    private Position[] m_positions;
    private Velocity[] m_velocities;
    private ContactPositionConstraint[] m_positionConstraints;
    public ContactVelocityConstraint[] m_velocityConstraints;
    private Contact[] m_contacts;
    private int m_count;

    public ContactSolver() {
        m_positionConstraints = new ContactPositionConstraint[INITIAL_NUM_CONSTRAINTS];
        m_velocityConstraints = new ContactVelocityConstraint[INITIAL_NUM_CONSTRAINTS];
        for (var i = 0; i < INITIAL_NUM_CONSTRAINTS; i++) {
            m_positionConstraints[i] = new ContactPositionConstraint();
            m_velocityConstraints[i] = new ContactVelocityConstraint();
        }
    }

    public final void init(ContactSolverDef def) {

        var m_step = def.step;
        m_count = def.count;

        if (m_positionConstraints.length < m_count) {
            var old = m_positionConstraints;
            m_positionConstraints = new ContactPositionConstraint[MathUtils.max(old.length * 2, m_count)];
            System.arraycopy(old, 0, m_positionConstraints, 0, old.length);
            for (var i = old.length; i < m_positionConstraints.length; i++) {
                m_positionConstraints[i] = new ContactPositionConstraint();
            }
        }

        if (m_velocityConstraints.length < m_count) {
            var old = m_velocityConstraints;
            m_velocityConstraints = new ContactVelocityConstraint[MathUtils.max(old.length * 2, m_count)];
            System.arraycopy(old, 0, m_velocityConstraints, 0, old.length);
            for (var i = old.length; i < m_velocityConstraints.length; i++) {
                m_velocityConstraints[i] = new ContactVelocityConstraint();
            }
        }

        m_positions = def.positions;
        m_velocities = def.velocities;
        m_contacts = def.contacts;

        for (var i = 0; i < m_count; ++i) {

            var contact = m_contacts[i];

            var fixtureA = contact.aFixture;
            var fixtureB = contact.bFixture;
            var shapeA = fixtureA.shape();
            var shapeB = fixtureB.shape();
            var radiusA = shapeA.skinRadius;
            var radiusB = shapeB.skinRadius;
            var bodyA = fixtureA.getBody();
            var bodyB = fixtureB.getBody();
            var manifold = contact.getManifold();

            var pointCount = manifold.pointCount;
            assert (pointCount > 0);

            var vc = m_velocityConstraints[i];
            vc.friction = contact.m_friction;
            vc.restitution = contact.m_restitution;
            vc.tangentSpeed = contact.m_tangentSpeed;
            vc.indexA = bodyA.island;
            vc.indexB = bodyB.island;
            vc.invMassA = bodyA.m_invMass;
            vc.invMassB = bodyB.m_invMass;
            vc.invIA = bodyA.m_invI;
            vc.invIB = bodyB.m_invI;
            vc.contactIndex = i;
            vc.pointCount = pointCount;
            vc.K.setZero();
            vc.normalMass.setZero();

            var pc = m_positionConstraints[i];
            pc.indexA = bodyA.island;
            pc.indexB = bodyB.island;
            pc.invMassA = bodyA.m_invMass;
            pc.invMassB = bodyB.m_invMass;
            pc.localCenterA.set(bodyA.sweep.localCenter);
            pc.localCenterB.set(bodyB.sweep.localCenter);
            pc.invIA = bodyA.m_invI;
            pc.invIB = bodyB.m_invI;
            pc.localNormal.set(manifold.localNormal);
            pc.localPoint.set(manifold.localPoint);
            pc.pointCount = pointCount;
            pc.radiusA = radiusA;
            pc.radiusB = radiusB;
            pc.type = manifold.type;

            
            for (var j = 0; j < pointCount; j++) {
                var cp = manifold.points[j];
                var vcp = vc.points[j];

                if (m_step.warmStarting) {
                    
                    
                    vcp.normalImpulse = m_step.dtRatio * cp.normalImpulse;
                    vcp.tangentImpulse = m_step.dtRatio * cp.tangentImpulse;
                } else {
                    vcp.normalImpulse = 0;
                    vcp.tangentImpulse = 0;
                }

                vcp.rA.setZero();
                vcp.rB.setZero();
                vcp.normalMass = 0;
                vcp.tangentMass = 0;
                vcp.velocityBias = 0;
                pc.localPoints[j].x = cp.localPoint.x;
                pc.localPoints[j].y = cp.localPoint.y;
            }
        }
    }

    public void warmStart() {
        
        for (var i = 0; i < m_count; ++i) {
            var vc = m_velocityConstraints[i];

            var indexA = vc.indexA;
            var indexB = vc.indexB;
            var mA = vc.invMassA;
            var iA = vc.invIA;
            var mB = vc.invMassB;
            var iB = vc.invIB;
            var pointCount = vc.pointCount;

            v2 vA = m_velocities[indexA];
            var wA = m_velocities[indexA].w;
            v2 vB = m_velocities[indexB];
            var wB = m_velocities[indexB].w;

            var normal = vc.normal;
            var tangentx = 1.0f * normal.y;
            var tangenty = -1.0f * normal.x;

            for (var j = 0; j < pointCount; ++j) {
                var vcp = vc.points[j];
                var Px = tangentx * vcp.tangentImpulse + normal.x * vcp.normalImpulse;
                var Py = tangenty * vcp.tangentImpulse + normal.y * vcp.normalImpulse;

                wA -= iA * (vcp.rA.x * Py - vcp.rA.y * Px);
                vA.x -= Px * mA;
                vA.y -= Py * mA;
                wB += iB * (vcp.rB.x * Py - vcp.rB.y * Px);
                vB.x += Px * mB;
                vB.y += Py * mB;
            }
            m_velocities[indexA].w = wA;
            m_velocities[indexB].w = wB;
        }
    }

    
    private final Transform xfA = new Transform();
    private final Transform xfB = new Transform();
    private final WorldManifold worldManifold = new WorldManifold();

    public final void initializeVelocityConstraints() {

        
        for (var i = 0; i < m_count; ++i) {
            var vc = m_velocityConstraints[i];
            var pc = m_positionConstraints[i];

            var radiusA = pc.radiusA;
            var radiusB = pc.radiusB;
            var manifold = m_contacts[vc.contactIndex].getManifold();

            var indexA = vc.indexA;
            var indexB = vc.indexB;

            var mA = vc.invMassA;
            var mB = vc.invMassB;
            var iA = vc.invIA;
            var iB = vc.invIB;
            var localCenterA = pc.localCenterA;
            var localCenterB = pc.localCenterB;

            v2 cA = m_positions[indexA];
            var aA = m_positions[indexA].a;
            v2 vA = m_velocities[indexA];
            var wA = m_velocities[indexA].w;

            v2 cB = m_positions[indexB];
            var aB = m_positions[indexB].a;
            v2 vB = m_velocities[indexB];
            var wB = m_velocities[indexB].w;

            assert (manifold.pointCount > 0);

            Rot xfAq = xfA;
            Rot xfBq = xfB;
            xfAq.set(aA);
            xfBq.set(aB);
            xfA.pos.x = cA.x - (xfAq.c * localCenterA.x - xfAq.s * localCenterA.y);
            xfA.pos.y = cA.y - (xfAq.s * localCenterA.x + xfAq.c * localCenterA.y);
            xfB.pos.x = cB.x - (xfBq.c * localCenterB.x - xfBq.s * localCenterB.y);
            xfB.pos.y = cB.y - (xfBq.s * localCenterB.x + xfBq.c * localCenterB.y);

            worldManifold.initialize(manifold, xfA, radiusA, xfB, radiusB);

            var vcnormal = vc.normal;
            vcnormal.x = worldManifold.normal.x;
            vcnormal.y = worldManifold.normal.y;

            var pointCount = vc.pointCount;
            for (var j = 0; j < pointCount; ++j) {
                var vcp = vc.points[j];
                var wmPj = worldManifold.points[j];
                var vcprA = vcp.rA;
                var vcprB = vcp.rB;
                vcprA.x = wmPj.x - cA.x;
                vcprA.y = wmPj.y - cA.y;
                vcprB.x = wmPj.x - cB.x;
                vcprB.y = wmPj.y - cB.y;

                var rnA = vcprA.x * vcnormal.y - vcprA.y * vcnormal.x;
                var rnB = vcprB.x * vcnormal.y - vcprB.y * vcnormal.x;

                var kNormal = mA + mB + iA * rnA * rnA + iB * rnB * rnB;

                vcp.normalMass = kNormal > 0.0f ? 1.0f / kNormal : 0.0f;

                var tangentx = 1.0f * vcnormal.y;
                var tangenty = -1.0f * vcnormal.x;

                var rtA = vcprA.x * tangenty - vcprA.y * tangentx;
                var rtB = vcprB.x * tangenty - vcprB.y * tangentx;

                var kTangent = mA + mB + iA * rtA * rtA + iB * rtB * rtB;

                vcp.tangentMass = kTangent > 0.0f ? 1.0f / kTangent : 0.0f;

                
                vcp.velocityBias = 0.0f;
                var tempx = vB.x + -wB * vcprB.y - vA.x - (-wA * vcprA.y);
                var tempy = vB.y + wB * vcprB.x - vA.y - (wA * vcprA.x);
                var vRel = vcnormal.x * tempx + vcnormal.y * tempy;
                if (vRel < -Settings.velocityThreshold) {
                    vcp.velocityBias = -vc.restitution * vRel;
                }
            }

            
            if (vc.pointCount == 2) {
                var vcp1 = vc.points[0];
                var vcp2 = vc.points[1];
                var rn1A = vcp1.rA.x * vcnormal.y - vcp1.rA.y * vcnormal.x;
                var rn1B = vcp1.rB.x * vcnormal.y - vcp1.rB.y * vcnormal.x;
                var rn2A = vcp2.rA.x * vcnormal.y - vcp2.rA.y * vcnormal.x;
                var rn2B = vcp2.rB.x * vcnormal.y - vcp2.rB.y * vcnormal.x;

                var k11 = mA + mB + iA * rn1A * rn1A + iB * rn1B * rn1B;
                var k22 = mA + mB + iA * rn2A * rn2A + iB * rn2B * rn2B;
                var k12 = mA + mB + iA * rn1A * rn2A + iB * rn1B * rn2B;
                if (k11 * k11 < k_maxConditionNumber * (k11 * k22 - k12 * k12)) {
                    
                    vc.K.ex.x = k11;
                    vc.K.ex.y = k12;
                    vc.K.ey.x = k12;
                    vc.K.ey.y = k22;
                    vc.K.invertToOut(vc.normalMass);
                } else {
                    
                    
                    vc.pointCount = 1;
                }
            }
        }
    }


    public final void solveVelocityConstraints() {
        for (var i = 0; i < m_count; ++i) {
            var vc = m_velocityConstraints[i];

            var indexA = vc.indexA;
            var indexB = vc.indexB;

            var mA = vc.invMassA;
            var mB = vc.invMassB;
            var iA = vc.invIA;
            var iB = vc.invIB;
            var pointCount = vc.pointCount;

            v2 vA = m_velocities[indexA];
            var wA = m_velocities[indexA].w;
            v2 vB = m_velocities[indexB];
            var wB = m_velocities[indexB].w;

            var normal = vc.normal;
            var normalx = normal.x;
            var normaly = normal.y;
            var tangentx = 1.0f * vc.normal.y;
            var tangenty = -1.0f * vc.normal.x;
            var friction = vc.friction;

            assert (pointCount == 1 || pointCount == 2);

            
            for (var j = 0; j < pointCount; ++j) {
                var vcp = vc.points[j];
                var a = vcp.rA;
                var dvx = -wB * vcp.rB.y + vB.x - vA.x + wA * a.y;
                var dvy = wB * vcp.rB.x + vB.y - vA.y - wA * a.x;


                var vt = dvx * tangentx + dvy * tangenty - vc.tangentSpeed;
                var lambda = vcp.tangentMass * (-vt);


                var maxFriction = friction * vcp.normalImpulse;
                var newImpulse =
                        MathUtils.clamp(vcp.tangentImpulse + lambda, -maxFriction, maxFriction);
                lambda = newImpulse - vcp.tangentImpulse;
                vcp.tangentImpulse = newImpulse;


                var Px = tangentx * lambda;
                var Py = tangenty * lambda;

                
                vA.x -= Px * mA;
                vA.y -= Py * mA;
                wA -= iA * (vcp.rA.x * Py - vcp.rA.y * Px);

                
                vB.x += Px * mB;
                vB.y += Py * mB;
                wB += iB * (vcp.rB.x * Py - vcp.rB.y * Px);
            }

            
            if (vc.pointCount == 1) {
                var vcp = vc.points[0];


                var dvx = -wB * vcp.rB.y + vB.x - vA.x + wA * vcp.rA.y;
                var dvy = wB * vcp.rB.x + vB.y - vA.y - wA * vcp.rA.x;


                var vn = dvx * normalx + dvy * normaly;
                var lambda = -vcp.normalMass * (vn - vcp.velocityBias);


                var a = vcp.normalImpulse + lambda;
                var newImpulse = (Math.max(a, 0.0f));
                lambda = newImpulse - vcp.normalImpulse;
                vcp.normalImpulse = newImpulse;


                var Px = normalx * lambda;
                var Py = normaly * lambda;

                
                vA.x -= Px * mA;
                vA.y -= Py * mA;
                wA -= iA * (vcp.rA.x * Py - vcp.rA.y * Px);

                
                vB.x += Px * mB;
                vB.y += Py * mB;
                wB += iB * (vcp.rB.x * Py - vcp.rB.y * Px);
            } else {


                var cp1 = vc.points[0];
                var cp2 = vc.points[1];
                var cp1rA = cp1.rA;
                var cp1rB = cp1.rB;
                var cp2rA = cp2.rA;
                var cp2rB = cp2.rB;
                var ax = cp1.normalImpulse;
                var ay = cp2.normalImpulse;

                assert (ax >= 0.0f && ay >= 0.0f);


                var dv1x = -wB * cp1rB.y + vB.x - vA.x + wA * cp1rA.y;
                var dv1y = wB * cp1rB.x + vB.y - vA.y - wA * cp1rA.x;


                var dv2x = -wB * cp2rB.y + vB.x - vA.x + wA * cp2rA.y;
                var dv2y = wB * cp2rB.x + vB.y - vA.y - wA * cp2rA.x;


                var vn1 = dv1x * normalx + dv1y * normaly;
                var vn2 = dv2x * normalx + dv2y * normaly;

                var bx = vn1 - cp1.velocityBias;
                var by = vn2 - cp2.velocityBias;


                var R = vc.K;
                bx -= R.ex.x * ax + R.ey.x * ay;
                by -= R.ex.y * ax + R.ey.y * ay;

                
                
                for (; ; ) {


                    var R1 = vc.normalMass;
                    var xx = R1.ex.x * bx + R1.ey.x * by;
                    var xy = R1.ex.y * bx + R1.ey.y * by;
                    xx *= -1;
                    xy *= -1;

                    if (xx >= 0.0f && xy >= 0.0f) {


                        var dx = xx - ax;
                        var dy = xy - ay;


                        var P1x = dx * normalx;
                        var P2x = dy * normalx;

                        /*
                         * vA -= invMassA * (P1 + P2); wA -= invIA * (Cross(cp1.rA, P1) + Cross(cp2.rA, P2));
                         *
                         * vB += invMassB * (P1 + P2); wB += invIB * (Cross(cp1.rB, P1) + Cross(cp2.rB, P2));
                         */

                        vA.x -= mA * (P1x + P2x);
                        var P2y = dy * normaly;
                        var P1y = dx * normaly;
                        vA.y -= mA * (P1y + P2y);
                        vB.x += mB * (P1x + P2x);
                        vB.y += mB * (P1y + P2y);

                        wA -= iA * (cp1rA.x * P1y - cp1rA.y * P1x + (cp2rA.x * P2y - cp2rA.y * P2x));
                        wB += iB * (cp1rB.x * P1y - cp1rB.y * P1x + (cp2rB.x * P2y - cp2rB.y * P2x));

                        
                        cp1.normalImpulse = xx;
                        cp2.normalImpulse = xy;

                        /*
                         * #if B2_DEBUG_SOLVER == 1 
                         * Cross(wA, cp1.rA); dv2 = vB + Cross(wB, cp2.rB) - vA - Cross(wA, cp2.rA);
                         *
                         * 
                         *
                         * assert(Abs(vn1 - cp1.velocityBias) < k_errorTol); assert(Abs(vn2 - cp2.velocityBias)
                         * < k_errorTol); #endif
                         */
                        if (DEBUG_SOLVER) {

                            var dv1 = vB.addToNew(v2.cross(wB, cp1rB).subbed(vA).subbed(v2.cross(wA, cp1rA)));
                            var dv2 = vB.addToNew(v2.cross(wB, cp2rB).subbed(vA).subbed(v2.cross(wA, cp2rA)));
                            
                            vn1 = v2.dot(dv1, normal);
                            vn2 = v2.dot(dv2, normal);

                            assert (Math.abs(vn1 - cp1.velocityBias) < k_errorTol);
                            assert (Math.abs(vn2 - cp2.velocityBias) < k_errorTol);
                        }
                        break;
                    }

                    
                    
                    
                    
                    
                    
                    xx = -cp1.normalMass * bx;
                    xy = 0.0f;
                    vn1 = 0.0f;
                    vn2 = vc.K.ex.y * xx + by;

                    if (xx >= 0.0f && vn2 >= 0.0f) {

                        var dx = xx - ax;
                        var dy = xy - ay;


                        var P1x = normalx * dx;
                        var P2x = normalx * dy;

                        /*
                         * Vec2 P1 = d.x * normal; Vec2 P2 = d.y * normal; vA -= invMassA * (P1 + P2); wA -=
                         * invIA * (Cross(cp1.rA, P1) + Cross(cp2.rA, P2));
                         *
                         * vB += invMassB * (P1 + P2); wB += invIB * (Cross(cp1.rB, P1) + Cross(cp2.rB, P2));
                         */

                        vA.x -= mA * (P1x + P2x);
                        var P2y = normaly * dy;
                        var P1y = normaly * dx;
                        vA.y -= mA * (P1y + P2y);
                        vB.x += mB * (P1x + P2x);
                        vB.y += mB * (P1y + P2y);

                        wA -= iA * (cp1rA.x * P1y - cp1rA.y * P1x + (cp2rA.x * P2y - cp2rA.y * P2x));
                        wB += iB * (cp1rB.x * P1y - cp1rB.y * P1x + (cp2rB.x * P2y - cp2rB.y * P2x));

                        
                        cp1.normalImpulse = xx;
                        cp2.normalImpulse = xy;

                        /*
                         * #if B2_DEBUG_SOLVER == 1 
                         * Cross(wA, cp1.rA);
                         *
                         * 
                         *
                         * assert(Abs(vn1 - cp1.velocityBias) < k_errorTol); #endif
                         */
                        if (DEBUG_SOLVER) {

                            var dv1 = vB.addToNew(v2.cross(wB, cp1rB).subbed(vA).subbed(v2.cross(wA, cp1rA)));
                            
                            vn1 = v2.dot(dv1, normal);

                            assert (Math.abs(vn1 - cp1.velocityBias) < k_errorTol);
                        }
                        break;
                    }

                    
                    
                    
                    
                    
                    
                    xx = 0.0f;
                    xy = -cp2.normalMass * by;
                    vn1 = vc.K.ey.x * xy + bx;
                    vn2 = 0.0f;

                    if (xy >= 0.0f && vn1 >= 0.0f) {

                        var dx = xx - ax;
                        var dy = xy - ay;

                        
                        /*
                         * Vec2 P1 = d.x * normal; Vec2 P2 = d.y * normal; vA -= invMassA * (P1 + P2); wA -=
                         * invIA * (Cross(cp1.rA, P1) + Cross(cp2.rA, P2));
                         *
                         * vB += invMassB * (P1 + P2); wB += invIB * (Cross(cp1.rB, P1) + Cross(cp2.rB, P2));
                         */

                        var P1x = normalx * dx;
                        var P2x = normalx * dy;

                        vA.x -= mA * (P1x + P2x);
                        var P2y = normaly * dy;
                        var P1y = normaly * dx;
                        vA.y -= mA * (P1y + P2y);
                        vB.x += mB * (P1x + P2x);
                        vB.y += mB * (P1y + P2y);

                        wA -= iA * (cp1rA.x * P1y - cp1rA.y * P1x + (cp2rA.x * P2y - cp2rA.y * P2x));
                        wB += iB * (cp1rB.x * P1y - cp1rB.y * P1x + (cp2rB.x * P2y - cp2rB.y * P2x));

                        
                        cp1.normalImpulse = xx;
                        cp2.normalImpulse = xy;

                        /*
                         * #if B2_DEBUG_SOLVER == 1 
                         * Cross(wA, cp2.rA);
                         *
                         * 
                         *
                         * assert(Abs(vn2 - cp2.velocityBias) < k_errorTol); #endif
                         */
                        if (DEBUG_SOLVER) {

                            var dv2 = vB.addToNew(v2.cross(wB, cp2rB).subbed(vA).subbed(v2.cross(wA, cp2rA)));
                            
                            vn2 = v2.dot(dv2, normal);

                            assert (Math.abs(vn2 - cp2.velocityBias) < k_errorTol);
                        }
                        break;
                    }

                    
                    
                    
                    
                    
                    xx = 0.0f;
                    xy = 0.0f;
                    vn1 = bx;
                    vn2 = by;

                    if (vn1 >= 0.0f && vn2 >= 0.0f) {

                        var dx = xx - ax;
                        var dy = xy - ay;

                        
                        /*
                         * Vec2 P1 = d.x * normal; Vec2 P2 = d.y * normal; vA -= invMassA * (P1 + P2); wA -=
                         * invIA * (Cross(cp1.rA, P1) + Cross(cp2.rA, P2));
                         *
                         * vB += invMassB * (P1 + P2); wB += invIB * (Cross(cp1.rB, P1) + Cross(cp2.rB, P2));
                         */

                        var P1x = normalx * dx;
                        var P2x = normalx * dy;

                        vA.x -= mA * (P1x + P2x);
                        var P2y = normaly * dy;
                        var P1y = normaly * dx;
                        vA.y -= mA * (P1y + P2y);
                        vB.x += mB * (P1x + P2x);
                        vB.y += mB * (P1y + P2y);

                        wA -= iA * (cp1rA.x * P1y - cp1rA.y * P1x + (cp2rA.x * P2y - cp2rA.y * P2x));
                        wB += iB * (cp1rB.x * P1y - cp1rB.y * P1x + (cp2rB.x * P2y - cp2rB.y * P2x));

                        
                        cp1.normalImpulse = xx;
                        cp2.normalImpulse = xy;

                        break;
                    }

                    
                    break;
                }
            }

            
            m_velocities[indexA].w = wA;
            
            m_velocities[indexB].w = wB;
        }
    }

    public void storeImpulses() {
        for (var i = 0; i < m_count; i++) {
            var vc = m_velocityConstraints[i];
            var manifold = m_contacts[vc.contactIndex].getManifold();

            for (var j = 0; j < vc.pointCount; j++) {
                manifold.points[j].normalImpulse = vc.points[j].normalImpulse;
                manifold.points[j].tangentImpulse = vc.points[j].tangentImpulse;
            }
        }
    }

    /*
     * #if 0 
     * float minSeparation = 0.0f;
     *
     * for (int i = 0; i < m_constraintCount; ++i) { ContactConstraint* c = m_constraints + i; Body*
     * bodyA = c.bodyA; Body* bodyB = c.bodyB; float invMassA = bodyA.m_mass * bodyA.m_invMass; float
     * invIA = bodyA.m_mass * bodyA.m_invI; float invMassB = bodyB.m_mass * bodyB.m_invMass; float
     * invIB = bodyB.m_mass * bodyB.m_invI;
     *
     * Vec2 normal = c.normal;
     *
     * 
     * ccp = c.points + j;
     *
     * Vec2 r1 = Mul(bodyA.GetXForm().R, ccp.localAnchorA - bodyA.GetLocalCenter()); Vec2 r2 =
     * Mul(bodyB.GetXForm().R, ccp.localAnchorB - bodyB.GetLocalCenter());
     *
     * Vec2 p1 = bodyA.m_sweep.c + r1; Vec2 p2 = bodyB.m_sweep.c + r2; Vec2 dp = p2 - p1;
     *
     * 
     *
     * 
     *
     * 
     * _linearSlop), -_maxLinearCorrection, 0.0f);
     *
     * 
     *
     * Vec2 P = impulse * normal;
     *
     * bodyA.m_sweep.c -= invMassA * P; bodyA.m_sweep.a -= invIA * Cross(r1, P);
     * bodyA.SynchronizeTransform();
     *
     * bodyB.m_sweep.c += invMassB * P; bodyB.m_sweep.a += invIB * Cross(r2, P);
     * bodyB.SynchronizeTransform(); } }
     *
     * 
     * -_linearSlop. return minSeparation >= -1.5f * _linearSlop; }
     */

    private final PositionSolverManifold psolver = new PositionSolverManifold();

    /**
     * Sequential solver.
     */
    public final boolean solvePositionConstraints() {
        var minSeparation = 0.0f;

        for (var i = 0; i < m_count; ++i) {
            var pc = m_positionConstraints[i];

            var indexA = pc.indexA;
            var indexB = pc.indexB;

            var mA = pc.invMassA;
            var iA = pc.invIA;
            var localCenterA = pc.localCenterA;
            var localCenterAx = localCenterA.x;
            var localCenterAy = localCenterA.y;
            var mB = pc.invMassB;
            var iB = pc.invIB;
            var localCenterB = pc.localCenterB;
            var localCenterBx = localCenterB.x;
            var localCenterBy = localCenterB.y;
            var pointCount = pc.pointCount;

            v2 cA = m_positions[indexA];
            var aA = m_positions[indexA].a;
            v2 cB = m_positions[indexB];
            var aB = m_positions[indexB].a;

            
            for (var j = 0; j < pointCount; ++j) {
                Rot xfAq = xfA;
                Rot xfBq = xfB;
                xfAq.set(aA);
                xfBq.set(aB);
                xfA.pos.x = cA.x - xfAq.c * localCenterAx + xfAq.s * localCenterAy;
                xfA.pos.y = cA.y - xfAq.s * localCenterAx - xfAq.c * localCenterAy;
                xfB.pos.x = cB.x - xfBq.c * localCenterBx + xfBq.s * localCenterBy;
                xfB.pos.y = cB.y - xfBq.s * localCenterBx - xfBq.c * localCenterBy;

                var psm = psolver;
                psm.initialize(pc, xfA, xfB, j);
                var normal = psm.normal;
                var point = psm.point;
                var separation = psm.separation;

                var rAx = point.x - cA.x;
                var rAy = point.y - cA.y;
                var rBx = point.x - cB.x;
                var rBy = point.y - cB.y;

                
                minSeparation = MathUtils.min(minSeparation, separation);


                var C =
                        MathUtils.clamp(Settings.baumgarte * (separation + Settings.linearSlop),
                                -Settings.maxLinearCorrection, 0.0f);


                var rnA = rAx * normal.y - rAy * normal.x;
                var rnB = rBx * normal.y - rBy * normal.x;
                var K = mA + mB + iA * rnA * rnA + iB * rnB * rnB;


                var impulse = K > 0.0f ? -C / K : 0.0f;

                var Px = normal.x * impulse;
                var Py = normal.y * impulse;

                cA.x -= Px * mA;
                cA.y -= Py * mA;
                aA -= iA * (rAx * Py - rAy * Px);

                cB.x += Px * mB;
                cB.y += Py * mB;
                aB += iB * (rBx * Py - rBy * Px);
            }

            
            m_positions[indexA].a = aA;

            
            m_positions[indexB].a = aB;
        }

        
        
        return minSeparation >= -3.0f * Settings.linearSlop;
    }

    
    public boolean solveTOIPositionConstraints(int toiIndexA, int toiIndexB) {
        var minSeparation = 0.0f;

        for (var i = 0; i < m_count; ++i) {
            var pc = m_positionConstraints[i];

            var indexA = pc.indexA;
            var indexB = pc.indexB;
            var localCenterA = pc.localCenterA;
            var localCenterB = pc.localCenterB;
            var localCenterAx = localCenterA.x;
            var localCenterAy = localCenterA.y;
            var localCenterBx = localCenterB.x;
            var localCenterBy = localCenterB.y;
            var pointCount = pc.pointCount;

            var mA = 0.0f;
            var iA = 0.0f;
            if (indexA == toiIndexA || indexA == toiIndexB) {
                mA = pc.invMassA;
                iA = pc.invIA;
            }

            var mB = 0.0f;
            var iB = 0.0f;
            if (indexB == toiIndexA || indexB == toiIndexB) {
                mB = pc.invMassB;
                iB = pc.invIB;
            }

            v2 cA = m_positions[indexA];
            var aA = m_positions[indexA].a;

            v2 cB = m_positions[indexB];
            var aB = m_positions[indexB].a;

            
            for (var j = 0; j < pointCount; ++j) {
                Rot xfAq = xfA;
                Rot xfBq = xfB;
                xfAq.set(aA);
                xfBq.set(aB);
                xfA.pos.x = cA.x - xfAq.c * localCenterAx + xfAq.s * localCenterAy;
                xfA.pos.y = cA.y - xfAq.s * localCenterAx - xfAq.c * localCenterAy;
                xfB.pos.x = cB.x - xfBq.c * localCenterBx + xfBq.s * localCenterBy;
                xfB.pos.y = cB.y - xfBq.s * localCenterBx - xfBq.c * localCenterBy;

                var psm = psolver;
                psm.initialize(pc, xfA, xfB, j);
                var normal = psm.normal;

                var point = psm.point;
                var separation = psm.separation;

                var rAx = point.x - cA.x;
                var rAy = point.y - cA.y;
                var rBx = point.x - cB.x;
                var rBy = point.y - cB.y;

                
                minSeparation = MathUtils.min(minSeparation, separation);


                var C =
                        MathUtils.clamp(Settings.toiBaugarte * (separation + Settings.linearSlop),
                                -Settings.maxLinearCorrection, 0.0f);


                var rnA = rAx * normal.y - rAy * normal.x;
                var rnB = rBx * normal.y - rBy * normal.x;
                var K = mA + mB + iA * rnA * rnA + iB * rnB * rnB;


                var impulse = K > 0.0f ? -C / K : 0.0f;

                var Px = normal.x * impulse;
                var Py = normal.y * impulse;

                cA.x -= Px * mA;
                cA.y -= Py * mA;
                aA -= iA * (rAx * Py - rAy * Px);

                cB.x += Px * mB;
                cB.y += Py * mB;
                aB += iB * (rBx * Py - rBy * Px);
            }

            
            m_positions[indexA].a = aA;

            
            m_positions[indexB].a = aB;
        }

        
        
        return minSeparation >= -1.5f * Settings.linearSlop;
    }

    public static class ContactSolverDef {
        public TimeStep step;
        public Contact[] contacts;
        public int count;
        public Position[] positions;
        public Velocity[] velocities;
    }
}


class PositionSolverManifold {

    public final v2 normal = new v2();
    public final v2 point = new v2();
    public float separation;

    public void initialize(ContactPositionConstraint pc, Transform xfA, Transform xfB, int index) {
        assert (pc.pointCount > 0);

        Rot xfAq = xfA;
        Rot xfBq = xfB;
        var pcLocalPointsI = pc.localPoints[index];
        switch (pc.type) {
            case CIRCLES: {


                var plocalPoint = pc.localPoint;
                var pLocalPoints0 = pc.localPoints[0];
                var pointAx = (xfAq.c * plocalPoint.x - xfAq.s * plocalPoint.y) + xfA.pos.x;
                var pointAy = (xfAq.s * plocalPoint.x + xfAq.c * plocalPoint.y) + xfA.pos.y;
                var pointBx = (xfBq.c * pLocalPoints0.x - xfBq.s * pLocalPoints0.y) + xfB.pos.x;
                var pointBy = (xfBq.s * pLocalPoints0.x + xfBq.c * pLocalPoints0.y) + xfB.pos.y;
                normal.x = pointBx - pointAx;
                normal.y = pointBy - pointAy;
                normal.normalize();

                point.x = (pointAx + pointBx) * 0.5f;
                point.y = (pointAy + pointBy) * 0.5f;
                var tempx = pointBx - pointAx;
                var tempy = pointBy - pointAy;
                separation = tempx * normal.x + tempy * normal.y - pc.radiusA - pc.radiusB;
                break;
            }

            case FACE_A: {


                var pcLocalNormal = pc.localNormal;
                var pcLocalPoint = pc.localPoint;
                normal.x = xfAq.c * pcLocalNormal.x - xfAq.s * pcLocalNormal.y;
                normal.y = xfAq.s * pcLocalNormal.x + xfAq.c * pcLocalNormal.y;
                var planePointx = (xfAq.c * pcLocalPoint.x - xfAq.s * pcLocalPoint.y) + xfA.pos.x;
                var planePointy = (xfAq.s * pcLocalPoint.x + xfAq.c * pcLocalPoint.y) + xfA.pos.y;

                var clipPointx = (xfBq.c * pcLocalPointsI.x - xfBq.s * pcLocalPointsI.y) + xfB.pos.x;
                var clipPointy = (xfBq.s * pcLocalPointsI.x + xfBq.c * pcLocalPointsI.y) + xfB.pos.y;
                var tempx = clipPointx - planePointx;
                var tempy = clipPointy - planePointy;
                separation = tempx * normal.x + tempy * normal.y - pc.radiusA - pc.radiusB;
                point.x = clipPointx;
                point.y = clipPointy;
                break;
            }

            case FACE_B: {


                var pcLocalNormal = pc.localNormal;
                var pcLocalPoint = pc.localPoint;
                normal.x = xfBq.c * pcLocalNormal.x - xfBq.s * pcLocalNormal.y;
                normal.y = xfBq.s * pcLocalNormal.x + xfBq.c * pcLocalNormal.y;
                var planePointx = (xfBq.c * pcLocalPoint.x - xfBq.s * pcLocalPoint.y) + xfB.pos.x;
                var planePointy = (xfBq.s * pcLocalPoint.x + xfBq.c * pcLocalPoint.y) + xfB.pos.y;

                var clipPointx = (xfAq.c * pcLocalPointsI.x - xfAq.s * pcLocalPointsI.y) + xfA.pos.x;
                var clipPointy = (xfAq.s * pcLocalPointsI.x + xfAq.c * pcLocalPointsI.y) + xfA.pos.y;
                var tempx = clipPointx - planePointx;
                var tempy = clipPointy - planePointy;
                separation = tempx * normal.x + tempy * normal.y - pc.radiusA - pc.radiusB;
                point.x = clipPointx;
                point.y = clipPointy;
                normal.x *= -1;
                normal.y *= -1;
            }
            break;
        }
    }
}
