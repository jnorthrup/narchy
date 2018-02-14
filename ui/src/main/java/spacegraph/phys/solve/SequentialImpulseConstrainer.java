/*
 * Java port of Bullet (c) 2008 Martin Dvorak <jezek2@advel.cz>
 *
 * Bullet Continuous Collision Detection and Physics Library
 * Copyright (c) 2003-2008 Erwin Coumans  http://www.bulletphysics.com/
 *
 * This software is provided 'as-is', without any express or implied warranty.
 * In no event will the authors be held liable for any damages arising from
 * the use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose, 
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 * 1. The origin of this software must not be misrepresented; you must not
 *    claim that you wrote the original software. If you use this software
 *    in a product, an acknowledgment in the product documentation would be
 *    appreciated but is not required.
 * 2. Altered source versions must be plainly marked as such, and must not be
 *    misrepresented as being the original software.
 * 3. This notice may not be removed or altered from any source distribution.
 */

package spacegraph.phys.solve;

import jcog.list.FasterList;
import spacegraph.math.Matrix3f;
import spacegraph.math.v3;
import spacegraph.phys.BulletGlobals;
import spacegraph.phys.BulletStats;
import spacegraph.phys.Collidable;
import spacegraph.phys.Dynamic;
import spacegraph.phys.collision.broad.Intersecter;
import spacegraph.phys.collision.narrow.ManifoldPoint;
import spacegraph.phys.collision.narrow.PersistentManifold;
import spacegraph.phys.constraint.ContactConstraint;
import spacegraph.phys.constraint.SolverConstraint;
import spacegraph.phys.constraint.TypedConstraint;
import spacegraph.phys.math.MiscUtil;
import spacegraph.phys.math.Transform;
import spacegraph.phys.math.TransformUtil;
import spacegraph.phys.util.IntArrayList;

import java.util.Collection;

/**
 * SequentialImpulseConstraintSolver uses a Propagation Method and Sequentially applies impulses.
 * The approach is the 3D version of Erin Catto's GDC 2006 tutorial. See http://www.gphysics.com<p>
 * <p>
 * Although Sequential Impulse is more intuitive, it is mathematically equivalent to Projected
 * Successive Overrelaxation (iterative LCP).<p>
 * <p>
 * Applies impulses for combined restitution and penetration recovery and to simulate friction.
 *
 * @author jezek2
 */
public class SequentialImpulseConstrainer extends Constrainer {

    private static final int MAX_CONTACT_SOLVER_TYPES = ContactConstraintEnum.MAX_CONTACT_SOLVER_TYPES.ordinal();

    private static final int SEQUENTIAL_IMPULSE_MAX_SOLVER_POINTS = 16384;
    private final OrderIndex[] gOrder = new OrderIndex[SEQUENTIAL_IMPULSE_MAX_SOLVER_POINTS];


    {
        for (int i = 0; i < gOrder.length; i++) {
            gOrder[i] = new OrderIndex();
        }
    }

    ////////////////////////////////////////////////////////////////////////////

    private final FasterList<SolverBody> tmpSolverBodyPool = new FasterList<>();
    private final FasterList<SolverConstraint> tmpSolverConstraintPool = new FasterList<>();
    private final FasterList<SolverConstraint> tmpSolverFrictionConstraintPool = new FasterList<>();
    private final IntArrayList orderTmpConstraintPool = new IntArrayList();
    private final IntArrayList orderFrictionConstraintPool = new IntArrayList();

    protected final ContactSolverFunc[][] contactDispatch = new ContactSolverFunc[MAX_CONTACT_SOLVER_TYPES][MAX_CONTACT_SOLVER_TYPES];
    protected final ContactSolverFunc[][] frictionDispatch = new ContactSolverFunc[MAX_CONTACT_SOLVER_TYPES][MAX_CONTACT_SOLVER_TYPES];

    // btSeed2 is used for re-arranging the constraint rows. improves convergence/quality of friction
    protected long btSeed2;

    public SequentialImpulseConstrainer() {
//        globals.setContactDestroyedCallback(new ContactDestroyedCallback() {
//            @Override
//            public boolean contactDestroyed(Object userPersistentData) {
//                assert (userPersistentData != null);
//                ConstraintPersistentData cpd = (ConstraintPersistentData) userPersistentData;
//                //btAlignedFree(cpd);
//                //totalCpd--;
//                //printf("totalCpd = %i. DELETED Ptr %x\n",totalCpd,userPersistentData);
//                return true;
//            }
//        });

        // initialize default friction/contact funcs
        for (int i = 0; i < MAX_CONTACT_SOLVER_TYPES; i++) {
            ContactSolverFunc[] ci = this.contactDispatch[i];
            ContactSolverFunc[] fi = this.frictionDispatch[i];
            for (int j = 0; j < MAX_CONTACT_SOLVER_TYPES; j++) {
                ci[j] = ContactConstraint.resolveSingleCollision;
                fi[j] = ContactConstraint.resolveSingleFriction;
            }
        }
    }

    long rand2() {
        btSeed2 = (1664525L * btSeed2 + 1013904223L) & 0xffffffff;
        return btSeed2;
    }

    // See ODE: adam's all-int straightforward(?) dRandInt (0..n-1)
    int randInt2(int n) {
        // seems good; xor-fold and modulus
        long un = n;
        long r = rand2();

        // note: probably more aggressive than it needs to be -- might be
        //       able to get away without one or two of the innermost branches.
        if (un <= 0x00010000L) {
            r ^= (r >>> 16);
            if (un <= 0x00000100L) {
                r ^= (r >>> 8);
                if (un <= 0x00000010L) {
                    r ^= (r >>> 4);
                    if (un <= 0x00000004L) {
                        r ^= (r >>> 2);
                        if (un <= 0x00000002L) {
                            r ^= (r >>> 1);
                        }
                    }
                }
            }
        }

        // TODO: check modulo C vs Java mismatch
        return (int) Math.abs(r % un);
    }

    private static void initSolverBody(SolverBody solverBody, Collidable collidable) {
        Dynamic rb = Dynamic.ifDynamic(collidable);
        if (rb != null) {
            rb.getAngularVelocity(solverBody.angularVelocity);
            solverBody.centerOfMassPosition.set(collidable.transform);
            solverBody.friction = collidable.getFriction();
            solverBody.invMass = rb.getInvMass();
            rb.getLinearVelocity(solverBody.linearVelocity);
            solverBody.body = rb;
            solverBody.angularFactor = rb.getAngularFactor();
        } else {
            solverBody.angularVelocity.set(0f, 0f, 0f);
            solverBody.centerOfMassPosition.set(collidable.transform);
            solverBody.friction = collidable.getFriction();
            solverBody.invMass = 0f;
            solverBody.linearVelocity.set(0f, 0f, 0f);
            solverBody.body = null;
            solverBody.angularFactor = 1f;
        }

        solverBody.pushVelocity.set(0f, 0f, 0f);
        solverBody.turnVelocity.set(0f, 0f, 0f);
    }

    private static float restitutionCurve(float rel_vel, float restitution) {
        float rest = restitution * -rel_vel;
        return rest;
    }

