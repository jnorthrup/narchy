/*
 * Java port of Bullet (c) 2008 Martin Dvorak <jezek2@advel.cz>
 *
 * Stan Melax Convex Hull Computation
 * Copyright (c) 2008 Stan Melax http:
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



package spacegraph.space3d.phys.math.convexhull;

import jcog.data.list.FasterList;
import jcog.math.v3;
import spacegraph.space3d.phys.BulletGlobals;
import spacegraph.space3d.phys.math.MiscUtil;
import spacegraph.space3d.phys.math.VectorUtil;
import spacegraph.space3d.phys.shape.ShapeHull;
import spacegraph.space3d.phys.util.IntArrayList;

/**
 * HullLibrary class can create a convex hull from a collection of vertices, using
 * the ComputeHull method. The {@link ShapeHull} class uses this HullLibrary to create
 * a approximate convex mesh given a general (non-polyhedral) convex shape.
 *
 * @author jezek2
 */
public class HullLibrary {

	private final IntArrayList vertexIndexMapping = new IntArrayList();

	private final FasterList<Tri> tris = new FasterList<>();

	/**
	 * Converts point cloud to polygonal representation.
	 *
	 * @param desc   describes the input request
	 * @param result contains the result
	 * @return whether conversion was successful
	 */
	public boolean createConvexHull(HullDesc desc, HullResult result) {

		var hr = new PHullResult();

		var vcount = desc.vcount;
		if (vcount < 8) vcount = 8;

		var vertexSource = new FasterList<v3>();
		MiscUtil.resize(vertexSource, vcount, v3.class);

		var scale = new v3();

		var ovcount = new int[1];

		var ok = cleanupVertices(desc.vcount, desc.vertices, desc.vertexStride, ovcount, vertexSource, desc.normalEpsilon, scale);

		var ret = false;
        if (ok) {
			
            for (var i = 0; i<ovcount[0]; i++) {

				var v = vertexSource.get(i);
                VectorUtil.mul(v, v, scale);
            }

            ok = computeHull(ovcount[0], vertexSource, hr, desc.maxVertices);

			if (ok) {

				var vertexScratch = new FasterList<v3>();
				MiscUtil.resize(vertexScratch, hr.vcount, v3.class);

				bringOutYourDead(hr.vertices, hr.vcount, vertexScratch, ovcount, hr.indices, hr.indexCount);

				ret = true;

				if (desc.hasHullFlag(HullFlags.TRIANGLES)) { 
					result.polygons = false;
					result.numOutputVertices = ovcount[0];
					MiscUtil.resize(result.outputVertices, ovcount[0], v3.class);
					result.numFaces = hr.faceCount;
					result.numIndices = hr.indexCount;

					MiscUtil.resize(result.indices, hr.indexCount, 0);

					for (var i = 0; i<ovcount[0]; i++) {
						
						
						result.outputVertices.get(i).set(vertexScratch.get(i));
					}

					if (desc.hasHullFlag(HullFlags.REVERSE_ORDER)) {
						var source_ptr = hr.indices;
						var source_idx = 0;

						var dest_ptr = result.indices;
						var dest_idx = 0;

						for (var i = 0; i<hr.faceCount; i++) {
							dest_ptr.set(dest_idx, source_ptr.get(source_idx + 2));
							dest_ptr.set(dest_idx + 1, source_ptr.get(source_idx + 1));
							dest_ptr.set(dest_idx + 2, source_ptr.get(source_idx));
							dest_idx += 3;
							source_idx += 3;
						}
					}
					else {
						for (var i = 0; i<hr.indexCount; i++) {
							result.indices.set(i, hr.indices.get(i));
						}
					}
				}
				else {
					result.polygons = true;
					result.numOutputVertices = ovcount[0];
					MiscUtil.resize(result.outputVertices, ovcount[0], v3.class);
					result.numFaces = hr.faceCount;
					result.numIndices = hr.indexCount + hr.faceCount;
					MiscUtil.resize(result.indices, result.numIndices, 0);
					for (var i = 0; i<ovcount[0]; i++) {
						
						
						result.outputVertices.get(i).set(vertexScratch.get(i));
					}


					var source_ptr = hr.indices;
					var source_idx = 0;

					var dest_ptr = result.indices;
					var dest_idx = 0;

                    for (var i = 0; i<hr.faceCount; i++) {
                        dest_ptr.set(dest_idx, 3);
                        if (desc.hasHullFlag(HullFlags.REVERSE_ORDER)) {
                            dest_ptr.set(dest_idx + 1, source_ptr.get(source_idx + 2));
                            dest_ptr.set(dest_idx + 2, source_ptr.get(source_idx + 1));
                            dest_ptr.set(dest_idx + 3, source_ptr.get(source_idx));
                        }
                        else {
                            dest_ptr.set(dest_idx + 1, source_ptr.get(source_idx));
                            dest_ptr.set(dest_idx + 2, source_ptr.get(source_idx + 1));
                            dest_ptr.set(dest_idx + 3, source_ptr.get(source_idx + 2));
                        }

                        dest_idx += 4;
                        source_idx += 3;
                    }
                }
				releaseHull(hr);
			}
		}

		return ret;
	}

