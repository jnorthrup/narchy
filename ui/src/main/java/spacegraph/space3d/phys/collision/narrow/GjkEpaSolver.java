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

package spacegraph.space3d.phys.collision.narrow;

import jcog.math.v3;
import spacegraph.space3d.phys.BulletGlobals;
import spacegraph.space3d.phys.math.MatrixUtil;
import spacegraph.space3d.phys.math.QuaternionUtil;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.math.VectorUtil;
import spacegraph.space3d.phys.shape.ConvexShape;
import spacegraph.util.math.Matrix3f;
import spacegraph.util.math.Quat4f;

import java.util.Arrays;
import java.util.stream.Stream;

/*
GJK-EPA collision solver by Nathanael Presson
Nov.2006
*/

/**
 * GjkEpaSolver contributed under zlib by Nathanael Presson.
 * 
 * @author jezek2
 */
public class GjkEpaSolver {



	public enum ResultsStatus {
		Separated,		/* Shapes doesnt penetrate												*/ 
		Penetrating,	/* Shapes are penetrating												*/ 
		GJK_Failed,		/* GJK phase fail, no big issue, shapes are probably just 'touching'	*/ 
		EPA_Failed,		/* EPA phase fail, bigger problem, need to save parameters, and debug	*/ 
	}
	
	public static class Results {
		ResultsStatus status;
		
		public final v3 witness0 = new v3();
		public final v3 witness1 = new v3();
		final v3 normal = new v3();
		float depth;
		int epa_iterations;
		int gjk_iterations;
	}
	
	
	
	private static final float cstInf = BulletGlobals.SIMD_INFINITY;
	private static final float cstPi = BulletGlobals.SIMD_PI;
	private static final float cst2Pi = BulletGlobals.SIMD_2_PI;
	private static final int GJK_maxiterations = 128;
	private static final int GJK_hashsize = 1 << 6;
	private static final int GJK_hashmask = GJK_hashsize - 1;
	private static final float GJK_insimplex_eps = 0.0001f;
	private static final float GJK_sqinsimplex_eps = GJK_insimplex_eps * GJK_insimplex_eps;
	private static final int EPA_maxiterations = 256;
	private static final float EPA_inface_eps = 0.01f;
	private static final float EPA_accuracy = 0.001f;
	
	

	public static class Mkv {
		final v3 w = new v3();
		final v3 r = new v3();

		void set(Mkv m) {
			w.set(m.w);
			r.set(m.r);
		}
	}

	public static class He {
		final v3 v = new v3();
		He n;
	}
	
	protected static class GJK {
		
		
		
		
		final He[] table = new He[GJK_hashsize];
		final Matrix3f[] wrotations/*[2]*/ = { new Matrix3f(), new Matrix3f() };
		final v3[] positions/*[2]*/ = { new v3(), new v3() };
		final ConvexShape[] shapes = new ConvexShape[2];
		final Mkv[] simplex = new Mkv[5];
		final v3 ray = new v3();
		/*unsigned*/ int order;
		/*unsigned*/ int iterations;
		float margin;
		boolean failed;
		
		{
			for (var i = 0; i<simplex.length; i++) simplex[i] = new Mkv();
		}

		GJK() {
		}

		public GJK(/*StackAlloc psa,*/
				Matrix3f wrot0, v3 pos0, ConvexShape shape0,
                   Matrix3f wrot1, v3 pos1, ConvexShape shape1) {
			this(wrot0, pos0, shape0, wrot1, pos1, shape1, 0f);
		}

		GJK(/*StackAlloc psa,*/
                Matrix3f wrot0, v3 pos0, ConvexShape shape0,
                Matrix3f wrot1, v3 pos1, ConvexShape shape1,
                float pmargin) {
			init(wrot0, pos0, shape0, wrot1, pos1, shape1, pmargin);
		}
		