    private static void resolveSplitPenetrationImpulseCacheFriendly(
            SolverBody body1,
            SolverBody body2,
            SolverConstraint contactConstraint,
            ContactSolverInfo solverInfo) {

        if (contactConstraint.penetration < solverInfo.splitImpulsePenetrationThreshold) {
            BulletStats.gNumSplitImpulseRecoveries++;
            float normalImpulse;

            //  Optimized version of projected relative velocity, use precomputed cross products with normal
            //      body1.getVelocityInLocalPoint(contactConstraint.m_rel_posA,vel1);
            //      body2.getVelocityInLocalPoint(contactConstraint.m_rel_posB,vel2);
            //      btVector3 vel = vel1 - vel2;
            //      btScalar  rel_vel = contactConstraint.m_contactNormal.dot(vel);

            float vel1Dotn = contactConstraint.contactNormal.dot(body1.pushVelocity) + contactConstraint.relpos1CrossNormal.dot(body1.turnVelocity);
            float vel2Dotn = contactConstraint.contactNormal.dot(body2.pushVelocity) + contactConstraint.relpos2CrossNormal.dot(body2.turnVelocity);

            float rel_vel = vel1Dotn - vel2Dotn;

            float positionalError = -contactConstraint.penetration * solverInfo.erp2 / solverInfo.timeStep;
            //      btScalar positionalError = contactConstraint.m_penetration;

            float velocityError = contactConstraint.restitution - rel_vel;// * damping;

            float penetrationImpulse = positionalError * contactConstraint.jacDiagABInv;
            float velocityImpulse = velocityError * contactConstraint.jacDiagABInv;
            normalImpulse = penetrationImpulse + velocityImpulse;

            // See Erin Catto's GDC 2006 paper: Clamp the accumulated impulse
            float oldNormalImpulse = contactConstraint.appliedPushImpulse;
            float sum = oldNormalImpulse + normalImpulse;
            contactConstraint.appliedPushImpulse = 0f > sum ? 0f : sum;

            normalImpulse = contactConstraint.appliedPushImpulse - oldNormalImpulse;

            v3 tmp = new v3();

            tmp.scale(body1.invMass, contactConstraint.contactNormal);
            //tmp.scale(body1.linearFactor);
            body1.internalApplyPushImpulse(tmp, contactConstraint.angularComponentA, normalImpulse);

            tmp.scale(body2.invMass, contactConstraint.contactNormal);
            //tmp.scale(body2.linearFactor);
            body2.internalApplyPushImpulse(tmp, contactConstraint.angularComponentB, -normalImpulse);
        }
    }

    /**
     * velocity + friction
     * response  between two dynamic objects with friction
     */
    private static float resolveSingleCollisionCombinedCacheFriendly(
            SolverBody body1,
            SolverBody body2,
            SolverConstraint contactConstraint,
            ContactSolverInfo solverInfo) {

        float normalImpulse;

        //  Optimized version of projected relative velocity, use precomputed cross products with normal
        //	body1.getVelocityInLocalPoint(contactConstraint.m_rel_posA,vel1);
        //	body2.getVelocityInLocalPoint(contactConstraint.m_rel_posB,vel2);
        //	btVector3 vel = vel1 - vel2;
        //	btScalar  rel_vel = contactConstraint.m_contactNormal.dot(vel);

        float rel_vel;
        float vel1Dotn = contactConstraint.contactNormal.dot(body1.linearVelocity) + contactConstraint.relpos1CrossNormal.dot(body1.angularVelocity);
        float vel2Dotn = contactConstraint.contactNormal.dot(body2.linearVelocity) + contactConstraint.relpos2CrossNormal.dot(body2.angularVelocity);

        rel_vel = vel1Dotn - vel2Dotn;

        float positionalError = 0.f;
        if (!solverInfo.splitImpulse || (contactConstraint.penetration > solverInfo.splitImpulsePenetrationThreshold)) {
            positionalError = -contactConstraint.penetration * solverInfo.erp / solverInfo.timeStep;
        }

        float velocityError = contactConstraint.restitution - rel_vel;// * damping;

        float penetrationImpulse = positionalError * contactConstraint.jacDiagABInv;
        float velocityImpulse = velocityError * contactConstraint.jacDiagABInv;
        normalImpulse = penetrationImpulse + velocityImpulse;


        // See Erin Catto's GDC 2006 paper: Clamp the accumulated impulse
        float oldNormalImpulse = contactConstraint.appliedImpulse;
        float sum = oldNormalImpulse + normalImpulse;
        contactConstraint.appliedImpulse = 0f > sum ? 0f : sum;

        normalImpulse = contactConstraint.appliedImpulse - oldNormalImpulse;

        v3 tmp = new v3();

        tmp.scale(body1.invMass, contactConstraint.contactNormal);
        //tmp.scale(body1.linearFactor);
        body1.internalApplyImpulse(tmp, contactConstraint.angularComponentA, normalImpulse);

        tmp.scale(body2.invMass, contactConstraint.contactNormal);
        //tmp.scale(body2.linearFactor);
        body2.internalApplyImpulse(tmp, contactConstraint.angularComponentB, -normalImpulse);

        return normalImpulse;
    }

    private static float resolveSingleFrictionCacheFriendly(
            SolverBody body1,
            SolverBody body2,
            SolverConstraint contactConstraint,
            ContactSolverInfo solverInfo,
            float appliedNormalImpulse) {
        float combinedFriction = contactConstraint.friction;

        float limit = appliedNormalImpulse * combinedFriction;

        if (appliedNormalImpulse > 0f) //friction
        {

            float j1;

            float rel_vel;
            float vel1Dotn = contactConstraint.contactNormal.dot(body1.linearVelocity) + contactConstraint.relpos1CrossNormal.dot(body1.angularVelocity);
            float vel2Dotn = contactConstraint.contactNormal.dot(body2.linearVelocity) + contactConstraint.relpos2CrossNormal.dot(body2.angularVelocity);
            rel_vel = vel1Dotn - vel2Dotn;

            // calculate j that moves us to zero relative velocity
            j1 = -rel_vel * contactConstraint.jacDiagABInv;
            //#define CLAMP_ACCUMULATED_FRICTION_IMPULSE 1
            //#ifdef CLAMP_ACCUMULATED_FRICTION_IMPULSE
            float oldTangentImpulse = contactConstraint.appliedImpulse;
            contactConstraint.appliedImpulse = oldTangentImpulse + j1;

            if (limit < contactConstraint.appliedImpulse) {
                contactConstraint.appliedImpulse = limit;
            } else {
                if (contactConstraint.appliedImpulse < -limit) {
                    contactConstraint.appliedImpulse = -limit;
                }
            }
            j1 = contactConstraint.appliedImpulse - oldTangentImpulse;
            //	#else
            //	if (limit < j1)
            //	{
            //		j1 = limit;
            //	} else
            //	{
            //		if (j1 < -limit)
            //			j1 = -limit;
            //	}
            //	#endif

            //GEN_set_min(contactConstraint.m_appliedImpulse, limit);
            //GEN_set_max(contactConstraint.m_appliedImpulse, -limit);

            v3 tmp = new v3();

            tmp.scale(body1.invMass, contactConstraint.contactNormal);
            body1.internalApplyImpulse(tmp, contactConstraint.angularComponentA, j1);

            tmp.scale(body2.invMass, contactConstraint.contactNormal);
            body2.internalApplyImpulse(tmp, contactConstraint.angularComponentB, -j1);
        }
        return 0f;
    }


