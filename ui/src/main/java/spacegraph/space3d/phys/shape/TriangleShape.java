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
import spacegraph.space3d.phys.collision.broad.BroadphaseNativeType;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.math.VectorUtil;

/**
 * Single triangle shape.
 * 
 * @author jezek2
 */
public class TriangleShape extends PolyhedralConvexShape {
	
	public final v3[] vertices1/*[3]*/ = { new v3(), new v3(), new v3() };

	
	public TriangleShape() {
	}
	
	public TriangleShape(v3 p0, v3 p1, v3 p2) {
		vertices1[0].set(p0);
		vertices1[1].set(p1);
		vertices1[2].set(p2);
	}
	
	
	public void init(v3 p0, v3 p1, v3 p2) {
		vertices1[0].set(p0);
		vertices1[1].set(p1);
		vertices1[2].set(p2);
	}

	@Override
	public int getNumVertices() {
		return 3;
	}

	public v3 getVertexPtr(int index) {
		return vertices1[index];
	}
	
	@Override
	public void getVertex(int index, v3 vert) {
		vert.set(vertices1[index]);
	}

	@Override
	public BroadphaseNativeType getShapeType() {
		return BroadphaseNativeType.TRIANGLE_SHAPE_PROXYTYPE;
	}

	@Override
	public int getNumEdges() {
		return 3;
	}

	@Override
	public void getEdge(int i, v3 pa, v3 pb) {
		getVertex(i, pa);
		getVertex((i + 1) % 3, pb);
	}

	@Override
	public void getAabb(Transform t, v3 aabbMin, v3 aabbMax) {

		getAabbSlow(t, aabbMin, aabbMax);
	}

	@Override
	public v3 localGetSupportingVertexWithoutMargin(v3 dir, v3 out) {
		v3 dots = new v3();
		dots.set(dir.dot(vertices1[0]), dir.dot(vertices1[1]), dir.dot(vertices1[2]));
		out.set(vertices1[VectorUtil.maxAxis(dots)]);
		return out;
	}

	@Override
	public void batchedUnitVectorGetSupportingVertexWithoutMargin(v3[] vectors, v3[] supportVerticesOut, int numVectors) {
		v3 dots = new v3();

		for (int i = 0; i < numVectors; i++) {
			v3 dir = vectors[i];
			dots.set(dir.dot(vertices1[0]), dir.dot(vertices1[1]), dir.dot(vertices1[2]));
			supportVerticesOut[i].set(vertices1[VectorUtil.maxAxis(dots)]);
		}
	}

	@Override
	public void getPlane(v3 planeNormal, v3 planeSupport, int i) {
		getPlaneEquation(i,planeNormal,planeSupport);
	}

	@Override
	public int getNumPlanes() {
		return 1;
	}

	private void calcNormal(v3 normal) {
		v3 tmp1 = new v3();
		v3 tmp2 = new v3();

		tmp1.sub(vertices1[1], vertices1[0]);
		tmp2.sub(vertices1[2], vertices1[0]);

		normal.cross(tmp1, tmp2);
		normal.normalize();
	}

	private void getPlaneEquation(int i, v3 planeNormal, v3 planeSupport) {
		calcNormal(planeNormal);
		planeSupport.set(vertices1[0]);
	}

	@Override
	public void calculateLocalInertia(float mass, v3 inertia) {
		assert (false);
		inertia.set(0f, 0f, 0f);
	}
	
	@Override
	public boolean isInside(v3 pt, float tolerance) {
		v3 normal = new v3();
		calcNormal(normal);
		
		float dist = pt.dot(normal);
		float planeconst = vertices1[0].dot(normal);
		dist -= planeconst;
		if (dist >= -tolerance && dist <= tolerance) {
			
			int i;
			for (i = 0; i < 3; i++) {
				v3 pa = new v3(), pb = new v3();
				getEdge(i, pa, pb);
				v3 edge = new v3();
				edge.sub(pb, pa);
				v3 edgeNormal = new v3();
				edgeNormal.cross(edge, normal);
				edgeNormal.normalize();
				/*float*/ dist = pt.dot(edgeNormal);
				float edgeConst = pa.dot(edgeNormal);
				dist -= edgeConst;
				if (dist < -tolerance) {
					return false;
				}
			}

			return true;
		}

		return false;
	}

	@Override
	public String getName() {
		return "Triangle";
	}

	@Override
	public int getNumPreferredPenetrationDirections() {
		return 2;
	}

	@Override
	public void getPreferredPenetrationDirection(int index, v3 penetrationVector) {
		calcNormal(penetrationVector);
		if (index != 0) {
			penetrationVector.scaled(-1f);
		}
	}

}
