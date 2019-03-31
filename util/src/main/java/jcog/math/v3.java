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
import jcog.tree.rtree.Spatialization;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

import static jcog.Util.notNaN;

/**
 * A 3-element vector that is represented by single-precision floating point
 * x,y,z coordinates.  If this value represents a normal, then it should
 * be normalized.
 */
public class v3 implements java.io.Serializable, Cloneable {


    public static v2 one = new v2(1, 1);
    /**
     * The x coordinate.
     */
    public float x;
    /**
     * The y coordinate.
     */
    public float y;
    /**
     * The z coordinate.
     */
    public float z;


    /**
     * Constructs and initializes a Vector3f from the specified xyz coordinates.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     */
    public v3(float x, float y, float z) {
        this.x = notNaN(x);
        this.y = notNaN(y);
        this.z = notNaN(z);
    }


    /**
     * Constructs and initializes a Vector3f from the array of length 3.
     *
     * @param v the array of length 3 containing xyz in order
     */
    public v3(float[] v) {
        this(v[0], v[1], v[2]);
        //assert(v.length >= 3);
    }

    /**
     * Constructs and initializes a Vector3f from the specified Vector3f.
     *
     * @param v1 the Vector3f containing the initialization x y z data
     */
    public v3(v3 v) {
        this(v.x, v.y, v.z);
    }

    public v3(v3 v1, float scale) {
        set(v1.x * scale, v1.y * scale, v1.z * scale);
    }

    /**
     * Constructs and initializes a Vector3f to (0,0,0).
     */
    public v3() {
        //x= y= z=0;
    }


//    /**
//     * Constructs and initializes a Vector3f from the specified v3.
//     *
//     * @param t1 the v3 containing the initialization x y z data
//     */
//    public v3(v3 t1) {
//        super(t1);
//    }