		void init(/*StackAlloc psa,*/
                Matrix3f wrot0, v3 pos0, ConvexShape shape0,
                Matrix3f wrot1, v3 pos1, ConvexShape shape1,
                float pmargin) {
			wrotations[0].set(wrot0);
			positions[0].set(pos0);
			shapes[0] = shape0;
			wrotations[1].set(wrot1);
			positions[1].set(pos1);
			shapes[1] = shape1;
			
			
			margin = pmargin;
			failed = false;
		}
		
		void destroy() {
		}
		
		
		static /*unsigned*/ int Hash(v3 v) {
			var h = (int)(v.x * 15461) ^ (int)(v.y * 83003) ^ (int)(v.z * 15473);
			return (h * 169639) & GJK_hashmask;
		}

		v3 LocalSupport(v3 d, /*unsigned*/ int i, v3 out) {
			var tmp = new v3();
			MatrixUtil.transposeTransform(tmp, d, wrotations[i]);

			shapes[i].localGetSupportingVertex(tmp, out);
			wrotations[i].transform(out);
			out.add(positions[i]);

			return out;
		}
		
		void Support(v3 d, Mkv v) {
			v.r.set(d);

			var tmp1 = LocalSupport(d, 0, new v3());

			var tmp = new v3();
			tmp.set(d);
			tmp.negated();
			var tmp2 = LocalSupport(tmp, 1, new v3());

			v.w.sub(tmp1, tmp2);
			v.w.scaleAdd(margin, d, v.w);
		}

		boolean FetchSupport() {
			var h = Hash(ray);
			var e = table[h];
			while (e != null) {
				if (e.v.equals(ray)) {
					--order;
					return false;
				}
				else {
					e = e.n;
				}
			}
			
			e = new He();
			e.v.set(ray);
			e.n = table[h];
			table[h] = e;
			Support(ray, simplex[++order]);
			return (ray.dot(simplex[order].w) > 0);
		}

		boolean SolveSimplex2(v3 ao, v3 ab) {
			if (ab.dot(ao) >= 0) {
				var cabo = new v3();
				cabo.cross(ab, ao);
				if (cabo.lengthSquared() > GJK_sqinsimplex_eps) {
					ray.cross(cabo, ab);
				}
				else {
					return true;
				}
			}
			else {
				order = 0;
				simplex[0].set(simplex[1]);
				ray.set(ao);
			}
			return (false);
		}

		boolean SolveSimplex3(v3 ao, v3 ab, v3 ac)
		{
			var tmp = new v3();
			tmp.cross(ab, ac);
			return (SolveSimplex3a(ao,ab,ac,tmp));
		}
		
		boolean SolveSimplex3a(v3 ao, v3 ab, v3 ac, v3 cabc) {


			var tmp = new v3();
			tmp.cross(cabc, ab);

			var tmp2 = new v3();
			tmp2.cross(cabc, ac);

			if (tmp.dot(ao) < -GJK_insimplex_eps) {
				order = 1;
				simplex[0].set(simplex[1]);
				simplex[1].set(simplex[2]);
				return SolveSimplex2(ao, ab);
			}
			else if (tmp2.dot(ao) > +GJK_insimplex_eps) {
				order = 1;
				simplex[1].set(simplex[2]);
				return SolveSimplex2(ao, ac);
			}
			else {
				var d = cabc.dot(ao);
				if (Math.abs(d) > GJK_insimplex_eps) {
					if (d > 0) {
						ray.set(cabc);
					}
					else {
						ray.negated(cabc);

						var swapTmp = new Mkv();
						swapTmp.set(simplex[0]);
						simplex[0].set(simplex[1]);
						simplex[1].set(swapTmp);
					}
					return false;
				}
				else {
					return true;
				}
			}
		}
		
