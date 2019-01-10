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


import spacegraph.space3d.phys.math.VectorUtil;
import jcog.math.v3;

/**
 * VoronoiSimplexSolver is an implementation of the closest point distance algorithm
 * from a 1-4 points simplex to the origin. Can be used with GJK, as an alternative
 * to Johnson distance algorithm.
 * 
 * @author jezek2
 */
public class VoronoiSimplexSolver extends SimplexSolverInterface {

	private static final int VORONOI_SIMPLEX_MAX_VERTS = 5;
	
	private static final int VERTA = 0;
	private static final int VERTB = 1;
	private static final int VERTC = 2;
	private static final int VERTD = 3;

	private int numVertices;

	private final v3[] simplexVectorW = new v3[VORONOI_SIMPLEX_MAX_VERTS];
	private final v3[] simplexPointsP = new v3[VORONOI_SIMPLEX_MAX_VERTS];
	private final v3[] simplexPointsQ = new v3[VORONOI_SIMPLEX_MAX_VERTS];

	private final v3 cachedP1 = new v3();
	private final v3 cachedP2 = new v3();
	private final v3 cachedV = new v3();
	private final v3 lastW = new v3();
	private boolean cachedValidClosest;

	private final SubSimplexClosestResult cachedBC = new SubSimplexClosestResult();



	/** TODO there are many instances where instantiating this can be replaced by a reusable instance, but call reset() before use */
	@Deprecated public VoronoiSimplexSolver() {

	}

	private boolean needsUpdate;
	
	{
		for (int i=0; i<VORONOI_SIMPLEX_MAX_VERTS; i++) {
			simplexVectorW[i] = new v3();
			simplexPointsP[i] = new v3();
			simplexPointsQ[i] = new v3();
		}
	}

	private void removeVertex(int index) {
		assert(numVertices>0);
		numVertices--;
		simplexVectorW[index].set(simplexVectorW[numVertices]);
		simplexPointsP[index].set(simplexPointsP[numVertices]);
		simplexPointsQ[index].set(simplexPointsQ[numVertices]);
	}
	
	private void	reduceVertices(UsageBitfield usedVerts) {
		if ((numVertices >= 4) && (!usedVerts.usedVertexD))
			removeVertex(3);

		if ((numVertices >= 3) && (!usedVerts.usedVertexC))
			removeVertex(2);

		if ((numVertices >= 2) && (!usedVerts.usedVertexB))
			removeVertex(1);

		if ((numVertices >= 1) && (!usedVerts.usedVertexA))
			removeVertex(0);
	}
	

