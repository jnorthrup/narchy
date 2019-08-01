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
import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.math.VectorUtil;
import spacegraph.util.math.Matrix3f;
import spacegraph.util.math.Vector4f;

/**
 *
 * @author jezek2
 */
public class BoxCollision {
	
	private static final float BOX_PLANE_EPSILON = 0.000001f;

	private static boolean BT_GREATER(float x, float y) {
		return Math.abs(x) > y;
	}

	private static float BT_MAX3(float a, float b, float c) {
		return Math.max(a, Math.max(b, c));
	}

	private static float BT_MIN3(float a, float b, float c) {
		return Math.min(a, Math.min(b, c));
	}
	
	private static boolean TEST_CROSS_EDGE_BOX_MCR(v3 edge, v3 absolute_edge, v3 pointa, v3 pointb, v3 _extend, int i_dir_0, int i_dir_1, int i_comp_0, int i_comp_1) {
		float dir0 = -VectorUtil.coord(edge, i_dir_0);
		float dir1 = VectorUtil.coord(edge, i_dir_1);
		float pmin = VectorUtil.coord(pointa, i_comp_0) * dir0 + VectorUtil.coord(pointa, i_comp_1) * dir1;
		float pmax = VectorUtil.coord(pointb, i_comp_0) * dir0 + VectorUtil.coord(pointb, i_comp_1) * dir1;
		if (pmin > pmax) {
			
			pmin = pmin + pmax;
			pmax = pmin - pmax;
			pmin = pmin - pmax;
		}
		float abs_dir0 = VectorUtil.coord(absolute_edge, i_dir_0);
		float abs_dir1 = VectorUtil.coord(absolute_edge, i_dir_1);
		float rad = VectorUtil.coord(_extend, i_comp_0) * abs_dir0 + VectorUtil.coord(_extend, i_comp_1) * abs_dir1;
        return !(pmin > rad || -rad > pmax);
    }

	private static boolean TEST_CROSS_EDGE_BOX_X_AXIS_MCR(v3 edge, v3 absolute_edge, v3 pointa, v3 pointb, v3 _extend) {
		return TEST_CROSS_EDGE_BOX_MCR(edge, absolute_edge, pointa, pointb, _extend, 2, 1, 1, 2);
	}

	private static boolean TEST_CROSS_EDGE_BOX_Y_AXIS_MCR(v3 edge, v3 absolute_edge, v3 pointa, v3 pointb, v3 _extend) {
		return TEST_CROSS_EDGE_BOX_MCR(edge, absolute_edge, pointa, pointb, _extend, 0, 2, 2, 0);
	}

	private static boolean TEST_CROSS_EDGE_BOX_Z_AXIS_MCR(v3 edge, v3 absolute_edge, v3 pointa, v3 pointb, v3 _extend) {
		return TEST_CROSS_EDGE_BOX_MCR(edge, absolute_edge, pointa, pointb, _extend, 1, 0, 0, 1);
	}
	
	/**
	 * Returns the dot product between a vec3f and the col of a matrix.
	 */
	private static float bt_mat3_dot_col(Matrix3f mat, v3 vec3, int colindex) {
		return vec3.x*mat.get(0, colindex) + vec3.y*mat.get(1, colindex) + vec3.z*mat.get(2, colindex);
	}







	
	

	public static class BoxBoxTransformCache {
		final v3 T1to0 = new v3();
		final Matrix3f R1to0 = new Matrix3f();
		final Matrix3f AR = new Matrix3f();
		
		public static void set(BoxBoxTransformCache cache) {
			throw new UnsupportedOperationException();
		}
		
		void calc_absolute_matrix() {
			
			
			
			

			for (int i=0; i<3; i++) {
				for (int j=0; j<3; j++) {
					AR.setElement(i, j, 1e-6f + Math.abs(R1to0.get(i, j)));
				}
			}
		}

		/**
		 * Calc the transformation relative  1 to 0. Inverts matrics by transposing.
		 */
		public void calc_from_homogenic(Transform trans0, Transform trans1) {
			Transform temp_trans = new Transform();
			temp_trans.invert(trans0);
			temp_trans.mul(trans1);

			T1to0.set(temp_trans);
			R1to0.set(temp_trans.basis);

			calc_absolute_matrix();
		}
		
		/**
		 * Calcs the full invertion of the matrices. Useful for scaling matrices.
		 */
		public void calc_from_full_invert(Transform trans0, Transform trans1) {
			R1to0.invert(trans0.basis);
			T1to0.negated(trans0);
			R1to0.transform(T1to0);

			v3 tmp = new v3();
			tmp.set(trans1);
			R1to0.transform(tmp);
			T1to0.add(tmp);

			R1to0.mul(trans1.basis);

			calc_absolute_matrix();
		}
		