	/**
	 * Release memory allocated for this result, we are done with it.
	 */
	public static boolean releaseResult(HullResult result) {
		if (!result.outputVertices.isEmpty()) {
			result.numOutputVertices = 0;
			result.outputVertices.clear();
		}
		if (result.indices.size() != 0) {
			result.numIndices = 0;
			result.indices.clear();
		}
		return true;
	}

	private boolean computeHull(int vcount, FasterList<v3> vertices, PHullResult result, int vlimit) {
		var tris_count = new int[1];
		var ret = calchull(vertices, vcount, result.indices, tris_count, vlimit);
		if (ret == 0) return false;
		result.indexCount = tris_count[0] * 3;
		result.faceCount = tris_count[0];
		result.vertices = vertices;
		result.vcount = vcount;
		return true;
	}

	private Tri allocateTriangle(int a, int b, int c) {
		var tr = new Tri(a, b, c);
		tr.id = tris.size();
		tris.add(tr);

		return tr;
	}

	private void deAllocateTriangle(Tri tri) {
		
		assert (tris.get(tri.id) == tri);
		tris.setFast(tri.id, null);
	}

	private void b2bfix(Tri s, Tri t) {
		for (var i = 0; i<3; i++) {
			var i1 = (i + 1) % 3;
			var i2 = (i + 2) % 3;
			var a = s.getCoord(i1);
			var b = s.getCoord(i2);
			
			assert (tris.get(s.neib(a, b).get()).neib(b, a).get() == s.id);
			
			assert (tris.get(t.neib(a, b).get()).neib(b, a).get() == t.id);
			
			tris.get(s.neib(a, b).get()).neib(b, a).set(t.neib(b, a).get());
			
			tris.get(t.neib(b, a).get()).neib(a, b).set(s.neib(a, b).get());
		}
	}

	private void removeb2b(Tri s, Tri t) {
		b2bfix(s, t);
		deAllocateTriangle(s);

		deAllocateTriangle(t);
	}

	private void checkit(Tri t) {
		
		assert (tris.get(t.id) == t);
		for (var i = 0; i<3; i++) {
			var i1 = (i + 1) % 3;
			var i2 = (i + 2) % 3;
			var a = t.getCoord(i1);
			var b = t.getCoord(i2);

			assert (a != b);
			
			assert (tris.get(t.n.getCoord(i)).neib(b, a).get() == t.id);
		}
	}

	private Tri extrudable(float epsilon) {
		Tri t = null;
        for (var tri : tris) {


            if (t == null || (tri != null && t.rise < tri.rise)) {

                t = tri;
            }
        }
		return (t.rise > epsilon) ? t : null;
	}

	private int calchull(FasterList<v3> verts, int verts_count, IntArrayList tris_out, int[] tris_count, int vlimit) {
		var rc = calchullgen(verts, verts_count, vlimit);
		if (rc == 0) return 0;

		var ts = new IntArrayList();

        for (var tri : tris) {

            if (tri != null) {
                for (var j = 0; j < 3; j++) {

                    ts.add(tri.getCoord(j));
                }

                deAllocateTriangle(tri);
            }
        }
		tris_count[0] = ts.size() / 3;
		MiscUtil.resize(tris_out, ts.size(), 0);

		for (var i = 0; i<ts.size(); i++) {
			tris_out.set(i, ts.get(i));
		}
		MiscUtil.resize(tris, 0, Tri.class);

		return 1;
	}

