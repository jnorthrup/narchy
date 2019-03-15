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

package spacegraph.space3d.phys.dynamics.vehicle;

import jcog.math.v3;
import spacegraph.space3d.phys.Body3D;
import spacegraph.space3d.phys.math.Transform;

/**
 * WheelInfo contains information per wheel about friction and suspension.
 * 
 * @author jezek2
 */
public class WheelInfo {

	
	
	public final RaycastInfo raycastInfo = new RaycastInfo();

	public final Transform worldTransform = new Transform();
	
	private final v3 chassisConnectionPointCS = new v3();
	private final v3 wheelDirectionCS = new v3();
	private final v3 wheelAxleCS = new v3();
	private final float suspensionRestLength1;
	private final float maxSuspensionTravelCm;
	private final float maxSuspensionForce;
	private final float wheelsRadius;
	private final float suspensionStiffness;
	private final float wheelsDampingCompression;
	private final float wheelsDampingRelaxation;
	private final float frictionSlip;
	private final float steering;
	private final float rotation;
	private final float deltaRotation;
	private final float rollInfluence;

	private final float engineForce;

	private final float brake;
	
	private final boolean bIsFrontWheel;
	
	public Object clientInfo; 

	private float clippedInvContactDotSuspension;
	private float suspensionRelativeVelocity;
	
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
			
            raycastInfo.suspensionLength = suspensionRestLength1;
			suspensionRelativeVelocity = 0f;
			raycastInfo.contactNormalWS.negated(raycastInfo.wheelDirectionWS);
			clippedInvContactDotSuspension = 1f;
		}
	}
	
	
	
	public static class RaycastInfo {
		
		final v3 contactNormalWS = new v3();
		final v3 contactPointWS = new v3();
		float suspensionLength;
		public final v3 hardPointWS = new v3(); 
		final v3 wheelDirectionWS = new v3();
		public final v3 wheelAxleWS = new v3(); 
		boolean isInContact;
		public Object groundObject; 
	}
	
}
