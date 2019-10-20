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

import jcog.math.v2;

/**
 * Contains methods from MathUtils that rely on JVM features. These are separated out from
 * MathUtils so that they can be overridden when compiling for GWT.
 */
public class PlatformMathUtils {

    private static final float SHIFT23 = (float) (1 << 23);
    private static final float INV_SHIFT23 = 1.0f / SHIFT23;
    private static final double DEFICIT = 0.0001; 

    public static float fastPow(float a, float b) {
        float x = (float) Float.floatToRawIntBits(a);
        x *= INV_SHIFT23;
        x -= 127.0F;
        float y = x - (float) (x >= (float) 0 ? (int) x : (int) x - 1);
        b *= x + (y - y * y) * 0.346607f;
        y = b - (float) (b >= (float) 0 ? (int) b : (int) b - 1);
        y = (y - y * y) * 0.33971f;
        return Float.intBitsToFloat((int) ((b + 127.0F - y) * SHIFT23));
    }

    /**
     * @param a
     * @param b
     * @return Kvadraticky uhol v rozmedzi (0-4) medzi vektorom (b - a) a vektorom (0, 1).
     */
    public static double angle(v2 a, v2 b) {
        double vx = (double) (b.x - a.x);
        double vy = (double) (b.y - a.y);
        double x = vx * vx;
        double cos = x / (x + vy * vy);
        return vx > (double) 0 ? vy > (double) 0 ? 3.0 + cos : 1.0 - cos : vy > (double) 0 ? 3.0 - cos : 1.0 + cos;
    }

    /**
     * @param a 1. bod usecky
     * @param b 2. bod usecky
     * @param v Bod, u ktoreho sa rozhoduje, na ktorej strane sa nachadza.
     * @return <tt>-1</tt>, ak sa bod <tt>v</tt> nachadza na lavo od usecky |ab|<br>
     * <tt>0</tt>, ak body <tt>a, b, v</tt> lezia na jednej priamke.<br>
     * <tt>1</tt>, ak sa bod <tt>v</tt> nachadza na pravo od usecky |ab|<br>
     */
    public static int site(v2 a, v2 b, v2 v) {
        double g = (double) ((b.x - a.x) * (v.y - b.y));
        double h = (double) ((v.x - b.x) * (b.y - a.y));
        return Double.compare(g, h);
    }

    /**
     * @param a 1. bod usecky
     * @param b 2. bod usecky
     * @param v Bod, u ktoreho sa rozhoduje, na ktorej strane sa nachadza.
     * @return Rovnako ako funkcia site, s tym rozdielom, ze zohladnuje deficit.
     */
    public static int siteDef(v2 a, v2 b, v2 v) {
        double ux = (double) (b.x - a.x);
        double uy = (double) (b.y - a.y);
        double wx = (double) (b.x - v.x);
        double wy = (double) (b.y - v.y);
        double sin = (ux * wy - wx * uy) / Math.sqrt((ux * ux + uy * uy) * (wx * wx + wy * wy));
        if (Double.isNaN(sin) || Math.abs(sin) < DEFICIT) {
            return 0;
        }
        return sin < (double) 0 ? 1 : -1;
    }
}