    protected void addFrictionConstraint(v3 normalAxis, int solverBodyIdA, int solverBodyIdB, int frictionIndex, ManifoldPoint cp, v3 rel_pos1, v3 rel_pos2, Collidable colObj0, Collidable colObj1, float relaxation) {
        Dynamic body0 = Dynamic.ifDynamic(colObj0);
        Dynamic body1 = Dynamic.ifDynamic(colObj1);

        SolverConstraint solverConstraint = new SolverConstraint();
        tmpSolverFrictionConstraintPool.add(solverConstraint);

        solverConstraint.contactNormal.set(normalAxis);

        solverConstraint.solverBodyIdA = solverBodyIdA;
        solverConstraint.solverBodyIdB = solverBodyIdB;
        solverConstraint.constraintType = SolverConstraint.SolverConstraintType.SOLVER_FRICTION_1D;
        solverConstraint.frictionIndex = frictionIndex;

        solverConstraint.friction = cp.combinedFriction;
        solverConstraint.originalContactPoint = null;

        solverConstraint.appliedImpulse = 0f;
        solverConstraint.appliedPushImpulse = 0f;
        solverConstraint.penetration = 0f;

        v3 ftorqueAxis1 = new v3();
        Matrix3f tmpMat = new Matrix3f();

        ftorqueAxis1.cross(rel_pos1, solverConstraint.contactNormal);
        solverConstraint.relpos1CrossNormal.set(ftorqueAxis1);
        if (body0 != null) {
            solverConstraint.angularComponentA.set(ftorqueAxis1);
            body0.getInvInertiaTensorWorld(tmpMat).transform(solverConstraint.angularComponentA);
        } else {
            solverConstraint.angularComponentA.set(0f, 0f, 0f);
        }
        ftorqueAxis1.cross(rel_pos2, solverConstraint.contactNormal);
        solverConstraint.relpos2CrossNormal.set(ftorqueAxis1);
        if (body1 != null) {
            solverConstraint.angularComponentB.set(ftorqueAxis1);
            body1.getInvInertiaTensorWorld(tmpMat).transform(solverConstraint.angularComponentB);
        } else {
            solverConstraint.angularComponentB.set(0f, 0f, 0f);
        }

        //#ifdef COMPUTE_IMPULSE_DENOM
        //	btScalar denom0 = rb0->computeImpulseDenominator(pos1,solverConstraint.m_contactNormal);
        //	btScalar denom1 = rb1->computeImpulseDenominator(pos2,solverConstraint.m_contactNormal);
        //#else
        v3 vec = new v3();
        float denom0 = 0f;
        float denom1 = 0f;
        if (body0 != null) {
            vec.cross(solverConstraint.angularComponentA, rel_pos1);
            denom0 = body0.getInvMass() + normalAxis.dot(vec);
        }
        if (body1 != null) {
            vec.cross(solverConstraint.angularComponentB, rel_pos2);
            denom1 = body1.getInvMass() + normalAxis.dot(vec);
        }
        //#endif //COMPUTE_IMPULSE_DENOM

        float denom = relaxation / (denom0 + denom1);
        solverConstraint.jacDiagABInv = denom;
    }

