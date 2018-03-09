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
package org.jbox2d.collision.shapes;

import jcog.Util;
import org.jbox2d.collision.AABB;
import org.jbox2d.collision.RayCastInput;
import org.jbox2d.collision.RayCastOutput;
import org.jbox2d.common.Rot;
import org.jbox2d.common.Settings;
import org.jbox2d.common.Transform;
import org.jbox2d.common.Vec2;
import spacegraph.math.Tuple2f;
import spacegraph.math.v2;

/**
 * A convex polygon shape. Polygons have a maximum number of vertices equal to _maxPolygonVertices.
 * In most cases you should not need many vertices for a convex polygon.
 */
public class PolygonShape extends Shape {
    /**
     * Dump lots of debug information.
     */
    private final static boolean m_debug = false;

    /**
     * Local position of the shape centroid in parent body frame.
     */
    public final Vec2 centroid = new Vec2();

    /**
     * The vertices of the shape. Note: use getVertexCount(), not m_vertices.length, to get number of
     * active vertices.
     */
    public final Tuple2f vertex[];

    /**
     * The normals of the shape. Note: use getVertexCount(), not m_normals.length, to get number of
     * active normals.
     */
    public final v2 normals[];

    /**
     * Number of active vertices in the shape.
     */
    public int vertices;

    // pooling
    private final Tuple2f pool1 = new Vec2();
    private final Vec2 pool2 = new Vec2();
    private final Tuple2f pool3 = new Vec2();
    private final Tuple2f pool4 = new Vec2();
    private final Transform poolt1 = new Transform();

    public PolygonShape() {
        this(Settings.maxPolygonVertices);
    }

    public PolygonShape(int maxVertices) {
        super(ShapeType.POLYGON);

        vertices = 0;
        vertex = new Tuple2f[maxVertices];
        for (int i = 0; i < vertex.length; i++) {
            vertex[i] = new v2(0,0);
        }
        normals = new v2[maxVertices];
        for (int i = 0; i < normals.length; i++) {
            normals[i] = new v2(0,0);
        }
        setRadius(Settings.polygonRadius);
        centroid.setZero();
    }

    public final Shape clone() {
        PolygonShape shape = new PolygonShape(vertex.length);
        shape.centroid.set(this.centroid);
        for (int i = 0; i < shape.normals.length; i++) {
            shape.normals[i].set(normals[i]);
            shape.vertex[i].set(vertex[i]);
        }
        shape.setRadius(this.getRadius());
        shape.vertices = this.vertices;
        return shape;
    }

    /**
     * Create a convex hull from the given array of points. The count must be in the range [3,
     * Settings.maxPolygonVertices]. This method takes an arraypool for pooling.
     *
     * @param verts
     * @param num
     * @warning the points may be re-ordered, even if they form a convex polygon.
     * @warning collinear points are removed.
     */
    public final void set(final Tuple2f[] verts, final int num) {
        assert (3 <= num && num <= Settings.maxPolygonVertices);

        // Create the convex hull using the Gift wrapping algorithm
        // http://en.wikipedia.org/wiki/Gift_wrapping_algorithm

        // Find the right most point on the hull
        int i0 = 0;
        float x0 = verts[0].x;
        for (int i = 1; i < num; ++i) {
            float x = verts[i].x;
            if (x > x0 || (x == x0 && verts[i].y < verts[i0].y)) {
                i0 = i;
                x0 = x;
            }
        }

        int[] hull = new int[Settings.maxPolygonVertices];
        int m = 0;
        int ih = i0;

        while (true) {
            hull[m] = ih;

            int ie = 0;
            for (int j = 1; j < num; ++j) {
                if (ie == ih) {
                    ie = j;
                    continue;
                }

                Tuple2f r = pool1.set(verts[ie]).subbed(verts[hull[m]]);
                Tuple2f v = pool2.set(verts[j]).subbed(verts[hull[m]]);
                float c = Tuple2f.cross(r, v);
                if (c < 0.0f) {
                    ie = j;
                }

                // Collinearity check
                if (c == 0.0f && v.lengthSquared() > r.lengthSquared()) {
                    ie = j;
                }
            }

            ++m;
            ih = ie;

            if (ie == i0) {
                break;
            }
        }

        this.vertices = m;

        // Copy vertices.
        for (int i = 0; i < vertices; ++i) {
            if (vertex[i] == null) {
                vertex[i] = new Vec2();
            }
            vertex[i].set(verts[hull[i]]);
        }

        Tuple2f edge = pool1;
        for (int i = 0; i < vertices; ++i) {
            final int i1 = i;
            final int i2 = i + 1 < vertices ? i + 1 : 0;
            edge.set(vertex[i2]).subbed(vertex[i1]);

            assert (edge.lengthSquared() > Settings.EPSILON * Settings.EPSILON);
            Tuple2f.crossToOutUnsafe(edge, 1f, normals[i]);
            normals[i].normalize();
        }

        // Compute the polygon centroid.
        computeCentroidToOut(vertex, vertices, centroid);
    }