		v3 transform(v3 point, v3 out) {
			if (point == out) {
				point = new v3(point);
			}
			
			v3 tmp = new v3();
			R1to0.getRow(0, tmp);
			out.x = tmp.dot(point) + T1to0.x;
			R1to0.getRow(1, tmp);
			out.y = tmp.dot(point) + T1to0.y;
			R1to0.getRow(2, tmp);
			out.z = tmp.dot(point) + T1to0.z;
			return out;
		}
	}
	
	
	
	public static class AABB {
		public final v3 min = new v3();
		public final v3 max = new v3();
		
		public AABB() {
		}

		public AABB(v3 V1, v3 V2, v3 V3) {
			calc_from_triangle(V1, V2, V3);
		}

		public AABB(v3 V1, v3 V2, v3 V3, float margin) {
			calc_from_triangle_margin(V1, V2, V3, margin);
		}

		public AABB(AABB other) {
			set(other);
		}

		public AABB(AABB other, float margin) {
			this(other);
			min.x -= margin;
			min.y -= margin;
			min.z -= margin;
			max.x += margin;
			max.y += margin;
			max.z += margin;
		}

		public void init(v3 V1, v3 V2, v3 V3, float margin) {
			calc_from_triangle_margin(V1, V2, V3, margin);
		}

		public void set(AABB other) {
			min.set(other.min);
			max.set(other.max);
		}

		public void invalidate() {
			min.set(BulletGlobals.SIMD_INFINITY, BulletGlobals.SIMD_INFINITY, BulletGlobals.SIMD_INFINITY);
			max.set(-BulletGlobals.SIMD_INFINITY, -BulletGlobals.SIMD_INFINITY, -BulletGlobals.SIMD_INFINITY);
		}

		public void increment_margin(float margin) {
			min.x -= margin;
			min.y -= margin;
			min.z -= margin;
			max.x += margin;
			max.y += margin;
			max.z += margin;
		}

		public void copy_with_margin(AABB other, float margin) {
			min.x = other.min.x - margin;
			min.y = other.min.y - margin;
			min.z = other.min.z - margin;

			max.x = other.max.x + margin;
			max.y = other.max.y + margin;
			max.z = other.max.z + margin;
		}
		
		void calc_from_triangle(v3 V1, v3 V2, v3 V3) {
			min.x = BT_MIN3(V1.x, V2.x, V3.x);
			min.y = BT_MIN3(V1.y, V2.y, V3.y);
			min.z = BT_MIN3(V1.z, V2.z, V3.z);

			max.x = BT_MAX3(V1.x, V2.x, V3.x);
			max.y = BT_MAX3(V1.y, V2.y, V3.y);
			max.z = BT_MAX3(V1.z, V2.z, V3.z);
		}

		public void calc_from_triangle_margin(v3 V1, v3 V2, v3 V3, float margin) {
			calc_from_triangle(V1, V2, V3);
			min.x -= margin;
			min.y -= margin;
			min.z -= margin;
			max.x += margin;
			max.y += margin;
			max.z += margin;
		}
		
		/**
		 * Apply a transform to an AABB.
		 */
		public void appy_transform(Transform trans) {
			v3 tmp = new v3();

			v3 center = new v3();
			center.add(max, min);
			center.scaled(0.5f);

			v3 extends_ = new v3();
			extends_.sub(max, center);

			
			trans.transform(center);

			v3 textends = new v3();

			trans.basis.getRow(0, tmp);
			tmp.absolute();
			textends.x = extends_.dot(tmp);

			trans.basis.getRow(1, tmp);
			tmp.absolute();
			textends.y = extends_.dot(tmp);

			trans.basis.getRow(2, tmp);
			tmp.absolute();
			textends.z = extends_.dot(tmp);

			min.sub(center, textends);
			max.add(center, textends);
		}

		/**
		 * Apply a transform to an AABB.
		 */
        void appy_transform_trans_cache(BoxBoxTransformCache trans) {
			v3 tmp = new v3();

			v3 center = new v3();
			center.add(max, min);
			center.scaled(0.5f);

			v3 extends_ = new v3();
			extends_.sub(max, center);

			
			trans.transform(center, center);

			v3 textends = new v3();

			trans.R1to0.getRow(0, tmp);
			tmp.absolute();
			textends.x = extends_.dot(tmp);

			trans.R1to0.getRow(1, tmp);
			tmp.absolute();
			textends.y = extends_.dot(tmp);

			trans.R1to0.getRow(2, tmp);
			tmp.absolute();
			textends.z = extends_.dot(tmp);

			min.sub(center, textends);
			max.add(center, textends);
		}
		
