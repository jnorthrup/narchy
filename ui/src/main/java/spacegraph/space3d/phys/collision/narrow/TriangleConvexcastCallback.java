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

import jcog.math.v3;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.shape.ConvexShape;
import spacegraph.space3d.phys.shape.TriangleCallback;
import spacegraph.space3d.phys.shape.TriangleShape;

/**
 *
 * @author jezek2
 */
public abstract class TriangleConvexcastCallback extends TriangleCallback {

	private final ConvexShape convexShape;
	private final Transform convexShapeFrom = new Transform();
	private final Transform convexShapeTo = new Transform();
	private final Transform triangleToWorld = new Transform();
	public float hitFraction;
	private final float triangleCollisionMargin;

	protected TriangleConvexcastCallback(ConvexShape convexShape, Transform convexShapeFrom, Transform convexShapeTo, Transform triangleToWorld, float triangleCollisionMargin) {
		this.convexShape = convexShape;
		this.convexShapeFrom.set(convexShapeFrom);
		this.convexShapeTo.set(convexShapeTo);
		this.triangleToWorld.set(triangleToWorld);
		this.hitFraction = 1f;
		this.triangleCollisionMargin = triangleCollisionMargin;
	}
	
	@Override
	public void processTriangle(v3[] triangle, int partId, int triangleIndex) {
		TriangleShape triangleShape = new TriangleShape(triangle[0], triangle[1], triangle[2]);
		triangleShape.setMargin(triangleCollisionMargin);

		VoronoiSimplexSolver simplexSolver = new VoronoiSimplexSolver();
		GjkEpaPenetrationDepthSolver gjkEpaPenetrationSolver = new GjkEpaPenetrationDepthSolver();

		
		
		
		
		SubsimplexConvexCast convexCaster = new SubsimplexConvexCast(convexShape, triangleShape, simplexSolver);
		
		
		
		

		ConvexCast.CastResult castResult = new ConvexCast.CastResult();
		castResult.fraction = 1f;
		if (convexCaster.calcTimeOfImpact(convexShapeFrom, convexShapeTo, triangleToWorld, triangleToWorld, castResult)) {
			
			if (castResult.normal.lengthSquared() > 0.0001f) {
				if (castResult.fraction < hitFraction) {

					/* btContinuousConvexCast's normal is already in world space */
					/*
					
					
					convexShapeFrom.basis.transform(castResult.normal);
					
					*/
					castResult.normal.normalize();

					reportHit(castResult.normal,
							castResult.hitPoint,
							castResult.fraction,
							partId,
							triangleIndex);
				}
			}
		}
	}

	protected abstract float reportHit(v3 hitNormalLocal, v3 hitPointLocal, float hitFraction, int partId, int triangleIndex);
	
}