    public static PolygonShape box(final float hx, final float hy) {
        return new PolygonShape(4).setAsBox(hx, hy);
    }

    /**
     * Build vertices to represent an axis-aligned box.
     *
     * @param hx the half-width.
     * @param hy the half-height.
     */
    public final org.jbox2d.collision.shapes.PolygonShape setAsBox(final float hx, final float hy) {
        vertices = 4;
        vertex[0].set(-hx, -hy);
        vertex[1].set(hx, -hy);
        vertex[2].set(hx, hy);
        vertex[3].set(-hx, hy);
        normals[0].set(0.0f, -1.0f);
        normals[1].set(1.0f, 0.0f);
        normals[2].set(0.0f, 1.0f);
        normals[3].set(-1.0f, 0.0f);
        centroid.setZero();
        return this;
    }

    public final org.jbox2d.collision.shapes.PolygonShape lerpAsBox(final float hx, final float hy, float rate) {
        if (vertices!=4) {
            return setAsBox(0.1f, 0.1f); //TODO epsilon
        } else {
            float currentWidth = vertex[1].x;
            float currentHeight = -vertex[1].y;
            float nextWidth = Util.lerp(rate, currentWidth, hx);
            float nextHeight = Util.lerp(rate, currentHeight, hy);
            return setAsBox(nextWidth, nextHeight);
        }
    }

    public final static org.jbox2d.collision.shapes.PolygonShape regular(int n, float r) {
        PolygonShape p = new PolygonShape(n);
        p.vertices = n;
        for (int i = 0; i < n; i++) {
            double theta = i / (float) n * 2 * Math.PI;
            p.vertex[i].set( (float)(r * Math.cos(theta)), ((float)(r * Math.sin(theta))));
        }
        p.centroid.setZero();
        p.set(p.vertex, n);
        return p;
    }

    /**
     * Build vertices to represent an oriented box.
     *
     * @param hx     the half-width.
     * @param hy     the half-height.
     * @param center the center of the box in local coordinates.
     * @param angle  the rotation of the box in local coordinates.
     */
    public final void setAsBox(final float hx, final float hy, final Tuple2f center, final float angle) {
        vertices = 4;
        vertex[0].set(-hx, -hy);
        vertex[1].set(hx, -hy);
        vertex[2].set(hx, hy);
        vertex[3].set(-hx, hy);
        normals[0].set(0.0f, -1.0f);
        normals[1].set(1.0f, 0.0f);
        normals[2].set(0.0f, 1.0f);
        normals[3].set(-1.0f, 0.0f);
        centroid.set(center);

        final Transform xf = poolt1;
        xf.pos.set(center);
        xf.set(angle);

        // Transform vertices and normals.
        for (int i = 0; i < vertices; ++i) {
            Transform.mulToOut(xf, vertex[i], vertex[i]);
            Rot.mulToOut(xf, normals[i], normals[i]);
        }
    }

    public int getChildCount() {
        return 1;
    }