		boolean SolveSimplex4(v3 ao, v3 ab, v3 ac, v3 ad) {


			var crs = new v3();

			var tmp = new v3();
			tmp.cross(ab, ac);

			var tmp2 = new v3();
			tmp2.cross(ac, ad);

			var tmp3 = new v3();
			tmp3.cross(ad, ab);

			if (tmp.dot(ao) > GJK_insimplex_eps) {
				crs.set(tmp);
				order = 2;
				simplex[0].set(simplex[1]);
				simplex[1].set(simplex[2]);
				simplex[2].set(simplex[3]);
				return SolveSimplex3a(ao, ab, ac, crs);
			}
			else if (tmp2.dot(ao) > GJK_insimplex_eps) {
				crs.set(tmp2);
				order = 2;
				simplex[2].set(simplex[3]);
				return SolveSimplex3a(ao, ac, ad, crs);
			}
			else if (tmp3.dot(ao) > GJK_insimplex_eps) {
				crs.set(tmp3);
				order = 2;
				simplex[1].set(simplex[0]);
				simplex[0].set(simplex[2]);
				simplex[2].set(simplex[3]);
				return SolveSimplex3a(ao, ad, ab, crs);
			}
			else {
				return (true);
			}
		}
		
		boolean SearchOrigin() {
			var tmp = new v3();
			tmp.set(1f, 0f, 0f);
			return SearchOrigin(tmp);
		}
		
		boolean SearchOrigin(v3 initray) {
			var tmp1 = new v3();
			var tmp2 = new v3();
			var tmp3 = new v3();
			var tmp4 = new v3();

			iterations = 0;
			order = -1;
			failed = false;
			ray.set(initray);
			ray.normalize();

			Arrays.fill(table, null);

			FetchSupport();
			ray.negated(simplex[0].w);
			for (; iterations < GJK_maxiterations; ++iterations) {
				var rl = ray.length();
				ray.scaled(1f / (rl > 0f ? rl : 1f));
				if (FetchSupport()) {
					var found = false;
					switch (order) {
						case 1:
                            tmp1.negated(simplex[1].w);
                            tmp2.sub(simplex[0].w, simplex[1].w);
                            found = SolveSimplex2(tmp1, tmp2);
                            break;
                        case 2:
                            tmp1.negated(simplex[2].w);
                            tmp2.sub(simplex[1].w, simplex[2].w);
                            tmp3.sub(simplex[0].w, simplex[2].w);
                            found = SolveSimplex3(tmp1, tmp2, tmp3);
                            break;
                        case 3:
                            tmp1.negated(simplex[3].w);
                            tmp2.sub(simplex[2].w, simplex[3].w);
                            tmp3.sub(simplex[1].w, simplex[3].w);
                            tmp4.sub(simplex[0].w, simplex[3].w);
                            found = SolveSimplex4(tmp1, tmp2, tmp3, tmp4);
                            break;
                    }
					if (found) {
						return true;
					}
				}
				else {
					return false;
				}
			}
			failed = true;
			return false;
		}
		
		boolean EncloseOrigin() {
			var tmp = new v3();
			var tmp1 = new v3();
			var tmp2 = new v3();

			switch (order) {
				
				case 0:
					break;
				
				case 1:
					var ab = new v3();
                    ab.sub(simplex[1].w, simplex[0].w);

                    v3[] b = { new v3(), new v3(), new v3() };
                    b[0].set(1f, 0f, 0f);
                    b[1].set(0f, 1f, 0f);
                    b[2].set(0f, 0f, 1f);

                    b[0].cross(ab, b[0]);
                    b[1].cross(ab, b[1]);
                    b[2].cross(ab, b[2]);

                    float[] m = {b[0].lengthSquared(), b[1].lengthSquared(), b[2].lengthSquared()};

					var tmpQuat = new Quat4f();
                    tmp.normalize(ab);
                    QuaternionUtil.setRotation(tmpQuat, tmp, cst2Pi / 3f);

					var r = new Matrix3f();
                    MatrixUtil.setRotation(r, tmpQuat);

					var w = new v3();
                    w.set(b[m[0] > m[1] ? m[0] > m[2] ? 0 : 2 : m[1] > m[2] ? 1 : 2]);

                    tmp.normalize(w);
                    Support(tmp, simplex[4]);
                    r.transform(w);
                    tmp.normalize(w);
                    Support(tmp, simplex[2]);
                    r.transform(w);
                    tmp.normalize(w);
                    Support(tmp, simplex[3]);
                    r.transform(w);
                    order = 4;
                    return (true);
                
				case 2:
                    tmp1.sub(simplex[1].w, simplex[0].w);
                    tmp2.sub(simplex[2].w, simplex[0].w);
					var n = new v3();
                    n.cross(tmp1, tmp2);
                    n.normalize();

                    Support(n, simplex[3]);

                    tmp.negated(n);
                    Support(tmp, simplex[4]);
                    order = 4;
                    return (true);
                
				case 3:

				case 4:
					return (true);
			}
			return (false);
		}
		
	}

	