    public static v3 v(v3 copy) {
        return new v3(copy.x, copy.y, copy.z);
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

    public static float dist(v3 a, v3 b) {
        v3 x = new v3(a);
        x.sub(b);
        return x.length();
    }

    @Override
    public v3 clone() {
        return new v3(x, y, z);
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

//    public static v3 cross(v3 a, v3 b) {
//        return new v3(a.y * b.z - a.z * b.y, a.z * b.x - a.x * b.z, a.x * b.y - a.y * b.x);
//    }

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

    public final v3 normalized(float scale) {
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
        return Util.max(x, y, z);
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

    /**
     * Returns a string that contains the values of this v3.
     * The form is (x,y,z).
     *
     * @return the String representation
     */
    public String toString() {
        return "(" + this.x + ", " + this.y + ", " + this.z + ')';
    }

    /**
     * Sets the value of this tuple to the specified xyz coordinates.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     */
    public void set(float x, float y, float z) {


        this.x = (x);
        this.y = (y);
        this.z = (z);


    }

    /**
     * assumes z=0
     */
    public void set(float x, float y) {
        set(x, y, this.z);
    }

    public final void setNegative(v3 v) {
        set(-v.x, -v.y, -v.z);
    }

    public final void zero() {
        set(0, 0, 0);
    }

    /**
     * Sets the value of this tuple to the xyz coordinates specified in
     * the array of length 3.
     *
     * @param t the array of length 3 containing xyz in order
     */
    private void set(float[] t) {
        this.set(t[0], t[1], t[2]);
    }

    /**
     * Sets the value of this tuple to the value of tuple t1.
     *
     * @param t1 the tuple to be copied
     */
    public final void set(v3 t1) {
        this.set(t1.x, t1.y, t1.z);
    }

    /**
     * Gets the value of this tuple and copies the values into t.
     *
     * @param t the array of length 3 into which the values are copied
     */
    public final void get(float[] t) {
        t[0] = this.x;
        t[1] = this.y;
        t[2] = this.z;
    }

    /**
     * Gets the value of this tuple and copies the values into t.
     *
     * @param t the v3 object into which the values of this object are copied
     */
    public final void get(v3 t) {
        t.x = this.x;
        t.y = this.y;
        t.z = this.z;
    }

    /**
     * Sets the value of this tuple to the vector sum of tuples t1 and t2.
     *
     * @param t1 the first tuple
     * @param t2 the second tuple
     */
    public final void add(v3 t1, v3 t2) {
        set(t1.x + t2.x, t1.y + t2.y, t1.z + t2.z);
    }

    /**
     * Sets the value of this tuple to the vector sum of itself and tuple t1.
     *
     * @param t1 the other tuple
     */
    public final void add(v3 t1) {
        set(this.x + t1.x, this.y + t1.y, this.z + t1.z);
    }

    public final void add(v3 t1, float minX, float minY, float maxX, float maxY) {
        add(t1);
        if (x < minX) x = minX;
        if (y < minY) y = minY;
        if (x > maxX) x = maxX;
        if (y > maxY) y = maxY;
    }

    public void add(float dx, float dy) {
        add(dx, dy, 0);
    }

    public void add(float dx, float dy, float dz) {
        set(this.x + dx, this.y + dy, this.z + dz);
    }

    /**
     * Sets the value of this tuple to the vector difference
     * of tuples t1 and t2 (this = t1 - t2).
     *
     * @param t1 the first tuple
     * @param t2 the second tuple
     */
    public final void sub(v3 t1, v3 t2) {
        set(t1.x - t2.x,
                t1.y - t2.y,
                t1.z - t2.z);
    }

    /**
     * Sets the value of this tuple to the vector difference of
     * itself and tuple t1 (this = this - t1) .
     *
     * @param t1 the other tuple
     */
    public final void sub(v3 t1) {
        set(this.x - t1.x, this.y - t1.y, this.z - t1.z);
    }

    /**
     * Sets the value of this tuple to the negation of tuple t1.
     *
     * @param t1 the source tuple
     */
    public final void negated(v3 t1) {
        set(-t1.x, -t1.y, -t1.z);
    }

    /**
     * Negates the value of this tuple in place.
     */
    public final void negated() {
        set(-x, -y, -z);
    }

    /**
     * Sets the value of this vector to the scalar multiplication
     * of tuple t1.
     *
     * @param s  the scalar value
     * @param t1 the source tuple
     */
    public final void scale(float s, v3 t1) {
        set(s * t1.x, s * t1.y, s * t1.z);
    }

    public final void scale(v3 t1) {
        set(t1.x, t1.y, t1.z);
    }

    /**
     * Sets the value of this tuple to the scalar multiplication
     * of the scale factor with this.
     *
     * @param s the scalar value
     */
    public final void scale(float s) {
        set(s * x, s * y, s * z);
    }

    /**
     * Sets the value of this tuple to the scalar multiplication
     * of tuple t1 and then adds tuple t2 (this = add + s*mul ).
     *
     * @param s   the scalar value
     * @param mul the tuple to be scaled and added
     * @param add the tuple to be added without a scale
     */
    public final void scaleAdd(float s, v3 mul, v3 add) {

        set(s * mul.x + add.x,
                s * mul.y + add.y,
                s * mul.z + add.z);
    }

    public final void scaleAdd(float s, float mx, float my, float mz, v3 add) {
        set(s * mx + add.x,
                s * my + add.y,
                s * mz + add.z);
    }

    /**
     * this = add + s * mul1 * mul2
     */
    public final void scaleAdd(float s, v3 mul1, v3 mul2, v3 add) {

        set(s * mul1.x * mul2.x + add.x,
                s * mul1.y * mul2.y + add.y,
                s * mul1.z * mul2.z + add.z);
    }

    /**
     * Sets the value of this tuple to the scalar multiplication
     * of itself and then adds tuple t1 (this = s*this + t1).
     *
     * @param s  the scalar value
     * @param t1 the tuple to be added
     */
    public final void scaleAdd(float s, v3 t1) {
        set(s * this.x + t1.x, s * this.y + t1.y, s * this.z + t1.z);
    }

    public final v3 addScaled(v3 t1, float s) {
        set(this.x + s * t1.x, this.y + s * t1.y, this.z + s * t1.z);
        return this;
    }

    /**
     * Returns true if the Object t1 is of type v3 and all of the
     * data members of t1 are equal to the corresponding data members in
     * this v3.
     *
     * @param t1 the vector with which the comparison is made
     * @return true or false
     */
    public boolean equals(@Nullable v3 t1) {
        return equals(t1, Spatialization.EPSILONf);
    }

    public boolean equals(@Nullable v3 t1, float epsilon) {
        return (this == t1) ||
                ((t1 != null) &&
                        Util.equals(x, t1.x, epsilon) &&
                        Util.equals(y, t1.y, epsilon) &&
                        Util.equals(z, t1.z, epsilon));
    }

    /**
     * Returns true if the Object t1 is of type v3 and all of the
     * data members of t1 are equal to the corresponding data members in
     * this v3.
     *
     * @param t1 the Object with which the comparison is made
     * @return true or false
     */
    public boolean equals(Object obj) {

//        v3 t2 = (v3) t1;
//        return equals(t2);

        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        v3 other = (v3) obj;
        if (Float.floatToIntBits(x) != Float.floatToIntBits(other.x)) return false;
        if (Float.floatToIntBits(y) != Float.floatToIntBits(other.y)) return false;
        return Float.floatToIntBits(z) == Float.floatToIntBits(other.z);


    }

    /**
     * Returns a hash code value based on the data values in this
     * object.  Two different v3 objects with identical data values
     * (i.e., v3.equals returns true) will return the same hash
     * code value.  Two objects with different data members may return the
     * same hash value, although this is not likely.
     *
     * @return the integer hash code value
     */
    public int hashCode() {
        long bits = 1L;
        bits = 31L * bits + VecMathUtil.floatToIntBits(x);
        bits = 31L * bits + VecMathUtil.floatToIntBits(y);
        bits = 31L * bits + VecMathUtil.floatToIntBits(z);
        return (int) (bits ^ (bits >> 32));
    }

    /**
     * Clamps the tuple parameter to the range [low, high] and
     * places the values into this tuple.
     *
     * @param min the lowest value in the tuple after clamping
     * @param max the highest value in the tuple after clamping
     * @param t   the source tuple, which will not be modified
     */
    public final void clamp(float min, float max, v3 t) {
        float x;
        if (t.x > max) {
            x = max;
        } else if (t.x < min) {
            x = min;
        } else {
            x = t.x;
        }

        float y;
        if (t.y > max) {
            y = max;
        } else if (t.y < min) {
            y = min;
        } else {
            y = t.y;
        }

        float z;
        if (t.z > max) {
            z = max;
        } else if (t.z < min) {
            z = min;
        } else {
            z = t.z;
        }

        set(x, y, z);
    }

    /**
     * Clamps the minimum value of the tuple parameter to the min
     * parameter and places the values into this tuple.
     *
     * @param min the lowest value in the tuple after clamping
     * @param t   the source tuple, which will not be modified
     */
    public final void clampMin(float min, v3 t) {
        set(t.x < min ? min : t.x,
                t.y < min ? min : t.y,
                t.z < min ? min : t.z);

    }

    /**
     * Clamps the maximum value of the tuple parameter to the max
     * parameter and places the values into this tuple.
     *
     * @param max the highest value in the tuple after clamping
     * @param t   the source tuple, which will not be modified
     */
    public final void clampMax(float max, v3 t) {
        set(t.x > max ? max : t.x,
                t.y > max ? max : t.y,
                t.z > max ? max : t.z);
    }

    public final void clamp(v3 min, v3 max) {
        if (x < min.x) x = min.x;
        if (x > max.x) x = max.x;
        if (y < min.y) y = min.y;
        if (y > max.y) y = max.y;
        if (z < min.z) z = min.z;
        if (z > max.z) z = max.z;
    }

    /**
     * Sets each component of the tuple parameter to its absolute
     * value and places the modified values into this tuple.
     *
     * @param t the source tuple, which will not be modified
     */
    public final void absolute(v3 t) {
        set(Math.abs(t.x),
                Math.abs(t.y),
                Math.abs(t.z));
    }

    /**
     * Clamps this tuple to the range [low, high].
     *
     * @param min the lowest value in this tuple after clamping
     * @param max the highest value in this tuple after clamping
     */
    public final void clamp(float min, float max) {
        float x = this.x;
        if (x > max) {
            x = max;
        } else if (x < min) {
            x = min;
        }

        float y = this.y;
        if (y > max) {
            y = max;
        } else if (y < min) {
            y = min;
        }

        float z = this.z;
        if (z > max) {
            z = max;
        } else if (z < min) {
            z = min;
        }

        set(x, y, z);
    }

    /**
     * Clamps the minimum value of this tuple to the min parameter.
     *
     * @param min the lowest value in this tuple after clamping
     */
    public final void clampMin(float min) {
        float x = this.x;
        if (x < min) x = min;
        float y = this.y;
        if (y < min) y = min;
        float z = this.z;
        if (z < min) z = min;
        set(x, y, z);
    }

    /**
     * Clamps the maximum value of this tuple to the max parameter.
     *
     * @param max the highest value in the tuple after clamping
     */
    public final void clampMax(float max) {
        float x = this.x;
        if (x > max) x = max;
        float y = this.y;
        if (y > max) y = max;
        float z = this.z;
        if (z > max) z = max;
        set(x, y, z);
    }

    /**
     * Sets each component of this tuple to its absolute value.
     */
    public final void absolute() {
        set(Math.abs(x),
                Math.abs(y),
                Math.abs(z));
    }

    /**
     * Linearly interpolates between tuples t1 and t2 and places the
     * result into this tuple:  this = (1-alpha)*t1 + alpha*t2.
     *
     * @param t1    the first tuple
     * @param t2    the second tuple
     * @param alpha the alpha interpolation parameter
     */
    public final void interpolate(v3 t1, v3 t2, float alpha) {
        float na = 1f - alpha;
        set(na * t1.x + alpha * t2.x,
                na * t1.y + alpha * t2.y,
                na * t1.z + alpha * t2.z);


    }

    /**
     * Linearly interpolates between this tuple and tuple t1 and
     * places the result into this tuple:  this = (1-alpha)*this + alpha*t1.
     *
     * @param t1    the first tuple
     * @param alpha the alpha interpolation parameter
     */
    public final void interpolate(v3 t1, float alpha) {
        float na = 1f - alpha;
        set(na * this.x + alpha * t1.x,
                na * this.y + alpha * t1.y,
                na * this.z + alpha * t1.z);


    }

    public boolean min(v3 b) {
        boolean bb = false;
        if (x < b.x) {
            x = b.x;
            bb = true;
        }
        if (y < b.y) {
            y = b.y;
            bb = true;
        }
        if (z < b.z) {
            z = b.z;
            bb = true;
        }
        return bb;
    }

    public boolean max(v3 b) {
        boolean bb = false;
        if (x > b.x) {
            x = b.x;
            bb = true;
        }
        if (y > b.y) {
            y = b.y;
            bb = true;
        }
        if (z > b.z) {
            z = b.z;
            bb = true;
        }
        return bb;
    }

    public boolean isFinite() {
        return Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z);
    }
}
