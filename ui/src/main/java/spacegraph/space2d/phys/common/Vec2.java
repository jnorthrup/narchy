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
package spacegraph.space2d.phys.common;

import spacegraph.util.math.Tuple2f;
import spacegraph.util.math.v2;

/**
 * A 2D column vector
 */
@Deprecated
public class Vec2 extends v2 {


    public Vec2() {
        this(0, 0);
    }

    private Vec2(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Vec2(Tuple2f toCopy) {
        this(toCopy.x, toCopy.y);
    }

    /**
     * Zero out this vector.
     */
    public final void setZero() {
        set(0, 0);
    }

    /**
     //     * Set the vector component-wise.
     //     */
//    public final void set(float x, float y) {
//        this.x = x;
//        this.y = y;
//    }
//
//    /**
//     * Set this vector to another vector.
//     */
//    public final Vec2 set(Vec2 v) {
//        this.x = v.x;
//        this.y = v.y;
//        return this;
//    }

    /**
     * Return the sum of this vector and another; does not alter either one.
     */
    public final v2 add(Tuple2f v) {
        return new v2(x + v.x, y + v.y);
    }


    /**
     * Return the difference of this vector and another; does not alter either one.
     */
    public final v2 sub(Tuple2f v) {
        return new v2(x - v.x, y - v.y);
    }

    /**
     * Return this vector multiplied by a scalar; does not alter this vector.
     */
    public final v2 mul(float a) {
        return new v2(x * a, y * a);
    }


    /**
     * Flip the vector and return it - alters this vector.
     */
    public final Tuple2f negateLocal() {
        x = -x;
        y = -y;
        return this;
    }

    /**
     * Add another vector to this one and returns result - alters this vector.
     */
    public final Vec2 addLocal(Tuple2f v) {
        x += v.x;
        y += v.y;
        return this;
    }

    /**
     * Adds values to this vector and returns result - alters this vector.
     */
    public final Tuple2f addLocal(float x, float y) {
        this.x += x;
        this.y += y;
        return this;
    }

    /**
     * Subtract another vector from this one and return result - alters this vector.
     */
    public final Tuple2f subLocal(Tuple2f v) {
        x -= v.x;
        y -= v.y;
        return this;
    }

    /**
     * Multiply this vector by a number and return result - alters this vector.
     */
    public final Tuple2f scaled(float a) {
        x *= a;
        y *= a;
        return this;
    }

    /**
     * Get the skew vector such that dot(skew_vec, other) == cross(vec, other)
     */
    public final Tuple2f skew() {
        float x1 = -y;
        return new v2(x1, x);
    }

    /**
     * Get the skew vector such that dot(skew_vec, other) == cross(vec, other)
     */
    public final void skew(Tuple2f out) {
        out.x = -y;
        out.y = x;
    }

    /**
     * Return the length of this vector.
     */
//    public final float length() {
//        return (float) Math.sqrt(x * x + y * y);
//    }

    /**
     * Return the squared length of this vector.
     */
//    public final float lengthSquared() {
//        return (x * x + y * y);
//    }

//    /**
//     * Normalize this vector and return the length before normalization. Alters this vector.
//     */
//    public final float normalize() {
//        float length = length();
//        if (length < Settings.EPSILON) {
//            return 0f;
//        }
//
//        float invLength = 1.0f / length;
//        x *= invLength;
//        y *= invLength;
//        return length;
//    }


    /**
     * Return a new vector that has positive components.
     */
    public final Tuple2f abs() {
        return new v2(Math.abs(x), Math.abs(y));
    }


    // @Override // annotation omitted for GWT-compatibility

    /**
     * Return a copy of this vector.
     */
    public final v2 clone() {
        return new v2(x, y);
    }

    @Override
    public final String toString() {
        return "(" + x + ',' + y + ')';
    }

    /**
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() { // automatically generated by Eclipse
        final int prime = 31;
        int result = 1;
        result = prime * result + Float.floatToIntBits(x);
        result = prime * result + Float.floatToIntBits(y);
        return result;
    }

    /**
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object obj) { // automatically generated by Eclipse
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof Tuple2f)) return false;
        Tuple2f other = (Tuple2f) obj;
        if (Float.floatToIntBits(x) != Float.floatToIntBits(other.x)) return false;
        return Float.floatToIntBits(y) == Float.floatToIntBits(other.y);
    }
}