		/**
		 * Merges a Box.
		 */
		public void merge(AABB box) {
			min.x = Math.min(min.x, box.min.x);
			min.y = Math.min(min.y, box.min.y);
			min.z = Math.min(min.z, box.min.z);

			max.x = Math.max(max.x, box.max.x);
			max.y = Math.max(max.y, box.max.y);
			max.z = Math.max(max.z, box.max.z);
		}

		/**
		 * Merges a point.
		 */
		public void merge_point(v3 point) {
			min.x = Math.min(min.x, point.x);
			min.y = Math.min(min.y, point.y);
			min.z = Math.min(min.z, point.z);

			max.x = Math.max(max.x, point.x);
			max.y = Math.max(max.y, point.y);
			max.z = Math.max(max.z, point.z);
		}
		
		/**
		 * Gets the extend and center.
		 */
        void get_center_extend(v3 center, v3 extend) {
			center.add(max, min);
			center.scaled(0.5f);

			extend.sub(max, center);
		}
		
		/**
		 * Finds the intersecting box between this box and the other.
		 */
		public void find_intersection(AABB other, AABB intersection) {
			intersection.min.x = Math.max(other.min.x, min.x);
			intersection.min.y = Math.max(other.min.y, min.y);
			intersection.min.z = Math.max(other.min.z, min.z);

			intersection.max.x = Math.min(other.max.x, max.x);
			intersection.max.y = Math.min(other.max.y, max.y);
			intersection.max.z = Math.min(other.max.z, max.z);
		}

		public boolean has_collision(AABB other) {
            return !(min.x > other.max.x ||
                    max.x < other.min.x ||
                    min.y > other.max.y ||
                    max.y < other.min.y ||
                    min.z > other.max.z ||
                    max.z < other.min.z);
        }
		
		/**
		 * Finds the Ray intersection parameter.
		 *
		 * @param vorigin  a vec3f with the origin of the ray
		 * @param vdir     a vec3f with the direction of the ray
		 */
		public boolean collide_ray(v3 vorigin, v3 vdir) {
			v3 extents = new v3(), center = new v3();
			get_center_extend(center, extents);

			float Dx = vorigin.x - center.x;
			if (BT_GREATER(Dx, extents.x) && Dx * vdir.x >= 0.0f) return false;
			
			float Dy = vorigin.y - center.y;
			if (BT_GREATER(Dy, extents.y) && Dy * vdir.y >= 0.0f) return false;
			
			float Dz = vorigin.z - center.z;
			if (BT_GREATER(Dz, extents.z) && Dz * vdir.z >= 0.0f) return false;
			
			float f = vdir.y * Dz - vdir.z * Dy;
			if (Math.abs(f) > extents.y * Math.abs(vdir.z) + extents.z * Math.abs(vdir.y)) return false;
			
			f = vdir.z * Dx - vdir.x * Dz;
			if (Math.abs(f) > extents.x * Math.abs(vdir.z) + extents.z * Math.abs(vdir.x)) return false;
			
			f = vdir.x * Dy - vdir.y * Dx;
            return Math.abs(f) <= extents.x * Math.abs(vdir.y) + extents.y * Math.abs(vdir.x);

        }
	
		void projection_interval(v3 direction, float[] vmin, float[] vmax) {
			v3 tmp = new v3();

			v3 center = new v3();
			v3 extend = new v3();
			get_center_extend(center, extend);

			float _fOrigin = direction.dot(center);
			tmp.absolute(direction);
			float _fMaximumExtent = extend.dot(tmp);
			vmin[0] = _fOrigin - _fMaximumExtent;
			vmax[0] = _fOrigin + _fMaximumExtent;
		}

		public PlaneIntersectionType plane_classify(Vector4f plane) {
			v3 tmp = new v3();

			float[] _fmin = new float[1], _fmax = new float[1];
			tmp.set(plane.x, plane.y, plane.z);
			projection_interval(tmp, _fmin, _fmax);

			if (plane.w > _fmax[0] + BOX_PLANE_EPSILON) {
				return PlaneIntersectionType.BACK_PLANE; 
			}

			if (plane.w + BOX_PLANE_EPSILON >= _fmin[0]) {
				return PlaneIntersectionType.COLLIDE_PLANE; 
			}
			
			return PlaneIntersectionType.FRONT_PLANE; 
		}
		
		public boolean overlapping_trans_conservative(AABB box, Transform trans1_to_0) {
			AABB tbox = new AABB(box);
			tbox.appy_transform(trans1_to_0);
			return has_collision(tbox);
		}

		public boolean overlapping_trans_conservative2(AABB box, BoxBoxTransformCache trans1_to_0) {
			AABB tbox = new AABB(box);
			tbox.appy_transform_trans_cache(trans1_to_0);
			return has_collision(tbox);
		}