	private boolean updateClosestVectorAndPoints() {
		if (needsUpdate)
		{
			cachedBC.reset();

			needsUpdate = false;

			switch (numVertices())
			{
			case 0:
					cachedValidClosest = false;
					break;
			case 1:
                cachedP1.set(simplexPointsP[0]);
                cachedP2.set(simplexPointsQ[0]);
                cachedV.sub(cachedP1, cachedP2); 
                cachedBC.reset();
                cachedBC.setBarycentricCoordinates(1f, 0f, 0f, 0f);
                cachedValidClosest = cachedBC.isValid();
                break;
                case 2:
                v3 tmp = new v3();

                
                v3 from = simplexVectorW[0];
                v3 to = simplexVectorW[1];
                v3 nearest = new v3();


                v3 diff = new v3();
                diff.setNegative(from);

                v3 v = new v3();
                v.sub(to, from);

                float t = v.dot(diff);

                if (t > 0) {
                    float dotVV = v.dot(v);
                    if (t < dotVV) {
                        t /= dotVV;
                        tmp.scale(t, v);
                        diff.sub(tmp);
                        cachedBC.usedVertexA = true;
                        cachedBC.usedVertexB = true;
                    } else {
                        t = 1;
                        diff.sub(v);
                        
                        cachedBC.usedVertexB = true;
                    }
                } else
                {
                    t = 0;
                    
                    cachedBC.usedVertexA = true;
                }
                cachedBC.setBarycentricCoordinates(1f-t, t, 0f, 0f);

                tmp.scale(t, v);
                nearest.add(from, tmp);

                tmp.sub(simplexPointsP[1], simplexPointsP[0]);
                tmp.scale(t);
                cachedP1.add(simplexPointsP[0], tmp);

                tmp.sub(simplexPointsQ[1], simplexPointsQ[0]);
                tmp.scale(t);
                cachedP2.add(simplexPointsQ[0], tmp);

                cachedV.sub(cachedP1, cachedP2);

                reduceVertices(cachedBC);

                cachedValidClosest = cachedBC.isValid();
                break;
                case 3:
				{ 
					v3 tmp1 = new v3();
					v3 tmp2 = new v3();
					v3 tmp3 = new v3();

					
					v3 p = new v3();
					p.set(0f, 0f, 0f);

					v3 a = simplexVectorW[0];
					v3 b = simplexVectorW[1];
					v3 c = simplexVectorW[2];

					closestPtPointTriangle(p,a,b,c,cachedBC);

					tmp1.scale(cachedBC.barycentricCoords[0], simplexPointsP[0]);
					tmp2.scale(cachedBC.barycentricCoords[1], simplexPointsP[1]);
					tmp3.scale(cachedBC.barycentricCoords[2], simplexPointsP[2]);
					VectorUtil.add(cachedP1, tmp1, tmp2, tmp3);

					tmp1.scale(cachedBC.barycentricCoords[0], simplexPointsQ[0]);
					tmp2.scale(cachedBC.barycentricCoords[1], simplexPointsQ[1]);
					tmp3.scale(cachedBC.barycentricCoords[2], simplexPointsQ[2]);
					VectorUtil.add(cachedP2, tmp1, tmp2, tmp3);

					cachedV.sub(cachedP1, cachedP2);

					reduceVertices(cachedBC);
					cachedValidClosest = cachedBC.isValid(); 

					break; 
				}
			case 4:
				v3 tmp1 = new v3();
				v3 tmp2 = new v3();
				v3 tmp3 = new v3();
				v3 tmp4 = new v3();

				v3 p = new v3();
				p.set(0f, 0f, 0f);

				v3 a = simplexVectorW[0];
				v3 b = simplexVectorW[1];
				v3 c = simplexVectorW[2];
				v3 d = simplexVectorW[3];

				boolean hasSeperation = closestPtPointTetrahedron(p,a,b,c,d,cachedBC);

				if (hasSeperation)
                {
                    tmp1.scale(cachedBC.barycentricCoords[0], simplexPointsP[0]);
                    tmp2.scale(cachedBC.barycentricCoords[1], simplexPointsP[1]);
                    tmp3.scale(cachedBC.barycentricCoords[2], simplexPointsP[2]);
                    tmp4.scale(cachedBC.barycentricCoords[3], simplexPointsP[3]);
                    VectorUtil.add(cachedP1, tmp1, tmp2, tmp3, tmp4);

                    tmp1.scale(cachedBC.barycentricCoords[0], simplexPointsQ[0]);
                    tmp2.scale(cachedBC.barycentricCoords[1], simplexPointsQ[1]);
                    tmp3.scale(cachedBC.barycentricCoords[2], simplexPointsQ[2]);
                    tmp4.scale(cachedBC.barycentricCoords[3], simplexPointsQ[3]);
                    VectorUtil.add(cachedP2, tmp1, tmp2, tmp3, tmp4);

                    cachedV.sub(cachedP1, cachedP2);
                    reduceVertices (cachedBC);
                } else
                {


                    if (cachedBC.degenerate)
                    {
                        cachedValidClosest = false;
                    } else
                    {
                        cachedValidClosest = true;
                        
                        cachedV.set(0f, 0f, 0f);
                    }
                    break;
                }

				cachedValidClosest = cachedBC.isValid();

				
				break;
				default:
				cachedValidClosest = false;
			}
		}

		return cachedValidClosest;
	}


