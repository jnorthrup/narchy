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
		var normalLenSqr = normal.lengthSquared();
		assert (Math.abs(normalLenSqr) < 1.1f);
		if (normalLenSqr > 1.1f) {
			impulse[0] = 0f;
			return;
		}

		var tmp = new v3();

		var rel_pos1 = new v3();
		rel_pos1.sub(pos1, body1.getCenterOfMassPosition(tmp));

		var rel_pos2 = new v3();
		rel_pos2.sub(pos2, body2.getCenterOfMassPosition(tmp));


		var vel1 = new v3();
		body1.getVelocityInLocalPoint(rel_pos1, vel1);

		var vel2 = new v3();
		body2.getVelocityInLocalPoint(rel_pos2, vel2);

		var vel = new v3();
		vel.sub(vel1, vel2);

		var mat1 = body1.getCenterOfMassTransform(new Transform()).basis;
		mat1.transpose();

		var mat2 = body2.getCenterOfMassTransform(new Transform()).basis;
		mat2.transpose();

		var jac = new JacobianEntry();
		jac.init(mat1, mat2,
				rel_pos1, rel_pos2, normal,
				body1.getInvInertiaDiagLocal(new v3()), body1.getInvMass(),
				body2.getInvInertiaDiagLocal(new v3()), body2.getInvMass());

		var jacDiagAB = jac.Adiag;

		var tmp1 = body1.getAngularVelocity(new v3());
		mat1.transform(tmp1);

		var tmp2 = body2.getAngularVelocity(new v3());
		mat2.transform(tmp2);

		var rel_vel = jac.getRelativeVelocity(
				body1.getLinearVelocity(new v3()),
				tmp1,
				body2.getLinearVelocity(new v3()),
				tmp2);

		var jacDiagABInv = 1f / jacDiagAB;
		var a = jacDiagABInv;


		rel_vel = normal.dot(vel);


		var contactDamping = 0.2f;


		var velocityImpulse = -contactDamping * rel_vel * jacDiagABInv;
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

		var tmpVec = new v3();

		var pos1_ = contactPoint.getPositionWorldOnA(new v3());
		var pos2_ = contactPoint.getPositionWorldOnB(new v3());
		var normal = contactPoint.normalWorldOnB;


		var rel_pos1 = new v3();
		rel_pos1.sub(pos1_, body1.getCenterOfMassPosition(tmpVec));

		var rel_pos2 = new v3();
		rel_pos2.sub(pos2_, body2.getCenterOfMassPosition(tmpVec));

		var vel1 = body1.getVelocityInLocalPoint(rel_pos1, new v3());
		var vel2 = body2.getVelocityInLocalPoint(rel_pos2, new v3());
		var vel = new v3();
		vel.sub(vel1, vel2);

		var rel_vel = normal.dot(vel);

		var Kfps = 1f / solverInfo.timeStep;


		var Kerp = solverInfo.erp;

		var cpd = (ConstraintPersistentData) contactPoint.userPersistentData;
		assert (cpd != null);
		var distance = cpd.penetration;
		var Kcor = Kerp * Kfps;
		var positionalError = Kcor * -distance;
		var velocityError = cpd.restitution - rel_vel;

		var penetrationImpulse = positionalError * cpd.jacDiagABInv;

		var velocityImpulse = velocityError * cpd.jacDiagABInv;

		var normalImpulse = penetrationImpulse + velocityImpulse;


		var oldNormalImpulse = cpd.appliedImpulse;
		var sum = oldNormalImpulse + normalImpulse;
		cpd.appliedImpulse = Math.max(0f, sum);

		normalImpulse = cpd.appliedImpulse - oldNormalImpulse;


		var tmp = new v3();
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

		var tmpVec = new v3();

		var pos1 = contactPoint.getPositionWorldOnA(new v3());
		var pos2 = contactPoint.getPositionWorldOnB(new v3());

		var rel_pos1 = new v3();
		rel_pos1.sub(pos1, body1.getCenterOfMassPosition(tmpVec));

		var rel_pos2 = new v3();
		rel_pos2.sub(pos2, body2.getCenterOfMassPosition(tmpVec));

		var cpd = (ConstraintPersistentData) contactPoint.userPersistentData;
		assert (cpd != null);

		var combinedFriction = cpd.friction;

		var limit = cpd.appliedImpulse * combinedFriction;

		if (cpd.appliedImpulse > 0f) 
		{


			var vel1 = new v3();
			body1.getVelocityInLocalPoint(rel_pos1, vel1);

			var vel2 = new v3();
			body2.getVelocityInLocalPoint(rel_pos2, vel2);

			var vel = new v3();
			vel.sub(vel1, vel2);

			float j1;

            {
				var vrel = cpd.frictionWorldTangential0.dot(vel);

				
				j1 = -vrel * cpd.jacDiagABInvTangent0;
				var oldTangentImpulse = cpd.accumulatedTangentImpulse0;
				cpd.accumulatedTangentImpulse0 = oldTangentImpulse + j1;

				cpd.accumulatedTangentImpulse0 = Math.min(cpd.accumulatedTangentImpulse0, limit);
				cpd.accumulatedTangentImpulse0 = Math.max(cpd.accumulatedTangentImpulse0, -limit);
				j1 = cpd.accumulatedTangentImpulse0 - oldTangentImpulse;
			}


			var vrel = cpd.frictionWorldTangential1.dot(vel);


			var j2 = -vrel * cpd.jacDiagABInvTangent1;
			var oldTangentImpulse = cpd.accumulatedTangentImpulse1;
            cpd.accumulatedTangentImpulse1 = oldTangentImpulse + j2;

            cpd.accumulatedTangentImpulse1 = Math.min(cpd.accumulatedTangentImpulse1, limit);
            cpd.accumulatedTangentImpulse1 = Math.max(cpd.accumulatedTangentImpulse1, -limit);
            j2 = cpd.accumulatedTangentImpulse1 - oldTangentImpulse;


			var tmp = new v3();

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

		var tmpVec = new v3();

		var pos1 = contactPoint.getPositionWorldOnA(new v3());
		var pos2 = contactPoint.getPositionWorldOnB(new v3());
		var normal = contactPoint.normalWorldOnB;

		var rel_pos1 = new v3();
		rel_pos1.sub(pos1, body1.getCenterOfMassPosition(tmpVec));

		var rel_pos2 = new v3();
		rel_pos2.sub(pos2, body2.getCenterOfMassPosition(tmpVec));

		var vel1 = body1.getVelocityInLocalPoint(rel_pos1, new v3());
		var vel2 = body2.getVelocityInLocalPoint(rel_pos2, new v3());
		var vel = new v3();
		vel.sub(vel1, vel2);

		var rel_vel = normal.dot(vel);

		var Kfps = 1f / solverInfo.timeStep;


		var Kerp = solverInfo.erp;

		var cpd = (ConstraintPersistentData) contactPoint.userPersistentData;
		assert (cpd != null);
		var distance = cpd.penetration;
		var Kcor = Kerp * Kfps;
		var positionalError = Kcor * -distance;
		var velocityError = cpd.restitution - rel_vel;

		var penetrationImpulse = positionalError * cpd.jacDiagABInv;

		var velocityImpulse = velocityError * cpd.jacDiagABInv;

		var normalImpulse = penetrationImpulse + velocityImpulse;


		var oldNormalImpulse = cpd.appliedImpulse;
		var sum = oldNormalImpulse + normalImpulse;
		cpd.appliedImpulse = Math.max(0f, sum);

		normalImpulse = cpd.appliedImpulse - oldNormalImpulse;


		var tmp = new v3();
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
		var lat_vel = new v3();
        lat_vel.sub(vel, tmp);
		var lat_rel_vel = lat_vel.length();

		var combinedFriction = cpd.friction;

        if (cpd.appliedImpulse > 0) {
            if (lat_rel_vel > BulletGlobals.FLT_EPSILON) {
                lat_vel.scaled(1f / lat_rel_vel);

				var temp1 = new v3();
                temp1.cross(rel_pos1, lat_vel);
                body1.getInvInertiaTensorWorld(new Matrix3f()).transform(temp1);

				var temp2 = new v3();
                temp2.cross(rel_pos2, lat_vel);
                body2.getInvInertiaTensorWorld(new Matrix3f()).transform(temp2);

				var java_tmp1 = new v3();
                java_tmp1.cross(temp1, rel_pos1);

				var java_tmp2 = new v3();
                java_tmp2.cross(temp2, rel_pos2);

                tmp.add(java_tmp1, java_tmp2);

				var friction_impulse = lat_rel_vel /
                        (body1.getInvMass() + body2.getInvMass() + lat_vel.dot(tmp));
				var normal_impulse = cpd.appliedImpulse * combinedFriction;

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
