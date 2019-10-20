/*
 * Java port of Bullet (c) 2008 Martin Dvorak <jezek2@advel.cz>
 *
 * Bullet Continuous Collision Detection and Physics Library
 * Copyright (c) 2003-2008 Erwin Coumans  http:
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

package spacegraph.space3d.phys.constraint;

import jcog.math.v3;
import spacegraph.space3d.phys.Body3D;
import spacegraph.space3d.phys.BulletGlobals;
import spacegraph.space3d.phys.collision.narrow.ManifoldPoint;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.solve.ConstraintPersistentData;
import spacegraph.space3d.phys.solve.ContactSolverFunc;
import spacegraph.space3d.phys.solve.ContactSolverInfo;
import spacegraph.space3d.phys.solve.JacobianEntry;
import spacegraph.util.math.Matrix3f;

/**
 * Functions for resolving contacts.
 * 
 * @author jezek2
 */
public class ContactConstraint {
	
	public static final ContactSolverFunc resolveSingleCollision = new ContactSolverFunc() {
		@Override
        public float resolveContact(Body3D body1, Body3D body2, ManifoldPoint contactPoint, ContactSolverInfo info) {
			return resolveSingleCollision(body1, body2, contactPoint, info);
		}
	};

	public static final ContactSolverFunc resolveSingleFriction = new ContactSolverFunc() {
		@Override
        public float resolveContact(Body3D body1, Body3D body2, ManifoldPoint contactPoint, ContactSolverInfo info) {
			return resolveSingleFriction(body1, body2, contactPoint, info);
		}
	};

	public static final ContactSolverFunc resolveSingleCollisionCombined = new ContactSolverFunc() {
		@Override
        public float resolveContact(Body3D body1, Body3D body2, ManifoldPoint contactPoint, ContactSolverInfo info) {
			return resolveSingleCollisionCombined(body1, body2, contactPoint, info);
		}
	};

	/**
	 * Bilateral constraint between two dynamic objects.
	 */
	public static void resolveSingleBilateral(Body3D body1, v3 pos1,
                                              Body3D body2, v3 pos2,
                                              float distance, v3 normal, float[] impulse, float timeStep) {
        float normalLenSqr = normal.lengthSquared();
		assert (Math.abs(normalLenSqr) < 1.1f);
		if (normalLenSqr > 1.1f) {
			impulse[0] = 0f;
			return;
		}

        v3 tmp = new v3();

        v3 rel_pos1 = new v3();
		rel_pos1.sub(pos1, body1.getCenterOfMassPosition(tmp));

        v3 rel_pos2 = new v3();
		rel_pos2.sub(pos2, body2.getCenterOfMassPosition(tmp));


        v3 vel1 = new v3();
		body1.getVelocityInLocalPoint(rel_pos1, vel1);

        v3 vel2 = new v3();
		body2.getVelocityInLocalPoint(rel_pos2, vel2);

        v3 vel = new v3();
		vel.sub(vel1, vel2);

        Matrix3f mat1 = body1.getCenterOfMassTransform(new Transform()).basis;
		mat1.transpose();

        Matrix3f mat2 = body2.getCenterOfMassTransform(new Transform()).basis;
		mat2.transpose();

        JacobianEntry jac = new JacobianEntry();
		jac.init(mat1, mat2,
				rel_pos1, rel_pos2, normal,
				body1.getInvInertiaDiagLocal(new v3()), body1.getInvMass(),
				body2.getInvInertiaDiagLocal(new v3()), body2.getInvMass());

        float jacDiagAB = jac.Adiag;

        v3 tmp1 = body1.getAngularVelocity(new v3());
		mat1.transform(tmp1);

        v3 tmp2 = body2.getAngularVelocity(new v3());
		mat2.transform(tmp2);

        float rel_vel = jac.getRelativeVelocity(
				body1.getLinearVelocity(new v3()),
				tmp1,
				body2.getLinearVelocity(new v3()),
				tmp2);

        float jacDiagABInv = 1f / jacDiagAB;
        float a = jacDiagABInv;


		rel_vel = normal.dot(vel);


        float contactDamping = 0.2f;


        float velocityImpulse = -contactDamping * rel_vel * jacDiagABInv;
		impulse[0] = velocityImpulse;
		
	}

