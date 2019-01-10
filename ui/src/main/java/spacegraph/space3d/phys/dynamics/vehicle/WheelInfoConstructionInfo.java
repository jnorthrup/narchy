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

/**
 * 
 * @author jezek2
 */
class WheelInfoConstructionInfo {

	public final v3 chassisConnectionCS = new v3();
	public final v3 wheelDirectionCS = new v3();
	public final v3 wheelAxleCS = new v3();
	public float suspensionRestLength;
	public float maxSuspensionTravelCm;
	public float maxSuspensionForce;
	public float wheelRadius;
	
	public float suspensionStiffness;
	public float wheelsDampingCompression;
	public float wheelsDampingRelaxation;
	public float frictionSlip;
	public boolean bIsFrontWheel;
	
}