    public float solveGroupCacheFriendlySetup(Collection<Collidable> bodies, int numBodies, FasterList<PersistentManifold> manifoldPtr, int manifold_offset, int numManifolds, FasterList<TypedConstraint> constraints, int constraints_offset, int numConstraints, ContactSolverInfo infoGlobal/*,btStackAlloc* stackAlloc*/) {

            if ((numConstraints + numManifolds) == 0) {
                // printf("empty\n");
                return 0f;
            }
            PersistentManifold manifold = null;
            Collidable colObj0 = null, colObj1 = null;

            //btRigidBody* rb0=0,*rb1=0;

            //	//#ifdef FORCE_REFESH_CONTACT_MANIFOLDS
            //
            //		BEGIN_PROFILE("refreshManifolds");
            //
            //		int i;
            //
            //
            //
            //		for (i=0;i<numManifolds;i++)
            //		{
            //			manifold = manifoldPtr[i];
            //			rb1 = (btRigidBody*)manifold->getBody1();
            //			rb0 = (btRigidBody*)manifold->getBody0();
            //
            //			manifold->refreshContactPoints(rb0->getCenterOfMassTransform(),rb1->getCenterOfMassTransform());
            //
            //		}
            //
            //		END_PROFILE("refreshManifolds");
            //	//#endif //FORCE_REFESH_CONTACT_MANIFOLDS


            //int sizeofSB = sizeof(btSolverBody);
            //int sizeofSC = sizeof(btSolverConstraint);

            //if (1)
            //if m_stackAlloc, try to pack bodies/constraints to speed up solving
            //		btBlock*					sablock;
            //		sablock = stackAlloc->beginBlock();

            //	int memsize = 16;
            //		unsigned char* stackMemory = stackAlloc->allocate(memsize);


            // todo: use stack allocator for this temp memory
            //int minReservation = numManifolds * 2;

            //m_tmpSolverBodyPool.reserve(minReservation);

            //don't convert all bodies, only the one we need so solver the constraints
                /*
				{
				for (int i=0;i<numBodies;i++)
				{
				btRigidBody* rb = btRigidBody::upcast(bodies[i]);
				if (rb && 	(rb->getIslandTag() >= 0))
				{
				btAssert(rb->getCompanionId() < 0);
				int solverBodyId = m_tmpSolverBodyPool.size();
				btSolverBody& solverBody = m_tmpSolverBodyPool.expand();
				initSolverBody(&solverBody,rb);
				rb->setCompanionId(solverBodyId);
				}
				}
				}
				*/

            //m_tmpSolverConstraintPool.reserve(minReservation);
            //m_tmpSolverFrictionConstraintPool.reserve(minReservation);

            {
                int i;

                v3 rel_pos1 = new v3();
                v3 rel_pos2 = new v3();

                v3 pos1 = new v3();
                v3 pos2 = new v3();
                v3 vel = new v3();
                v3 torqueAxis0 = new v3();
                v3 torqueAxis1 = new v3();
                v3 vel1 = new v3();
                v3 vel2 = new v3();
                //Vector3f frictionDir1 = new Vector3f();
                //Vector3f frictionDir2 = new Vector3f();
                v3 vec = new v3();

                Matrix3f tmpMat = new Matrix3f();

                for (i = 0; i < numManifolds; i++) {
                    //return array[index];
                    manifold = manifoldPtr.get(manifold_offset + i);
                    colObj0 = (Collidable) manifold.getBody0();
                    colObj1 = (Collidable) manifold.getBody1();

                    int solverBodyIdA = -1;
                    int solverBodyIdB = -1;

                    if (manifold.numContacts() != 0) {
                        if (colObj0.tag() >= 0) {
                            if (colObj0.getCompanionId() >= 0) {
                                // body has already been converted
                                solverBodyIdA = colObj0.getCompanionId();
                            } else {
                                solverBodyIdA = tmpSolverBodyPool.size();
                                SolverBody solverBody = new SolverBody();
                                tmpSolverBodyPool.add(solverBody);
                                initSolverBody(solverBody, colObj0);
                                colObj0.setCompanionId(solverBodyIdA);
                            }
                        } else {
                            // create a static body
                            solverBodyIdA = tmpSolverBodyPool.size();
                            SolverBody solverBody = new SolverBody();
                            tmpSolverBodyPool.add(solverBody);
                            initSolverBody(solverBody, colObj0);
                        }

                        if (colObj1.tag() >= 0) {
                            if (colObj1.getCompanionId() >= 0) {
                                solverBodyIdB = colObj1.getCompanionId();
                            } else {
                                solverBodyIdB = tmpSolverBodyPool.size();
                                SolverBody solverBody = new SolverBody();
                                tmpSolverBodyPool.add(solverBody);
                                initSolverBody(solverBody, colObj1);
                                colObj1.setCompanionId(solverBodyIdB);
                            }
                        } else {
                            // create a static body
                            solverBodyIdB = tmpSolverBodyPool.size();
                            SolverBody solverBody = new SolverBody();
                            tmpSolverBodyPool.add(solverBody);
                            initSolverBody(solverBody, colObj1);
                        }
                    }

                    float relaxation;

                    for (int j = 0; j < manifold.numContacts(); j++) {

                        ManifoldPoint cp = manifold.getContactPoint(j);

                        if (cp.distance1 <= 0f) {
                            cp.getPositionWorldOnA(pos1);
                            cp.getPositionWorldOnB(pos2);

                            rel_pos1.sub(pos1, colObj0.transform);
                            rel_pos2.sub(pos2, colObj1.transform);

                            relaxation = 1f;
                            float rel_vel;

                            int frictionIndex = tmpSolverConstraintPool.size();

                            SolverConstraint solverConstraint = new SolverConstraint();
                            tmpSolverConstraintPool.add(solverConstraint);
                            Dynamic rb0 = Dynamic.ifDynamic(colObj0);
                            Dynamic rb1 = Dynamic.ifDynamic(colObj1);

                            solverConstraint.solverBodyIdA = solverBodyIdA;
                            solverConstraint.solverBodyIdB = solverBodyIdB;
                            solverConstraint.constraintType = SolverConstraint.SolverConstraintType.SOLVER_CONTACT_1D;

                            solverConstraint.originalContactPoint = cp;

                            torqueAxis0.cross(rel_pos1, cp.normalWorldOnB);

                            if (rb0 != null) {
                                solverConstraint.angularComponentA.set(torqueAxis0);
                                rb0.getInvInertiaTensorWorld(tmpMat).transform(solverConstraint.angularComponentA);
                            } else {
                                solverConstraint.angularComponentA.set(0f, 0f, 0f);
                            }

                            torqueAxis1.cross(rel_pos2, cp.normalWorldOnB);

                            if (rb1 != null) {
                                solverConstraint.angularComponentB.set(torqueAxis1);
                                rb1.getInvInertiaTensorWorld(tmpMat).transform(solverConstraint.angularComponentB);
                            } else {
                                solverConstraint.angularComponentB.set(0f, 0f, 0f);
                            }

                            //#ifdef COMPUTE_IMPULSE_DENOM
                            //btScalar denom0 = rb0->computeImpulseDenominator(pos1,cp.m_normalWorldOnB);
                            //btScalar denom1 = rb1->computeImpulseDenominator(pos2,cp.m_normalWorldOnB);
                            //#else
                            float denom0 = 0f;
                            float denom1 = 0f;
                            if (rb0 != null) {
                                vec.cross(solverConstraint.angularComponentA, rel_pos1);
                                denom0 = rb0.getInvMass() + cp.normalWorldOnB.dot(vec);
                            }
                            if (rb1 != null) {
                                vec.cross(solverConstraint.angularComponentB, rel_pos2);
                                denom1 = rb1.getInvMass() + cp.normalWorldOnB.dot(vec);
                            }
                            //#endif //COMPUTE_IMPULSE_DENOM

                            float denom = relaxation / (denom0 + denom1);
                            solverConstraint.jacDiagABInv = denom;

                            solverConstraint.contactNormal.set(cp.normalWorldOnB);
                            solverConstraint.relpos1CrossNormal.cross(rel_pos1, cp.normalWorldOnB);
                            solverConstraint.relpos2CrossNormal.cross(rel_pos2, cp.normalWorldOnB);

                            if (rb0 != null) {
                                rb0.getVelocityInLocalPoint(rel_pos1, vel1);
                            } else {
                                vel1.zero();
                            }

                            if (rb1 != null) {
                                rb1.getVelocityInLocalPoint(rel_pos2, vel2);
                            } else {
                                vel2.zero();
                            }

                            vel.sub(vel1, vel2);

                            rel_vel = cp.normalWorldOnB.dot(vel);

                            solverConstraint.penetration = Math.min(cp.distance1 + infoGlobal.linearSlop, 0f);
                            //solverConstraint.m_penetration = cp.getDistance();

                            solverConstraint.friction = cp.combinedFriction;
                            solverConstraint.restitution = restitutionCurve(rel_vel, cp.combinedRestitution);
                            if (solverConstraint.restitution <= 0f) {
                                solverConstraint.restitution = 0f;
                            }

                            float penVel = -solverConstraint.penetration / infoGlobal.timeStep;

                            if (solverConstraint.restitution > penVel) {
                                solverConstraint.penetration = 0f;
                            }

                            v3 tmp = new v3();

                            // warm starting (or zero if disabled)
                            if ((infoGlobal.solverMode & SolverMode.SOLVER_USE_WARMSTARTING) != 0) {
                                solverConstraint.appliedImpulse = cp.appliedImpulse * infoGlobal.warmstartingFactor;
                                if (rb0 != null) {
                                    tmp.scale(rb0.getInvMass(), solverConstraint.contactNormal);
                                    //return array[index];
                                    tmpSolverBodyPool.get(solverConstraint.solverBodyIdA).internalApplyImpulse(tmp, solverConstraint.angularComponentA, solverConstraint.appliedImpulse);
                                }
                                if (rb1 != null) {
                                    tmp.scale(rb1.getInvMass(), solverConstraint.contactNormal);
                                    //return array[index];
                                    tmpSolverBodyPool.get(solverConstraint.solverBodyIdB).internalApplyImpulse(tmp, solverConstraint.angularComponentB, -solverConstraint.appliedImpulse);
                                }
                            } else {
                                solverConstraint.appliedImpulse = 0f;
                            }

                            solverConstraint.appliedPushImpulse = 0f;

                            solverConstraint.frictionIndex = tmpSolverFrictionConstraintPool.size();
                            if (!cp.lateralFrictionInitialized) {
                                cp.lateralFrictionDir1.scale(rel_vel, cp.normalWorldOnB);
                                cp.lateralFrictionDir1.sub(vel, cp.lateralFrictionDir1);

                                float lat_rel_vel = cp.lateralFrictionDir1.lengthSquared();
                                if (lat_rel_vel > BulletGlobals.FLT_EPSILON)//0.0f)
                                {
                                    cp.lateralFrictionDir1.scale(1f / (float) Math.sqrt(lat_rel_vel));
                                    addFrictionConstraint(cp.lateralFrictionDir1, solverBodyIdA, solverBodyIdB, frictionIndex, cp, rel_pos1, rel_pos2, colObj0, colObj1, relaxation);
                                    cp.lateralFrictionDir2.cross(cp.lateralFrictionDir1, cp.normalWorldOnB);
                                    cp.lateralFrictionDir2.normalize(); //??
                                    addFrictionConstraint(cp.lateralFrictionDir2, solverBodyIdA, solverBodyIdB, frictionIndex, cp, rel_pos1, rel_pos2, colObj0, colObj1, relaxation);
                                } else {
                                    // re-calculate friction direction every frame, todo: check if this is really needed

                                    TransformUtil.planeSpace1(cp.normalWorldOnB, cp.lateralFrictionDir1, cp.lateralFrictionDir2);
                                    addFrictionConstraint(cp.lateralFrictionDir1, solverBodyIdA, solverBodyIdB, frictionIndex, cp, rel_pos1, rel_pos2, colObj0, colObj1, relaxation);
                                    addFrictionConstraint(cp.lateralFrictionDir2, solverBodyIdA, solverBodyIdB, frictionIndex, cp, rel_pos1, rel_pos2, colObj0, colObj1, relaxation);
                                }
                                cp.lateralFrictionInitialized = true;

                            } else {
                                addFrictionConstraint(cp.lateralFrictionDir1, solverBodyIdA, solverBodyIdB, frictionIndex, cp, rel_pos1, rel_pos2, colObj0, colObj1, relaxation);
                                addFrictionConstraint(cp.lateralFrictionDir2, solverBodyIdA, solverBodyIdB, frictionIndex, cp, rel_pos1, rel_pos2, colObj0, colObj1, relaxation);
                            }

                            //return array[index];
                            SolverConstraint frictionConstraint1 = tmpSolverFrictionConstraintPool.get(solverConstraint.frictionIndex);
                            if ((infoGlobal.solverMode & SolverMode.SOLVER_USE_WARMSTARTING) != 0) {
                                frictionConstraint1.appliedImpulse = cp.appliedImpulseLateral1 * infoGlobal.warmstartingFactor;
                                if (rb0 != null) {
                                    tmp.scale(rb0.getInvMass(), frictionConstraint1.contactNormal);
                                    //return array[index];
                                    tmpSolverBodyPool.get(solverConstraint.solverBodyIdA).internalApplyImpulse(tmp, frictionConstraint1.angularComponentA, frictionConstraint1.appliedImpulse);
                                }
                                if (rb1 != null) {
                                    tmp.scale(rb1.getInvMass(), frictionConstraint1.contactNormal);
                                    //return array[index];
                                    tmpSolverBodyPool.get(solverConstraint.solverBodyIdB).internalApplyImpulse(tmp, frictionConstraint1.angularComponentB, -frictionConstraint1.appliedImpulse);
                                }
                            } else {
                                frictionConstraint1.appliedImpulse = 0f;
                            }
                            //return array[index];
                            SolverConstraint frictionConstraint2 = tmpSolverFrictionConstraintPool.get(solverConstraint.frictionIndex + 1);
                            if ((infoGlobal.solverMode & SolverMode.SOLVER_USE_WARMSTARTING) != 0) {
                                frictionConstraint2.appliedImpulse = cp.appliedImpulseLateral2 * infoGlobal.warmstartingFactor;
                                if (rb0 != null) {
                                    tmp.scale(rb0.getInvMass(), frictionConstraint2.contactNormal);
                                    //return array[index];
                                    tmpSolverBodyPool.get(solverConstraint.solverBodyIdA).internalApplyImpulse(tmp, frictionConstraint2.angularComponentA, frictionConstraint2.appliedImpulse);
                                }
                                if (rb1 != null) {
                                    tmp.scale(rb1.getInvMass(), frictionConstraint2.contactNormal);
                                    //return array[index];
                                    tmpSolverBodyPool.get(solverConstraint.solverBodyIdB).internalApplyImpulse(tmp, frictionConstraint2.angularComponentB, -frictionConstraint2.appliedImpulse);
                                }
                            } else {
                                frictionConstraint2.appliedImpulse = 0f;
                            }
                        }
                    }
                }
            }

            // TODO: btContactSolverInfo info = infoGlobal;

            int j;
            for (j = 0; j < numConstraints; j++) {
                constraints.get(constraints_offset + j).buildJacobian();
            }

//			int j;
//			for (j = 0; j < numConstraints; j++) {
//                constraints.get(constraints_offset + j).getInfo2(infoGlobal);
//            }


            int numConstraintPool = tmpSolverConstraintPool.size();
            int numFrictionPool = tmpSolverFrictionConstraintPool.size();

            // todo: use stack allocator for such temporarily memory, same for solver bodies/constraints
            MiscUtil.resize(orderTmpConstraintPool, numConstraintPool, 0);
            MiscUtil.resize(orderFrictionConstraintPool, numFrictionPool, 0);
            int i;
            for (i = 0; i < numConstraintPool; i++) {
                orderTmpConstraintPool.setBoth(i);
            }
            for (i = 0; i < numFrictionPool; i++) {
                orderFrictionConstraintPool.setBoth(i);
            }

            return 0f;
    }

