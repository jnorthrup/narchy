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

package spacegraph.util.math;

import jcog.Util;
import jcog.tree.rtree.Spatialization;
import org.jetbrains.annotations.NotNull;
import spacegraph.space3d.phys.BulletGlobals;

/**
 * A generic 2-element tuple that is represented by single-precision
 * floating point x,y coordinates.
 */
public abstract class Tuple2f implements java.io.Serializable, Cloneable {


    /**
     * The x coordinate.
     */
    public float x;

    /**
     * The y coordinate.
     */
    public float y;


    /**
     * Constructs and initializes a Tuple2f from the specified xy coordinates.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     */
    Tuple2f(float x, float y) {
        this.x = x;
        this.y = y;
    }


    /**
     * Constructs and initializes a Tuple2f from the specified array.
     *
     * @param t the array of length 2 containing xy in order
     */
    Tuple2f(float[] t) {
        this.x = t[0];
        this.y = t[1];
    }

    public static Tuple2f abs(Tuple2f a) {
        return new v2(Math.abs(a.x), Math.abs(a.y));
    }

    public static void absToOut(Tuple2f a, Tuple2f out) {
        out.x = Math.abs(a.x);
        out.y = Math.abs(a.y);
    }

    public static float dot(final Tuple2f a, final Tuple2f b) {
        return a.x * b.x + a.y * b.y;
    }

    public static void negateToOut(Tuple2f a, Tuple2f out) {
        out.x = -a.x;
        out.y = -a.y;
    }

    public static void minToOut(Tuple2f a, Tuple2f b, Tuple2f out) {
        out.x = a.x < b.x ? a.x : b.x;
        out.y = a.y < b.y ? a.y : b.y;
    }

    public static void maxToOut(Tuple2f a, Tuple2f b, Tuple2f out) {
        out.x = a.x > b.x ? a.x : b.x;
        out.y = a.y > b.y ? a.y : b.y;
    }


    /**
     * Returns the length of this vector.
     * @return the length of this vector
     */
    public final float length()
    {
        return (float) Math.sqrt(this.x*this.x + this.y*this.y);
    }

    /**
     * Returns the squared length of this vector.
     * @return the squared length of this vector
     */
    public final float lengthSquared()
    {
        return (this.x*this.x + this.y*this.y);
    }


    /**
     * Constructs and initializes a Tuple2f from the specified Tuple2f.
     *
     * @param t1 the Tuple2f containing the initialization x y data
     */
    Tuple2f(Tuple2f t1) {
        this.x = t1.x;
        this.y = t1.y;
    }


    /**
     * Constructs and initializes a Tuple2f from the specified Tuple2d.
     *
     * @param t1 the Tuple2d containing the initialization x y data
     */
    Tuple2f(Tuple2d t1) {
        this.x = (float) t1.x;
        this.y = (float) t1.y;
    }


    /**
     * Constructs and initializes a Tuple2f to (0,0).
     */
    Tuple2f() {
        this.x = 0.0f;
        this.y = 0.0f;
    }


    /**
     * Sets the value of this tuple to the specified xy coordinates.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     */
    public Tuple2f set(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }


    /**
     * Sets the value of this tuple from the 2 values specified in
     * the array.
     *
     * @param t the array of length 2 containing xy in order
     */
    public final void set(float[] t) {
        this.x = t[0];
        this.y = t[1];
    }


    /**
     * Sets the value of this tuple to the value of the Tuple2f argument.
     *
     * @param t1 the tuple to be copied
     */
    public final Tuple2f set(Tuple2f t1) {
        this.x = t1.x;
        this.y = t1.y;
        return this;
    }









    /**
     * Sets the value of this tuple to the value of the Tuple2d argument.
     *
     * @param t1 the tuple to be copied
     */
    public final void set(Tuple2d t1) {
        this.x = (float) t1.x;
        this.y = (float) t1.y;
    }


    /**
     * Copies the value of the elements of this tuple into the array t.
     *
     * @param t the array that will contain the values of the vector
     */
    public final void get(float[] t) {
        t[0] = this.x;
        t[1] = this.y;
    }


    /**
     * Sets the value of this tuple to the vector sum of tuples t1 and t2.
     *
     * @param t1 the first tuple
     * @param t2 the second tuple
     */
    public final void add(Tuple2f t1, Tuple2f t2) {
        this.x = t1.x + t2.x;
        this.y = t1.y + t2.y;
    }