	/**
	 * Response between two dynamic objects with friction.
	 */
	private static float resolveSingleCollision(
            Body3D body1,
            Body3D body2,
            ManifoldPoint contactPoint,
            ContactSolverInfo solverInfo) {

        v3 tmpVec = new v3();

        v3 pos1_ = contactPoint.getPositionWorldOnA(new v3());
        v3 pos2_ = contactPoint.getPositionWorldOnB(new v3());
        v3 normal = contactPoint.normalWorldOnB;


        v3 rel_pos1 = new v3();
		rel_pos1.sub(pos1_, body1.getCenterOfMassPosition(tmpVec));

        v3 rel_pos2 = new v3();
		rel_pos2.sub(pos2_, body2.getCenterOfMassPosition(tmpVec));

        v3 vel1 = body1.getVelocityInLocalPoint(rel_pos1, new v3());
        v3 vel2 = body2.getVelocityInLocalPoint(rel_pos2, new v3());
        v3 vel = new v3();
		vel.sub(vel1, vel2);

        float rel_vel = normal.dot(vel);

        float Kfps = 1f / solverInfo.timeStep;


        float Kerp = solverInfo.erp;

        ConstraintPersistentData cpd = (ConstraintPersistentData) contactPoint.userPersistentData;
		assert (cpd != null);
        float distance = cpd.penetration;
        float Kcor = Kerp * Kfps;
        float positionalError = Kcor * -distance;
        float velocityError = cpd.restitution - rel_vel;

        float penetrationImpulse = positionalError * cpd.jacDiagABInv;

        float velocityImpulse = velocityError * cpd.jacDiagABInv;

        float normalImpulse = penetrationImpulse + velocityImpulse;


        float oldNormalImpulse = cpd.appliedImpulse;
        float sum = oldNormalImpulse + normalImpulse;
		cpd.appliedImpulse = Math.max(0f, sum);

		normalImpulse = cpd.appliedImpulse - oldNormalImpulse;


        v3 tmp = new v3();
		if (body1.getInvMass() != 0f) {
			tmp.scale(body1.getInvMass(), contactPoint.normalWorldOnB);
			body1.internalApplyImpulse(tmp, cpd.angularComponentA, normalImpulse);
		}
		if (body2.getInvMass() != 0f) {
			tmp.scale(body2.getInvMass(), contactPoint.normalWorldOnB);
			body2.internalApplyImpulse(tmp, cpd.angularComponentB, -normalImpulse);
		}
		
		
		
		

		return normalImpulse;
	}

	private static float resolveSingleFriction(
            Body3D body1,
            Body3D body2,
            ManifoldPoint contactPoint,
            ContactSolverInfo solverInfo) {

        v3 tmpVec = new v3();

        v3 pos1 = contactPoint.getPositionWorldOnA(new v3());
        v3 pos2 = contactPoint.getPositionWorldOnB(new v3());

        v3 rel_pos1 = new v3();
		rel_pos1.sub(pos1, body1.getCenterOfMassPosition(tmpVec));

        v3 rel_pos2 = new v3();
		rel_pos2.sub(pos2, body2.getCenterOfMassPosition(tmpVec));

        ConstraintPersistentData cpd = (ConstraintPersistentData) contactPoint.userPersistentData;
		assert (cpd != null);

        float combinedFriction = cpd.friction;

        float limit = cpd.appliedImpulse * combinedFriction;

		if (cpd.appliedImpulse > 0f) 
		{


            v3 vel1 = new v3();
			body1.getVelocityInLocalPoint(rel_pos1, vel1);

            v3 vel2 = new v3();
			body2.getVelocityInLocalPoint(rel_pos2, vel2);

            v3 vel = new v3();
			vel.sub(vel1, vel2);

			float j1;

            {
                float vrel = cpd.frictionWorldTangential0.dot(vel);

				
				j1 = -vrel * cpd.jacDiagABInvTangent0;
                float oldTangentImpulse = cpd.accumulatedTangentImpulse0;
				cpd.accumulatedTangentImpulse0 = oldTangentImpulse + j1;

				cpd.accumulatedTangentImpulse0 = Math.min(cpd.accumulatedTangentImpulse0, limit);
				cpd.accumulatedTangentImpulse0 = Math.max(cpd.accumulatedTangentImpulse0, -limit);
				j1 = cpd.accumulatedTangentImpulse0 - oldTangentImpulse;
			}


            float vrel = cpd.frictionWorldTangential1.dot(vel);


            float j2 = -vrel * cpd.jacDiagABInvTangent1;
            float oldTangentImpulse = cpd.accumulatedTangentImpulse1;
            cpd.accumulatedTangentImpulse1 = oldTangentImpulse + j2;

            cpd.accumulatedTangentImpulse1 = Math.min(cpd.accumulatedTangentImpulse1, limit);
            cpd.accumulatedTangentImpulse1 = Math.max(cpd.accumulatedTangentImpulse1, -limit);
            j2 = cpd.accumulatedTangentImpulse1 - oldTangentImpulse;


            v3 tmp = new v3();

			if (body1.getInvMass() != 0f) {
				tmp.scale(body1.getInvMass(), cpd.frictionWorldTangential0);
				body1.internalApplyImpulse(tmp, cpd.frictionAngularComponent0A, j1);

				tmp.scale(body1.getInvMass(), cpd.frictionWorldTangential1);
				body1.internalApplyImpulse(tmp, cpd.frictionAngularComponent1A, j2);
			}
			if (body2.getInvMass() != 0f) {
				tmp.scale(body2.getInvMass(), cpd.frictionWorldTangential0);
				body2.internalApplyImpulse(tmp, cpd.frictionAngularComponent0B, -j1);

				tmp.scale(body2.getInvMass(), cpd.frictionWorldTangential1);
				body2.internalApplyImpulse(tmp, cpd.frictionAngularComponent1B, -j2);
			}
			
			
			
			
		}
		return cpd.appliedImpulse;
	}

