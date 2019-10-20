/*
 * Java port of Bullet (c) 2008 Martin Dvorak <jezek2@advel.cz>
 *
 * This source file is part of GIMPACT Library.
 *
 * For the latest info, see http:
 *
 * Copyright (c) 2007 Francisco Leon Najera. C.C. 80087371.
 * email: projectileman@yahoo.com
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

package spacegraph.space3d.phys.dynamics.gimpact;

import jcog.math.v3;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.shape.TriangleShape;
import spacegraph.util.math.Vector4f;

/**
 *
 * @author jezek2
 */
public class TriangleShapeEx extends TriangleShape {

	public TriangleShapeEx() {
		super();
	}

	public TriangleShapeEx(v3 p0, v3 p1, v3 p2) {
		super(p0, p1, p2);
	}

	@Override
	public void getAabb(Transform t, v3 aabbMin, v3 aabbMax) {
		var tv0 = new v3(vertices1[0]);
		t.transform(tv0);
		var tv1 = new v3(vertices1[1]);
		t.transform(tv1);
		var tv2 = new v3(vertices1[2]);
		t.transform(tv2);

		var trianglebox = new BoxCollision.AABB();
		trianglebox.init(tv0,tv1,tv2,collisionMargin);
		
		aabbMin.set(trianglebox.min);
		aabbMax.set(trianglebox.max);
	}

	public void applyTransform(Transform t) {
		t.transform(vertices1[0]);
		t.transform(vertices1[1]);
		t.transform(vertices1[2]);
	}

	private void buildTriPlane(Vector4f plane) {
		var tmp1 = new v3();
		var tmp2 = new v3();

		var normal = new v3();
		tmp1.sub(vertices1[1], vertices1[0]);
		tmp2.sub(vertices1[2], vertices1[0]);
		normal.cross(tmp1, tmp2);
		normal.normalize();

		plane.set(normal.x, normal.y, normal.z, vertices1[0].dot(normal));
	}

	public boolean overlap_test_conservative(TriangleShapeEx other) {
		var total_margin = getMargin() + other.getMargin();

		var plane0 = new Vector4f();
		buildTriPlane(plane0);
		var plane1 = new Vector4f();
		other.buildTriPlane(plane1);


		var dis0 = ClipPolygon.distance_point_plane(plane0, other.vertices1[0]) - total_margin;

		var dis1 = ClipPolygon.distance_point_plane(plane0, other.vertices1[1]) - total_margin;

		var dis2 = ClipPolygon.distance_point_plane(plane0, other.vertices1[2]) - total_margin;

		if (dis0 > 0.0f && dis1 > 0.0f && dis2 > 0.0f) {
			return false; 
		}
		dis0 = ClipPolygon.distance_point_plane(plane1, vertices1[0]) - total_margin;

		dis1 = ClipPolygon.distance_point_plane(plane1, vertices1[1]) - total_margin;

		dis2 = ClipPolygon.distance_point_plane(plane1, vertices1[2]) - total_margin;

        return !(dis0 > 0.0f && dis1 > 0.0f && dis2 > 0.0f);
    }
	
}
