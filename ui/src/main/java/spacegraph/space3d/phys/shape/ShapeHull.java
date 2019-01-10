/*
 * Java port of Bullet (c) 2008 Martin Dvorak <jezek2@advel.cz>
 * 
 * ShapeHull implemented by John McCutchan.
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

import jcog.data.list.FasterList;
import spacegraph.space3d.phys.math.MiscUtil;
import spacegraph.space3d.phys.math.convexhull.HullDesc;
import spacegraph.space3d.phys.math.convexhull.HullFlags;
import spacegraph.space3d.phys.math.convexhull.HullLibrary;
import spacegraph.space3d.phys.math.convexhull.HullResult;
import spacegraph.space3d.phys.util.IntArrayList;
import spacegraph.space3d.phys.util.OArrayList;
import jcog.math.v3;

/**
 * ShapeHull takes a {@link ConvexShape}, builds the convex hull using {@link HullLibrary}
 * and provides triangle indices and vertices.
 *
 * @author jezek2
 */
public class ShapeHull {

	private final FasterList<v3> vertices = new FasterList<>();
	private final IntArrayList indices = new IntArrayList();
	private int numIndices;
	private ConvexShape shape;

	private final FasterList<v3> unitSpherePoints = new FasterList<>();

	public ShapeHull(ConvexShape shape) {
		this.shape = shape;
		this.vertices.clearFast();
		this.indices.clear();
		this.numIndices = 0;

		MiscUtil.resize(unitSpherePoints, NUM_UNITSPHERE_POINTS+ ConvexShape.MAX_PREFERRED_PENETRATION_DIRECTIONS*2, v3.class);
		for (int i=0; i<constUnitSpherePoints.size(); i++) {
            
            
            unitSpherePoints.get(i).set(constUnitSpherePoints.get(i));
		}
	}

	public boolean buildHull(float margin) {
		v3 norm = new v3();

		int numSampleDirections = NUM_UNITSPHERE_POINTS;
		int numPDA = shape.getNumPreferredPenetrationDirections();
		if (numPDA != 0) {
            for (int i=0; i<numPDA; i++) {
                shape.getPreferredPenetrationDirection(i, norm);
                
                unitSpherePoints.get(numSampleDirections).set(norm);
                numSampleDirections++;
            }
        }

		FasterList<v3> supportPoints = new FasterList<>();
		MiscUtil.resize(supportPoints, NUM_UNITSPHERE_POINTS + ConvexShape.MAX_PREFERRED_PENETRATION_DIRECTIONS * 2, v3.class);

		for (int i=0; i<numSampleDirections; i++) {
            
            
            shape.localGetSupportingVertex(unitSpherePoints.get(i), supportPoints.get(i));
		}

		HullDesc hd = new HullDesc();
		hd.flags = HullFlags.TRIANGLES;
		hd.vcount = numSampleDirections;

		
		
		
		
		hd.vertices = supportPoints;
		
		

		HullLibrary hl = new HullLibrary();
		HullResult hr = new HullResult();
		if (!hl.createConvexHull(hd, hr)) {
			return false;
		}

		MiscUtil.resize(vertices, hr.numOutputVertices, v3.class);

		for (int i=0; i<hr.numOutputVertices; i++) {
            
            
            vertices.get(i).set(hr.outputVertices.get(i));
		}
		numIndices = hr.numIndices;
		MiscUtil.resize(indices, numIndices, 0);
		for (int i=0; i<numIndices; i++) {
			indices.set(i, hr.indices.get(i));
		}

		
		HullLibrary.releaseResult(hr);

		return true;
	}

	public int numTriangles() {
		return numIndices / 3;
	}

	public int numVertices() {
		return vertices.size();
	}

	public int numIndices() {
		return numIndices;
	}

	public FasterList<v3> getVertexPointer() {
		return vertices;
	}

	public IntArrayList getIndexPointer() {
		return indices;
	}

	
	
	private static final int NUM_UNITSPHERE_POINTS = 42;
	
	private static final OArrayList<v3> constUnitSpherePoints = new OArrayList<>();
	