	private int calchullgen(FasterList<v3> verts, int verts_count, int vlimit) {
		if (verts_count < 4) return 0;

		var tmp = new v3();
		var tmp1 = new v3();
		var tmp2 = new v3();

		if (vlimit == 0) {
			vlimit = 1000000000;
		}


		var bmin = new v3(verts.get(0));

		var bmax = new v3(verts.get(0));
		var isextreme = new IntArrayList();

		var allow = new IntArrayList();
		

		for (var j = 0; j<verts_count; j++) {
			allow.add(1);
			isextreme.add(0);
			
			VectorUtil.setMin(bmin, verts.get(j));
			
			VectorUtil.setMax(bmax, verts.get(j));
		}
		tmp.sub(bmax, bmin);
		var epsilon = tmp.length() * 0.001f;
		assert (epsilon != 0f);

		var p = findSimplex(verts, verts_count, allow, new Int4());
		if (p.x == -1) {
			return 0; 

		
		}
		var center = new v3();
		
		
		
		
		VectorUtil.add(center, verts.get(p.getCoord(0)), verts.get(p.getCoord(1)), verts.get(p.getCoord(2)), verts.get(p.getCoord(3)));
		center.scaled(1f / 4f);

		var t0 = allocateTriangle(p.getCoord(2), p.getCoord(3), p.getCoord(1));
		t0.n.set(2, 3, 1);
		var t1 = allocateTriangle(p.getCoord(3), p.getCoord(2), p.getCoord(0));
		t1.n.set(3, 2, 0);
		var t2 = allocateTriangle(p.getCoord(0), p.getCoord(1), p.getCoord(3));
		t2.n.set(0, 1, 3);
		var t3 = allocateTriangle(p.getCoord(1), p.getCoord(0), p.getCoord(2));
		t3.n.set(1, 0, 2);
		isextreme.set(p.getCoord(0), 1);
		isextreme.set(p.getCoord(1), 1);
		isextreme.set(p.getCoord(2), 1);
		isextreme.set(p.getCoord(3), 1);
		checkit(t0);
		checkit(t1);
		checkit(t2);
		checkit(t3);

		var n = new v3();

        for (var t : tris) {

            assert (t != null);
            assert (t.vmax < 0);


            triNormal(verts.get(t.getCoord(0)), verts.get(t.getCoord(1)), verts.get(t.getCoord(2)), n);
            t.vmax = maxdirsterid(verts, verts_count, n, allow);


            tmp.sub(verts.get(t.vmax), verts.get(t.getCoord(0)));
            t.rise = n.dot(tmp);
        }
        vlimit -= 4;
        Tri te;
        while (vlimit > 0 && ((te = extrudable(epsilon)) != null)) {
			Int3 ti = te;
			var v = te.vmax;
			assert (v != -1);
			assert (isextreme.get(v) == 0);  
			isextreme.set(v, 1);

			var j = tris.size();
			while ((j--) != 0) {
				
				if (tris.get(j) == null) {
					continue;
				}
				
				Int3 t = tris.get(j);
				
				if (above(verts, t, verts.get(v), 0.01f * epsilon)) {
					
					extrude(tris.get(j), v);
				}
			}
			
			j = tris.size();
			while ((j--) != 0) {
				
				if (tris.get(j) == null) {
					continue;
				}
				
				if (!hasvert(tris.get(j), v)) {
					break;
				}
				
				Int3 nt = tris.get(j);
				
				
				tmp1.sub(verts.get(nt.getCoord(1)), verts.get(nt.getCoord(0)));
				
				
				tmp2.sub(verts.get(nt.getCoord(2)), verts.get(nt.getCoord(1)));
				tmp.cross(tmp1, tmp2);
				if (above(verts, nt, center, 0.01f * epsilon) || tmp.length() < epsilon * epsilon * 0.1f) {


					var nb = tris.get(tris.get(j).n.getCoord(0));
					assert (nb != null);
					assert (!hasvert(nb, v));
					assert (nb.id < j);
					extrude(nb, v);
					j = tris.size();
				}
			}
			j = tris.size();
			while ((j--) != 0) {

				var t = tris.get(j);
				if (t == null) {
					continue;
				}
				if (t.vmax >= 0) {
					break;
				}
				
				
				
				triNormal(verts.get(t.getCoord(0)), verts.get(t.getCoord(1)), verts.get(t.getCoord(2)), n);
				t.vmax = maxdirsterid(verts, verts_count, n, allow);
				if (isextreme.get(t.vmax) != 0) {
					t.vmax = -1; 
				}
				else {
					
					
					tmp.sub(verts.get(t.vmax), verts.get(t.getCoord(0)));
					t.rise = n.dot(tmp);
				}
			}
			vlimit--;
		}
		return 1;
	}

