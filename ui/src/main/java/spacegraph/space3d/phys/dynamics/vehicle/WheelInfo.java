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

    private final float suspensionRestLength1;

    public Object clientInfo;

    public float wheelsSuspensionForce;
	public float skidInfo;
	
	public WheelInfo(WheelInfoConstructionInfo ci) {
		suspensionRestLength1 = ci.suspensionRestLength;
		var maxSuspensionTravelCm = ci.maxSuspensionTravelCm;
		var maxSuspensionForce = ci.maxSuspensionForce;

		var wheelsRadius = ci.wheelRadius;
		var suspensionStiffness = ci.suspensionStiffness;
		var wheelsDampingCompression = ci.wheelsDampingCompression;
		var wheelsDampingRelaxation = ci.wheelsDampingRelaxation;
		var chassisConnectionPointCS = new v3();
        chassisConnectionPointCS.set(ci.chassisConnectionCS);
		var wheelDirectionCS = new v3();
        wheelDirectionCS.set(ci.wheelDirectionCS);
		var wheelAxleCS = new v3();
        wheelAxleCS.set(ci.wheelAxleCS);
		var frictionSlip = ci.frictionSlip;
		var steering = 0f;
		var engineForce = 0f;
		var rotation = 0f;
		var deltaRotation = 0f;
		var brake = 0f;
		var rollInfluence = 0.1f;
		var bIsFrontWheel = ci.bIsFrontWheel;
	}
	
	public float getSuspensionRestLength() {
		return suspensionRestLength1;
	}

	public void updateWheel(Body3D chassis, RaycastInfo raycastInfo) {
        float suspensionRelativeVelocity;
        float clippedInvContactDotSuspension;
        if (raycastInfo.isInContact) {
			var project = raycastInfo.contactNormalWS.dot(raycastInfo.wheelDirectionWS);
			var chassis_velocity_at_contactPoint = new v3();
			var relpos = new v3();
			relpos.sub(raycastInfo.contactPointWS, chassis.getCenterOfMassPosition(new v3()));
			chassis.getVelocityInLocalPoint(relpos, chassis_velocity_at_contactPoint);
			var projVel = raycastInfo.contactNormalWS.dot(chassis_velocity_at_contactPoint);
			if (project >= -0.1f) {
				suspensionRelativeVelocity = 0f;
				clippedInvContactDotSuspension = 1f / 0.1f;
			}
			else {
				var inv = -1f / project;
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
