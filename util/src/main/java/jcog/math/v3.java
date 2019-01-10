/*
 * $RCSfile$
 *
 * Copyright 1997-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 *
 * $Revision: 127 $
 * $Date: 2008-02-28 17:18:51 -0300 (Thu, 28 Feb 2008) $
 * $State$
 */

package jcog.math;

import jcog.Util;


import java.util.Random;

/**
 * A 3-element vector that is represented by single-precision floating point
 * x,y,z coordinates.  If this value represents a normal, then it should
 * be normalized.
 */
public class v3 extends Tuple3f {

    
    static final long serialVersionUID = -7031930069184524614L;
    public static v2 one = new v2(1, 1);


    /**
     * Constructs and initializes a Vector3f from the specified xyz coordinates.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     */
    public v3(float x, float y, float z) {
        super(x, y, z);
    }


    /**
     * Constructs and initializes a Vector3f from the array of length 3.
     *
     * @param v the array of length 3 containing xyz in order
     */
    public v3(float[] v) {
        super(v);
    }

    public v3 clone() {
        return new v3(x,y,z);
    }

    /**
     * Constructs and initializes a Vector3f from the specified Vector3f.
     *
     * @param v1 the Vector3f containing the initialization x y z data
     */
    public v3(v3 v1) {
        super(v1);
    }
    public v3(v3 v1, float scale) {
        set(v1.x * scale, v1.y * scale, v1.z * scale);
    }












    /**
     * Constructs and initializes a Vector3f from the specified Tuple3f.
     *
     * @param t1 the Tuple3f containing the initialization x y z data
     */
    public v3(Tuple3f t1) {
        super(t1);
    }












    /**
     * Constructs and initializes a Vector3f to (0,0,0).
     */
    public v3() {
        super();
    }

    public static v3 v(v3 copy) {
        return new v3(copy);
    }

    public static v3 v() {
        return new v3();
    }

    public static v3 v(v3 base, float mult) {
        v3 v = v();
        v.scale(mult, base);
        return v;
    }

    public static v3 v(float x, float y, float z) {
        return new v3(x, y, z);
    }

    public static v2 v(float x, float y) {
        return new v2(x, y);
    }

    public static void crossToOutUnsafe(v3 a, v3 b, v3 out) {
        assert (out != b);
        assert (out != a);
        out.x = a.y * b.z - a.z * b.y;
        out.y = a.z * b.x - a.x * b.z;
        out.z = a.x * b.y - a.y * b.x;
    }


    /**
     * Returns the squared length of this vector.
     *
     * @return the squared length of this vector
     */
    public final float lengthSquared() {
        return (this.x * this.x + this.y * this.y + this.z * this.z);
    }

    /**
     * Returns the length of this vector.
     *
     * @return the length of this vector
     */
    public final float length() {
        return (float)
                Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
    }


    /**
     * Sets this vector to be the vector cross product of vectors v1 and v2.
     *
     * @param v1 the first vector
     * @param v2 the second vector
     */
    public final v3 cross(v3 v1, v3 v2) {


        float v1z = v1.z;
        float v2y = v2.y;
        float v2z = v2.z;
        float v1y = v1.y;
        float v1x = v1.x;
        float v2x = v2.x;
        set(v1y * v2z - v1z * v2y,
                v2x * v1z - v2z * v1x,
                v1x * v2y - v1y * v2x);

        return this;

    }

//    public static v3 cross(v3 a, v3 b) {
//        return new v3(a.y * b.z - a.z * b.y, a.z * b.x - a.x * b.z, a.x * b.y - a.y * b.x);
//    }

    public final v3 cross(v3 v2) {
        return cross(this, v2);
    }

    /**
     * Computes the dot product of this vector and vector v1.
     *
     * @param v1 the other vector
     * @return the dot product of this vector and v1
     */
    public final float dot(v3 v1) {
        return (this.x * v1.x + this.y * v1.y + this.z * v1.z);
    }

    /**
     * Sets the value of this vector to the normalization of vector v1.
     *
     * @param v1 the un-normalized vector
     */
    public final void normalize(v3 v1) {
        float norm = (float) (1.0 / Math.sqrt(v1.x * v1.x + v1.y * v1.y + v1.z * v1.z));
        set(v1.x * norm,
                v1.y * norm,
                v1.z * norm);
    }

    /**
     * Normalizes this vector in place.
     */
    public final float normalize() {

        float norm = (float) Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
        if (norm >= Float.MIN_NORMAL) {

            set(this.x / norm,
                    this.y / norm,
                    this.z / norm);
        }
        return norm;
    }


    public final v3 normalized(float scale)     {
        normalize();
        scale(scale);
        return this;
    }

    /**
     * Returns the angle in radians between this vector and the vector
     * parameter; the return value is constrained to the range [0,PI].
     *
     * @param v1 the other vector
     * @return the angle in radians in the range [0,PI]
     */
    public final float angle(v3 v1) {
        float div = this.length() * v1.length();
        if (Util.equals(div, 0, Float.MIN_NORMAL))
            return Float.NaN;

        double vDot = this.dot(v1) / div;
        if (vDot < -1.0) vDot = -1.0;
        if (vDot > 1.0) vDot = 1.0;
        return ((float) (Math.acos(vDot)));
    }

    public static float dist(v3 a, v3 b) {
        v3 x = new v3(a);
        x.sub(b);
        return x.length();
    }

    public void normalize(float thenScale) {
        normalize();
        scale(thenScale);
    }



    public void randomize(Random r, float scale) {
        set(r.nextFloat() * scale, r.nextFloat() * scale, r.nextFloat() * scale);
    }

    public v3 scale(float mx, float my, float mz) {
        x *= mx;
        y *= my;
        z *= mz;
        return this;
    }

    public float maxComponent() {
        return Util.max(x,y,z);
    }

    protected boolean setIfChange(float xx, float yy, float zz, float epsilon) {

        if (!Util.equals(x, xx, epsilon) ||
                !Util.equals(y, yy, epsilon) ||
                    !Util.equals(z, zz, epsilon)) {

            this.x = xx;
            this.y = yy;
            this.z = zz;
            return true;
        }

        return false;
    }
}
