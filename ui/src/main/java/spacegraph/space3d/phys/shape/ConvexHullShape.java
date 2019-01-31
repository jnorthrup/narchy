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
import spacegraph.space3d.phys.BulletGlobals;
import spacegraph.space3d.phys.collision.broad.BroadphaseNativeType;
import spacegraph.space3d.phys.math.VectorUtil;
import spacegraph.space3d.phys.util.OArrayList;

import java.util.Collection;

/**
 * ConvexHullShape implements an implicit convex hull of an array of vertices.
 * Bullet provides a general and fast collision detector for convex shapes based
 * on GJK and EPA using localGetSupportingVertex.
 * 
 * @author jezek2
 */
public class ConvexHullShape extends PolyhedralConvexShape {

	private final OArrayList<v3> points = new OArrayList<>();

	public ConvexHullShape() {

	}

	/**
	 * TODO: This constructor optionally takes in a pointer to points. Each point is assumed to be 3 consecutive float (x,y,z), the striding defines the number of bytes between each point, in memory.
	 * It is easier to not pass any points in the constructor, and just addAt one point at a time, using addPoint.
	 * ConvexHullShape make an internal copy of the points.
	 */
	
	public ConvexHullShape(OArrayList<v3> points) {


        for (v3 point : points) {

            this.points.add(new v3(point));
        }
		
		recalcLocalAabb();
	}

	@Override
	public void setLocalScaling(v3 scaling) {
		localScaling.set(scaling);
		recalcLocalAabb();
	}
	
	public ConvexHullShape add(v3... points) {
		for (v3 point : points)
			this.points.add(new v3(point));
		recalcLocalAabb();
		return this;
	}

	public Collection<v3> getPoints() {
		return points;
	}

	public int getNumPoints() {
		return points.size();
	}

	@Override
	public v3 localGetSupportingVertexWithoutMargin(v3 vec0, v3 out) {
		v3 supVec = out;
		supVec.set(0f, 0f, 0f);
		float newDot, maxDot = BulletGlobals.SIMD_EPSILON; 

		v3 vec = new v3(vec0);
		float lenSqr = vec.lengthSquared();
		if (lenSqr < 0.0001f) {
			vec.set(1f, 0f, 0f);
		}
		else {
			float rlen = 1f / (float) Math.sqrt(lenSqr);
			vec.scale(rlen);
		}


		v3 vtx = new v3();
        for (v3 point : points) {

            VectorUtil.mul(vtx, point, localScaling);

            newDot = vec.dot(vtx);
            if (newDot > maxDot) {
                maxDot = newDot;
                supVec.set(vtx);
            }
        }
		return out;
	}

	@Override
	public void batchedUnitVectorGetSupportingVertexWithoutMargin(v3[] vectors, v3[] supportVerticesOut, int numVectors) {
		float newDot;

		
		
		float[] wcoords = new float[numVectors];

		
		for (int i = 0; i < numVectors; i++) {
            
            wcoords[i] = BulletGlobals.SIMD_EPSILON /*-1e30f*/;
        }
		v3 vtx = new v3();
        for (v3 point : points) {

            VectorUtil.mul(vtx, point, localScaling);

            for (int j = 0; j < numVectors; j++) {
                v3 vec = vectors[j];

                newDot = vec.dot(vtx);

                if (newDot > wcoords[j]) {

                    supportVerticesOut[j].set(vtx);

                    wcoords[j] = newDot;
                }
            }
        }
	}

	@Override
	public v3 localGetSupportingVertex(v3 vec, v3 out) {
		v3 supVertex = localGetSupportingVertexWithoutMargin(vec, out);

		if (getMargin() != 0f) {
			v3 vecnorm = new v3(vec);
			if (vecnorm.lengthSquared() < (BulletGlobals.FLT_EPSILON * BulletGlobals.FLT_EPSILON)) {
				vecnorm.set(-1f, -1f, -1f);
			}
			vecnorm.normalize();
			supVertex.scaleAdd(getMargin(), vecnorm, supVertex);
		}
		return out;
	}

	/**
	 * Currently just for debugging (drawing), perhaps future support for algebraic continuous collision detection.
	 * Please note that you can debug-draw ConvexHullShape with the Raytracer Demo.
	 */
	@Override
	public int getNumVertices() {
		return points.size();
	}

	@Override
	public int getNumEdges() {
		return points.size();
	}

	@Override
	public void getEdge(int i, v3 pa, v3 pb) {
		int index0 = i % points.size();
		int index1 = (i + 1) % points.size();
        
        VectorUtil.mul(pa, points.get(index0), localScaling);
        
        VectorUtil.mul(pb, points.get(index1), localScaling);
	}

	@Override
	public void getVertex(int i, v3 vtx) {
        
        VectorUtil.mul(vtx, points.get(i), localScaling);
	}

	@Override
	public int getNumPlanes() {
		return 0;
	}

	@Override
	public void getPlane(v3 planeNormal, v3 planeSupport, int i) {
		assert false;
	}

	@Override
	public boolean isInside(v3 pt, float tolerance) {
		assert false;
		return false;
	}

	@Override
	public BroadphaseNativeType getShapeType() {
		return BroadphaseNativeType.CONVEX_HULL_SHAPE_PROXYTYPE;
	}

	@Override
	public String getName() {
		return "Convex";
	}

}