	private static final int[] mod3 = { 0, 1, 2, 0, 1 };

	private static final int[][] tetrahedron_fidx/*[4][3]*/ = {{2,1,0},{3,0,1},{3,1,2},{3,2,0}};
	private static final int[][] tetrahedron_eidx/*[6][4]*/ = {{0,0,2,1},{0,1,1,1},{0,2,3,1},{1,0,3,2},{2,0,1,2},{3,0,2,2}};

	private static final int[][] hexahedron_fidx/*[6][3]*/ = {{2,0,4},{4,1,2},{1,4,0},{0,3,1},{0,2,3},{1,3,2}};
	private static final int[][] hexahedron_eidx/*[9][4]*/ = {{0,0,4,0},{0,1,2,1},{0,2,1,2},{1,1,5,2},{1,0,2,0},{2,2,3,2},{3,1,5,0},{3,0,4,2},{5,1,4,1}};

	public static class Face {
		final Mkv[] v = new Mkv[3];
		final Face[] f = new Face[3];
		final int[] e = new int[3];
		final v3 n = new v3();
		float d;
		int mark;
		Face prev;
		Face next;
	}
	
	static class EPA {
		
		
		GJK gjk;
		
		Face root;
		int nfaces;
		int iterations;
		final v3[][] features = new v3[2][3];
		final v3[] nearest/*[2]*/ = { new v3(), new v3() };
		final v3 normal = new v3();
		float depth;
		boolean failed;
		
		{
			for (var i = 0; i<features.length; i++) {
				for (var j = 0; j<features[i].length; j++) {
					features[i][j] = new v3();
				}
			}
		}

		EPA(GJK pgjk) {
			gjk = pgjk;
			
		}
		
		static v3 GetCoordinates(Face face) {
			var out = new v3();

			var tmp = new v3();
			var tmp1 = new v3();
			var tmp2 = new v3();

			var o = new v3();
			o.scale(-face.d, face.n);


			tmp1.sub(face.v[0].w, o);
			tmp2.sub(face.v[1].w, o);
			tmp.cross(tmp1, tmp2);
			out.x = tmp.length();

			tmp1.sub(face.v[1].w, o);
			tmp2.sub(face.v[2].w, o);
			tmp.cross(tmp1, tmp2);
			out.y = tmp.length();

			tmp1.sub(face.v[2].w, o);
			tmp2.sub(face.v[0].w, o);
			tmp.cross(tmp1, tmp2);
			out.z = tmp.length();

			var sm = out.x + out.y + out.z;

			out.scaled(1f / (sm > 0f ? sm : 1f));

			return out;
		}
		
		Face FindBest() {
			Face bf = null;
			if (root != null) {
				var cf = root;
				var bd = cstInf;
				do {
					if (cf.d < bd) {
						bd = cf.d;
						bf = cf;
					}
				}
				while (null != (cf = cf.next));
			}
			return bf;
		}

		static boolean Set(Face f, Mkv a, Mkv b, Mkv c) {
			var tmp1 = new v3();
			var tmp2 = new v3();
			var tmp3 = new v3();

			var nrm = new v3();
			tmp1.sub(b.w, a.w);
			tmp2.sub(c.w, a.w);
			nrm.cross(tmp1, tmp2);


			var lenSq = nrm.lengthSquared();

			tmp1.cross(a.w, b.w);
			tmp2.cross(b.w, c.w);
			tmp3.cross(c.w, a.w);

			var valid = Stream.of(tmp1, tmp2, tmp3).noneMatch(v3 -> (!(v3.dot(nrm) >= -EPA_inface_eps)));

			f.v[0] = a;
			f.v[1] = b;
			f.v[2] = c;
			f.mark = 0;
			f.n.scale(1f / (lenSq > 0f ? (float)Math.sqrt(lenSq) : cstInf), nrm);
			f.d = Math.max(0, -f.n.dot(a.w));
			return valid;
		}
		
