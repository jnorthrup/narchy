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
import spacegraph.space3d.phys.BulletGlobals;
import spacegraph.space3d.phys.math.MatrixUtil;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.math.VectorUtil;
import spacegraph.space3d.phys.shape.ConvexShape;

/**
 * SubsimplexConvexCast implements Gino van den Bergens' paper
 * "Ray Casting against bteral Convex Objects with Application to Continuous Collision Detection"
 * GJK based Ray Cast, optimized version
 * Objects should not start in overlap, otherwise results are not defined.
 * 
 * @author jezek2
 */
public class SubsimplexConvexCast extends ConvexCast {

	
	
	
	
	
	
	
	
	
	
	private static final int MAX_ITERATIONS = 32;
	
	private final SimplexSolverInterface simplexSolver;
	private final ConvexShape convexA;
	private final ConvexShape convexB;

	public SubsimplexConvexCast(ConvexShape shapeA, ConvexShape shapeB, SimplexSolverInterface simplexSolver) {
		this.convexA = shapeA;
		this.convexB = shapeB;
		this.simplexSolver = simplexSolver;
	}
	
	@Override
    public boolean calcTimeOfImpact(Transform fromA, Transform toA, Transform fromB, Transform toB, CastResult result) {
		v3 tmp = new v3();
		
		simplexSolver.reset();

		v3 linVelA = new v3();
		v3 linVelB = new v3();
		linVelA.sub(toA, fromA);
		linVelB.sub(toB, fromB);

        Transform interpolatedTransA = new Transform(fromA);
		Transform interpolatedTransB = new Transform(fromB);

		
		v3 r = new v3();
		r.sub(linVelA, linVelB);
		
		v3 v = new v3();

		tmp.negated(r);
		MatrixUtil.transposeTransform(tmp, tmp, fromA.basis);
		v3 supVertexA = convexA.localGetSupportingVertex(tmp, new v3());
		fromA.transform(supVertexA);
		
		MatrixUtil.transposeTransform(tmp, r, fromB.basis);
		v3 supVertexB = convexB.localGetSupportingVertex(tmp, new v3());
		fromB.transform(supVertexB);
		
		v.sub(supVertexA, supVertexB);

        v3 n = new v3();
		n.set(0f, 0f, 0f);
		boolean hasResult = false;
		v3 c = new v3();

        float lambda = 0f;
        float lastLambda = lambda;

		float dist2 = v.lengthSquared();
		
		
		
		float epsilon = 0.0001f;
		
		v3 w = new v3(), p = new v3();

        int maxIter = MAX_ITERATIONS;
        while ((dist2 > epsilon) && (maxIter--) != 0) {
			tmp.negated(v);
			MatrixUtil.transposeTransform(tmp, tmp, interpolatedTransA.basis);
			convexA.localGetSupportingVertex(tmp, supVertexA);
			interpolatedTransA.transform(supVertexA);
			
			MatrixUtil.transposeTransform(tmp, v, interpolatedTransB.basis);
			convexB.localGetSupportingVertex(tmp, supVertexB);
			interpolatedTransB.transform(supVertexB);
			
			w.sub(supVertexA, supVertexB);

			float VdotW = v.dot(w);

			if (lambda > 1f) {
				return false;
			}
			
			if (VdotW > 0f) {
                float VdotR = v.dot(r);

                if (VdotR >= -(BulletGlobals.FLT_EPSILON * BulletGlobals.FLT_EPSILON)) {
					return false;
				}
				else {
                    lambda -= VdotW / VdotR;
					
					
					
					VectorUtil.lerp(interpolatedTransA, fromA, toA, lambda);
					VectorUtil.lerp(interpolatedTransB, fromB, toB, lambda);
					
					
					w.sub(supVertexA, supVertexB);
					lastLambda = lambda;
					n.set(v);
					hasResult = true;
				}
			}
			simplexSolver.addVertex(w, supVertexA , supVertexB);
			if (simplexSolver.closest(v)) {
				dist2 = v.lengthSquared();
				hasResult = true;
				
				
				
				
				
			}
			else {
				dist2 = 0f;
			}
		}

		
		
	
		
		
		result.fraction = lambda;
		if (n.lengthSquared() >= BulletGlobals.SIMD_EPSILON * BulletGlobals.SIMD_EPSILON) {
			result.normal.normalize(n);
		}
		else {
			result.normal.set(0f, 0f, 0f);
		}

		
		if (result.normal.dot(r) >= -result.allowedPenetration)
			return false;

		v3 hitA = new v3();
		v3 hitB = new v3();
		simplexSolver.compute_points(hitA,hitB);
		result.hitPoint.set(hitB);
		return true;
	}

}