    @Override
    public final boolean testPoint(final Transform xf, final Tuple2f p) {
        float tempx, tempy;
        final Rot xfq = xf;

        tempx = p.x - xf.pos.x;
        tempy = p.y - xf.pos.y;
        final float pLocalx = xfq.c * tempx + xfq.s * tempy;
        final float pLocaly = -xfq.s * tempx + xfq.c * tempy;

        if (m_debug) {
            System.out.println("--testPoint debug--");
            System.out.println("Vertices: ");
            for (int i = 0; i < vertices; ++i) {
                System.out.println(vertex[i]);
            }
            System.out.println("pLocal: " + pLocalx + ", " + pLocaly);
        }

        for (int i = 0; i < vertices; ++i) {
            Tuple2f vertex = this.vertex[i];
            Tuple2f normal = normals[i];
            tempx = pLocalx - vertex.x;
            tempy = pLocaly - vertex.y;
            final float dot = normal.x * tempx + normal.y * tempy;
            if (dot > 0.0f) {
                return false;
            }
        }

        return true;
    }

    @Override
    public final void computeAABB(final AABB aabb, final Transform xf, int childIndex) {
        final Tuple2f lower = aabb.lowerBound;
        final Tuple2f upper = aabb.upperBound;
        final Tuple2f v1 = vertex[0];
        final float xfqc = xf.c;
        final float xfqs = xf.s;
        final float xfpx = xf.pos.x;
        final float xfpy = xf.pos.y;
        lower.x = (xfqc * v1.x - xfqs * v1.y) + xfpx;
        lower.y = (xfqs * v1.x + xfqc * v1.y) + xfpy;
        upper.x = lower.x;
        upper.y = lower.y;

        for (int i = 1; i < vertices; ++i) {
            Tuple2f v2 = vertex[i];
            // Vec2 v = Mul(xf, m_vertices[i]);
            float vx = (xfqc * v2.x - xfqs * v2.y) + xfpx;
            float vy = (xfqs * v2.x + xfqc * v2.y) + xfpy;
            lower.x = lower.x < vx ? lower.x : vx;
            lower.y = lower.y < vy ? lower.y : vy;
            upper.x = upper.x > vx ? upper.x : vx;
            upper.y = upper.y > vy ? upper.y : vy;
        }

        lower.x -= radius;
        lower.y -= radius;
        upper.x += radius;
        upper.y += radius;
    }

    /**
     * Get the vertex count.
     *
     * @return
     */
    public final int getVertexCount() {
        return vertices;
    }

    /**
     * Get a vertex by index.
     *
     * @param index
     * @return
     */
    public final Tuple2f getVertex(final int index) {
        assert (0 <= index && index < vertices);
        return vertex[index];
    }

    @Override
    public float computeDistanceToOut(Transform xf, Tuple2f p, int childIndex, v2 normalOut) {
        float xfqc = xf.c;
        float xfqs = xf.s;
        float tx = p.x - xf.pos.x;
        float ty = p.y - xf.pos.y;
        float pLocalx = xfqc * tx + xfqs * ty;
        float pLocaly = -xfqs * tx + xfqc * ty;

        float maxDistance = -Float.MAX_VALUE;
        float normalForMaxDistanceX = pLocalx;
        float normalForMaxDistanceY = pLocaly;

        for (int i = 0; i < vertices; ++i) {
            Tuple2f vertex = this.vertex[i];
            Tuple2f normal = normals[i];
            tx = pLocalx - vertex.x;
            ty = pLocaly - vertex.y;
            float dot = normal.x * tx + normal.y * ty;
            if (dot > maxDistance) {
                maxDistance = dot;
                normalForMaxDistanceX = normal.x;
                normalForMaxDistanceY = normal.y;
            }
        }

        float distance;
        if (maxDistance > 0) {
            float minDistanceX = normalForMaxDistanceX;
            float minDistanceY = normalForMaxDistanceY;
            float minDistance2 = maxDistance * maxDistance;
            for (int i = 0; i < vertices; ++i) {
                Tuple2f vertex = this.vertex[i];
                float distanceVecX = pLocalx - vertex.x;
                float distanceVecY = pLocaly - vertex.y;
                float distance2 = (distanceVecX * distanceVecX + distanceVecY * distanceVecY);
                if (minDistance2 > distance2) {
                    minDistanceX = distanceVecX;
                    minDistanceY = distanceVecY;
                    minDistance2 = distance2;
                }
            }
            distance = (float) Math.sqrt(minDistance2);
            normalOut.x = xfqc * minDistanceX - xfqs * minDistanceY;
            normalOut.y = xfqs * minDistanceX + xfqc * minDistanceY;
            normalOut.normalize();
        } else {
            distance = maxDistance;
            normalOut.x = xfqc * normalForMaxDistanceX - xfqs * normalForMaxDistanceY;
            normalOut.y = xfqs * normalForMaxDistanceX + xfqc * normalForMaxDistanceY;
        }

        return distance;
    }