    float solveGroupCacheFriendlyIterations(Collection<Collidable> bodies, int numBodies, Collection<PersistentManifold> manifoldPtr, int manifold_offset, int numManifolds, FasterList<TypedConstraint> constraints, int constraints_offset, int numConstraints, ContactSolverInfo infoGlobal/*,btStackAlloc* stackAlloc*/) {
            int numConstraintPool = tmpSolverConstraintPool.size();
            int numFrictionPool = tmpSolverFrictionConstraintPool.size();

            // should traverse the contacts random order...
            int iteration;
            IntArrayList constraintPool = this.orderTmpConstraintPool;
            IntArrayList frictionPool = this.orderFrictionConstraintPool;
            for (iteration = 0; iteration < infoGlobal.numIterations; iteration++) {

                if ((infoGlobal.solverMode & SolverMode.SOLVER_RANDMIZE_ORDER) != 0) {
                    if ((iteration & 7) == 0) {
                        for (int j = 0; j < numConstraintPool; ++j) {
                            orderPool(j, constraintPool);
                        }

                        for (int j = 0; j < numFrictionPool; ++j) {
                            orderPool(j, frictionPool);
                        }
                    }
                }

                for (int j = 0; j < numConstraints; j++) {
                    //return array[index];
                    final TypedConstraint constraint = constraints.get(constraints_offset + j);
                    // todo: use solver bodies, so we don't need to copy from/to btRigidBody

                    final Dynamic ca = constraint.getRigidBodyA();
                    final int cai = ca.getCompanionId();
                    if ((ca.tag() >= 0) && (cai >= 0)) {
                        //return array[index];
                        tmpSolverBodyPool.get(cai).writebackVelocity();
                    }
                    Dynamic cb = constraint.getRigidBodyB();
                    int cbi = cb.getCompanionId();
                    if ((cb.tag() >= 0) && (cbi >= 0)) {
                        //return array[index];
                        tmpSolverBodyPool.get(cbi).writebackVelocity();
                    }

                    constraint.solveConstraint(infoGlobal.timeStep);

                    if ((ca.tag() >= 0) && (cai >= 0)) {
                        //return array[index];
                        tmpSolverBodyPool.get(cai).readVelocity();
                    }
                    if ((cb.tag() >= 0) && (cbi >= 0)) {
                        //return array[index];
                        tmpSolverBodyPool.get(cbi).readVelocity();
                    }
                }

                int numPoolConstraints = tmpSolverConstraintPool.size();
                for (int j = 0; j < numPoolConstraints; j++) {
//return array[index];
                    SolverConstraint solveManifold = tmpSolverConstraintPool.get(constraintPool.get(j));
//return array[index];
//return array[index];
                    resolveSingleCollisionCombinedCacheFriendly(tmpSolverBodyPool.get(solveManifold.solverBodyIdA),
                            tmpSolverBodyPool.get(solveManifold.solverBodyIdB), solveManifold, infoGlobal);
                }

                int numFrictionPoolConstraints = tmpSolverFrictionConstraintPool.size();

                for (int j = 0; j < numFrictionPoolConstraints; j++) {
//return array[index];
                    SolverConstraint solveManifold = tmpSolverFrictionConstraintPool.get(frictionPool.get(j));

//return array[index];
//return array[index];
                    float totalImpulse = tmpSolverConstraintPool.get(solveManifold.frictionIndex).appliedImpulse +
                            tmpSolverConstraintPool.get(solveManifold.frictionIndex).appliedPushImpulse;

//return array[index];
//return array[index];
                    resolveSingleFrictionCacheFriendly(tmpSolverBodyPool.get(solveManifold.solverBodyIdA),
                            tmpSolverBodyPool.get(solveManifold.solverBodyIdB), solveManifold, infoGlobal,
                            totalImpulse);
                }
            }

            if (infoGlobal.splitImpulse) {
                for (iteration = 0; iteration < infoGlobal.numIterations; iteration++) {
                    int numPoolConstraints = tmpSolverConstraintPool.size();
                    for (int j = 0; j < numPoolConstraints; j++) {
//return array[index];
                        SolverConstraint solveManifold = tmpSolverConstraintPool.get(constraintPool.get(j));

//return array[index];
//return array[index];
                        resolveSplitPenetrationImpulseCacheFriendly(tmpSolverBodyPool.get(solveManifold.solverBodyIdA),
                                tmpSolverBodyPool.get(solveManifold.solverBodyIdB), solveManifold, infoGlobal);
                    }
                }
            }

            return 0f;
    }

