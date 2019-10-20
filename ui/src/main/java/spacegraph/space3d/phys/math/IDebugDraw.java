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

package spacegraph.space3d.phys.math;

import jcog.math.v3;
import spacegraph.space3d.phys.Collisions;
import spacegraph.space3d.phys.Dynamics3D;

/**
 * IDebugDraw interface class allows hooking up a debug renderer to visually debug
 * simulations.<p>
 * 
 * Typical use case: create a debug drawer object, and assign it to a {@link Collisions}
 * or {@link Dynamics3D} using setDebugDrawer and call debugDrawWorld.<p>
 * 
 * A class that implements the IDebugDraw interface has to implement the drawLine
 * method at a minimum.
 * 
 * @author jezek2
 */
public abstract class IDebugDraw {
	
	

	public abstract void drawLine(v3 from, v3 to, v3 color);
	
	public void drawTriangle(v3 v0, v3 v1, v3 v2, v3 n0, v3 n1, v3 n2, v3 color, float alpha) {
		drawTriangle(v0, v1, v2, color, alpha);
	}
	
	private void drawTriangle(v3 v0, v3 v1, v3 v2, v3 color, float alpha) {
		drawLine(v0, v1, color);
		drawLine(v1, v2, color);
		drawLine(v2, v0, color);
	}

	public abstract void drawContactPoint(v3 PointOnB, v3 normalOnB, float distance, int lifeTime, v3 color);

	public abstract void reportErrorWarning(String warningString);

	public abstract void draw3dText(v3 location, String textString);

	public abstract void setDebugMode(int debugMode);

	public abstract int getDebugMode();

	public void drawAabb(v3 from, v3 to, v3 color) {
		var halfExtents = new v3(to);
		halfExtents.sub(from);
		halfExtents.scaled(0.5f);

		var center = new v3(to);
		center.add(from);
		center.scaled(0.5f);

		var edgecoord = new v3();
		edgecoord.set(1f, 1f, 1f);
		v3 pa = new v3(), pb = new v3();
		for (var i = 0; i < 4; i++) {
			for (var j = 0; j < 3; j++) {
				pa.set(edgecoord.x * halfExtents.x, edgecoord.y * halfExtents.y, edgecoord.z * halfExtents.z);
				pa.add(center);

				var othercoord = j % 3;

				VectorUtil.mulCoord(edgecoord, othercoord, -1f);
				pb.set(edgecoord.x * halfExtents.x, edgecoord.y * halfExtents.y, edgecoord.z * halfExtents.z);
				pb.add(center);

				drawLine(pa, pb, color);
			}
			edgecoord.set(-1f, -1f, -1f);
			if (i < 3) {
				VectorUtil.mulCoord(edgecoord, i, -1f);
			}
		}
	}
}
