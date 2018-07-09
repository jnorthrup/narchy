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

import jcog.Util;
import spacegraph.space3d.phys.Body3D;
import spacegraph.space3d.phys.Collidable;
import spacegraph.space3d.phys.collision.broad.BroadphaseNativeType;
import spacegraph.space3d.phys.math.AabbUtil2;
import spacegraph.space3d.phys.math.ScalarUtil;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.util.math.Vector4f;
import spacegraph.util.math.v3;

/**
 * BoxShape is a box primitive around the origin, its sides axis aligned with length
 * specified by half extents, in local shape coordinates. When used as part of a
 * {@link Collidable} or {@link Body3D} it will be an oriented box in world space.
 *
 * @author jezek2
 */
public class SimpleBoxShape extends PolyhedralConvexShape {

	private float radius;


	public SimpleBoxShape() {
		this(1, 1, 1);
	}

	SimpleBoxShape(float w, float h, float d) {
		super();

		implicitShapeDimensions.set(w/2, h/2, d/2);

		setMargin(0f);

		
		updateRadius();

		/*float m = getMargin();

		implicitShapeDimensions.add(-m, -m, -m);*/
	}

	public void setSize(float w, float h, float d) {
		implicitShapeDimensions.set(w/2, h/2, d/2);
		updateRadius();
	}


	@Override
	public float getBoundingRadius() {
		return radius;
	}

	@Override
	public void setLocalScaling(float x, float y, float z) {
		throw new UnsupportedOperationException();


	}

	@Override
	public void setLocalScaling(v3 scaling) {
		throw new UnsupportedOperationException();


	}

	private void updateRadius() {
		radius = Util.max(
						implicitShapeDimensions.x,
						implicitShapeDimensions.y,
						implicitShapeDimensions.z
				);
	}

	public v3 getHalfExtentsWithMargin(v3 out) {
		v3 halfExtents = getHalfExtentsWithoutMargin(out);

		float m = getMargin();
		if (m!=0) {
			halfExtents.add(m, m, m);
		}

		return halfExtents;
	}


	final v3 getHalfExtentsWithoutMargin(v3 out) {
		out.set(implicitShapeDimensions); 
		return out;
	}

	@Override
	public BroadphaseNativeType getShapeType() {
		return BroadphaseNativeType.BOX_SHAPE_PROXYTYPE;
	}

	@Override
	public v3 localGetSupportingVertex(v3 vec, v3 out) {
		v3 halfExtents = implicitShapeDimensions; 
		





		float hx = halfExtents.x;
		float hy = halfExtents.y;
		float hz = halfExtents.z;
		out.set(
				ScalarUtil.fsel(vec.x, hx, -hx),
				ScalarUtil.fsel(vec.y, hy, -hy),
				ScalarUtil.fsel(vec.z, hz, -hz));
		return out;
	}

	@Override
	public v3 localGetSupportingVertexWithoutMargin(v3 vec, v3 out) {
		
		v3 halfExtents = this.implicitShapeDimensions;
		float hx = halfExtents.x;
		float hy = halfExtents.y;
		float hz = halfExtents.z;

		out.set(
				ScalarUtil.fsel(vec.x, hx, -hx),
				ScalarUtil.fsel(vec.y, hy, -hy),
				ScalarUtil.fsel(vec.z, hz, -hz));
		return out;
	}

	@Override
	public void batchedUnitVectorGetSupportingVertexWithoutMargin(v3[] vectors, v3[] supportVerticesOut, int numVectors) {
		
		v3 halfExtents = this.implicitShapeDimensions;
		float hx = halfExtents.x;
		float hy = halfExtents.y;
		float hz = halfExtents.z;

		for (int i = 0; i < numVectors; i++) {
			v3 vec = vectors[i];
			supportVerticesOut[i].set(ScalarUtil.fsel(vec.x, hx, -hx),
					ScalarUtil.fsel(vec.y, hy, -hy),
					ScalarUtil.fsel(vec.z, hz, -hz));
		}
	}

	@Override
	public SimpleBoxShape setMargin(float margin) {
		if (margin!=0)
			throw new UnsupportedOperationException();

		return this;














	}


















	@Override
	public void getAabb(Transform t, v3 aabbMin, v3 aabbMax) {
		AabbUtil2.transformAabb(implicitShapeDimensions, 0, t, aabbMin, aabbMax);
	}

	private v3 getHalfExtentsWithoutMargin() {
		return getHalfExtentsWithoutMargin(new v3());
	}

	@Override
	public void calculateLocalInertia(float mass, v3 inertia) {

		v3 halfExtents = implicitShapeDimensions; 

		float lx2 = Util.sqr(2f * halfExtents.x);
		float ly2 = Util.sqr(2f * halfExtents.y);
		float lz2 = Util.sqr(2f * halfExtents.z);

		inertia.set(
				mass / 12f * (ly2 + lz2),
				mass / 12f * (lx2 + lz2),
				mass / 12f * (lx2 + ly2));
	}

