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

import com.jogamp.opengl.GL2;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import spacegraph.space2d.phys.common.Color3f;
import spacegraph.video.Draw;

import java.awt.*;


/**
 * A four-element color represented by single precision floating point
 * x, y, z, and w values.  The x, y, z, and w values represent the red,
 * blue, green, and alpha color values, respectively. Color and alpha
 * components should be in the range [0.0, 1.0].
 * <p>
 * Java 3D assumes that a linear (gamma-corrected) visual is used for
 * all colors.
 */
public class Color4f extends Tuple4f {

    public static final Color3f BLACK = new Color3f((float) 0, (float) 0, (float) 0);

    /**
     * Constructs and initializes a Color4f from the specified xyzw
     * coordinates.
     *
     * @param x the red color value
     * @param y the green color value
     * @param z the blue color value
     * @param w the alpha value
     */
    public Color4f(float x, float y, float z, float w) {
        super(x, y, z, w);
    }


    /**
     * Constructs and initializes a Color4f from the array of length 4.
     *
     * @param c the array of length 4 containing r,g,b,a in order
     */
    public Color4f(float[] c) {
        super(c);
    }


    /**
     * Constructs and initializes a Color4f from the specified Color4f.
     *
     * @param c1 the Color4f containing the initialization r,g,b,a data
     */
    public Color4f(Color4f c1) {
        super(c1);
    }


    /**
     * Constructs and initializes a Color4f from the specified Tuple4f.
     *
     * @param t1 the Tuple4f containing the initialization r,g,b,a data
     */
    public Color4f(Tuple4f t1) {
        super(t1);
    }


    /**
     * Constructs and initializes a Color4f from the specified Tuple4d.
     *
     * @param t1 the Tuple4d containing the initialization r,g,b,a data
     */
    public Color4f(Tuple4d t1) {
        super(t1);
    }


    /**
     * Constructs and initializes a Color4f from the specified AWT
     * Color object.
     * No conversion is done on the color to compensate for
     * gamma correction.
     *
     * @param color the AWT color with which to initialize this
     *              Color4f object
     * @since vecmath 1.2
     */
    public Color4f(Color color) {
        super((float) color.getRed() / 255.0f,
                (float) color.getGreen() / 255.0f,
                (float) color.getBlue() / 255.0f,
                (float) color.getAlpha() / 255.0f);
    }


    /**
     * Constructs and initializes a Color4f to (0.0, 0.0, 0.0, 0.0).
     */
    public Color4f() {
        super();
    }


    /**
     * Sets the r,g,b,a values of this Color4f object to those of the
     * specified AWT Color object.
     * No conversion is done on the color to compensate for
     * gamma correction.
     *
     * @param color the AWT color to copy into this Color4f object
     * @since vecmath 1.2
     */
    public final void set(Color color) {
        x = (float) color.getRed() / 255.0f;
        y = (float) color.getGreen() / 255.0f;
        z = (float) color.getBlue() / 255.0f;
        w = (float) color.getAlpha() / 255.0f;
    }


    /**
     * Returns a new AWT color object initialized with the r,g,b,a
     * values of this Color4f object.
     *
     * @return a new AWT Color object
     * @since vecmath 1.2
     */
    public final Color awt() {
        int r = Math.round(x * 255.0f);
        int g = Math.round(y * 255.0f);
        int b = Math.round(z * 255.0f);
        int a = Math.round(w * 255.0f);

        return new Color(r, g, b, a);
    }

    public final void apply(GL2 gl) {
        gl.glColor4f(x, y, z, w);
    }

    public Color4f r(float v) {
        x = v;
        return this;
    }

    public Color4f g(float v) {
        y = v;
        return this;
    }

    public Color4f b(float v) {
        z = v;
        return this;
    }

    public Color4f a(float v) {
        w = v;
        return this;
    }

    public Color4f hsl(int hash, float sat, float lightness) {
        return hsl((float) Math.abs(hash) /1000f % 1.0F, sat, lightness, 1f);
    }

    public Color4f hsl(float hue, float sat, float lightness, float a) {
        //HACK
        float[] f = new float[3];

        //hsb(f, hue, saturation, brightness, a);
        Draw.hsl(f, hue, sat, lightness);

        x = f[0]; y = f[1]; z = f[2]; w = a;
        return this;
    }

    /** does not mutate the stored values.
     * componentFunction applied to the RGB but not the alpha channel */
    public void set(FloatToFloatFunction componentFunction, GL2 gl) {
        float r = componentFunction.valueOf(x);
        float g = componentFunction.valueOf(y);
        float b = componentFunction.valueOf(z);
        float a = this.w;
        gl.glColor4f(r, g, b, a);
    }

    public Color toAWT() {
        return new Color(x, y, z, w);
    }
}