    /**
     * Sets the value of this tuple to the vector sum of itself and tuple t1.
     *
     * @param t1 the other tuple
     */
    public final Tuple2f added(Tuple2f t1) {
        return added(t1.x, t1.y);
    }

    public final Tuple2f added(float tx, float ty) {
        this.x += tx;
        this.y += ty;
        return this;
    }

    /**
     * Sets the value of this tuple to the vector difference of
     * tuple t1 and t2 (this = t1 - t2).
     *
     * @param t1 the first tuple
     * @param t2 the second tuple
     */
    public final Tuple2f sub(Tuple2f t1, Tuple2f t2) {
        this.x = t1.x - t2.x;
        this.y = t1.y - t2.y;
        return this;
    }


    /**
     * Sets the value of this tuple to the vector difference of
     * itself and tuple t1 (this = this - t1).
     *
     * @param t1 the other tuple
     */
    public final Tuple2f subbed(Tuple2f t1) {
        return subbed(t1.x, t1.y);
    }

    public Tuple2f subbed(float x, float y) {
        this.x -= x;
        this.y -= y;
        return this;
    }

    public final void sub(float dx, float dy) {
        set(this.x - dx, this.y - dy);
    }

    public static float cross(final Tuple2f a, final Tuple2f b) {
        return a.x * b.y - a.y * b.x;
    }

    public static Tuple2f cross(Tuple2f a, float s) {
        float y1 = -s * a.x;
        return new v2(s * a.y, y1);
    }

    /**
     * True if the vector represents a pair of valid, non-infinite floating point numbers.
     */
    public final boolean isValid() {
        return !Float.isNaN(x) && !Float.isInfinite(x) && !Float.isNaN(y) && !Float.isInfinite(y);
    }

    public static Tuple2f min(Tuple2f a, Tuple2f b) {
        return new v2(Math.min(a.x, b.x), Math.min(a.y, b.y));
    }

    public static Tuple2f max(Tuple2f a, Tuple2f b) {
        return new v2(Math.max(a.x, b.x), Math.max(a.y, b.y));
    }


    /**
     * Sets the value of this tuple to the negation of tuple t1.
     *
     * @param t1 the source tuple
     */
    public final Tuple2f negated(Tuple2f t1) {
        this.x = -t1.x;
        this.y = -t1.y;
        return this;
    }


    /**
     * Negates the value of this vector in place.
     */
    public final Tuple2f negated() {
        this.x = -this.x;
        this.y = -this.y;
        return this;
    }


    /**
     * Sets the value of this tuple to the scalar multiplication
     * of tuple t1.
     *
     * @param s  the scalar value
     * @param t1 the source tuple
     */
    public final void scaled(float s, Tuple2f t1) {
        this.x = s * t1.x;
        this.y = s * t1.y;
    }


    public static void crossToOut(Tuple2f a, float s, Tuple2f out) {
        final float tempy = -s * a.x;
        out.x = s * a.y;
        out.y = tempy;
    }

    public static void crossToOutUnsafe(Tuple2f a, float s, Tuple2f out) {
        assert (out != a);
        out.x = s * a.y;
        out.y = -s * a.x;
    }

    public static Tuple2f cross(float s, Tuple2f a) {
        float x1 = -s * a.y;
        return new v2(x1, s * a.x);
    }

    public static void crossToOut(float s, Tuple2f a, Tuple2f out) {
        final float tempY = s * a.x;
        out.x = -s * a.y;
        out.y = tempY;
    }

    public static void crossToOutUnsafe(float s, Tuple2f a, Tuple2f out) {
        assert (out != a);
        out.x = -s * a.y;
        out.y = s * a.x;
    }

    /**
     * Sets the value of this tuple to the scalar multiplication
     * of itself.
     *
     * @param s the scalar value
     */
    public Tuple2f scaled(float s) {
        this.x *= s;
        this.y *= s;
        return this;
    }

    /** multiplies each component */
    public final Tuple2f scaled(Tuple2f z) {
        this.x *= z.x;
        this.y *= z.y;
        return this;
    }

    public final Tuple2f scaled(float sx, float sy) {
        this.x *= sx;
        this.y *= sy;
        return this;
    }

    /**
     * Sets the value of this tuple to the scalar multiplication
     * of tuple t1 and then adds tuple t2 (this = s*t1 + t2).
     *
     * @param s  the scalar value
     * @param t1 the tuple to be multipled
     * @param t2 the tuple to be added
     */
    public final void scaledAdded(float s, Tuple2f t1, Tuple2f t2) {
        this.x = s * t1.x + t2.x;
        this.y = s * t1.y + t2.y;
    }


