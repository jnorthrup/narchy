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
import spacegraph.space3d.phys.BulletStats;
import spacegraph.space3d.phys.math.MatrixUtil;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.shape.ConvexShape;

/**
 * GjkPairDetector uses GJK to implement the {@link DiscreteCollisionDetectorInterface}.
 *
 * @author jezek2
 */
public class GjkPairDetector extends DiscreteCollisionDetectorInterface {

	

	
	private static final float REL_ERROR2 =
           1.0e-4f;
            

	private final v3 cachedSeparatingAxis = new v3();
	private ConvexPenetrationDepthSolver penetrationDepthSolver;
	private SimplexSolverInterface simplexSolver;
	private ConvexShape minkowskiA;
	private ConvexShape minkowskiB;
	private boolean ignoreMargin;

	
	private int lastUsedMethod;
    private int catchDegeneracies;

	public void init(ConvexShape objectA, ConvexShape objectB, SimplexSolverInterface simplexSolver, ConvexPenetrationDepthSolver penetrationDepthSolver) {
		this.cachedSeparatingAxis.set(0f, 0f, 1f);
		this.ignoreMargin = false;
		this.lastUsedMethod = -1;
		this.catchDegeneracies = 1;
		
		this.penetrationDepthSolver = penetrationDepthSolver;
		this.simplexSolver = simplexSolver;
		this.minkowskiA = objectA;
		this.minkowskiB = objectB;
	}