	private static Int4 findSimplex(FasterList<v3> verts, int verts_count, IntArrayList allow, Int4 out) {
		var tmp = new v3();
		var tmp1 = new v3();
		var tmp2 = new v3();

		v3[] basis = { new v3(), new v3(), new v3() };
		basis[0].set(0.01f, 0.02f, 1.0f);
		var p0 = maxdirsterid(verts, verts_count, basis[0], allow);
		tmp.negated(basis[0]);
		var p1 = maxdirsterid(verts, verts_count, tmp, allow);
		
		
		basis[0].sub(verts.get(p0), verts.get(p1));
		if (p0 == p1 || (basis[0].x == 0f && basis[0].y == 0f && basis[0].z == 0f)) {
			out.set(-1, -1, -1, -1);
			return out;
		}
		tmp.set(1f, 0.02f, 0f);
		basis[1].cross(tmp, basis[0]);
		tmp.set(-0.02f, 1f, 0f);
		basis[2].cross(tmp, basis[0]);
		if (!(basis[1].length() > basis[2].length())) {
			basis[1].set(basis[2]);
		}
		basis[1].normalize();
		var p2 = maxdirsterid(verts, verts_count, basis[1], allow);
		if (p2 == p0 || p2 == p1) {
			tmp.negated(basis[1]);
			p2 = maxdirsterid(verts, verts_count, tmp, allow);
		}
		if (p2 == p0 || p2 == p1) {
			out.set(-1, -1, -1, -1);
			return out;
		}
		
		
		basis[1].sub(verts.get(p2), verts.get(p0));
		basis[2].cross(basis[1], basis[0]);
		basis[2].normalize();
		var p3 = maxdirsterid(verts, verts_count, basis[2], allow);
		if (p3 == p0 || p3 == p1 || p3 == p2) {
			tmp.negated(basis[2]);
			p3 = maxdirsterid(verts, verts_count, tmp, allow);
		}
		if (p3 == p0 || p3 == p1 || p3 == p2) {
			out.set(-1, -1, -1, -1);
			return out;
		}
		assert (!(p0 == p1 || p0 == p2 || p0 == p3 || p1 == p2 || p1 == p3 || p2 == p3));

		
		
		tmp1.sub(verts.get(p1), verts.get(p0));
		
		
		tmp2.sub(verts.get(p2), verts.get(p0));
		tmp2.cross(tmp1, tmp2);
		
		
		tmp1.sub(verts.get(p3), verts.get(p0));
		if (tmp1.dot(tmp2) < 0) {
			var swap_tmp = p2;
			p2 = p3;
			p3 = swap_tmp;
		}
		out.set(p0, p1, p2, p3);
		return out;
	}

	

	private void extrude(Tri t0, int v) {
		var t = new Int3(t0);
		var n = tris.size();
		var ta = allocateTriangle(v, t.getCoord(1), t.getCoord(2));
		ta.n.set(t0.n.getCoord(0), n + 1, n + 2);
		
		tris.get(t0.n.getCoord(0)).neib(t.getCoord(1), t.getCoord(2)).set(n);
		var tb = allocateTriangle(v, t.getCoord(2), t.getCoord(0));
		tb.n.set(t0.n.getCoord(1), n + 2, n);
		
		tris.get(t0.n.getCoord(1)).neib(t.getCoord(2), t.getCoord(0)).set(n + 1);
		var tc = allocateTriangle(v, t.getCoord(0), t.getCoord(1));
		tc.n.set(t0.n.getCoord(2), n, n + 1);
		
		tris.get(t0.n.getCoord(2)).neib(t.getCoord(0), t.getCoord(1)).set(n + 2);
		checkit(ta);
		checkit(tb);
		checkit(tc);
		
		if (hasvert(tris.get(ta.n.getCoord(0)), v)) {
			
			removeb2b(ta, tris.get(ta.n.getCoord(0)));
		}
		
		if (hasvert(tris.get(tb.n.getCoord(0)), v)) {
			
			removeb2b(tb, tris.get(tb.n.getCoord(0)));
		}
		
		if (hasvert(tris.get(tc.n.getCoord(0)), v)) {
			
			removeb2b(tc, tris.get(tc.n.getCoord(0)));
		}
		deAllocateTriangle(t0);
	}

	

	
	
	
	
