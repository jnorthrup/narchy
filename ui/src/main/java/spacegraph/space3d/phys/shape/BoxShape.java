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
import spacegraph.space3d.phys.Body3D;
import spacegraph.space3d.phys.Collidable;
import spacegraph.space3d.phys.math.ScalarUtil;
import spacegraph.space3d.phys.math.VectorUtil;
import spacegraph.util.math.Vector4f;

/**
 * BoxShape is a box primitive around the origin, its sides axis aligned with length
 * specified by half extents, in local shape coordinates. When used as part of a
 * {@link Collidable} or {@link Body3D} it will be an oriented box in world space.
 *
 * @author jezek2
 */
public class BoxShape extends SimpleBoxShape {

	public BoxShape(v3 boxHalfExtents) {
		this(boxHalfExtents.x*2f, boxHalfExtents.y*2f, boxHalfExtents.z*2f);
	}

	public BoxShape(float w, float h, float d) {
		super(w, h, d);
	}

	@Override
	public v3 localGetSupportingVertex(v3 vec, v3 out) {
        v3 halfExtents = getHalfExtentsWithoutMargin(out);

        float margin = getMargin();
		halfExtents.x += margin;
		halfExtents.y += margin;
		halfExtents.z += margin;

		out.set(
				ScalarUtil.fsel(vec.x, halfExtents.x, -halfExtents.x),
				ScalarUtil.fsel(vec.y, halfExtents.y, -halfExtents.y),
				ScalarUtil.fsel(vec.z, halfExtents.z, -halfExtents.z));
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

        float m = getMargin();
        v3 oldMargin = new v3(m, m,m);

        v3 implicitShapeDimensionsWithMargin = new v3();
		implicitShapeDimensionsWithMargin.add(implicitShapeDimensions, oldMargin);

		super.setMargin(margin);

        float n = getMargin();
        v3 newMargin = new v3(n, n, n);
		implicitShapeDimensions.sub(implicitShapeDimensionsWithMargin, newMargin);
		return this;
	}

	@Override
	public void setLocalScaling(v3 scaling) {

        float m = getMargin();
        v3 oldMargin = new v3(m, m, m);

        v3 implicitShapeDimensionsWithMargin = new v3();
		implicitShapeDimensionsWithMargin.add(implicitShapeDimensions, oldMargin);
        v3 unScaledImplicitShapeDimensionsWithMargin = new v3();
		VectorUtil.div(unScaledImplicitShapeDimensionsWithMargin, implicitShapeDimensionsWithMargin, localScaling);

		super.setLocalScaling(scaling);

		VectorUtil.mul(implicitShapeDimensions, unScaledImplicitShapeDimensionsWithMargin, localScaling);
		implicitShapeDimensions.sub(oldMargin);
	}

	@Override
	public void calculateLocalInertia(float mass, v3 inertia) {

        v3 halfExtents = getHalfExtentsWithMargin(new v3());

        float lx = 2f * halfExtents.x;
        float ly = 2f * halfExtents.y;
        float lz = 2f * halfExtents.z;

		inertia.set(
				mass / 12f * (ly * ly + lz * lz),
				mass / 12f * (lx * lx + lz * lz),
				mass / 12f * (lx * lx + ly * ly));
	}

	@Override
	public void getPlane(v3 planeNormal, v3 planeSupport, int i) {

        Vector4f plane = new Vector4f();
        v3 tmp = new v3();
		getPlaneEquation(plane, i, tmp);
		planeNormal.set(plane.x, plane.y, plane.z);

		tmp.negated(planeNormal);
		localGetSupportingVertex(tmp, planeSupport);
	}

	@Override
	public void getVertex(int i, v3 vtx) {
        v3 halfExtents = getHalfExtentsWithoutMargin(new v3());

		vtx.set(halfExtents.x * (1 - (i & 1)) - halfExtents.x * (i & 1),
				halfExtents.y * (1 - ((i & 2) >> 1)) - halfExtents.y * ((i & 2) >> 1),
				halfExtents.z * (1 - ((i & 4) >> 2)) - halfExtents.z * ((i & 4) >> 2));
	}
	
	@Override
    public void getPlaneEquation(Vector4f plane, int i, v3 tmp) {
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
	public String getName() {
		return "Box";
	}

}