    /**
     * Sets the value of this tuple to the scalar multiplication
     * of itself and then adds tuple t1 (this = s*this + t1).
     *
     * @param s  the scalar value
     * @param t1 the tuple to be added
     */
    public final void scaledAdded(float s, Tuple2f t1) {
        this.x = s * this.x + t1.x;
        this.y = s * this.y + t1.y;
    }


    /**
     * Returns a hash code value based on the data values in this
     * object.  Two different Tuple2f objects with identical data values
     * (i.e., Tuple2f.equals returns true) will return the same hash
     * code value.  Two objects with different data members may return the
     * same hash value, although this is not likely.
     *
     * @return the integer hash code value
     */
    public int hashCode() {
        long bits = 1L;
        bits = 31L * bits + (long) VecMathUtil.floatToIntBits(x);
        bits = 31L * bits + (long) VecMathUtil.floatToIntBits(y);
        return (int) (bits ^ (bits >> 32));
    }


    /**
     * Returns true if all of the data members of Tuple2f t1 are
     * equal to the corresponding data members in this Tuple2f.
     *
     * @param t the vector with which the comparison is made
     * @return true or false
     */
    public boolean equals(@NotNull Tuple2f t, float epsilon) {
        return (this == t) ||
               (
               Util.equals(this.x, t.x, epsilon) && Util.equals(this.y, t.y, epsilon)
               );
    }

    public boolean equals(@NotNull Tuple2f t1) {
        return equals(t1, BulletGlobals.SIMD_EPSILON);
    }

    /**
     * Returns true if the Object t1 is of type Tuple2f and all of the
     * data members of t1 are equal to the corresponding data members in
     * this Tuple2f.
     *
     * @param t1 the object with which the comparison is made
     * @return true or false
     */
    public boolean equals(Object t1) {
        try {
            Tuple2f t2 = (Tuple2f) t1;
            return (this.x == t2.x && this.y == t2.y);
        } catch (NullPointerException | ClassCastException e2) {
            return false;
        }

    }

    /**
     * Returns true if the L-infinite distance between this tuple
     * and tuple t1 is less than or equal to the epsilon parameter,
     * otherwise returns false.  The L-infinite
     * distance is equal to MAX[abs(x1-x2), abs(y1-y2)].
     *
     * @param t1      the tuple to be compared to this tuple
     * @param epsilon the threshold value
     * @return true or false
     */
    public boolean epsilonEquals(Tuple2f t1, float epsilon) {

        float diff = x - t1.x;
        if (Float.isNaN(diff)) return false;
        if ((diff < 0 ? -diff : diff) > epsilon) return false;

        diff = y - t1.y;
        if (Float.isNaN(diff)) return false;
        return (diff < 0 ? -diff : diff) <= epsilon;

    }

    /**
     * Returns a string that contains the values of this Tuple2f.
     * The form is (x,y).
     *
     * @return the String representation
     */
    public String toString() {
        return ("(" + this.x + ", " + this.y + ')');
    }


    /**
     * Clamps the tuple parameter to the range [low, high] and
     * places the values into this tuple.
     *
     * @param min the lowest value in the tuple after clamping
     * @param max the highest value in the tuple after clamping
     * @param t   the source tuple, which will not be modified
     */
    public final void clamp(float min, float max, Tuple2f t) {
        if (t.x > max) {
            x = max;
        } else if (t.x < min) {
            x = min;
        } else {
            x = t.x;
        }

        if (t.y > max) {
            y = max;
        } else if (t.y < min) {
            y = min;
        } else {
            y = t.y;
        }

    }


    /**
     * Clamps the minimum value of the tuple parameter to the min
     * parameter and places the values into this tuple.
     *
     * @param min the lowest value in the tuple after clamping
     * @param t   the source tuple, which will not be modified
     */
    public final void clampMin(float min, Tuple2f t) {
        x = t.x < min ? min : t.x;

        y = t.y < min ? min : t.y;

    }


    /**
     * Clamps the maximum value of the tuple parameter to the max
     * parameter and places the values into this tuple.
     *
     * @param max the highest value in the tuple after clamping
     * @param t   the source tuple, which will not be modified
     */
    public final void clampMax(float max, Tuple2f t) {
        x = t.x > max ? max : t.x;

        y = t.y > max ? max : t.y;

    }


    /**
     * Sets each component of the tuple parameter to its absolute
     * value and places the modified values into this tuple.
     *
     * @param t the source tuple, which will not be modified
     */
    public final void absolute(Tuple2f t) {
        x = Math.abs(t.x);
        y = Math.abs(t.y);
    }