    @Override
    public final boolean raycast(RayCastOutput output, RayCastInput input, Transform xf,
                                 int childIndex) {
        final float xfqc = xf.c;
        final float xfqs = xf.s;
        final Tuple2f xfp = xf.pos;
        float tempx, tempy;
        // b2Vec2 p1 = b2MulT(xf.q, input.p1 - xf.p);
        // b2Vec2 p2 = b2MulT(xf.q, input.p2 - xf.p);
        tempx = input.p1.x - xfp.x;
        tempy = input.p1.y - xfp.y;
        final float p1x = xfqc * tempx + xfqs * tempy;
        final float p1y = -xfqs * tempx + xfqc * tempy;

        tempx = input.p2.x - xfp.x;
        tempy = input.p2.y - xfp.y;
        final float p2x = xfqc * tempx + xfqs * tempy;
        final float p2y = -xfqs * tempx + xfqc * tempy;

        final float dx = p2x - p1x;
        final float dy = p2y - p1y;

        float lower = 0, upper = input.maxFraction;

        int index = -1;

        for (int i = 0; i < vertices; ++i) {
            Tuple2f normal = normals[i];
            Tuple2f vertex = this.vertex[i];
            // p = p1 + a * d
            // dot(normal, p - v) = 0
            // dot(normal, p1 - v) + a * dot(normal, d) = 0
            float tempxn = vertex.x - p1x;
            float tempyn = vertex.y - p1y;
            final float numerator = normal.x * tempxn + normal.y * tempyn;
            final float denominator = normal.x * dx + normal.y * dy;

            if (denominator == 0.0f) {
                if (numerator < 0.0f) {
                    return false;
                }
            } else {
                // Note: we want this predicate without division:
                // lower < numerator / denominator, where denominator < 0
                // Since denominator < 0, we have to flip the inequality:
                // lower < numerator / denominator <==> denominator * lower >
                // numerator.
                if (denominator < 0.0f && numerator < lower * denominator) {
                    // Increase lower.
                    // The segment enters this half-space.
                    lower = numerator / denominator;
                    index = i;
                } else if (denominator > 0.0f && numerator < upper * denominator) {
                    // Decrease upper.
                    // The segment exits this half-space.
                    upper = numerator / denominator;
                }
            }

            if (upper < lower) {
                return false;
            }
        }

        assert (0.0f <= lower && lower <= input.maxFraction);

        if (index >= 0) {
            output.fraction = lower;
            // normal = Mul(xf.R, m_normals[index]);
            Tuple2f normal = normals[index];
            Tuple2f out = output.normal;
            out.x = xfqc * normal.x - xfqs * normal.y;
            out.y = xfqs * normal.x + xfqc * normal.y;
            return true;
        }
        return false;
    }

    public final void computeCentroidToOut(final Tuple2f[] vs, final int count, final Vec2 out) {
        assert (count >= 3);

        out.set(0.0f, 0.0f);
        float area = 0.0f;

        // pRef is the reference point for forming triangles.
        // It's location doesn't change the result (except for rounding error).
        final Tuple2f pRef = pool1;
        pRef.setZero();

        final Tuple2f e1 = pool2;
        final Tuple2f e2 = pool3;

        final float inv3 = 1.0f / 3.0f;

        for (int i = 0; i < count; ++i) {
            // Triangle vertices.
            final Tuple2f p1 = pRef;
            final Tuple2f p2 = vs[i];
            final Tuple2f p3 = i + 1 < count ? vs[i + 1] : vs[0];

            e1.set(p2).subbed(p1);
            e2.set(p3).subbed(p1);

            final float D = Tuple2f.cross(e1, e2);

            final float triangleArea = 0.5f * D;
            area += triangleArea;

            // Area weighted centroid
            e1.set(p1).added(p2).added(p3).scaled(triangleArea * inv3);
            out.addLocal(e1);
        }

        // Centroid
        assert (area > Settings.EPSILON);
        out.scaled(1.0f / area);
    }

