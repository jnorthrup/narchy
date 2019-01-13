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
import spacegraph.space3d.phys.math.VectorUtil;
import spacegraph.space3d.phys.shape.TriangleCallback;

/**
 *
 * @author jezek2
 */
public abstract class TriangleRaycastCallback extends TriangleCallback {
	
	

	private final v3 from = new v3();
	private final v3 to = new v3();

	public float hitFraction;

	protected TriangleRaycastCallback(v3 from, v3 to) {
		this.from.set(from);
		this.to.set(to);
		this.hitFraction = 1f;
	}
	
	@Override
    public void processTriangle(v3[] triangle, int partId, int triangleIndex) {
		v3 vert0 = triangle[0];
		v3 vert1 = triangle[1];
		v3 vert2 = triangle[2];

		v3 v10 = new v3();
		v10.sub(vert1, vert0);

		v3 v20 = new v3();
		v20.sub(vert2, vert0);

		v3 triangleNormal = new v3();
		triangleNormal.cross(v10, v20);

		float dist = vert0.dot(triangleNormal);
		float dist_a = triangleNormal.dot(from);
		dist_a -= dist;
		float dist_b = triangleNormal.dot(to);
		dist_b -= dist;

		if (dist_a * dist_b >= 0f) {
			return; 
		}

		float proj_length = dist_a - dist_b;
		float distance = (dist_a) / (proj_length);
		
		
		
		

		if (distance < hitFraction) {
			float edge_tolerance = triangleNormal.lengthSquared();
			edge_tolerance *= -0.0001f;
			v3 point = new v3();
			VectorUtil.setInterpolate3(point, from, to, distance);
            v3 v0p = new v3();
            v0p.sub(vert0, point);
            v3 v1p = new v3();
            v1p.sub(vert1, point);
            v3 cp0 = new v3();
            cp0.cross(v0p, v1p);

            if (cp0.dot(triangleNormal) >= edge_tolerance) {
                v3 v2p = new v3();
                v2p.sub(vert2, point);
                v3 cp1 = new v3();
                cp1.cross(v1p, v2p);
                if (cp1.dot(triangleNormal) >= edge_tolerance) {
                    v3 cp2 = new v3();
                    cp2.cross(v2p, v0p);

                    if (cp2.dot(triangleNormal) >= edge_tolerance) {

                        if (dist_a > 0f) {
                            hitFraction = reportHit(triangleNormal, distance, partId, triangleIndex);
                        }
                        else {
                            v3 tmp = new v3();
                            tmp.negated(triangleNormal);
                            hitFraction = reportHit(tmp, distance, partId, triangleIndex);
                        }
                    }
                }
            }
        }
	}

	protected abstract float reportHit(v3 hitNormalLocal, float hitFraction, int partId, int triangleIndex);

}
