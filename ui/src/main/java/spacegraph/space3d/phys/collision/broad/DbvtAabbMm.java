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



package spacegraph.space3d.phys.collision.broad;

import jcog.math.v3;
import spacegraph.space3d.phys.math.MatrixUtil;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.math.VectorUtil;

import static spacegraph.space3d.phys.math.VectorUtil.coord;

/**
 *
 * @author jezek2
 */
public class DbvtAabbMm {

	private final v3 mi;
	private final v3 mx;

	public DbvtAabbMm() {
		mi = new v3();
		mx = new v3();
	}

	public DbvtAabbMm(v3 mi, v3 mx) {
		this.mi = mi;
		this.mx = mx;
	}

	public DbvtAabbMm(DbvtAabbMm o) {
		this();
		set(o);
	}
	
	public void set(DbvtAabbMm o) {
		mi.set(o.mi);
		mx.set(o.mx);
	}
	
	public static void swap(DbvtAabbMm p1, DbvtAabbMm p2) {
		v3 tmp = new v3();
		
		tmp.set(p1.mi);
		p1.mi.set(p2.mi);
		p2.mi.set(tmp);

		tmp.set(p1.mx);
		p1.mx.set(p2.mx);
		p2.mx.set(tmp);
	}

	public v3 center(v3 out) {
		out.add(mi, mx);
		out.scaled(0.5f);
		return out;
	}
	
	public v3 lengths(v3 out) {
		out.sub(mx, mi);
		return out;
	}

	public v3 extents(v3 out) {
		out.sub(mx, mi);
		out.scaled(0.5f);
		return out;
	}
	
	public v3 mins() {
		return mi;
	}

	public v3 maxs() {
		return mx;
	}
	
	private static DbvtAabbMm FromCE(v3 c, v3 e, DbvtAabbMm out) {
		DbvtAabbMm box = out;
		box.mi.sub(c, e);
		box.mx.add(c, e);
		return box;
	}

	public static DbvtAabbMm fromCR(v3 c, float r, DbvtAabbMm out) {
		v3 tmp = new v3();
		tmp.set(r, r, r);
		return FromCE(c, tmp, out);
	}

	public static DbvtAabbMm FromMM(v3 mi, v3 mx, DbvtAabbMm out) {
		DbvtAabbMm box = out;
		box.mi.set(mi);
		box.mx.set(mx);
		return box;
	}
	
	
	
	
	public void Expand(v3 e) {
		mi.sub(e);
		mx.add(e);
	}

	public void SignedExpand(v3 e) {
		if (e.x > 0) {
			mx.x += e.x;
		}
		else {
			mi.x += e.x;
		}
		
		if (e.y > 0) {
			mx.y += e.y;
		}
		else {
			mi.y += e.y;
		}
		
		if (e.z > 0) {
			mx.z += e.z;
		}
		else {
			mi.z += e.z;
		}
	}

	public boolean Contain(DbvtAabbMm a) {
		return ((mi.x <= a.mi.x) &&
		        (mi.y <= a.mi.y) &&
		        (mi.z <= a.mi.z) &&
		        (mx.x >= a.mx.x) &&
		        (mx.y >= a.mx.y) &&
		        (mx.z >= a.mx.z));
	}

	public int Classify(v3 n, float o, int s) {
		v3 pi = new v3();
		v3 px = new v3();

		switch (s) {
			case (0 + 0 + 0):
				px.set(mi.x, mi.y, mi.z);
				pi.set(mx.x, mx.y, mx.z);
				break;
			case (1 + 0 + 0):
				px.set(mx.x, mi.y, mi.z);
				pi.set(mi.x, mx.y, mx.z);
				break;
			case (0 + 2 + 0):
				px.set(mi.x, mx.y, mi.z);
				pi.set(mx.x, mi.y, mx.z);
				break;
			case (1 + 2 + 0):
				px.set(mx.x, mx.y, mi.z);
				pi.set(mi.x, mi.y, mx.z);
				break;
			case (0 + 0 + 4):
				px.set(mi.x, mi.y, mx.z);
				pi.set(mx.x, mx.y, mi.z);
				break;
			case (1 + 0 + 4):
				px.set(mx.x, mi.y, mx.z);
				pi.set(mi.x, mx.y, mi.z);
				break;
			case (0 + 2 + 4):
				px.set(mi.x, mx.y, mx.z);
				pi.set(mx.x, mi.y, mi.z);
				break;
			case (1 + 2 + 4):
				px.set(mx.x, mx.y, mx.z);
				pi.set(mi.x, mi.y, mi.z);
				break;
		}
		
		if ((n.dot(px) + o) < 0) {
			return -1;
		}
		if ((n.dot(pi) + o) >= 0) {
			return +1;
		}
		return 0;
	}