    /**
     * Clamps this tuple to the range [low, high].
     *
     * @param min the lowest value in this tuple after clamping
     * @param max the highest value in this tuple after clamping
     */
    public final void clamp(float min, float max) {
        if (x > max) {
            x = max;
        } else if (x < min) {
            x = min;
        }

        if (y > max) {
            y = max;
        } else if (y < min) {
            y = min;
        }

    }


    /**
     * Clamps the minimum value of this tuple to the min parameter.
     *
     * @param min the lowest value in this tuple after clamping
     */
    public final void clampMin(float min) {
        if (x < min) x = min;
        if (y < min) y = min;
    }


    /**
     * Clamps the maximum value of this tuple to the max parameter.
     *
     * @param max the highest value in the tuple after clamping
     */
    public final void clampMax(float max) {
        if (x > max) x = max;
        if (y > max) y = max;
    }


    /**
     * Sets each component of this tuple to its absolute value.
     */
    public final void absolute() {
        x = Math.abs(x);
        y = Math.abs(y);
    }


    /**
     * Linearly interpolates between tuples t1 and t2 and places the
     * result into this tuple:  this = (1-alpha)*t1 + alpha*t2.
     *
     * @param t1    the first tuple
     * @param t2    the second tuple
     * @param alpha the alpha interpolation parameter
     */
    public final void interpolate(Tuple2f t1, Tuple2f t2, float alpha) {
        this.x = (1 - alpha) * t1.x + alpha * t2.x;
        this.y = (1 - alpha) * t1.y + alpha * t2.y;

    }


    /**
     * Linearly interpolates between this tuple and tuple t1 and
     * places the result into this tuple:  this = (1-alpha)*this + alpha*t1.
     *
     * @param t1    the first tuple
     * @param alpha the alpha interpolation parameter
     */
    public final void interpolate(Tuple2f t1, float alpha) {

        this.x = (1 - alpha) * this.x + alpha * t1.x;
        this.y = (1 - alpha) * this.y + alpha * t1.y;

    }




















    public v2 clone() {
        return new v2(x, y);
    }
    public final void absLocal() {
        x = Math.abs(x);
        y = Math.abs(y);
    }

    /**
     * Normalizes this vector in place.
     */
    public final float normalize() {
        float sqrNorm = (this.x * this.x + this.y * this.y);
        if (sqrNorm >= Spatialization.sqrEPSILONf) {
            float norm = (float) Math.sqrt(sqrNorm);
            if (norm >= Spatialization.EPSILONf) {
                set(this.x / norm, this.y / norm);
                return norm;
            }
        }
        this.x = this.y = 0;
        return 0;
    }

    /**
     * Get the <i>x</i> coordinate.
     *
     * @return the <i>x</i> coordinate.
     * @since vecmath 1.5
     */
    public final float getX() {
        return x;
    }


    /**
     * Set the <i>x</i> coordinate.
     *
     * @param x value to <i>x</i> coordinate.
     * @since vecmath 1.5
     */
    public final void setX(float x) {
        this.x = x;
    }


    /**
     * Get the <i>y</i> coordinate.
     *
     * @return the <i>y</i> coordinate.
     * @since vecmath 1.5
     */
    public final float getY() {
        return y;
    }


    /**
     * Set the <i>y</i> coordinate.
     *
     * @param y value to <i>y</i> coordinate.
     * @since vecmath 1.5
     */
    public final void setY(float y) {
        this.y = y;
    }

    public void add(float dx, float dy) {
        set(x + dx, y + dy);
    }

    public float distanceSq(Tuple2f v) {
        if (v == this) return 0;
        float d = Util.sqr(x - v.x) + Util.sqr(y - v.y);
        return d;
    }

    public void setZero() {
        set(0,0);
    }

    public v2 add(Tuple2f u) {
        return new v2(x + u.x, y + u.y);
    }
    public v2 sub(Tuple2f u) {
        return new v2(x - u.x, y - u.y);
    }

    public v2 scale(float s) {
        return new v2(x * s, y * s);
    }
    public v2 scale(float sx, float sy) {
        return new v2(x * sx, y * sy);
    }

    public boolean setIfChanged(float x, float y, float epsilon) {
        if (!Util.equals(this.x, x, epsilon) || !Util.equals(this.y, y, epsilon)) {
            set(x, y);
            return true;
        }
        return false;
    }

    public void ensureFinite() {
        Util.assertFinite(x); Util.assertFinite(y);
    }
}
