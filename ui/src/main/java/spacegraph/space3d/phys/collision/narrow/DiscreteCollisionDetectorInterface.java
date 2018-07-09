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

package spacegraph.space3d.phys.collision.narrow;

import spacegraph.space3d.phys.math.Transform;
import spacegraph.util.math.v3;

/**
 * This interface is made to be used by an iterative approach to do TimeOfImpact calculations.<p>
 * 
 * This interface allows to query for closest points and penetration depth between two (convex) objects
 * the closest point is on the second object (B), and the normal points from the surface on B towards A.
 * distance is between closest points on B and closest point on A. So you can calculate closest point on A
 * by taking <code>closestPointInA = closestPointInB + distance * normalOnSurfaceB</code>.
 * 
 * @author jezek2
 */
public abstract class DiscreteCollisionDetectorInterface {

	public static abstract class Result {
		
		public abstract void setShapeIdentifiers(int partId0, int index0, int partId1, int index1);

		protected abstract void addContactPoint(v3 normalOnBInWorld, v3 pointInWorld, float depth, float breakingThresh);
	}
	
	public static class ClosestPointInput {
		public final Transform transformA = new Transform();
		public final Transform transformB = new Transform();
		public float maximumDistanceSquared;
		

		public ClosestPointInput() {
			init();
		}
		
		public void init() {
			maximumDistanceSquared = Float.MAX_VALUE;
		}
	}

	/**
	 * Give either closest points (distance > 0) or penetration (distance)
	 * the normal always points from B towards A.
	 */
	public final void getClosestPoints(ClosestPointInput input,Result output) {
		getClosestPoints(input, output, false);
	}
	
	/**
	 * Give either closest points (distance > 0) or penetration (distance)
	 * the normal always points from B towards A.
	 */
	protected abstract void getClosestPoints(ClosestPointInput input, Result output, boolean swapResults);
	
}