    void orderPool(int j, IntArrayList pool) {
        int tmp = pool.get(j);
        int swapi = randInt2(j + 1);

        pool.set(j, pool.get(swapi));
        pool.set(swapi, tmp);
    }

    float solveGroupCacheFriendly(Collection<Collidable> bodies, int numBodies, FasterList<PersistentManifold> manifoldPtr, int manifold_offset, int numManifolds, FasterList<TypedConstraint> constraints, int constraints_offset, int numConstraints, ContactSolverInfo infoGlobal/*,btStackAlloc* stackAlloc*/) {
        solveGroupCacheFriendlySetup(bodies, numBodies, manifoldPtr, manifold_offset, numManifolds, constraints, constraints_offset, numConstraints, infoGlobal/*, stackAlloc*/);
        solveGroupCacheFriendlyIterations(bodies, numBodies, manifoldPtr, manifold_offset, numManifolds, constraints, constraints_offset, numConstraints, infoGlobal/*, stackAlloc*/);

        int numPoolConstraints = tmpSolverConstraintPool.size();
        for (int j = 0; j < numPoolConstraints; j++) {

            //return array[index];
            SolverConstraint solveManifold = tmpSolverConstraintPool.get(j);
            ManifoldPoint pt = (ManifoldPoint) solveManifold.originalContactPoint;
            assert (pt != null);
            pt.appliedImpulse = solveManifold.appliedImpulse;

            //return array[index];
            pt.appliedImpulseLateral1 = tmpSolverFrictionConstraintPool.get(solveManifold.frictionIndex).appliedImpulse;
            //return array[index];
            pt.appliedImpulseLateral1 = tmpSolverFrictionConstraintPool.get(solveManifold.frictionIndex + 1).appliedImpulse;

            // do a callback here?
        }

        if (infoGlobal.splitImpulse) {
            for (int i = 0; i < tmpSolverBodyPool.size(); i++) {
                //return array[index];
                tmpSolverBodyPool.get(i).writebackVelocity(infoGlobal.timeStep);
            }
        } else {
            for (int i = 0; i < tmpSolverBodyPool.size(); i++) {
                //return array[index];
                tmpSolverBodyPool.get(i).writebackVelocity();
            }
        }

        //	printf("m_tmpSolverConstraintPool.size() = %i\n",m_tmpSolverConstraintPool.size());

		/*
		printf("m_tmpSolverBodyPool.size() = %i\n",m_tmpSolverBodyPool.size());
		printf("m_tmpSolverConstraintPool.size() = %i\n",m_tmpSolverConstraintPool.size());
		printf("m_tmpSolverFrictionConstraintPool.size() = %i\n",m_tmpSolverFrictionConstraintPool.size());
		printf("m_tmpSolverBodyPool.capacity() = %i\n",m_tmpSolverBodyPool.capacity());
		printf("m_tmpSolverConstraintPool.capacity() = %i\n",m_tmpSolverConstraintPool.capacity());
		printf("m_tmpSolverFrictionConstraintPool.capacity() = %i\n",m_tmpSolverFrictionConstraintPool.capacity());
		*/

        tmpSolverBodyPool.clearFast();

        tmpSolverConstraintPool.clearFast();

        tmpSolverFrictionConstraintPool.clearFast();

        return 0f;
    }

