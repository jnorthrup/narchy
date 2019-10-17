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
import spacegraph.space3d.phys.math.VectorUtil;
import spacegraph.space3d.phys.shape.ConvexShape;

/**
 * GjkConvexCast performs a raycast on a convex object using support mapping.
 * 
 * @author jezek2
 */
public class GjkConvexCast extends ConvexCast {




	private static final int MAX_ITERATIONS = 32;

	
	private final SimplexSolverInterface simplexSolver;
	private final ConvexShape convexA;
	private final ConvexShape convexB;
	
	private final GjkPairDetector gjk = new GjkPairDetector();

	public GjkConvexCast(ConvexShape convexA, ConvexShape convexB, SimplexSolverInterface simplexSolver) {
		this.simplexSolver = simplexSolver;
		this.convexA = convexA;
		this.convexB = convexB;
	}
	
	@Override
	public boolean calcTimeOfImpact(Transform fromA, Transform toA, Transform fromB, Transform toB, CastResult result) {
		simplexSolver.reset();

		
		
		v3 linVelA = new v3();
		v3 linVelB = new v3();

		linVelA.sub(toA, fromA);
		linVelB.sub(toB, fromB);

		float radius = 0.001f;
		float lambda = 0f;
		v3 v = new v3();
		v.set(1f, 0f, 0f);

		int maxIter = MAX_ITERATIONS;

		v3 n = new v3();
		n.set(0f, 0f, 0f);
        v3 c = new v3();
		v3 r = new v3();
		r.sub(linVelA, linVelB);

		float lastLambda = lambda;
		

		int numIter = 0;
		

		Transform identityTrans = new Transform();
		identityTrans.setIdentity();

		

		PointCollector pointCollector = new PointCollector();

		gjk.init(convexA, convexB, simplexSolver, null); 
		DiscreteCollisionDetectorInterface.ClosestPointInput input = new DiscreteCollisionDetectorInterface.ClosestPointInput();
		input.init();
		
		

		input.transformA.set(fromA);
		input.transformB.set(fromB);
		gjk.getClosestPoints(input, pointCollector);

        boolean hasResult = pointCollector.hasResult;
		c.set(pointCollector.pointInWorld);

		if (hasResult) {
            float dist = pointCollector.distance;
            n.set(pointCollector.normalOnBInWorld);

            
            while (dist > radius) {
                numIter++;
                if (numIter > maxIter) {
                    return false; 
                }

                float projectedLinearVelocity = r.dot(n);

                float dLambda = dist / (projectedLinearVelocity);

                lambda = lambda - dLambda;

                if (lambda > 1f) {
                    return false;
                }
                if (lambda < 0f) {
                    return false;					
                }

                if (lambda <= lastLambda) {
                    return false;
                
                
                }
                lastLambda = lambda;

                
                result.debugDraw(lambda);
                VectorUtil.lerp(input.transformA, fromA, toA, lambda);
                VectorUtil.lerp(input.transformB, fromB, toB, lambda);

                gjk.getClosestPoints(input, pointCollector);
                if (pointCollector.hasResult) {
                    if (pointCollector.distance < 0f) {
                        result.fraction = lastLambda;
                        n.set(pointCollector.normalOnBInWorld);
                        result.normal.set(n);
                        result.hitPoint.set(pointCollector.pointInWorld);
                        return true;
                    }
                    c.set(pointCollector.pointInWorld);
                    n.set(pointCollector.normalOnBInWorld);
                    dist = pointCollector.distance;
                }
                else {
                    
                    return false;
                }

            }

            
            
            if (n.dot(r) >= -result.allowedPenetration) {
                return false;
            }
            result.fraction = lambda;
            result.normal.set(n);
            result.hitPoint.set(c);
            return true;
        }

		return false;
	}
	
}
