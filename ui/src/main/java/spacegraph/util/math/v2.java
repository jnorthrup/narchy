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
import jcog.pri.ScalarValue;

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
    private float dot(Tuple2f v1) {
        return (this.x * v1.x + this.y * v1.y);
    }

    public boolean inUnit() {
        return x >= 0 && x <= 1f && y >= 0 && y <= 1f;
    }

    public float minDimension() {
        return Math.min(x, y);
    }


    public int xInt() {
        return Math.round(x);
    }
    public int yInt() {
        return Math.round(y);
    }

    public boolean equalsZero() {
        return Util.equals(x, 0, ScalarValue.EPSILON) && Util.equals(y, 0, ScalarValue.EPSILON);
    }
}