		Face NewFace(Mkv a, Mkv b, Mkv c) {
			var pf = new Face();
			if (Set(pf, a, b, c)) {
				if (root != null) {
					root.prev = pf;
				}
				pf.prev = null;
				pf.next = root;
				root = pf;
				++nfaces;
			}
			else {
				pf.prev = pf.next = null;
			}
			return (pf);
		}
	
		void Detach(Face face) {
			if (face.prev != null || face.next != null) {
				--nfaces;
				if (face == root) {
					root = face.next;
					root.prev = null;
				}
				else {
					if (face.next == null) {
						face.prev.next = null;
					}
					else {
						face.prev.next = face.next;
						face.next.prev = face.prev;
					}
				}
				face.prev = face.next = null;
			}
		}

		static void Link(Face f0, int e0, Face f1, int e1) {
			f0.f[e0] = f1; f1.e[e1] = e0;
			f1.f[e1] = f0; f0.e[e0] = e1;
		}

		Mkv Support(v3 w) {
			var v = new Mkv();
			gjk.Support(w, v);
			return v;
		}
		
		int BuildHorizon(int markid, Mkv w, Face f, int e, Face[] cf, Face[] ff) {
			var ne = 0;
			if (f.mark != markid) {
				var e1 = mod3[e + 1];
				if ((f.n.dot(w.w) + f.d) > 0) {
					var nf = NewFace(f.v[e1], f.v[e], w);
					Link(nf, 0, f, e);
					if (cf[0] != null) {
						Link(cf[0], 1, nf, 2);
					}
					else {
						ff[0] = nf;
					}
					cf[0] = nf;
					ne = 1;
				}
				else {
					var e2 = mod3[e + 2];
					Detach(f);
					f.mark = markid;
					ne += BuildHorizon(markid, w, f.f[e1], f.e[e1], cf, ff);
					ne += BuildHorizon(markid, w, f.f[e2], f.e[e2], cf, ff);
				}
			}
			return (ne);
		}

		float EvaluatePD() {
			return EvaluatePD(EPA_accuracy);
		}
		