	public float ProjectMinimum(v3 v, int signs) {
		v3[] b = { mx, mi };
		v3 p = new v3();
		p.set(b[(signs >> 0) & 1].x,
		      b[(signs >> 1) & 1].y,
		      b[(signs >> 2) & 1].z);
		return p.dot(v);
	}
	 
	public static boolean intersect(DbvtAabbMm a, DbvtAabbMm b) {
		return ((a.mi.x <= b.mx.x) &&
		        (a.mx.x >= b.mi.x) &&
		        (a.mi.y <= b.mx.y) &&
		        (a.mx.y >= b.mi.y) &&
		        (a.mi.z <= b.mx.z) &&
		        (a.mx.z >= b.mi.z));
	}

	public static boolean intersect(DbvtAabbMm a, DbvtAabbMm b, Transform xform) {
		v3 d0 = new v3();
		v3 d1 = new v3();
		v3 tmp = new v3();

		
		b.center(d0);
		xform.transform(d0);
		d0.sub(a.center(tmp));

		MatrixUtil.transposeTransform(d1, d0, xform.basis);

        float[] s1 = new float[2];
		s1[0] = xform.dot(d0);
		s1[1] = s1[0];

        float[] s0 = {0, 0};
        a.addSpan(d0, s0, 0, s0, 1);
		b.addSpan(d1, s1, 0, s1, 1);
		if (s0[0] > (s1[1])) {
			return false;
		}
        return s0[1] >= (s1[0]);
    }

	public static boolean intersect(DbvtAabbMm a, v3 b) {
		return ((b.x >= a.mi.x) &&
		        (b.y >= a.mi.y) &&
		        (b.z >= a.mi.z) &&
		        (b.x <= a.mx.x) &&
		        (b.y <= a.mx.y) &&
		        (b.z <= a.mx.z));
	}

	public static boolean intersect(DbvtAabbMm a, v3 org, v3 invdir, int[] signs) {
		v3[] bounds = {a.mi, a.mx};
		float txmin = (bounds[signs[0]].x - org.x) * invdir.x;
		float txmax = (bounds[1 - signs[0]].x - org.x) * invdir.x;
		float tymin = (bounds[signs[1]].y - org.y) * invdir.y;
		float tymax = (bounds[1 - signs[1]].y - org.y) * invdir.y;
		if ((txmin > tymax) || (tymin > txmax)) {
			return false;
		}
		
		if (tymin > txmin) {
			txmin = tymin;
		}
		if (tymax < txmax) {
			txmax = tymax;
		}
		float tzmin = (bounds[signs[2]].z - org.z) * invdir.z;
		float tzmax = (bounds[1 - signs[2]].z - org.z) * invdir.z;
		if ((txmin > tzmax) || (tzmin > txmax)) {
			return false;
		}
		
		if (tzmin > txmin) {
			txmin = tzmin;
		}
		if (tzmax < txmax) {
			txmax = tzmax;
		}
		return (txmax > 0);
	}

	public static float Proximity(DbvtAabbMm a, DbvtAabbMm b) {
		v3 d = new v3();
		v3 tmp = new v3();

		d.add(a.mi, a.mx);
		tmp.add(b.mi, b.mx);
		d.sub(tmp);
		return Math.abs(d.x) + Math.abs(d.y) + Math.abs(d.z);
	}

	public static void merge(DbvtAabbMm a, DbvtAabbMm b, DbvtAabbMm r) {
		for (int i=0; i<3; i++) {
			if (coord(a.mi, i) < coord(b.mi, i)) {
				VectorUtil.setCoord(r.mi, i, coord(a.mi, i));
			}
			else {
				VectorUtil.setCoord(r.mi, i, coord(b.mi, i));
			}
			
			if (coord(a.mx, i) > coord(b.mx, i)) {
				VectorUtil.setCoord(r.mx, i, coord(a.mx, i));
			}
			else {
				VectorUtil.setCoord(r.mx, i, coord(b.mx, i));
			}
		}
	}

	public static boolean NotEqual(DbvtAabbMm a, DbvtAabbMm b) {
		return ((a.mi.x != b.mi.x) ||
		        (a.mi.y != b.mi.y) ||
		        (a.mi.z != b.mi.z) ||
		        (a.mx.x != b.mx.x) ||
		        (a.mx.y != b.mx.y) ||
		        (a.mx.z != b.mx.z));
	}
	
	private void addSpan(v3 d, float[] smi, int smi_idx, float[] smx, int smx_idx) {
		v3 mx = this.mx;
		v3 mi = this.mi;
		for (int i=0; i<3; i++) {
			float cx = coord(mx, i);
			float cd = coord(d, i);
			float ci = coord(mi, i);
			if (cd < 0) {
				smi[smi_idx] += cx * cd;
				smx[smx_idx] += ci * cd;
			}
			else {
				smi[smi_idx] += ci * cd;
				smx[smx_idx] += cx * cd;
			}
		}
	}
	
}