    /**
     * Sequentially applies impulses.
     */
    @Override
    public float solveGroup(Collection<Collidable> bodies, int numBodies, FasterList<PersistentManifold> manifoldPtr, int manifold_offset, int numManifolds, FasterList<TypedConstraint> constraints, int constraints_offset, int numConstraints, ContactSolverInfo infoGlobal, Intersecter intersecter) {
            // TODO: solver cache friendly
            if ((infoGlobal.solverMode & SolverMode.SOLVER_CACHE_FRIENDLY) != 0) {
                // you need to provide at least some bodies
                // SimpleDynamicsWorld needs to switch off SOLVER_CACHE_FRIENDLY
                assert (bodies != null);
                assert (numBodies != 0);
                float value = solveGroupCacheFriendly(bodies, numBodies, manifoldPtr, manifold_offset, numManifolds, constraints, constraints_offset, numConstraints, infoGlobal/*,stackAlloc*/);
                return value;
            }

            ContactSolverInfo info = new ContactSolverInfo(infoGlobal);

            int numiter = infoGlobal.numIterations;

            int totalPoints = 0;
            OrderIndex[] gOrder = this.gOrder;
            {
                short j;
                for (j = 0; j < numManifolds; j++) {
                    //return array[index];
                    PersistentManifold manifold = manifoldPtr.get(manifold_offset + j);
                    prepareConstraints(manifold, info);

                    //return array[index];
                    for (short p = 0; p < manifoldPtr.get(manifold_offset + j).numContacts(); p++) {
                        gOrder[totalPoints].manifoldIndex = j;
                        gOrder[totalPoints].pointIndex = p;
                        totalPoints++;
                    }
                }
            }

            {
                int j;
                for (j = 0; j < numConstraints; j++) {
                    constraints.get(constraints_offset + j).buildJacobian();
                }
            }

            // should traverse the contacts random order...
            int iteration;
            for (iteration = 0; iteration < numiter; iteration++) {
                int j;
                if ((infoGlobal.solverMode & SolverMode.SOLVER_RANDMIZE_ORDER) != 0) {
                    if ((iteration & 7) == 0) {
                        for (j = 0; j < totalPoints; ++j) {
                            // JAVA NOTE: swaps references instead of copying values (but that's fine in this context)
                            OrderIndex tmp = gOrder[j];
                            int swapi = randInt2(j + 1);
                            gOrder[j] = gOrder[swapi];
                            gOrder[swapi] = tmp;
                        }
                    }
                }

                for (j = 0; j < numConstraints; j++) {
                    constraints.get(constraints_offset + j).solveConstraint(info.timeStep);
                }

                for (j = 0; j < totalPoints; j++) {
                    PersistentManifold manifold = manifoldPtr.get(manifold_offset + gOrder[j].manifoldIndex);
                    solve((Dynamic) manifold.getBody0(),
                            (Dynamic) manifold.getBody1(),
                            manifold.getContactPoint(gOrder[j].pointIndex), info, iteration);
                }

                for (j = 0; j < totalPoints; j++) {
                    //return array[index];
                    PersistentManifold manifold = manifoldPtr.get(manifold_offset + gOrder[j].manifoldIndex);
                    solveFriction(
                            (Dynamic) manifold.getBody0(),
                            (Dynamic) manifold.getBody1(),
                            manifold.getContactPoint(gOrder[j].pointIndex), info, iteration);
                }

            }

            return 0f;
    }

    protected void prepareConstraints(PersistentManifold manifoldPtr, ContactSolverInfo info) {
        Dynamic body0 = (Dynamic) manifoldPtr.getBody0();
        Dynamic body1 = (Dynamic) manifoldPtr.getBody1();

        // only necessary to refresh the manifold once (first iteration). The integration is done outside the loop
        //#ifdef FORCE_REFESH_CONTACT_MANIFOLDS
        //manifoldPtr->refreshContactPoints(body0->getCenterOfMassTransform(),body1->getCenterOfMassTransform());
        //#endif //FORCE_REFESH_CONTACT_MANIFOLDS
        int numpoints = manifoldPtr.numContacts();

        BulletStats.gTotalContactPoints += numpoints;

        v3 tmpVec = new v3();

        v3 pos1 = new v3();
        v3 pos2 = new v3();
        v3 rel_pos1 = new v3();
        v3 rel_pos2 = new v3();
        v3 vel1 = new v3();
        v3 vel2 = new v3();
        v3 vel = new v3();
        v3 totalImpulse = new v3();
        v3 torqueAxis0 = new v3();
        v3 torqueAxis1 = new v3();
        v3 ftorqueAxis0 = new v3();
        v3 ftorqueAxis1 = new v3();

        final Transform tt1 = new Transform(), tt2 = new Transform();

        JacobianEntry jac = new JacobianEntry();

        for (int i = 0; i < numpoints; i++) {
            ManifoldPoint cp = manifoldPtr.getContactPoint(i);
            if (cp.distance1 <= 0f) {
                cp.getPositionWorldOnA(pos1);
                cp.getPositionWorldOnB(pos2);

                rel_pos1.sub(pos1, body0.transform);
                rel_pos2.sub(pos2, body1.transform);

                // this jacobian entry is re-used for all iterations
                Matrix3f mat1 = body0.transform.basis;
                mat1.transpose();

                Matrix3f mat2 = body1.transform.basis;
                mat2.transpose();


                jac.init(mat1, mat2,
                        rel_pos1, rel_pos2, cp.normalWorldOnB,
                        body0.invInertiaLocal, body0.getInvMass(),
                        body1.invInertiaLocal, body1.getInvMass());

                float jacDiagAB = jac.Adiag;

                ConstraintPersistentData cpd = (ConstraintPersistentData) cp.userPersistentData;
                if (cpd != null) {
                    // might be invalid
                    cpd.persistentLifeTime++;
                    if (cpd.persistentLifeTime != cp.lifeTime) {
                        //printf("Invalid: cpd->m_persistentLifeTime = %i cp.getLifeTime() = %i\n",cpd->m_persistentLifeTime,cp.getLifeTime());
                        //new (cpd) btConstraintPersistentData;
                        cpd.reset();
                        cpd.persistentLifeTime = cp.lifeTime;

                    } else {
                        //printf("Persistent: cpd->m_persistentLifeTime = %i cp.getLifeTime() = %i\n",cpd->m_persistentLifeTime,cp.getLifeTime());
                    }
                } else {
                    // todo: should this be in a pool?
                    //void* mem = btAlignedAlloc(sizeof(btConstraintPersistentData),16);
                    //cpd = new (mem)btConstraintPersistentData;
                    cpd = new ConstraintPersistentData();
                    //assert(cpd != null);

                    //totalCpd++;
                    //printf("totalCpd = %i Created Ptr %x\n",totalCpd,cpd);
                    cp.userPersistentData = cpd;
                    cpd.persistentLifeTime = cp.lifeTime;
                    //printf("CREATED: %x . cpd->m_persistentLifeTime = %i cp.getLifeTime() = %i\n",cpd,cpd->m_persistentLifeTime,cp.getLifeTime());
                }
                assert (cpd != null);

                cpd.jacDiagABInv = 1f / jacDiagAB;

                // Dependent on Rigidbody A and B types, fetch the contact/friction response func
                // perhaps do a similar thing for friction/restutution combiner funcs...

                cpd.frictionSolverFunc = frictionDispatch[body0.frictionSolverType][body1.frictionSolverType];
                cpd.contactSolverFunc = contactDispatch[body0.contactSolverType][body1.contactSolverType];

                body0.getVelocityInLocalPoint(rel_pos1, vel1);
                body1.getVelocityInLocalPoint(rel_pos2, vel2);
                vel.sub(vel1, vel2);

                float rel_vel;
                rel_vel = cp.normalWorldOnB.dot(vel);

                float combinedRestitution = cp.combinedRestitution;

                cpd.penetration = cp.distance1; ///btScalar(info.m_numIterations);
                cpd.friction = cp.combinedFriction;
                cpd.restitution = restitutionCurve(rel_vel, combinedRestitution);
                if (cpd.restitution <= 0f) {
                    cpd.restitution = 0f;
                }

                // restitution and penetration work in same direction so
                // rel_vel

                float penVel = -cpd.penetration / info.timeStep;

                if (cpd.restitution > penVel) {
                    cpd.penetration = 0f;
                }

                float relaxation = info.damping;
                if ((info.solverMode & SolverMode.SOLVER_USE_WARMSTARTING) != 0) {
                    cpd.appliedImpulse *= relaxation;
                } else {
                    cpd.appliedImpulse = 0f;
                }

                // for friction
                cpd.prevAppliedImpulse = cpd.appliedImpulse;

                // re-calculate friction direction every frame, todo: check if this is really needed
                TransformUtil.planeSpace1(cp.normalWorldOnB, cpd.frictionWorldTangential0, cpd.frictionWorldTangential1);

                //#define NO_FRICTION_WARMSTART 1
                //#ifdef NO_FRICTION_WARMSTART
                cpd.accumulatedTangentImpulse0 = 0f;
                cpd.accumulatedTangentImpulse1 = 0f;
                //#endif //NO_FRICTION_WARMSTART
                float denom0 = body0.computeImpulseDenominator(pos1, cpd.frictionWorldTangential0);
                float denom1 = body1.computeImpulseDenominator(pos2, cpd.frictionWorldTangential0);
                float denom = relaxation / (denom0 + denom1);
                cpd.jacDiagABInvTangent0 = denom;

                denom0 = body0.computeImpulseDenominator(pos1, cpd.frictionWorldTangential1);
                denom1 = body1.computeImpulseDenominator(pos2, cpd.frictionWorldTangential1);
                denom = relaxation / (denom0 + denom1);
                cpd.jacDiagABInvTangent1 = denom;

                //btVector3 totalImpulse =
                //	//#ifndef NO_FRICTION_WARMSTART
                //	//cpd->m_frictionWorldTangential0*cpd->m_accumulatedTangentImpulse0+
                //	//cpd->m_frictionWorldTangential1*cpd->m_accumulatedTangentImpulse1+
                //	//#endif //NO_FRICTION_WARMSTART
                //	cp.normalWorldOnB*cpd.appliedImpulse;
                totalImpulse.scale(cpd.appliedImpulse, cp.normalWorldOnB);

                ///
                torqueAxis0.cross(rel_pos1, cp.normalWorldOnB);

                cpd.angularComponentA.set(torqueAxis0);
                body0.invInertiaTensorWorld.transform(cpd.angularComponentA);

                torqueAxis1.cross(rel_pos2, cp.normalWorldOnB);

                cpd.angularComponentB.set(torqueAxis1);
                body1.invInertiaTensorWorld.transform(cpd.angularComponentB);
                ftorqueAxis0.cross(rel_pos1, cpd.frictionWorldTangential0);

                cpd.frictionAngularComponent0A.set(ftorqueAxis0);
                body0.invInertiaTensorWorld.transform(cpd.frictionAngularComponent0A);
                ftorqueAxis1.cross(rel_pos1, cpd.frictionWorldTangential1);

                cpd.frictionAngularComponent1A.set(ftorqueAxis1);
                body0.invInertiaTensorWorld.transform(cpd.frictionAngularComponent1A);
                ftorqueAxis0.cross(rel_pos2, cpd.frictionWorldTangential0);

                cpd.frictionAngularComponent0B.set(ftorqueAxis0);
                body1.invInertiaTensorWorld.transform(cpd.frictionAngularComponent0B);
                ftorqueAxis1.cross(rel_pos2, cpd.frictionWorldTangential1);

                cpd.frictionAngularComponent1B.set(ftorqueAxis1);
                body1.invInertiaTensorWorld.transform(cpd.frictionAngularComponent1B);

                ///

                // apply previous frames impulse on both bodies
                body0.impulse(totalImpulse, rel_pos1);

                tmpVec.negate(totalImpulse);
                body1.impulse(tmpVec, rel_pos2);
            }

        }
    }