	@Override
	public void getPlane(v3 planeNormal, v3 planeSupport, int i) {
		
		Vector4f plane = new Vector4f();
		v3 tmp = new v3(implicitShapeDimensions);
		getPlaneEquation(plane, i, tmp);
		planeNormal.set(plane.x, plane.y, plane.z);

		tmp.negate(planeNormal);
		localGetSupportingVertex(tmp, planeSupport);
	}

	@Override
	public int getNumPlanes() {
		return 6;
	}

	@Override
	public int getNumVertices() {
		return 8;
	}

	@Override
	public int getNumEdges() {
		return 12;
	}

	@Override
	public void getVertex(int i, v3 vtx) {
		v3 halfExtents = implicitShapeDimensions; 

		float hx = halfExtents.x;
		float hy = halfExtents.y;
		float hz = halfExtents.z;
		vtx.set(hx * (1 - (i & 1)) - hx * (i & 1),
				hy * (1 - ((i & 2) >> 1)) - hy * ((i & 2) >> 1),
				hz * (1 - ((i & 4) >> 2)) - hz * ((i & 4) >> 2));
	}
	
	void getPlaneEquation(Vector4f plane, int i, v3 tmp) {
		v3 halfExtents = getHalfExtentsWithoutMargin(tmp);

		switch (i) {
			case 0:
				plane.set(1f, 0f, 0f, -halfExtents.x);
				break;
			case 1:
				plane.set(-1f, 0f, 0f, -halfExtents.x);
				break;
			case 2:
				plane.set(0f, 1f, 0f, -halfExtents.y);
				break;
			case 3:
				plane.set(0f, -1f, 0f, -halfExtents.y);
				break;
			case 4:
				plane.set(0f, 0f, 1f, -halfExtents.z);
				break;
			case 5:
				plane.set(0f, 0f, -1f, -halfExtents.z);
				break;
			default:
				assert (false);
		}
	}

	@Override
	public void getEdge(int i, v3 pa, v3 pb) {
		int edgeVert0 = 0;
		int edgeVert1 = 0;

		switch (i) {
			case 0:
				edgeVert0 = 0;
				edgeVert1 = 1;
				break;
			case 1:
				edgeVert0 = 0;
				edgeVert1 = 2;
				break;
			case 2:
				edgeVert0 = 1;
				edgeVert1 = 3;

				break;
			case 3:
				edgeVert0 = 2;
				edgeVert1 = 3;
				break;
			case 4:
				edgeVert0 = 0;
				edgeVert1 = 4;
				break;
			case 5:
				edgeVert0 = 1;
				edgeVert1 = 5;

				break;
			case 6:
				edgeVert0 = 2;
				edgeVert1 = 6;
				break;
			case 7:
				edgeVert0 = 3;
				edgeVert1 = 7;
				break;
			case 8:
				edgeVert0 = 4;
				edgeVert1 = 5;
				break;
			case 9:
				edgeVert0 = 4;
				edgeVert1 = 6;
				break;
			case 10:
				edgeVert0 = 5;
				edgeVert1 = 7;
				break;
			case 11:
				edgeVert0 = 6;
				edgeVert1 = 7;
				break;
			default:
				assert (false);
		}

		getVertex(edgeVert0, pa);
		getVertex(edgeVert1, pb);
	}

	@Override
	public final boolean isInside(v3 pt, float tolerance) {
		v3 halfExtents = getHalfExtentsWithoutMargin();

		float px = pt.x;
		float hx = halfExtents.x;
		if (px <= (hx + tolerance)) {
			if (px >= (-hx - tolerance)) {
				float py = pt.y;
				float hy = halfExtents.y;
				if (py <= (hy + tolerance)) {
					if (py >= (-hy - tolerance)) {
						float pz = pt.z;
						float hz = halfExtents.z;
						if (pz <= (hz + tolerance)) {
                            return pz >= (-hz - tolerance);
						}
					}
				}
			}
		}
		return false;
	}

	@Override
	public String getName() {
		return "SimpleBox";
	}

	@Override
	public int getNumPreferredPenetrationDirections() {
		return 6;
	}

	@Override
	public void getPreferredPenetrationDirection(int index, v3 penetrationVector) {
		switch (index) {
			case 0:
				penetrationVector.set(1f, 0f, 0f);
				break;
			case 1:
				penetrationVector.set(-1f, 0f, 0f);
				break;
			case 2:
				penetrationVector.set(0f, 1f, 0f);
				break;
			case 3:
				penetrationVector.set(0f, -1f, 0f);
				break;
			case 4:
				penetrationVector.set(0f, 0f, 1f);
				break;
			case 5:
				penetrationVector.set(0f, 0f, -1f);
				break;
			default:
				assert (false);
		}
	}

	public final float x() {
		return implicitShapeDimensions.x*2f;
	}

	public final float y() {
		return implicitShapeDimensions.y*2f;
	}

	public final float z() {
		return implicitShapeDimensions.z*2f;
	}
}