	private static boolean closestPtPointTriangle(v3 p, v3 a, v3 b, v3 c, SubSimplexClosestResult result) {
		result.reset();

		
		v3 ab = new v3();
		ab.sub(b, a);

		v3 ac = new v3();
		ac.sub(c, a);

		v3 ap = new v3();
		ap.sub(p, a);

		float d1 = ab.dot(ap);
		float d2 = ac.dot(ap);

		if (d1 <= 0f && d2 <= 0f) 
		{
			result.closestPointOnSimplex.set(a);
			result.usedVertexA = true;
			result.setBarycentricCoordinates(1f, 0f, 0f, 0f);
			return true; 
		}

		
		v3 bp = new v3();
		bp.sub(p, b);

		float d3 = ab.dot(bp);
		float d4 = ac.dot(bp);

		if (d3 >= 0f && d4 <= d3) 
		{
			result.closestPointOnSimplex.set(b);
			result.usedVertexB = true;
			result.setBarycentricCoordinates(0, 1f, 0f, 0f);

			return true; 
		}

		
		float vc = d1*d4 - d3*d2;
		if (vc <= 0f && d1 >= 0f && d3 <= 0f) {
			float v = d1 / (d1 - d3);
			result.closestPointOnSimplex.scaleAdd(v, ab, a);
			result.usedVertexA = true;
			result.usedVertexB = true;
			result.setBarycentricCoordinates(1f-v, v, 0f, 0f);
			return true;
			
		}

		
		v3 cp = new v3();
		cp.sub(p, c);

		float d5 = ab.dot(cp);
		float d6 = ac.dot(cp);

		if (d6 >= 0f && d5 <= d6) 
		{
			result.closestPointOnSimplex.set(c);
			result.usedVertexC = true;
			result.setBarycentricCoordinates(0f, 0f, 1f, 0f);
			return true;
		}

		
		float vb = d5*d2 - d1*d6;
		if (vb <= 0f && d2 >= 0f && d6 <= 0f) {
			float w = d2 / (d2 - d6);
			result.closestPointOnSimplex.scaleAdd(w, ac, a);
			result.usedVertexA = true;
			result.usedVertexC = true;
			result.setBarycentricCoordinates(1f-w, 0f, w, 0f);
			return true;
			
		}

		
		float va = d3*d6 - d5*d4;
		if (va <= 0f && (d4 - d3) >= 0f && (d5 - d6) >= 0f) {

			v3 tmp = new v3();
			tmp.sub(c, b);
			float w = (d4 - d3) / ((d4 - d3) + (d5 - d6));
			result.closestPointOnSimplex.scaleAdd(w, tmp, b);

			result.usedVertexB = true;
			result.usedVertexC = true;
			result.setBarycentricCoordinates(0, 1f-w, w, 0f);
			return true;		
		   
		}

		
		float denom = 1f / (va + vb + vc);
		float v = vb * denom;

		v3 tmp1 = new v3();
		v3 tmp2 = new v3();

		tmp1.scale(v, ab);
		float w = vc * denom;
		tmp2.scale(w, ac);
		VectorUtil.add(result.closestPointOnSimplex, a, tmp1, tmp2);
		result.usedVertexA = true;
		result.usedVertexB = true;
		result.usedVertexC = true;
		result.setBarycentricCoordinates(1f-v-w, v, w, 0f);

		return true;
		
	}
	
	

	private static int pointOutsideOfPlane(v3 p, v3 a, v3 b, v3 c, v3 d)
	{
		v3 tmp = new v3();

		v3 normal = new v3();
		normal.sub(b, a);
		tmp.sub(c, a);
		normal.cross(normal, tmp);

		tmp.sub(p, a);
		float signp = tmp.dot(normal); 

		tmp.sub(d, a);
		float signd = tmp.dot(normal); 

	






		if (signd * signd < ((1e-4f) * (1e-4f)))
		{
	
			return -1;
		}
	

	
		
		return (signp * signd < 0f)? 1 : 0;
	}
	