		float EvaluatePD(float accuracy) {
			var tmp = new v3();


            depth = -cstInf;
            normal.set(0f, 0f, 0f);
            root = null;
            nfaces = 0;
            iterations = 0;
            failed = false;
            /* Prepare hull		*/
            if (gjk.EncloseOrigin()) {
                
                int[][] pfidx_ptr = null;
				var pfidx_index = 0;

				var nfidx = 0;
                
                int[][] peidx_ptr = null;
				var peidx_index = 0;

				var neidx = 0;
                switch (gjk.order) {
                    
                    case 3:

pfidx_ptr = tetrahedron_fidx;
pfidx_index = 0;

nfidx = 4;


peidx_ptr = tetrahedron_eidx;
peidx_index = 0;

neidx = 6;
break;
                    
                    case 4:

pfidx_ptr = hexahedron_fidx;
pfidx_index = 0;

nfidx = 6;


peidx_ptr = hexahedron_eidx;
peidx_index = 0;

neidx = 9;
break;
                }
                int i;

				var basemkv = new Mkv[5];
                for (i = 0; i <= gjk.order; ++i) {
                    basemkv[i] = new Mkv();
                    basemkv[i].set(gjk.simplex[i]);
                }
				var basefaces = new Face[6];
                for (i = 0; i < nfidx; ++i, pfidx_index++) {
                    basefaces[i] = NewFace(basemkv[pfidx_ptr[pfidx_index][0]], basemkv[pfidx_ptr[pfidx_index][1]], basemkv[pfidx_ptr[pfidx_index][2]]);
                }
                for (i = 0; i < neidx; ++i, peidx_index++) {
                    Link(basefaces[peidx_ptr[peidx_index][0]], peidx_ptr[peidx_index][1], basefaces[peidx_ptr[peidx_index][2]], peidx_ptr[peidx_index][3]);
                }
            }
            if (0 == nfaces) {
                
                return (depth);
            }
            /* Expand hull		*/
            Face bestface = null;
            for (var markid = 1; iterations < EPA_maxiterations; ++iterations) {
				var bf = FindBest();
                if (bf != null) {
                    tmp.negated(bf.n);
					var w = Support(tmp);
					var d = bf.n.dot(w.w) + bf.d;
                    bestface = bf;
                    if (d < -accuracy) {
                        Detach(bf);
                        bf.mark = ++markid;
						var nf = 0;
                        Face[] ff = {null};
                        Face[] cf = {null};
                        for (var i = 0; i < 3; ++i) {
                            nf += BuildHorizon(markid, w, bf.f[i], bf.e[i], cf, ff);
                        }
                        if (nf <= 2) {
                            break;
                        }
                        Link(cf[0], 1, ff[0], 2);
                    }
                    else {
                        break;
                    }
                }
                else {
                    break;
                }
            }
            /* Extract contact	*/
            if (bestface != null) {
				var b = GetCoordinates(bestface);
                normal.set(bestface.n);
                depth = Math.max(0, bestface.d);
                for (var i = 0; i < 2; ++i) {
					var s = i != 0 ? -1f : 1f;
                    for (var j = 0; j < 3; ++j) {
                        tmp.scale(s, bestface.v[j].r);
                        gjk.LocalSupport(tmp, i, features[i][j]);
                    }
                }

				var tmp1 = new v3();
				var tmp2 = new v3();
				var tmp3 = new v3();

                tmp1.scale(b.x, features[0][0]);
                tmp2.scale(b.y, features[0][1]);
                tmp3.scale(b.z, features[0][2]);
                VectorUtil.add(nearest[0], tmp1, tmp2, tmp3);

                tmp1.scale(b.x, features[1][0]);
                tmp2.scale(b.y, features[1][1]);
                tmp3.scale(b.z, features[1][2]);
                VectorUtil.add(nearest[1], tmp1, tmp2, tmp3);
            }
            else {
                failed = true;
            }
            
            return (depth);
        }
		
	}
	
	
	
	private final GJK gjk = new GJK();
	
	public boolean collide(ConvexShape shape0, Transform wtrs0,
                           ConvexShape shape1, Transform wtrs1,
                           float radialmargin/*,
			btStackAlloc* stackAlloc*/,
                           Results results) {
		
		
		results.witness0.set(0f, 0f, 0f);
		results.witness1.set(0f, 0f, 0f);
		results.normal.set(0f, 0f, 0f);
		results.depth = 0;
		results.status = ResultsStatus.Separated;
		results.epa_iterations = 0;
		results.gjk_iterations = 0;
		/* Use GJK to locate origin		*/
		gjk.init(/*stackAlloc,*/
				wtrs0.basis, wtrs0, shape0,
				wtrs1.basis, wtrs1, shape1,
				radialmargin + EPA_accuracy);
		try {
			var collide = gjk.SearchOrigin();
			results.gjk_iterations = gjk.iterations + 1;
			if (collide) {
				/* Then EPA for penetration depth	*/
				var epa = new EPA(gjk);
				var pd = epa.EvaluatePD();
				results.epa_iterations = epa.iterations + 1;
				if (pd > 0) {
					results.status = ResultsStatus.Penetrating;
					results.normal.set(epa.normal);
					results.depth = pd;
					results.witness0.set(epa.nearest[0]);
					results.witness1.set(epa.nearest[1]);
					return (true);
				}
				else {
					if (epa.failed) {
						results.status = ResultsStatus.EPA_Failed;
					}
				}
			}
			else {
				if (gjk.failed) {
					results.status = ResultsStatus.GJK_Failed;
				}
			}
			return (false);
		}
		finally {
			gjk.destroy();
		}
	}
	
}