	@Override
    public void getClosestPoints(ClosestPointInput input, Result output, boolean swapResults) {
        v3 tmp = new v3();

        v3 normalInB = new v3();
		normalInB.set(0f, 0f, 0f);
		v3 pointOnA = new v3(), pointOnB = new v3();
        Transform localTransA = new Transform(input.transformA);
        Transform localTransB = new Transform(input.transformB);
        v3 positionOffset = new v3();
        positionOffset.add(localTransA, localTransB);
		positionOffset.scaled(0.5f);
		localTransA.sub(positionOffset);
		localTransB.sub(positionOffset);

        float marginA = minkowskiA.getMargin();
        float marginB = minkowskiB.getMargin();

		BulletStats.gNumGjkChecks++;

		
		if (ignoreMargin) {
			marginA = 0f;
			marginB = 0f;
		}

        cachedSeparatingAxis.set(0f, 1f, 0f);

        lastUsedMethod = -1;

        float margin = marginA + marginB;

        simplexSolver.reset();

        v3 seperatingAxisInA = new v3();
        v3 seperatingAxisInB = new v3();

        v3 pInA = new v3();
        v3 qInB = new v3();

        v3 pWorld = new v3();
        v3 qWorld = new v3();
        v3 w = new v3();

        v3 tmpPointOnA = new v3(), tmpPointOnB = new v3();
        v3 tmpNormalInB = new v3();

        float squaredDistance = BulletGlobals.SIMD_INFINITY;
        int degenerateSimplex = 0;
        boolean checkPenetration = true;
        boolean checkSimplex = false;
        int gGjkMaxIter = 1000;
        int curIter = 0;
        for (float delta = 0f; ;)
        {
            seperatingAxisInA.negated(cachedSeparatingAxis);
            MatrixUtil.transposeTransform(seperatingAxisInA, seperatingAxisInA, input.transformA.basis);

            seperatingAxisInB.set(cachedSeparatingAxis);
            MatrixUtil.transposeTransform(seperatingAxisInB, seperatingAxisInB, input.transformB.basis);

            minkowskiA.localGetSupportingVertexWithoutMargin(seperatingAxisInA, pInA);
            minkowskiB.localGetSupportingVertexWithoutMargin(seperatingAxisInB, qInB);

            pWorld.set(pInA);
            localTransA.transform(pWorld);

            qWorld.set(qInB);
            localTransB.transform(qWorld);

            w.sub(pWorld, qWorld);

            delta = cachedSeparatingAxis.dot(w);

            
            if ((delta > 0f) && (delta * delta > squaredDistance * input.maximumDistanceSquared)) {
                checkPenetration = false;
                break;
            }

            
            if (simplexSolver.inSimplex(w )) {
                degenerateSimplex = 1;
                checkSimplex = true;
                break;
            }

            float f0 = squaredDistance - delta;
            float f1 = squaredDistance * REL_ERROR2;

            if (f0 <= f1) {
                if (f0 <= 0f) {
                    degenerateSimplex = 2;
                }
                checkSimplex = true;
                break;
            }
            
            simplexSolver.addVertex(w, pWorld, qWorld);

            
            if (!simplexSolver.closest(cachedSeparatingAxis)) {
                degenerateSimplex = 3;
                checkSimplex = true;
                break;
            }

            if (cachedSeparatingAxis.lengthSquared() < REL_ERROR2) {
                degenerateSimplex = 6;
                checkSimplex = true;
                break;
            }

            float previousSquaredDistance = squaredDistance;
            squaredDistance = cachedSeparatingAxis.lengthSquared();

            

            
            if (previousSquaredDistance - squaredDistance <= BulletGlobals.FLT_EPSILON * previousSquaredDistance) {
                simplexSolver.backup_closest(cachedSeparatingAxis);
                checkSimplex = true;
                break;
            }

            
            if (curIter++ > gGjkMaxIter) {
                
                if (BulletGlobals.DEBUG) {
                    System.err.printf("btGjkPairDetector maxIter exceeded:%i\n", curIter);
                    System.err.printf("sepAxis=(%f,%f,%f), squaredDistance = %f, shapeTypeA=%i,shapeTypeB=%i\n",
                            cachedSeparatingAxis.x,
                            cachedSeparatingAxis.y,
                            cachedSeparatingAxis.z,
                            squaredDistance,
                            minkowskiA.getShapeType().ordinal(),
                            minkowskiB.getShapeType().ordinal());
                }
                
                break;

            }

            boolean check = (!simplexSolver.fullSimplex());
            

            if (!check) {
                
                simplexSolver.backup_closest(cachedSeparatingAxis);
                break;
            }
        }

        boolean isValid = false;
        float distance = 0f;
        if (checkSimplex) {
            simplexSolver.compute_points(pointOnA, pointOnB);
            normalInB.sub(pointOnA, pointOnB);
            float lenSqr = cachedSeparatingAxis.lengthSquared();
            
            if (lenSqr < 0.0001f) {
                degenerateSimplex = 5;
            }
            if (lenSqr > BulletGlobals.FLT_EPSILON * BulletGlobals.FLT_EPSILON) {
                float rlen = 1f / (float) Math.sqrt(lenSqr);
                normalInB.scaled(rlen);
                float s = (float) Math.sqrt(squaredDistance);

                assert (s > 0f);

                tmp.scale((marginA / s), cachedSeparatingAxis);
                pointOnA.sub(tmp);

                tmp.scale((marginB / s), cachedSeparatingAxis);
                pointOnB.add(tmp);

                distance = ((1f / rlen) - margin);
                isValid = true;

                lastUsedMethod = 1;
            }
            else {
                lastUsedMethod = 2;
            }
        }

        boolean catchDegeneratePenetrationCase =
                (catchDegeneracies != 0 && penetrationDepthSolver != null && degenerateSimplex != 0 && ((distance + margin) < 0.01f));

        
        if (checkPenetration && (!isValid || catchDegeneratePenetrationCase)) {
            

            
            if (penetrationDepthSolver != null) {
                
                BulletStats.gNumDeepPenetrationChecks++;

                boolean isValid2 = penetrationDepthSolver.calcPenDepth(
                        simplexSolver,
                        minkowskiA, minkowskiB,
                        localTransA, localTransB,
                        cachedSeparatingAxis, tmpPointOnA, tmpPointOnB
                        /*,input.stackAlloc*/);

                if (isValid2) {
                    tmpNormalInB.sub(tmpPointOnB, tmpPointOnA);

                    float lenSqr = tmpNormalInB.lengthSquared();
                    if (lenSqr > (BulletGlobals.FLT_EPSILON * BulletGlobals.FLT_EPSILON)) {
                        tmpNormalInB.scaled(1f / (float) Math.sqrt(lenSqr));
                        tmp.sub(tmpPointOnA, tmpPointOnB);
                        float distance2 = -tmp.length();
                        
                        if (!isValid || (distance2 < distance)) {
                            distance = distance2;
                            pointOnA.set(tmpPointOnA);
                            pointOnB.set(tmpPointOnB);
                            normalInB.set(tmpNormalInB);
                            isValid = true;
                            lastUsedMethod = 3;
                        }
                        else {

                        }
                    }
                    else {
                        
                        lastUsedMethod = 4;
                    }
                }
                else {
                    lastUsedMethod = 5;
                }

            }
        }

        if (isValid) {
			
			
			

			tmp.add(pointOnB, positionOffset);
			output.addContactPoint(
					normalInB,
					tmp,
					distance, BulletGlobals.the.get().getContactBreakingThreshold());
		
		}
	}

	public void setMinkowskiA(ConvexShape minkA) {
		minkowskiA = minkA;
	}

	public void setMinkowskiB(ConvexShape minkB) {
		minkowskiB = minkB;
	}

	public void setCachedSeperatingAxis(v3 seperatingAxis) {
		cachedSeparatingAxis.set(seperatingAxis);
	}

	public void setPenetrationDepthSolver(ConvexPenetrationDepthSolver penetrationDepthSolver) {
		this.penetrationDepthSolver = penetrationDepthSolver;
	}

	/**
	 * Don't use setIgnoreMargin, it's for Bullet's internal use.
	 */
	public void setIgnoreMargin(boolean ignoreMargin) {
		this.ignoreMargin = ignoreMargin;
	}
	
}
