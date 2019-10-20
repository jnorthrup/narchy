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
import spacegraph.space3d.phys.util.OArrayList;
import spacegraph.util.math.Vector4f;

import static spacegraph.space3d.phys.BulletGlobals.SIMD_EPSILON;

/**
 *
 * @author jezek2
 */
class TriangleContact {
	
	
	
	public static final int MAX_TRI_CLIPPING = 16;

    public float penetration_depth;
    public int point_count;
    public final Vector4f separating_normal = new Vector4f();
    public final v3[] points = new v3[MAX_TRI_CLIPPING];

	public TriangleContact() {
		for (var i = 0; i<points.length; i++) {
			points[i] = new v3();
		}
	}

	public TriangleContact(TriangleContact other) {
		copy_from(other);
	}

	public void set(TriangleContact other) {
		copy_from(other);
	}
	
	public void copy_from(TriangleContact other) {
		penetration_depth = other.penetration_depth;
		separating_normal.set(other.separating_normal);
		point_count = other.point_count;
		var i = point_count;
		while ((i--) != 0) {
			points[i].set(other.points[i]);
		}
	}
	
	/**
	 * Classify points that are closer.
	 */
	public void merge_points(Vector4f plane, float margin, OArrayList<v3> points, int pointsPending) {
		this.point_count = 0;
		penetration_depth = -1000.0f;

		var point_indices = new short[Math.min(pointsPending, MAX_TRI_CLIPPING)];

		var penetratinDepthMinusEpsilon = penetration_depth - SIMD_EPSILON;

		for (short _k = 0; _k < pointsPending; _k++) {

			var _dist = -ClipPolygon.distance_point_plane(plane, points.get(_k)) + margin;

			if (_dist >= 0.0f) {
				int target;
				if (_dist > penetration_depth) {
					penetration_depth = _dist;
					this.point_count = 1;
					target = 0;
				} else if (_dist >= penetratinDepthMinusEpsilon) {
					target = this.point_count++;
				} else {
					continue;
				}

				point_indices[target] = _k;
			}
		}

		for (var _k = 0; _k < this.point_count; _k++) {
            
            this.points[_k].set(points.get(point_indices[_k]));
		}
		
		
	}

}
