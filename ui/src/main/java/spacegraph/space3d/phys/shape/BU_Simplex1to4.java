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

import spacegraph.space3d.phys.collision.broad.BroadphaseNativeType;
import jcog.math.v3;

/**
 * BU_Simplex1to4 implements feature based and implicit simplex of up to 4 vertices
 * (tetrahedron, triangle, line, vertex).
 * 
 * @author jezek2
 */
public class BU_Simplex1to4 extends PolyhedralConvexShape {

	protected int numVertices;
	protected v3[] vertices = new v3[4];

	protected BU_Simplex1to4() {
	}

	public BU_Simplex1to4(v3 pt0) {
		addVertex(pt0);
	}

	public BU_Simplex1to4(v3 pt0, v3 pt1) {
		addVertex(pt0);
		addVertex(pt1);
	}

	public BU_Simplex1to4(v3 pt0, v3 pt1, v3 pt2) {
		addVertex(pt0);
		addVertex(pt1);
		addVertex(pt2);
	}

	public BU_Simplex1to4(v3 pt0, v3 pt1, v3 pt2, v3 pt3) {
		addVertex(pt0);
		addVertex(pt1);
		addVertex(pt2);
		addVertex(pt3);
	}
	
	public void reset() {
		numVertices = 0;
	}
	
	@Override
	public BroadphaseNativeType getShapeType() {
		return BroadphaseNativeType.TETRAHEDRAL_SHAPE_PROXYTYPE;
	}
	
	private void addVertex(v3 pt) {
		if (vertices[numVertices] == null) {
			vertices[numVertices] = new v3();
		}
		
		vertices[numVertices++] = pt;

		recalcLocalAabb();
	}

	
	@Override
	public int getNumVertices() {
		return numVertices;
	}

	@Override
	public int getNumEdges() {
		

		switch (numVertices) {
			case 0: return 0;
			case 1: return 0;
			case 2: return 1;
			case 3: return 3;
			case 4: return 6;
		}

		return 0;
	}

	@Override
	public void getEdge(int i, v3 pa, v3 pb) {
		switch (numVertices) {
			case 2:
				pa.set(vertices[0]);
				pb.set(vertices[1]);
				break;
			case 3:
				switch (i) {
					case 0:
						pa.set(vertices[0]);
						pb.set(vertices[1]);
						break;
					case 1:
						pa.set(vertices[1]);
						pb.set(vertices[2]);
						break;
					case 2:
						pa.set(vertices[2]);
						pb.set(vertices[0]);
						break;
				}
				break;
			case 4:
				switch (i) {
					case 0:
						pa.set(vertices[0]);
						pb.set(vertices[1]);
						break;
					case 1:
						pa.set(vertices[1]);
						pb.set(vertices[2]);
						break;
					case 2:
						pa.set(vertices[2]);
						pb.set(vertices[0]);
						break;
					case 3:
						pa.set(vertices[0]);
						pb.set(vertices[3]);
						break;
					case 4:
						pa.set(vertices[1]);
						pb.set(vertices[3]);
						break;
					case 5:
						pa.set(vertices[2]);
						pb.set(vertices[3]);
						break;
				}
		}
	}

	@Override
	public void getVertex(int i, v3 vtx) {
		vtx.set(vertices[i]);
	}

	@Override
	public int getNumPlanes() {
		switch (numVertices) {
			case 0: return 0;
			case 1: return 0;
			case 2: return 0;
			case 3: return 2;
			case 4: return 4;
		}
		return 0;
	}

	@Override
	public void getPlane(v3 planeNormal, v3 planeSupport, int i) {
	}
	
	public static int getIndex(int i) {
		return 0;
	}

	@Override
	public boolean isInside(v3 pt, float tolerance) {
		return false;
	}

	@Override
	public String getName() {
		return "BU_Simplex1to4";
	}

}
