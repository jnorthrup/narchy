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
import spacegraph.space3d.phys.math.AabbUtil2;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.math.VectorUtil;

/**
 * PolyhedralConvexShape is an internal interface class for polyhedral convex shapes.
 * 
 * @author jezek2
 */
public abstract class PolyhedralConvexShape extends ConvexInternalShape {

	private static final v3[] _directions = {
		new v3( 1f,  0f,  0f),
		new v3( 0f,  1f,  0f),
		new v3( 0f,  0f,  1f),
		new v3(-1f,  0f,  0f),
		new v3( 0f, -1f,  0f),
		new v3( 0f,  0f, -1f)
	};

	private static final v3[] _supporting = {
		new v3(0f, 0f, 0f),
		new v3(0f, 0f, 0f),
		new v3(0f, 0f, 0f),
		new v3(0f, 0f, 0f),
		new v3(0f, 0f, 0f),
		new v3(0f, 0f, 0f)
	};
	
	private final v3 localAabbMin = new v3(1f, 1f, 1f);
	private final v3 localAabbMax = new v3(-1f, -1f, -1f);
	private boolean isLocalAabbValid;



	
	@Override
	public v3 localGetSupportingVertexWithoutMargin(v3 vec0, v3 out) {
        v3 supVec = out;
		supVec.set(0f, 0f, 0f);

        v3 vec = new v3(vec0);
		float lenSqr = vec.lengthSquared();
		if (lenSqr < 0.0001f) {
			vec.set(1f, 0f, 0f);
		}
		else {
			float rlen = 1f / (float) Math.sqrt(lenSqr);
			vec.scaled(rlen);
		}

		v3 vtx = new v3();

        float maxDot = -1e30f;
        for (int i = 0; i < getNumVertices(); i++) {
			getVertex(i, vtx);
            float newDot = vec.dot(vtx);
            if (newDot > maxDot) {
				maxDot = newDot;
				supVec = vtx;
			}
		}

		return out;
	}

	@Override
	public void batchedUnitVectorGetSupportingVertexWithoutMargin(v3[] vectors, v3[] supportVerticesOut, int numVectors) {
		int i;

		v3 vtx = new v3();


        float[] wcoords = new float[numVectors];

		for (i = 0; i < numVectors; i++) {
			
			
			wcoords[i] = -1e30f;
		}

		for (int j = 0; j < numVectors; j++) {
			v3 vec = vectors[j];

			for (i = 0; i < getNumVertices(); i++) {
				getVertex(i, vtx);
                float newDot = vec.dot(vtx);

                if (newDot > wcoords[j]) {
					
					supportVerticesOut[j].set(vtx);
					
					wcoords[j] = newDot;
				}
			}
		}
	}

	@Override
	public void calculateLocalInertia(float mass, v3 inertia) {
		

		float margin = getMargin();

		Transform ident = new Transform();
		ident.setIdentity();
		v3 aabbMin = new v3(), aabbMax = new v3();
		getAabb(ident, aabbMin, aabbMax);

		v3 halfExtents = new v3();
		halfExtents.sub(aabbMax, aabbMin);
		halfExtents.scaled(0.5f);

		float lx = 2f * (halfExtents.x + margin);
		float ly = 2f * (halfExtents.y + margin);
		float lz = 2f * (halfExtents.z + margin);
		float x2 = lx * lx;
		float y2 = ly * ly;
		float z2 = lz * lz;

        inertia.set(y2 + z2, x2 + z2, x2 + y2);
        float scaledmass = mass * 0.08333333f;
        inertia.scaled(scaledmass);
	}

	private void getNonvirtualAabb(Transform trans, v3 aabbMin, v3 aabbMax, float margin) {
		
		assert (isLocalAabbValid);

		AabbUtil2.transformAabb(localAabbMin, localAabbMax, margin, trans, aabbMin, aabbMax);
	}
	
	@Override
	public void getAabb(Transform trans, v3 aabbMin, v3 aabbMax) {
		getNonvirtualAabb(trans, aabbMin, aabbMax, getMargin());
	}

	final void _PolyhedralConvexShape_getAabb(Transform trans, v3 aabbMin, v3 aabbMax) {
		getNonvirtualAabb(trans, aabbMin, aabbMax, getMargin());
	}

	protected void recalcLocalAabb() {
		isLocalAabbValid = true;

		

		batchedUnitVectorGetSupportingVertexWithoutMargin(_directions, _supporting, 6);

		for (int i=0; i<3; i++) {
			VectorUtil.setCoord(localAabbMax, i, VectorUtil.coord(_supporting[i], i) + collisionMargin);
			VectorUtil.setCoord(localAabbMin, i, VectorUtil.coord(_supporting[i + 3], i) - collisionMargin);
		}
		
		
		
		
		
		
		
		
		
		
		
		
		
	}

	@Override
	public void setLocalScaling(v3 scaling) {
		super.setLocalScaling(scaling);
		recalcLocalAabb();
	}

	protected abstract int getNumVertices();

	public abstract int getNumEdges();

	public abstract void getEdge(int i, v3 pa, v3 pb);

	protected abstract void getVertex(int i, v3 vtx);

	public abstract int getNumPlanes();

	public abstract void getPlane(v3 planeNormal, v3 planeSupport, int i);
	

	
	public abstract boolean isInside(v3 pt, float tolerance);
	
}