	private static boolean closestPtPointTetrahedron(v3 p, v3 a, v3 b, v3 c, v3 d, SubSimplexClosestResult finalResult) {
		SubSimplexClosestResult tempResult = new SubSimplexClosestResult();
		tempResult.reset();

		v3 tmp = new v3();
		v3 q = new v3();

		
		finalResult.closestPointOnSimplex.set(p);
		finalResult.reset(true);

		int pointOutsideABC = pointOutsideOfPlane(p, a, b, c, d);
		int pointOutsideACD = pointOutsideOfPlane(p, a, c, d, b);
		int pointOutsideADB = pointOutsideOfPlane(p, a, d, b, c);
		int pointOutsideBDC = pointOutsideOfPlane(p, b, d, c, a);

		if (pointOutsideABC < 0 || pointOutsideACD < 0 || pointOutsideADB < 0 || pointOutsideBDC < 0) {
			finalResult.degenerate = true;
			return false;
		}

		if (pointOutsideABC == 0 && pointOutsideACD == 0 && pointOutsideADB == 0 && pointOutsideBDC == 0) {
			return false;
		}


		float bestSqDist = Float.MAX_VALUE;
		
		if (pointOutsideABC != 0) {
			closestPtPointTriangle(p, a, b, c, tempResult);
			q.set(tempResult.closestPointOnSimplex);

			tmp.sub(q, p);
			float sqDist = tmp.dot(tmp);
			
			if (sqDist < bestSqDist) {
				bestSqDist = sqDist;
				finalResult.closestPointOnSimplex.set(q);
				
				finalResult.reset();
				finalResult.usedVertexA = tempResult.usedVertexA;
				finalResult.usedVertexB = tempResult.usedVertexB;
				finalResult.usedVertexC = tempResult.usedVertexC;
				finalResult.setBarycentricCoordinates(
						tempResult.barycentricCoords[VERTA],
						tempResult.barycentricCoords[VERTB],
						tempResult.barycentricCoords[VERTC],
						0
				);

			}
		}


		
		if (pointOutsideACD != 0) {
			closestPtPointTriangle(p, a, c, d, tempResult);
			q.set(tempResult.closestPointOnSimplex);
			

			tmp.sub(q, p);
			float sqDist = tmp.dot(tmp);
			if (sqDist < bestSqDist) {
				bestSqDist = sqDist;
				finalResult.closestPointOnSimplex.set(q);
				finalResult.reset();
				finalResult.usedVertexA = tempResult.usedVertexA;

				finalResult.usedVertexC = tempResult.usedVertexB;
				finalResult.usedVertexD = tempResult.usedVertexC;
				finalResult.setBarycentricCoordinates(
						tempResult.barycentricCoords[VERTA],
						0,
						tempResult.barycentricCoords[VERTB],
						tempResult.barycentricCoords[VERTC]
				);

			}
		}
		


		if (pointOutsideADB != 0) {
			closestPtPointTriangle(p, a, d, b, tempResult);
			q.set(tempResult.closestPointOnSimplex);
			

			tmp.sub(q, p);
			float sqDist = tmp.dot(tmp);
			if (sqDist < bestSqDist) {
				bestSqDist = sqDist;
				finalResult.closestPointOnSimplex.set(q);
				finalResult.reset();
				finalResult.usedVertexA = tempResult.usedVertexA;
				finalResult.usedVertexB = tempResult.usedVertexC;

				finalResult.usedVertexD = tempResult.usedVertexB;
				finalResult.setBarycentricCoordinates(
						tempResult.barycentricCoords[VERTA],
						tempResult.barycentricCoords[VERTC],
						0,
						tempResult.barycentricCoords[VERTB]
				);

			}
		}
		


		if (pointOutsideBDC != 0) {
			closestPtPointTriangle(p, b, d, c, tempResult);
			q.set(tempResult.closestPointOnSimplex);
			
			tmp.sub(q, p);
			float sqDist = tmp.dot(tmp);
			if (sqDist < bestSqDist) {
				
				finalResult.closestPointOnSimplex.set(q);
				finalResult.reset();
				
				finalResult.usedVertexB = tempResult.usedVertexA;
				finalResult.usedVertexC = tempResult.usedVertexC;
				finalResult.usedVertexD = tempResult.usedVertexB;

				finalResult.setBarycentricCoordinates(
						0,
						tempResult.barycentricCoords[VERTA],
						tempResult.barycentricCoords[VERTC],
						tempResult.barycentricCoords[VERTB]
				);

			}
		}

		









		return true;
	}

	
	/**
	 * Clear the simplex, remove all the vertices.
	 */
	@Override
	public void reset() {
		cachedValidClosest = false;
		numVertices = 0;
		needsUpdate = true;
		lastW.set(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
		cachedBC.reset();
	}

	@Override
	public void addVertex(v3 w, v3 p, v3 q) {
		lastW.set(w);
		needsUpdate = true;

		int nv = this.numVertices;
		simplexVectorW[nv].set(w);
		simplexPointsP[nv].set(p);
		simplexPointsQ[nv].set(q);

		this.numVertices++;
	}

	/**
	 * Return/calculate the closest vertex.
	 */
	@Override
	public boolean closest(v3 v) {
		boolean succes = updateClosestVectorAndPoints();
		v.set(cachedV);
		return succes;
	}

	@Override
	public float maxVertex() {
		int numverts = numVertices();
		float maxV = 0f;
		v3[] vv = this.simplexVectorW;
		for (int i = 0; i < numverts; i++) {
			float curLen2 = vv[i].lengthSquared();
			if (maxV < curLen2) {
				maxV = curLen2;
			}
		}
		return maxV;
	}

	@Override
	public boolean fullSimplex() {
		return (numVertices == 4);
	}

	@Override
	public int getSimplex(v3[] pBuf, v3[] qBuf, v3[] yBuf) {
		for (int i = 0; i < numVertices(); i++) {
			yBuf[i].set(simplexVectorW[i]);
			pBuf[i].set(simplexPointsP[i]);
			qBuf[i].set(simplexPointsQ[i]);
		}
		return numVertices();
	}

	@Override
	public boolean inSimplex(v3 w) {
		boolean found = false;
		int numverts = numVertices();
		

		
		for (int i = 0; i < numverts; i++) {
			if (simplexVectorW[i].equals(w)) {
				found = true;
			}
		}

		
		if (w.equals(lastW)) {
			return true;
		}

		return found;
	}

	@Override
	public void backup_closest(v3 v) {
		v.set(cachedV);
	}

	@Override
	public boolean emptySimplex() {
		return (numVertices() == 0);
	}

	@Override
	public void compute_points(v3 p1, v3 p2) {
		updateClosestVectorAndPoints();
		p1.set(cachedP1);
		p2.set(cachedP2);
	}

	@Override
	public int numVertices() {
		return numVertices;
	}
	
	
	
	static class UsageBitfield {
		boolean usedVertexA;
		boolean usedVertexB;
		boolean usedVertexC;
		boolean usedVertexD;
		
		void reset() {
			usedVertexA = false;
			usedVertexB = false;
			usedVertexC = false;
			usedVertexD = false;
		}
		void reset(boolean x) {
			usedVertexA = x;
			usedVertexB = x;
			usedVertexC = x;
			usedVertexD = x;
		}
	}
	
	static class SubSimplexClosestResult extends UsageBitfield {
		final v3 closestPointOnSimplex = new v3();
		final float[] barycentricCoords = new float[4];
		boolean degenerate;
		
		void reset() {
			degenerate = false;
			setBarycentricCoordinates(0f, 0f, 0f, 0f);
			super.reset();
		}

		boolean isValid() {
			boolean valid = (barycentricCoords[0] >= 0f) &&
					(barycentricCoords[1] >= 0f) &&
					(barycentricCoords[2] >= 0f) &&
					(barycentricCoords[3] >= 0f);
			return valid;
		}

		void setBarycentricCoordinates(float a, float b, float c, float d) {
			barycentricCoords[0] = a;
			barycentricCoords[1] = b;
			barycentricCoords[2] = c;
			barycentricCoords[3] = d;
		}
	}
	
}