		/**
		 * transcache is the transformation cache from box to this AABB.
		 */
		public boolean overlapping_trans_cache(AABB box, BoxBoxTransformCache transcache, boolean fulltest) {
			v3 tmp = new v3();

			
			v3 ea = new v3(), eb = new v3(); 
			v3 ca = new v3(), cb = new v3(); 
			get_center_extend(ca, ea);
			box.get_center_extend(cb, eb);

			v3 T = new v3();
			float t, t2;

			
			for (int i=0; i<3; i++) {
				transcache.R1to0.getRow(i, tmp);
				VectorUtil.setCoord(T, i, tmp.dot(cb) + VectorUtil.coord(transcache.T1to0, i) - VectorUtil.coord(ca, i));

				transcache.AR.getRow(i, tmp);
				t = tmp.dot(eb) + VectorUtil.coord(ea, i);
				if (BT_GREATER(VectorUtil.coord(T, i), t)) {
					return false;
				}
			}
			
			for (int i=0; i<3; i++) {
				t = bt_mat3_dot_col(transcache.R1to0, T, i);
				t2 = bt_mat3_dot_col(transcache.AR, ea, i) + VectorUtil.coord(eb, i);
				if (BT_GREATER(t, t2)) {
					return false;
				}
			}
			
			if (fulltest) {
				int m, n, o, p, q, r;
				for (int i = 0; i < 3; i++) {
					m = (i+1) % 3;
					n = (i+2) % 3;
					o = (i == 0)? 1:0;
					p = (i == 2)? 1:2;
					for (int j=0; j<3; j++) {
						q = j == 2 ? 1 : 2;
						r = j == 0 ? 1 : 0;
						t = VectorUtil.coord(T, n) * transcache.R1to0.get(m, j) - VectorUtil.coord(T, m) * transcache.R1to0.get(n, j);
						t2 = VectorUtil.coord(ea, o) * transcache.AR.get(p, j) + VectorUtil.coord(ea, p) * transcache.AR.get(o, j) +
								VectorUtil.coord(eb, r) * transcache.AR.get(i, q) + VectorUtil.coord(eb, q) * transcache.AR.get(i, r);
						if (BT_GREATER(t, t2)) {
							return false;
						}
					}
				}
			}
			return true;
		}
		
		/**
		 * Simple test for planes.
		 */
        boolean collide_plane(Vector4f plane) {
			PlaneIntersectionType classify = plane_classify(plane);
			return (classify == PlaneIntersectionType.COLLIDE_PLANE);
		}
		
		/**
		 * Test for a triangle, with edges.
		 */
		public boolean collide_triangle_exact(v3 p1, v3 p2, v3 p3, Vector4f triangle_plane) {
			if (!collide_plane(triangle_plane)) {
				return false;
			}
			v3 center = new v3(), extends_ = new v3();
			get_center_extend(center, extends_);

			v3 v1 = new v3();
			v1.sub(p1, center);
			v3 v2 = new v3();
			v2.sub(p2, center);
			v3 v3 = new v3();
			v3.sub(p3, center);

			
			jcog.math.v3 diff = new v3();
			diff.sub(v2, v1);
			jcog.math.v3 abs_diff = new v3();
			abs_diff.absolute(diff);

			
			TEST_CROSS_EDGE_BOX_X_AXIS_MCR(diff, abs_diff, v1, v3, extends_);
			
			TEST_CROSS_EDGE_BOX_Y_AXIS_MCR(diff, abs_diff, v1, v3, extends_);
			
			TEST_CROSS_EDGE_BOX_Z_AXIS_MCR(diff, abs_diff, v1, v3, extends_);

			diff.sub(v3, v2);
			abs_diff.absolute(diff);

			
			TEST_CROSS_EDGE_BOX_X_AXIS_MCR(diff, abs_diff, v2, v1, extends_);
			
			TEST_CROSS_EDGE_BOX_Y_AXIS_MCR(diff, abs_diff, v2, v1, extends_);
			
			TEST_CROSS_EDGE_BOX_Z_AXIS_MCR(diff, abs_diff, v2, v1, extends_);

			diff.sub(v1, v3);
			abs_diff.absolute(diff);

			
			TEST_CROSS_EDGE_BOX_X_AXIS_MCR(diff, abs_diff, v3, v2, extends_);
			
			TEST_CROSS_EDGE_BOX_Y_AXIS_MCR(diff, abs_diff, v3, v2, extends_);
			
			TEST_CROSS_EDGE_BOX_Z_AXIS_MCR(diff, abs_diff, v3, v2, extends_);

			return true;
		}
	}
	
}
