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

package spacegraph.space3d.phys.shape;


import jcog.math.v3;

/**
 * Cylinder shape around the X axis.
 * 
 * @author jezek2
 */
public class CylinderShapeX extends CylinderShape {

	public CylinderShapeX(v3 halfExtents) {
		super(halfExtents, false);
		upAxis = 0;
		recalcLocalAabb();
	}

	@Override
	public v3 localGetSupportingVertexWithoutMargin(v3 vec, v3 out) {
		return cylinderLocalSupportX(getHalfExtentsWithoutMargin(new v3()), vec, out);
	}

	@Override
	public void batchedUnitVectorGetSupportingVertexWithoutMargin(v3[] vectors, v3[] supportVerticesOut, int numVectors) {
		for (var i = 0; i < numVectors; i++) {
			cylinderLocalSupportX(getHalfExtentsWithoutMargin(new v3()), vectors[i], supportVerticesOut[i]);
		}
	}

	@Override
	public float getRadius() {
		return getHalfExtentsWithMargin(new v3()).y;
	}

	@Override
	public String getName() {
		return "CylinderX";
	}
	
}