	private void bringOutYourDead(FasterList<v3> verts, int vcount, FasterList<v3> overts, int[] ocount, IntArrayList indices, int indexcount) {
		var vs = vertexIndexMapping.size();
		var tmpIndices = new IntArrayList(vs);
		for (var i = 0; i< vs; i++) {
			tmpIndices.add(vs);
		}

		var usedIndices = new IntArrayList();
		MiscUtil.resize(usedIndices, vcount, 0);
		/*
		JAVA NOTE: redudant
		for (int i=0; i<vcount; i++) {
		usedIndices.setAt(i, 0);
		}
		*/

		ocount[0] = 0;

		for (var i = 0; i<indexcount; i++) {
			var v = indices.get(i);

			assert (v >= 0 && v < vcount);

			if (usedIndices.get(v) != 0) { 
				indices.set(i, usedIndices.get(v) - 1); 
			}
			else {
				indices.set(i, ocount[0]);      

				
				
				overts.get(ocount[0]).set(verts.get(v)); 

				for (var k = 0; k < vertexIndexMapping.size(); k++) {
					if (tmpIndices.get(k) == v) {
						vertexIndexMapping.set(k, ocount[0]);
					}
				}

				ocount[0]++; 

				assert (ocount[0] >= 0 && ocount[0] <= vcount);

				usedIndices.set(v, ocount[0]); 
			}
		}
	}

	private static final float EPSILON = 0.000001f; /* close enough to consider two btScalaring point numbers to be 'the same'. */

