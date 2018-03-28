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

package spacegraph.space3d.phys.dynamics.vehicle;

import spacegraph.space3d.phys.Body3D;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.util.math.v3;

/**
 * WheelInfo contains information per wheel about friction and suspension.
 * 
 * @author jezek2
 */
public class WheelInfo {

	//protected final BulletStack stack = BulletStack.get();
	
	public final RaycastInfo raycastInfo = new RaycastInfo();

	public final Transform worldTransform = new Transform();
	
	public final v3 chassisConnectionPointCS = new v3(); // const
	public final v3 wheelDirectionCS = new v3(); // const
	public final v3 wheelAxleCS = new v3(); // const or modified by steering
	public float suspensionRestLength1; // const
	public float maxSuspensionTravelCm;
	public float maxSuspensionForce;
	public float wheelsRadius; // const
	public float suspensionStiffness; // const
	public float wheelsDampingCompression; // const
	public float wheelsDampingRelaxation; // const
	public float frictionSlip;
	public float steering;
	public float rotation;
	public float deltaRotation;
	public float rollInfluence;

	public float engineForce;

	public float brake;
	
	public boolean bIsFrontWheel;
	
	public Object clientInfo; // can be used to store pointer to sync transforms...

	public float clippedInvContactDotSuspension;
	public float suspensionRelativeVelocity;
	// calculated by suspension
	public float wheelsSuspensionForce;
	public float skidInfo;
	
	public WheelInfo(WheelInfoConstructionInfo ci) {
		suspensionRestLength1 = ci.suspensionRestLength;
		maxSuspensionTravelCm = ci.maxSuspensionTravelCm;
                maxSuspensionForce = ci.maxSuspensionForce;

		wheelsRadius = ci.wheelRadius;
		suspensionStiffness = ci.suspensionStiffness;
		wheelsDampingCompression = ci.wheelsDampingCompression;
		wheelsDampingRelaxation = ci.wheelsDampingRelaxation;
		chassisConnectionPointCS.set(ci.chassisConnectionCS);
		wheelDirectionCS.set(ci.wheelDirectionCS);
		wheelAxleCS.set(ci.wheelAxleCS);
		frictionSlip = ci.frictionSlip;
		steering = 0f;
		engineForce = 0f;
		rotation = 0f;
		deltaRotation = 0f;
		brake = 0f;
		rollInfluence = 0.1f;
		bIsFrontWheel = ci.bIsFrontWheel;
	}
	
	public float getSuspensionRestLength() {
		return suspensionRestLength1;
	}

	public void updateWheel(Body3D chassis, RaycastInfo raycastInfo) {
		if (raycastInfo.isInContact) {
			float project = raycastInfo.contactNormalWS.dot(raycastInfo.wheelDirectionWS);
			v3 chassis_velocity_at_contactPoint = new v3();
			v3 relpos = new v3();
			relpos.sub(raycastInfo.contactPointWS, chassis.getCenterOfMassPosition(new v3()));
			chassis.getVelocityInLocalPoint(relpos, chassis_velocity_at_contactPoint);
			float projVel = raycastInfo.contactNormalWS.dot(chassis_velocity_at_contactPoint);
			if (project >= -0.1f) {
				suspensionRelativeVelocity = 0f;
				clippedInvContactDotSuspension = 1f / 0.1f;
			}
			else {
				float inv = -1f / project;
				suspensionRelativeVelocity = projVel * inv;
				clippedInvContactDotSuspension = inv;
			}
		}
		else {
			// Not in contact : position wheel in a nice (rest length) position
            raycastInfo.suspensionLength = suspensionRestLength1;
			suspensionRelativeVelocity = 0f;
			raycastInfo.contactNormalWS.negate(raycastInfo.wheelDirectionWS);
			clippedInvContactDotSuspension = 1f;
		}
	}
	
	////////////////////////////////////////////////////////////////////////////
	
	public static class RaycastInfo {
		// set by raycaster
		public final v3 contactNormalWS = new v3(); // contactnormal
		public final v3 contactPointWS = new v3(); // raycast hitpoint
		public float suspensionLength;
		public final v3 hardPointWS = new v3(); // raycast starting point
		public final v3 wheelDirectionWS = new v3(); // direction in worldspace
		public final v3 wheelAxleWS = new v3(); // axle in worldspace
		public boolean isInContact;
		public Object groundObject; // could be general void* ptr
	}
	
}