    public void computeMass(final MassData massData, float density) {
        // Polygon mass, centroid, and inertia.
        // Let rho be the polygon density in mass per unit area.
        // Then:
        // mass = rho * int(dA)
        // centroid.x = (1/mass) * rho * int(x * dA)
        // centroid.y = (1/mass) * rho * int(y * dA)
        // I = rho * int((x*x + y*y) * dA)
        //
        // We can compute these integrals by summing all the integrals
        // for each triangle of the polygon. To evaluate the integral
        // for a single triangle, we make a change of variables to
        // the (u,v) coordinates of the triangle:
        // x = x0 + e1x * u + e2x * v
        // y = y0 + e1y * u + e2y * v
        // where 0 <= u && 0 <= v && u + v <= 1.
        //
        // We integrate u from [0,1-v] and then v from [0,1].
        // We also need to use the Jacobian of the transformation:
        // D = cross(e1, e2)
        //
        // Simplification: triangle centroid = (1/3) * (p1 + p2 + p3)
        //
        // The rest of the derivation is handled by computer algebra.

        assert (vertices >= 3);

        final Tuple2f center = pool1;
        center.setZero();
        float area = 0.0f;
        float I = 0.0f;

        // pRef is the reference point for forming triangles.
        // It's location doesn't change the result (except for rounding error).
        final Vec2 s = pool2;
        s.setZero();
        // This code would put the reference point inside the polygon.
        for (int i = 0; i < vertices; ++i) {
            s.addLocal(vertex[i]);
        }
        s.scaled(1.0f / vertices);

        final float k_inv3 = 1.0f / 3.0f;

        final Tuple2f e1 = pool3;
        final Tuple2f e2 = pool4;

        for (int i = 0; i < vertices; ++i) {
            // Triangle vertices.
            e1.set(vertex[i]).subbed(s);
            e2.set(s).negated().added(i + 1 < vertices ? vertex[i + 1] : vertex[0]);

            final float D = Tuple2f.cross(e1, e2);

            final float triangleArea = 0.5f * D;
            area += triangleArea;

            // Area weighted centroid
            center.x += triangleArea * k_inv3 * (e1.x + e2.x);
            center.y += triangleArea * k_inv3 * (e1.y + e2.y);

            final float ex1 = e1.x, ey1 = e1.y;
            final float ex2 = e2.x, ey2 = e2.y;

            float intx2 = ex1 * ex1 + ex2 * ex1 + ex2 * ex2;
            float inty2 = ey1 * ey1 + ey2 * ey1 + ey2 * ey2;

            I += (0.25f * k_inv3 * D) * (intx2 + inty2);
        }

        // Total mass
        massData.mass = density * area;

        // Center of mass
        assert (area > Settings.EPSILON);
        center.scaled(1.0f / area);
        massData.center.set(center).added(s);

        // Inertia tensor relative to the local origin (point s)
        massData.I = I * density;

        // Shift to center of mass then to original body origin.
        massData.I += massData.mass * (Tuple2f.dot(massData.center, massData.center));
    }

    /**
     * Validate convexity. This is a very time consuming operation.
     *
     * @return
     */
    public boolean validate() {
        for (int i = 0; i < vertices; ++i) {
            int i1 = i;
            int i2 = i < vertices - 1 ? i1 + 1 : 0;
            Tuple2f p = vertex[i1];
            Tuple2f e = pool1.set(vertex[i2]).subbed(p);

            for (int j = 0; j < vertices; ++j) {
                if (j == i1 || j == i2) {
                    continue;
                }

                Tuple2f v = pool2.set(vertex[j]).subbed(p);
                float c = Tuple2f.cross(e, v);
                if (c < 0.0f) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Get the vertices in local coordinates.
     */
    public Tuple2f[] getVertex() {
        return vertex;
    }

    /**
     * Get the edge normal vectors. There is one for each vertex.
     */
    public Tuple2f[] getNormals() {
        return normals;
    }

    /**
     * Get the centroid and apply the supplied transform.
     */
    public Tuple2f centroid(final Transform xf) {
        return Transform.mul(xf, centroid);
    }

    /**
     * Get the centroid and apply the supplied transform.
     */
    public Tuple2f centroidToOut(final Transform xf, final Tuple2f out) {
        Transform.mulToOutUnsafe(xf, centroid, out);
        return out;
    }
}