	/**
	 * velocity + friction<br>
	 * response between two dynamic objects with friction
	 */
	public static float resolveSingleCollisionCombined(
			Body3D body1,
			Body3D body2,
			ManifoldPoint contactPoint,
			ContactSolverInfo solverInfo) {

        v3 tmpVec = new v3();

        v3 pos1 = contactPoint.getPositionWorldOnA(new v3());
        v3 pos2 = contactPoint.getPositionWorldOnB(new v3());
        v3 normal = contactPoint.normalWorldOnB;

        v3 rel_pos1 = new v3();
		rel_pos1.sub(pos1, body1.getCenterOfMassPosition(tmpVec));

        v3 rel_pos2 = new v3();
		rel_pos2.sub(pos2, body2.getCenterOfMassPosition(tmpVec));

        v3 vel1 = body1.getVelocityInLocalPoint(rel_pos1, new v3());
        v3 vel2 = body2.getVelocityInLocalPoint(rel_pos2, new v3());
        v3 vel = new v3();
		vel.sub(vel1, vel2);

        float rel_vel = normal.dot(vel);

        float Kfps = 1f / solverInfo.timeStep;


        float Kerp = solverInfo.erp;

        ConstraintPersistentData cpd = (ConstraintPersistentData) contactPoint.userPersistentData;
		assert (cpd != null);
        float distance = cpd.penetration;
        float Kcor = Kerp * Kfps;
        float positionalError = Kcor * -distance;
        float velocityError = cpd.restitution - rel_vel;

        float penetrationImpulse = positionalError * cpd.jacDiagABInv;

        float velocityImpulse = velocityError * cpd.jacDiagABInv;

        float normalImpulse = penetrationImpulse + velocityImpulse;


        float oldNormalImpulse = cpd.appliedImpulse;
        float sum = oldNormalImpulse + normalImpulse;
		cpd.appliedImpulse = Math.max(0f, sum);

		normalImpulse = cpd.appliedImpulse - oldNormalImpulse;


        v3 tmp = new v3();
		if (body1.getInvMass() != 0f) {
			tmp.scale(body1.getInvMass(), contactPoint.normalWorldOnB);
			body1.internalApplyImpulse(tmp, cpd.angularComponentA, normalImpulse);
		}
		if (body2.getInvMass() != 0f) {
			tmp.scale(body2.getInvMass(), contactPoint.normalWorldOnB);
			body2.internalApplyImpulse(tmp, cpd.angularComponentB, -normalImpulse);
		}
		
		
		
		

        
        body1.getVelocityInLocalPoint(rel_pos1, vel1);
        body2.getVelocityInLocalPoint(rel_pos2, vel2);
        vel.sub(vel1, vel2);

        rel_vel = normal.dot(vel);

        tmp.scale(rel_vel, normal);
        v3 lat_vel = new v3();
        lat_vel.sub(vel, tmp);
        float lat_rel_vel = lat_vel.length();

        float combinedFriction = cpd.friction;

        if (cpd.appliedImpulse > (float) 0) {
            if (lat_rel_vel > BulletGlobals.FLT_EPSILON) {
                lat_vel.scaled(1f / lat_rel_vel);

                v3 temp1 = new v3();
                temp1.cross(rel_pos1, lat_vel);
                body1.getInvInertiaTensorWorld(new Matrix3f()).transform(temp1);

                v3 temp2 = new v3();
                temp2.cross(rel_pos2, lat_vel);
                body2.getInvInertiaTensorWorld(new Matrix3f()).transform(temp2);

                v3 java_tmp1 = new v3();
                java_tmp1.cross(temp1, rel_pos1);

                v3 java_tmp2 = new v3();
                java_tmp2.cross(temp2, rel_pos2);

                tmp.add(java_tmp1, java_tmp2);

                float friction_impulse = lat_rel_vel /
                        (body1.getInvMass() + body2.getInvMass() + lat_vel.dot(tmp));
                float normal_impulse = cpd.appliedImpulse * combinedFriction;

                friction_impulse = Math.min(friction_impulse, normal_impulse);
                friction_impulse = Math.max(friction_impulse, -normal_impulse);

                tmp.scale(-friction_impulse, lat_vel);
                body1.impulse(tmp, rel_pos1);

                tmp.scale(friction_impulse, lat_vel);
                body2.impulse(tmp, rel_pos2);
            }
        }

        return normalImpulse;
	}

	public static float resolveSingleFrictionEmpty(
			Body3D body1,
			Body3D body2,
			ManifoldPoint contactPoint,
			ContactSolverInfo solverInfo) {
		return 0f;
	}
	
}