	private boolean cleanupVertices(int svcount,
                                    FasterList<v3> svertices,
                                    int stride,
                                    int[] vcount, 
                                    FasterList<v3> vertices, 
                                    float normalepsilon,
                                    v3 scale) {

		if (svcount == 0) {
			return false;
		}

		vertexIndexMapping.clear();

		vcount[0] = 0;

        if (scale != null) {
			scale.set(1, 1, 1);
		}

		float[] bmin = { Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE };
		float[] bmax = { -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE };

		var vtx_ptr = svertices;
		var vtx_idx = 0;

		
        for (var i = 0; i<svcount; i++) {

			var p = vtx_ptr.get(vtx_idx);

            vtx_idx +=/*stride*/ 1;

            for (var j = 0; j<3; j++) {
                if (VectorUtil.coord(p, j) < bmin[j]) {
                    bmin[j] = VectorUtil.coord(p, j);
                }
                if (VectorUtil.coord(p, j) > bmax[j]) {
                    bmax[j] = VectorUtil.coord(p, j);
                }
            }
        }

		var dx = bmax[0] - bmin[0];
		var dy = bmax[1] - bmin[1];
		var dz = bmax[2] - bmin[2];

		var center = new v3();

		center.x = dx * 0.5f + bmin[0];
		center.y = dy * 0.5f + bmin[1];
		center.z = dz * 0.5f + bmin[2];

		if (dx < EPSILON || dy < EPSILON || dz < EPSILON || svcount < 3) {

			var len = Float.MAX_VALUE;

			if (dx > EPSILON && dx < len) len = dx;
			if (dy > EPSILON && dy < len) len = dy;
			if (dz > EPSILON && dz < len) len = dz;

			if (len == Float.MAX_VALUE) {
				dx = dy = dz = 0.01f; 
			}
			else {
				if (dx < EPSILON) dx = len * 0.05f; 
				if (dy < EPSILON) dy = len * 0.05f;
				if (dz < EPSILON) dz = len * 0.05f;
			}

			var x1 = center.x - dx;
			var x2 = center.x + dx;

			var y1 = center.y - dy;
			var y2 = center.y + dy;

			var z1 = center.z - dz;
			var z2 = center.z + dz;

			addPoint(vcount, vertices, x1, y1, z1);
			addPoint(vcount, vertices, x2, y1, z1);
			addPoint(vcount, vertices, x2, y2, z1);
			addPoint(vcount, vertices, x1, y2, z1);
			addPoint(vcount, vertices, x1, y1, z2);
			addPoint(vcount, vertices, x2, y1, z2);
			addPoint(vcount, vertices, x2, y2, z2);
			addPoint(vcount, vertices, x1, y2, z2);

			return true; 
		}
		var recip = new float[3];
        if (scale != null) {
            scale.x = dx;
            scale.y = dy;
            scale.z = dz;

            recip[0] = 1f / dx;
            recip[1] = 1f / dy;
            recip[2] = 1f / dz;

            center.x *= recip[0];
            center.y *= recip[1];
            center.z *= recip[2];
        }

        vtx_ptr = svertices;
		vtx_idx = 0;

		for (var i = 0; i<svcount; i++) {

			var p = vtx_ptr.get(vtx_idx);
			vtx_idx +=/*stride*/ 1;

			var px = p.x;
			var py = p.y;
			var pz = p.z;

			if (scale != null) {
				px *= recip[0];
				py *= recip[1];
				pz *= recip[2];
			}

			
			int j;

			for (j=0; j<vcount[0]; j++) {


				var v = vertices.get(j);

				var x = v.x;
				var y = v.y;
				var z = v.z;

                dx = Math.abs(x - px);
                dy = Math.abs(y - py);
                dz = Math.abs(z - pz);

                if (dx < normalepsilon && dy < normalepsilon && dz < normalepsilon) {


					var dist1 = getDist(px, py, pz, center);
					var dist2 = getDist(v.x, v.y, v.z, center);

                    if (dist1 > dist2) {
                        v.x = px;
                        v.y = py;
                        v.z = pz;
                    }

                    break;
                }
            }

			if (j == vcount[0]) {

				var dest = vertices.get(vcount[0]);
                dest.x = px;
                dest.y = py;
                dest.z = pz;
                vcount[0]++;
            }

			vertexIndexMapping.add(j);
		}

		
		
		bmin = new float[] { Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE };
		bmax = new float[] { -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE };

		for (var i = 0; i<vcount[0]; i++) {

			var p = vertices.get(i);
            for (var j = 0; j < 3; j++) {
                if (VectorUtil.coord(p, j) < bmin[j]) {
                    bmin[j] = VectorUtil.coord(p, j);
                }
                if (VectorUtil.coord(p, j) > bmax[j]) {
                    bmax[j] = VectorUtil.coord(p, j);
                }
            }
        }

		dx = bmax[0] - bmin[0];
		dy = bmax[1] - bmin[1];
		dz = bmax[2] - bmin[2];

		if (dx < EPSILON || dy < EPSILON || dz < EPSILON || vcount[0] < 3) {
			var cx = dx * 0.5f + bmin[0];
			var cy = dy * 0.5f + bmin[1];
			var cz = dz * 0.5f + bmin[2];

			var len = Float.MAX_VALUE;

            if (dx >= EPSILON && dx < len) len = dx;
            if (dy >= EPSILON && dy < len) len = dy;
            if (dz >= EPSILON && dz < len) len = dz;

            if (len == Float.MAX_VALUE) {
                dx = dy = dz = 0.01f; 
            }
            else {
                if (dx < EPSILON) dx = len * 0.05f; 
                if (dy < EPSILON) dy = len * 0.05f;
                if (dz < EPSILON) dz = len * 0.05f;
            }

			var x1 = cx - dx;
			var x2 = cx + dx;

			var y1 = cy - dy;
			var y2 = cy + dy;

			var z1 = cz - dz;
			var z2 = cz + dz;

            vcount[0] = 0; 

            addPoint(vcount, vertices, x1, y1, z1);
            addPoint(vcount, vertices, x2, y1, z1);
            addPoint(vcount, vertices, x2, y2, z1);
            addPoint(vcount, vertices, x1, y2, z1);
            addPoint(vcount, vertices, x1, y1, z2);
            addPoint(vcount, vertices, x2, y1, z2);
            addPoint(vcount, vertices, x2, y2, z2);
            addPoint(vcount, vertices, x1, y2, z2);

            return true;
        }

		return true;
	}

	

	private static boolean hasvert(Int3 t, int v) {
        for (var i : new int[]{0, 1, 2}) {
            if (t.getCoord(i) == v) {
                return (true);
            }
        }
        return (false);
	}