	static {
		constUnitSpherePoints.add(new v3(0.000000f, -0.000000f, -1.000000f));
		constUnitSpherePoints.add(new v3(0.723608f, -0.525725f, -0.447219f));
		constUnitSpherePoints.add(new v3(-0.276388f, -0.850649f, -0.447219f));
		constUnitSpherePoints.add(new v3(-0.894426f, -0.000000f, -0.447216f));
		constUnitSpherePoints.add(new v3(-0.276388f, 0.850649f, -0.447220f));
		constUnitSpherePoints.add(new v3(0.723608f, 0.525725f, -0.447219f));
		constUnitSpherePoints.add(new v3(0.276388f, -0.850649f, 0.447220f));
		constUnitSpherePoints.add(new v3(-0.723608f, -0.525725f, 0.447219f));
		constUnitSpherePoints.add(new v3(-0.723608f, 0.525725f, 0.447219f));
		constUnitSpherePoints.add(new v3(0.276388f, 0.850649f, 0.447219f));
		constUnitSpherePoints.add(new v3(0.894426f, 0.000000f, 0.447216f));
		constUnitSpherePoints.add(new v3(-0.000000f, 0.000000f, 1.000000f));
		constUnitSpherePoints.add(new v3(0.425323f, -0.309011f, -0.850654f));
		constUnitSpherePoints.add(new v3(-0.162456f, -0.499995f, -0.850654f));
		constUnitSpherePoints.add(new v3(0.262869f, -0.809012f, -0.525738f));
		constUnitSpherePoints.add(new v3(0.425323f, 0.309011f, -0.850654f));
		constUnitSpherePoints.add(new v3(0.850648f, -0.000000f, -0.525736f));
		constUnitSpherePoints.add(new v3(-0.525730f, -0.000000f, -0.850652f));
		constUnitSpherePoints.add(new v3(-0.688190f, -0.499997f, -0.525736f));
		constUnitSpherePoints.add(new v3(-0.162456f, 0.499995f, -0.850654f));
		constUnitSpherePoints.add(new v3(-0.688190f, 0.499997f, -0.525736f));
		constUnitSpherePoints.add(new v3(0.262869f, 0.809012f, -0.525738f));
		constUnitSpherePoints.add(new v3(0.951058f, 0.309013f, 0.000000f));
		constUnitSpherePoints.add(new v3(0.951058f, -0.309013f, 0.000000f));
		constUnitSpherePoints.add(new v3(0.587786f, -0.809017f, 0.000000f));
		constUnitSpherePoints.add(new v3(0.000000f, -1.000000f, 0.000000f));
		constUnitSpherePoints.add(new v3(-0.587786f, -0.809017f, 0.000000f));
		constUnitSpherePoints.add(new v3(-0.951058f, -0.309013f, -0.000000f));
		constUnitSpherePoints.add(new v3(-0.951058f, 0.309013f, -0.000000f));
		constUnitSpherePoints.add(new v3(-0.587786f, 0.809017f, -0.000000f));
		constUnitSpherePoints.add(new v3(-0.000000f, 1.000000f, -0.000000f));
		constUnitSpherePoints.add(new v3(0.587786f, 0.809017f, -0.000000f));
		constUnitSpherePoints.add(new v3(0.688190f, -0.499997f, 0.525736f));
		constUnitSpherePoints.add(new v3(-0.262869f, -0.809012f, 0.525738f));
		constUnitSpherePoints.add(new v3(-0.850648f, 0.000000f, 0.525736f));
		constUnitSpherePoints.add(new v3(-0.262869f, 0.809012f, 0.525738f));
		constUnitSpherePoints.add(new v3(0.688190f, 0.499997f, 0.525736f));
		constUnitSpherePoints.add(new v3(0.525730f, 0.000000f, 0.850652f));
		constUnitSpherePoints.add(new v3(0.162456f, -0.499995f, 0.850654f));
		constUnitSpherePoints.add(new v3(-0.425323f, -0.309011f, 0.850654f));
		constUnitSpherePoints.add(new v3(-0.425323f, 0.309011f, 0.850654f));
		constUnitSpherePoints.add(new v3(0.162456f, 0.499995f, 0.850654f));
	}
	
}
