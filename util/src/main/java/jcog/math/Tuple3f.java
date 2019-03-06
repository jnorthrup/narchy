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

import static jcog.Util.notNaN;

/**
 * A generic 3-element tuple that is represented by single precision-floating
 * point x,y,z coordinates.
 */
@Deprecated public abstract class Tuple3f implements java.io.Serializable, Cloneable {



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
     * Constructs and initializes a Tuple3f from the specified xyz coordinates.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     */
    protected Tuple3f(float x, float y, float z) {
        this.x = notNaN(x);
        this.y = notNaN(y);
        this.z = notNaN(z);
    }


    /**
     * Constructs and initializes a Tuple3f from the array of length 3.
     *
     * @param t the array of length 3 containing xyz in order
     */
    protected Tuple3f(float[] t) {
        set(t);
    }


    /**
     * Constructs and initializes a Tuple3f from the specified Tuple3f.
     *
     * @param t1 the Tuple3f containing the initialization x y z data
     */
    protected Tuple3f(Tuple3f t1) {
        set(t1);
    }












    /**
     * Constructs and initializes a Tuple3f to (0,0,0).
     */
    protected Tuple3f() {
    }


    /**
     * Returns a string that contains the values of this Tuple3f.
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

    /** assumes z=0 */
    public void set(float x, float y) {
        set(x, y, this.z);
    }











    public final void setNegative(Tuple3f v) {
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
    public final void set(Tuple3f t1) {
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
     * @param t the Tuple3f object into which the values of this object are copied
     */
    public final void get(Tuple3f t) {
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
    public final void add(Tuple3f t1, Tuple3f t2) {
        set(t1.x + t2.x, t1.y + t2.y, t1.z + t2.z);
    }


    /**
     * Sets the value of this tuple to the vector sum of itself and tuple t1.
     *
     * @param t1 the other tuple
     */
    public final void add(Tuple3f t1) {
        set(this.x + t1.x, this.y + t1.y, this.z + t1.z);
    }
    public final void add(Tuple3f t1, float minX, float minY, float maxX, float maxY) {
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
    public final void sub(Tuple3f t1, Tuple3f t2) {
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
    public final void sub(Tuple3f t1) {
        set(this.x - t1.x, this.y - t1.y, this.z - t1.z);
    }


    /**
     * Sets the value of this tuple to the negation of tuple t1.
     *
     * @param t1 the source tuple
     */
    public final void negated(Tuple3f t1) {
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
    public final void scale(float s, Tuple3f t1) {
        set(s * t1.x, s * t1.y, s * t1.z);
    }

    public final void scale(Tuple3f t1) {
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
     * @param s  the scalar value
     * @param mul the tuple to be scaled and added
     * @param add the tuple to be added without a scale
     */
    public final void scaleAdd(float s, Tuple3f mul, Tuple3f add) {

        set(s * mul.x + add.x,
                s * mul.y + add.y,
                s * mul.z + add.z);
    }
    public final void scaleAdd(float s, float mx, float my, float mz, Tuple3f add) {
        set(s * mx + add.x,
                s * my + add.y,
                s * mz + add.z);
    }

    /** this = add + s * mul1 * mul2 */
    public final void scaleAdd(float s, Tuple3f mul1, Tuple3f mul2, Tuple3f add) {

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
    public final void scaleAdd(float s, Tuple3f t1) {
        set(s * this.x + t1.x, s * this.y + t1.y, s * this.z + t1.z);
    }

    public final Tuple3f addScaled(Tuple3f t1, float s) {
        set(this.x + s * t1.x, this.y + s * t1.y, this.z + s * t1.z);
        return this;
    }


    /**
     * Returns true if the Object t1 is of type Tuple3f and all of the
     * data members of t1 are equal to the corresponding data members in
     * this Tuple3f.
     *
     * @param t1 the vector with which the comparison is made
     * @return true or false
     */
    public boolean equals(@Nullable Tuple3f t1) {
        return equals(t1, Spatialization.EPSILONf);
    }

    public boolean equals(@Nullable Tuple3f t1, float epsilon) {
        return (this==t1) ||
                ((t1 != null) &&
                        Util.equals(x, t1.x, epsilon) &&
                        Util.equals(y, t1.y, epsilon) &&
                        Util.equals(z, t1.z, epsilon));
    }

        /**
         * Returns true if the Object t1 is of type Tuple3f and all of the
         * data members of t1 are equal to the corresponding data members in
         * this Tuple3f.
         *
         * @param t1 the Object with which the comparison is made
         * @return true or false
         */
    public boolean equals(Object obj) {
        
//        Tuple3f t2 = (Tuple3f) t1;
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
     * object.  Two different Tuple3f objects with identical data values
     * (i.e., Tuple3f.equals returns true) will return the same hash
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
    public final void clamp(float min, float max, Tuple3f t) {
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
    public final void clampMin(float min, Tuple3f t) {
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
    public final void clampMax(float max, Tuple3f t) {
        set(t.x > max ? max : t.x,
                t.y > max ? max : t.y,
                t.z > max ? max : t.z);
    }
    public final void clamp(Tuple3f min, Tuple3f max) {
        if (x < min.x) x = min.x;
        if (x > max.x ) x = max.x;
        if (y < min.y) y = min.y;
        if (y > max.y ) y = max.y;
        if (z < min.z) z = min.z;
        if (z > max.z ) z = max.z;
    }

    /**
     * Sets each component of the tuple parameter to its absolute
     * value and places the modified values into this tuple.
     *
     * @param t the source tuple, which will not be modified
     */
    public final void absolute(Tuple3f t) {
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
    public final void interpolate(Tuple3f t1, Tuple3f t2, float alpha) {
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
    public final void interpolate(Tuple3f t1, float alpha) {
        float na = 1f - alpha;
        set(na * this.x + alpha * t1.x,
                na * this.y + alpha * t1.y,
                na * this.z + alpha * t1.z);


    }

    /**
     * Creates a new object of the same class as this object.
     *
     * @return a clone of this instance.
     * @throws OutOfMemoryError if there is not enough memory.
     * @see java.lang.Cloneable
     * @since vecmath 1.3
     */
    @Override
    public Object clone() {
        
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            
            throw new InternalError();
        }
    }

    public boolean min(Tuple3f b) {
        boolean bb = false;
        if (x < b.x) { x = b.x; bb = true; }
        if (y < b.y) { y = b.y; bb = true; }
        if (z < b.z) { z = b.z; bb = true; }
        return bb;
    }
    public boolean max(Tuple3f b) {
        boolean bb = false;
        if (x > b.x) { x = b.x; bb = true; }
        if (y > b.y) { y = b.y; bb = true; }
        if (z > b.z) { z = b.z; bb = true; }
        return bb;
    }

    public boolean isFinite() {
        return Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z);
    }



































































}