    public static float solveCombinedContactFriction(Dynamic body0, Dynamic body1, ManifoldPoint cp, ContactSolverInfo info, int iter) {
        float maxImpulse = 0f;

        if (cp.distance1 <= 0f) {
//btConstraintPersistentData* cpd = (btConstraintPersistentData*) cp.m_userPersistentData;
            float impulse = ContactConstraint.resolveSingleCollisionCombined(body0, body1, cp, info);

            if (maxImpulse < impulse) {
                maxImpulse = impulse;
            }
        }
        return maxImpulse;
    }

    protected static float solve(Dynamic body0, Dynamic body1, ManifoldPoint cp, ContactSolverInfo info, int iter) {
        float maxImpulse = 0f;

        if (cp.distance1 <= 0f) {
            ConstraintPersistentData cpd = (ConstraintPersistentData) cp.userPersistentData;
            float impulse = cpd.contactSolverFunc.resolveContact(body0, body1, cp, info);

            if (maxImpulse < impulse) {
                maxImpulse = impulse;
            }
        }

        return maxImpulse;
    }

    protected static float solveFriction(Dynamic body0, Dynamic body1, ManifoldPoint cp, ContactSolverInfo info, int iter) {
        if (cp.distance1 <= 0f) {
            ConstraintPersistentData cpd = (ConstraintPersistentData) cp.userPersistentData;
            cpd.frictionSolverFunc.resolveContact(body0, body1, cp, info);
        }
        return 0f;
    }

    @Override
    public void reset() {
        btSeed2 = 0;
    }

    /**
     * Advanced: Override the default contact solving function for contacts, for certain types of rigidbody<br>
     * See RigidBody.contactSolverType and RigidBody.frictionSolverType
     */
    public void setContactSolverFunc(ContactSolverFunc func, int type0, int type1) {
        contactDispatch[type0][type1] = func;
    }

    /**
     * Advanced: Override the default friction solving function for contacts, for certain types of rigidbody<br>
     * See RigidBody.contactSolverType and RigidBody.frictionSolverType
     */
    public void setFrictionSolverFunc(ContactSolverFunc func, int type0, int type1) {
        frictionDispatch[type0][type1] = func;
    }

//    public void setRandSeed(long seed) {
//        btSeed2 = seed;
//    }
//
//    public long getRandSeed() {
//        return btSeed2;
//    }

    ////////////////////////////////////////////////////////////////////////////

    private static class OrderIndex {
        public int manifoldIndex;
        public int pointIndex;
    }

    /**
     * TODO: name
     *
     * @author jezek2
     */
    public enum ContactConstraintEnum {
        DEFAULT_CONTACT_SOLVER_TYPE,
        CONTACT_SOLVER_TYPE1,
        CONTACT_SOLVER_TYPE2,
        USER_CONTACT_SOLVER_TYPE1,
        MAX_CONTACT_SOLVER_TYPES
    }
}
