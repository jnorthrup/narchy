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
import spacegraph.space3d.phys.BulletGlobals;
import spacegraph.space3d.phys.math.VectorUtil;
import spacegraph.util.math.Vector4f;

/**
 *
 * @author jezek2
 */
enum GeometryOperations {
	;

	private static final float PLANEDIREPSILON = 0.0000001f;
	public static final float PARALELENORMALS = 0.000001f;
	
	private static float CLAMP(float number, float minval, float maxval) {
		return (number < minval? minval : (Math.min(number, maxval)));
	}

	/**
	 * Calc a plane from a triangle edge an a normal.
	 */
	public static void edge_plane(v3 e1, v3 e2, v3 normal, Vector4f plane) {
		v3 planenormal = new v3();
		planenormal.sub(e2, e1);
		planenormal.cross(planenormal, normal);
		planenormal.normalize();

		plane.set(planenormal);
		plane.w = e2.dot(planenormal);
	}
	
	/**
	 * Finds the closest point(cp) to (v) on a segment (e1,e2).
	 */
	private static void closest_point_on_segment(v3 cp, v3 v, v3 e1, v3 e2) {
		v3 n = new v3();
		n.sub(e2, e1);
		cp.sub(v, e1);
		float _scalar = cp.dot(n) / n.dot(n);
		if (_scalar < 0.0f) {
			//cp = e1;
			cp.set(e1);
		}
		else if (_scalar > 1.0f) {
			//cp = e2;
			cp.set(e2);
		}
		else {
			cp.scaleAdd(_scalar, n, e1);
		}
	}
	
	/**
	 * Line plane collision.
	 * 
	 * @return -0 if the ray never intersects, -1 if the ray collides in front, -2 if the ray collides in back
	 */
	private static int line_plane_collision(Vector4f plane, v3 vDir, v3 vPoint, v3 pout, float[] tparam, float tmin, float tmax) {
		float _dotdir = VectorUtil.dot3(vDir, plane);

		if (Math.abs(_dotdir) < PLANEDIREPSILON) {
			tparam[0] = tmax;
			return 0;
		}

		float _dis = ClipPolygon.distance_point_plane(plane, vPoint);
		int returnvalue = _dis < 0.0f ? 2 : 1;
		tparam[0] = -_dis / _dotdir;

		if (tparam[0] < tmin) {
			returnvalue = 0;
			tparam[0] = tmin;
		}
		else if (tparam[0] > tmax) {
			returnvalue = 0;
			tparam[0] = tmax;
		}
		pout.scaleAdd(tparam[0], vDir, vPoint);
		return returnvalue;
	}
	
	/**
	 * Find closest points on segments.
	 */
	public static void segment_collision(v3 vA1, v3 vA2, v3 vB1, v3 vB2, v3 vPointA, v3 vPointB) {
		v3 AD = new v3();
		AD.sub(vA2, vA1);

		v3 BD = new v3();
		BD.sub(vB2, vB1);

		v3 N = new v3();
		N.cross(AD, BD);
		float[] tp = { N.lengthSquared() };

		Vector4f _M = new Vector4f();

		if (tp[0] < BulletGlobals.SIMD_EPSILON)
		{

            _M.x = vB1.dot(AD);
			_M.y = vB2.dot(AD);

            boolean invert_b_order = false;
            if (_M.x > _M.y) {
				invert_b_order = true;

                _M.x += _M.y;
				_M.y = _M.x - _M.y;
                _M.x -= _M.y;
			}
			_M.z = vA1.dot(AD);
			_M.w = vA2.dot(AD);
			
			N.x = (_M.x + _M.y) /2;
			N.y = (_M.z + _M.w) /2;

			if (N.x < N.y) {
				if (_M.y < _M.z) {
					vPointB = invert_b_order ? vB1 : vB2;
					vPointA = vA1;
				}
				else if (_M.y < _M.w) {
					vPointB = invert_b_order ? vB1 : vB2;
					closest_point_on_segment(vPointA, vPointB, vA1, vA2);
				}
				else {
					vPointA = vA2;
					closest_point_on_segment(vPointB, vPointA, vB1, vB2);
				}
			}
			else {
				if (_M.w < _M.x) {
					vPointB = invert_b_order ? vB2 : vB1;
					vPointA = vA2;
				}
				else if (_M.w < _M.y) {
					vPointA = vA2;
					closest_point_on_segment(vPointB, vPointA, vB1, vB2);
				}
				else {
					vPointB = invert_b_order ? vB1 : vB2;
					closest_point_on_segment(vPointA, vPointB, vA1, vA2);
				}
			}
			return;
		}

		N.cross(N, BD);
		_M.set(N.x, N.y, N.z, vB1.dot(N));

		
		line_plane_collision(_M, AD, vA1, vPointA, tp, 0f, 1f);

		/*Closest point on segment*/
		vPointB.sub(vPointA, vB1);
		tp[0] = vPointB.dot(BD);
		tp[0] /= BD.dot(BD);
		tp[0] = CLAMP(tp[0], 0.0f, 1.0f);

		vPointB.scaleAdd(tp[0], BD, vB1);
	}
	
}
