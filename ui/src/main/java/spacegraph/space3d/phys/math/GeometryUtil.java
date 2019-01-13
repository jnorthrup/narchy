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

package spacegraph.space3d.phys.math;

import jcog.math.v3;
import spacegraph.space3d.phys.util.OArrayList;
import spacegraph.util.math.Vector4f;

/**
 * GeometryUtil helper class provides a few methods to convert between plane
 * equations and vertices.
 * 
 * @author jezek2
 */
class GeometryUtil {

	public static boolean isPointInsidePlanes(OArrayList<Vector4f> planeEquations, v3 point, float margin) {
		int numbrushes = planeEquations.size();
        for (Vector4f N1 : planeEquations) {

            float dist = VectorUtil.dot3(N1, point) + N1.w - margin;
            if (dist > 0f) {
                return false;
            }
        }
		return true;
	}

	private static boolean areVerticesBehindPlane(Vector4f planeNormal, OArrayList<v3> vertices, float margin) {
		int numvertices = vertices.size();
        for (v3 N1 : vertices) {

            float dist = VectorUtil.dot3(planeNormal, N1) + planeNormal.w - margin;
            if (dist > 0f) {
                return false;
            }
        }
		return true;
	}

	private static boolean notExist(Vector4f planeEquation, OArrayList<Vector4f> planeEquations) {
		int numbrushes = planeEquations.size();
        for (Vector4f N1 : planeEquations) {

            if (VectorUtil.dot3(planeEquation, N1) > 0.999f) {
                return false;
            }
        }
		return true;
	}

	public static void getPlaneEquationsFromVertices(OArrayList<v3> vertices, OArrayList<Vector4f> planeEquationsOut) {
		Vector4f planeEquation = new Vector4f();
		v3 edge0 = new v3(), edge1 = new v3();
		v3 tmp = new v3();

		int numvertices = vertices.size();
		
		for (int i = 0; i < numvertices; i++) {
            
            v3 N1 = vertices.get(i);

			for (int j = i + 1; j < numvertices; j++) {
                
                v3 N2 = vertices.get(j);

				for (int k = j + 1; k < numvertices; k++) {
                    
                    v3 N3 = vertices.get(k);

					edge0.sub(N2, N1);
					edge1.sub(N3, N1);
					float normalSign = 1f;
					for (int ww = 0; ww < 2; ww++) {
						tmp.cross(edge0, edge1);
						planeEquation.x = normalSign * tmp.x;
						planeEquation.y = normalSign * tmp.y;
						planeEquation.z = normalSign * tmp.z;

						if (VectorUtil.lengthSquared3(planeEquation) > 0.0001f) {
							VectorUtil.normalize3(planeEquation);
							if (notExist(planeEquation, planeEquationsOut)) {
								planeEquation.w = -VectorUtil.dot3(planeEquation, N1);

								
								if (areVerticesBehindPlane(planeEquation, vertices, 0.01f)) {
									planeEquationsOut.add(new Vector4f(planeEquation));
								}
							}
						}
						normalSign = -1f;
					}
				}
			}
		}
	}























































	
}