	private static v3 orth(v3 v, v3 out) {
		var a = new v3();
		a.set(0f, 0f, 1f);
		a.cross(v, a);

		var b = new v3();
		b.set(0f, 1f, 0f);
		b.cross(v, b);

		if (a.length() > b.length()) {
			out.normalize(a);
		}
		else {
			out.normalize(b);
		}
		return out;
	}

	private static int maxdirfiltered(FasterList<v3> p, int count, v3 dir, IntArrayList allow) {
		assert (count != 0);
		var m = -1;
		for (var i = 0; i<count; i++) {
			if (allow.get(i) != 0) {
				
				
				if (m == -1 || p.get(i).dot(dir) > p.get(m).dot(dir)) {
					m = i;
				}
			}
		}
		assert (m != -1);
		return m;
	}

	private static int maxdirsterid(FasterList<v3> p, int count, v3 dir, IntArrayList allow) {
		var tmp = new v3();
		var tmp1 = new v3();
		var tmp2 = new v3();
		var u = new v3();
		var v = new v3();

		var m = -1;
		while (m == -1) {
			m = maxdirfiltered(p, count, dir, allow);
			if (allow.get(m) == 3) {
				return m;
			}
			orth(dir, u);
			v.cross(u, dir);
			var ma = -1;
			for (var x = 0f; x <= 360f; x += 45f) {
				var s = (float) Math.sin(BulletGlobals.SIMD_RADS_PER_DEG * (x));
				var c = (float) Math.cos(BulletGlobals.SIMD_RADS_PER_DEG * (x));

				tmp1.scale(s, u);
				tmp2.scale(c, v);
				tmp.add(tmp1, tmp2);
				tmp.scaled(0.025f);
				tmp.add(dir);
				var mb = maxdirfiltered(p, count, tmp, allow);
				if (ma == m && mb == m) {
					allow.set(m, 3);
					return m;
				}
				if (ma != -1 && ma != mb) {
					var mc = ma;
					for (var xx = x - 40f; xx <= x; xx += 5f) {
						s = (float) Math.sin(BulletGlobals.SIMD_RADS_PER_DEG * (xx));
						c = (float) Math.cos(BulletGlobals.SIMD_RADS_PER_DEG * (xx));

						tmp1.scale(s, u);
						tmp2.scale(c, v);
						tmp.add(tmp1, tmp2);
						tmp.scaled(0.025f);
						tmp.add(dir);

						var md = maxdirfiltered(p, count, tmp, allow);
						if (mc == m && md == m) {
							allow.set(m, 3);
							return m;
						}
						mc = md;
					}
				}
				ma = mb;
			}
			allow.set(m, 0);
			m = -1;
		}
		assert (false);
		return m;
	}

	private static v3 triNormal(v3 v0, v3 v1, v3 v2, v3 out) {
		var tmp1 = new v3();
		var tmp2 = new v3();

		
		
		tmp1.sub(v1, v0);
		tmp2.sub(v2, v1);
		var cp = new v3();
		cp.cross(tmp1, tmp2);
		var m = cp.length();
		if (m == 0) {
			out.set(1f, 0f, 0f);
			return out;
		}
		out.scale(1f / m, cp);
		return out;
	}

	private static boolean above(FasterList<v3> vertices, Int3 t, v3 p, float epsilon) {


		var n = triNormal(vertices.get(t.getCoord(0)), vertices.get(t.getCoord(1)), vertices.get(t.getCoord(2)), new v3());
		var tmp = new v3();
		
		tmp.sub(p, vertices.get(t.getCoord(0)));
		return (n.dot(tmp) > epsilon); 
	}

	private static void releaseHull(PHullResult result) {
		if (result.indices.size() != 0) {
			result.indices.clear();
		}

		result.vcount = 0;
		result.indexCount = 0;
		result.vertices = null;
	}
	
	private static void addPoint(int[] vcount, FasterList<v3> p, float x, float y, float z) {


		var dest = p.get(vcount[0]);
		dest.x = x;
		dest.y = y;
		dest.z = z;
		vcount[0]++;
	}
	
	private static float getDist(float px, float py, float pz, v3 p2) {
		var dx = px - p2.x;
		var dy = py - p2.y;
		var dz = pz - p2.z;

		return dx*dx + dy*dy + dz*dz;
	}
	
}
