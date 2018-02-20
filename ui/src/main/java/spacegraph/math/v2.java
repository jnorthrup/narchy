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

package spacegraph.math;

/**
 * A 2-element vector that is represented by single-precision floating
 * point x,y coordinates.
 */
public class v2 extends Tuple2f {


    /**
     * Constructs and initializes a Vector2f from the specified xy coordinates.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     */
    public v2(float x, float y) {
        super(x, y);
    }


    /**
     * Constructs and initializes a Vector2f from the specified array.
     *
     * @param v the array of length 2 containing xy in order
     */
    public v2(float[] v) {
        super(v);
    }


    /**
     * Constructs and initializes a Vector2f from the specified Vector2f.
     *
     * @param v1 the Vector2f containing the initialization x y data
     */
    public v2(v2 v1) {
        super(v1);
    }


    /**
     * Constructs and initializes a Vector2f from the specified Vector2d.
     *
     * @param v1 the Vector2d containing the initialization x y data
     */
    public v2(Vector2d v1) {
        super(v1);
    }


    /**
     * Constructs and initializes a Vector2f from the specified Tuple2f.
     *
     * @param t1 the Tuple2f containing the initialization x y data
     */
    public v2(Tuple2f t1) {
        super(t1);
    }


    /**
     * Constructs and initializes a Vector2f from the specified Tuple2d.
     *
     * @param t1 the Tuple2d containing the initialization x y data
     */
    public v2(Tuple2d t1) {
        super(t1);
    }


    /**
     * Constructs and initializes a Vector2f to (0,0).
     */
    public v2() {
        super();
    }


//    /**
//     * Sets the value of this vector to the normalization of vector v1.
//     * @param v1 the un-normalized vector
//     */
//    public final void normalize(v2 v1)
//    {
//
//        float norm = (float) (1.0 / Math.sqrt(v1.x * v1.x + v1.y * v1.y));
//        this.x = v1.x*norm;
//        this.y = v1.y*norm;
//    }

//    /**
//     * Normalizes this vector in place.
//     */
//    public final v2 normalize()
//    {
//
//        float norm = (float)
//                (1.0 / Math.sqrt(this.x * this.x + this.y * this.y));
//        this.x *= norm;
//        this.y *= norm;
//        return this;
//    }


    /**
     * Returns the angle in radians between this vector and the vector
     * parameter; the return value is constrained to the range [0,PI].
     *
     * @param v1 the other vector
     * @return the angle in radians in the range [0,PI]
     */
    public final float angle(Tuple2f v1) {
        double vDot = this.dot(v1) / (this.lengthSquared());
        if (vDot < -1.0) vDot = -1.0;
        if (vDot > 1.0) vDot = 1.0;
        return ((float) (Math.acos(vDot)));
    }

    /**
     * Computes the dot product of the this vector and vector v1.
     *
     * @param v1 the other vector
     */
    public final float dot(Tuple2f v1) {
        return (this.x * v1.x + this.y * v1.y);
    }

    public boolean inUnit() {
        return x >= 0 && x <= 1f && y >= 0 && y <= 1f;
    }




//    /**
//     * read-only v2
//     */
//    public static class v2RO extends v2 {
//        public v2RO(float x, float y) {
//            super(x, y);
//        }
//
//        @Override
//        public Tuple2f set(float x, float y) {
//            throw new RuntimeException("cant modify");
//        }
//    }

}
