/*******************************************************************************
 * Copyright (c) 2013, Daniel Murphy
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 	* Redistributions of source code must retain the above copyright notice,
 * 	  this list of conditions and the following disclaimer.
 * 	* Redistributions in binary form must reproduce the above copyright notice,
 * 	  this list of conditions and the following disclaimer in the documentation
 * 	  and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package spacegraph.space2d.phys.collision;

import spacegraph.space2d.phys.common.MathUtils;
import spacegraph.space2d.phys.common.Rot;
import spacegraph.space2d.phys.common.Settings;
import spacegraph.space2d.phys.common.Transform;
import spacegraph.util.math.Tuple2f;
import spacegraph.util.math.v2;

/**
 * This is used to compute the current state of a contact manifold.
 *
 * @author daniel
 */
public class WorldManifold {
    /**
     * World vector pointing from A to B
     */
    public final v2 normal;

    /**
     * World contact point (point of intersection)
     */
    public final Tuple2f[] points;

    /**
     * A negative value indicates overlap, in meters.
     */
    private final float[] separations;

    public WorldManifold() {
        normal = new v2();
        points = new Tuple2f[Settings.maxManifoldPoints];
        separations = new float[Settings.maxManifoldPoints];
        for (int i = 0; i < Settings.maxManifoldPoints; i++) {
            points[i] = new v2();
        }
    }


    public final void initialize(final Manifold manifold, final Transform xfA, float radiusA,
                                 final Transform xfB, float radiusB) {
        if (manifold.pointCount == 0) {
            return;
        }

        switch (manifold.type) {
            case CIRCLES: {
                final Tuple2f pointA = new v2();
                final Tuple2f pointB = new v2();

                normal.x = 1;
                normal.y = 0;
                Tuple2f v = manifold.localPoint;
                
                
                pointA.x = (xfA.c * v.x - xfA.s * v.y) + xfA.pos.x;
                pointA.y = (xfA.s * v.x + xfA.c * v.y) + xfA.pos.y;
                Tuple2f mp0p = manifold.points[0].localPoint;
                pointB.x = (xfB.c * mp0p.x - xfB.s * mp0p.y) + xfB.pos.x;
                pointB.y = (xfB.s * mp0p.x + xfB.c * mp0p.y) + xfB.pos.y;

                if (MathUtils.distanceSquared(pointA, pointB) > Settings.EPSILON * Settings.EPSILON) {
                    normal.x = pointB.x - pointA.x;
                    normal.y = pointB.y - pointA.y;
                    normal.normalize();
                }

                final float cAx = normal.x * radiusA + pointA.x;
                final float cAy = normal.y * radiusA + pointA.y;

                final float cBx = -normal.x * radiusB + pointB.x;
                final float cBy = -normal.y * radiusB + pointB.y;

                points[0].x = (cAx + cBx) * .5f;
                points[0].y = (cAy + cBy) * .5f;
                separations[0] = (cBx - cAx) * normal.x + (cBy - cAy) * normal.y;
            }
            break;
            case FACE_A: {
                final Tuple2f planePoint = new v2();

                Rot.mulToOutUnsafe(xfA, manifold.localNormal, normal);
                Transform.mulToOut(xfA, manifold.localPoint, planePoint);

                final Tuple2f clipPoint = new v2();

                for (int i = 0; i < manifold.pointCount; i++) {
                    
                    
                    
                    
                    
                    Transform.mulToOut(xfB, manifold.points[i].localPoint, clipPoint);
                    
                    
                    
                    
                    
                    

                    final float scalar =
                            radiusA
                                    - ((clipPoint.x - planePoint.x) * normal.x + (clipPoint.y - planePoint.y)
                                    * normal.y);

                    final float cAx = normal.x * scalar + clipPoint.x;
                    final float cAy = normal.y * scalar + clipPoint.y;

                    final float cBx = -normal.x * radiusB + clipPoint.x;
                    final float cBy = -normal.y * radiusB + clipPoint.y;

                    points[i].x = (cAx + cBx) * .5f;
                    points[i].y = (cAy + cBy) * .5f;
                    separations[i] = (cBx - cAx) * normal.x + (cBy - cAy) * normal.y;
                }
            }
            break;
            case FACE_B:
                final Tuple2f planePoint = new v2();
                Rot.mulToOutUnsafe(xfB, manifold.localNormal, normal);
                Transform.mulToOut(xfB, manifold.localPoint, planePoint);

                
                
                
                
                
                

                final Tuple2f clipPoint = new v2();

                for (int i = 0; i < manifold.pointCount; i++) {
                    
                    
                    
                    
                    

                    Transform.mulToOut(xfA, manifold.points[i].localPoint, clipPoint);
                    
                    
                    
                    
                    

                    

                    
                    
                    
                    
                    

                    final float scalar =
                            radiusB
                                    - ((clipPoint.x - planePoint.x) * normal.x + (clipPoint.y - planePoint.y)
                                    * normal.y);

                    final float cBx = normal.x * scalar + clipPoint.x;
                    final float cBy = normal.y * scalar + clipPoint.y;

                    final float cAx = -normal.x * radiusA + clipPoint.x;
                    final float cAy = -normal.y * radiusA + clipPoint.y;

                    points[i].set( (cAx + cBx) * .5f, (cAy + cBy) * .5f);
                    separations[i] = (cAx - cBx) * normal.x + (cAy - cBy) * normal.y;
                }
                
                normal.x = -normal.x;
                normal.y = -normal.y;
                break;
        }
    }
}
