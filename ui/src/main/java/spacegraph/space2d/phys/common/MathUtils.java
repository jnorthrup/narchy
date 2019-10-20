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
/*
 * JBox2D - A Java Port of Erin Catto's Box2D
 *
 * JBox2D homepage: http:
 * Box2D homepage: http:
 *
 * This software is provided 'as-is', without any express or implied
 * warranty.  In no event will the authors be held liable for any damages
 * arising from the use of this software.
 *
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 *
 * 1. The origin of this software must not be misrepresented; you must not
 * claim that you wrote the original software. If you use this software
 * in a product, an acknowledgment in the product documentation would be
 * appreciated but is not required.
 * 2. Altered source versions must be plainly marked as such, and must not be
 * misrepresented as being the original software.
 * 3. This notice may not be removed or altered from any source distribution.
 */

package spacegraph.space2d.phys.common;

import jcog.math.v2;

import java.util.Random;

/**
 * A few math methods that don't fit very well anywhere else.
 */
public class MathUtils extends PlatformMathUtils {
    public static final float PI = (float) Math.PI;
    public static final float TWOPI = (float) (Math.PI * 2.0);
    public static final float INV_PI = 1f / PI;
    private static final float HALF_PI = PI / 2.0F;
    public static final float QUARTER_PI = PI / 4.0F;
    public static final float THREE_HALVES_PI = TWOPI - HALF_PI;

    /**
     * Degrees to radians conversion factor
     */
    public static final float DEG2RAD = PI / 180.0F;

    /**
     * Radians to degrees conversion factor
     */
    public static final float RAD2DEG = 180.0F / PI;

    private static final float[] sinLUT = new float[Settings.SINCOS_LUT_LENGTH];

    static {
        for (int i = 0; i < Settings.SINCOS_LUT_LENGTH; i++) {
            sinLUT[i] = (float) Math.sin((double) (i * Settings.SINCOS_LUT_PRECISION));
        }
    }

    public static float sinLUT(float x) {
        x %= TWOPI;

        if (x < (float) 0) {
            x += TWOPI;
        }

        if (Settings.SINCOS_LUT_LERP) {

            x /= Settings.SINCOS_LUT_PRECISION;

            int index = (int) x;

            if (index != 0) {
                x = x % (float) index;
            }

            
            if (index == Settings.SINCOS_LUT_LENGTH - 1) {
                return ((1.0F - x) * sinLUT[index] + x * sinLUT[0]);
            } else {
                return ((1.0F - x) * sinLUT[index] + x * sinLUT[index + 1]);
            }

        } else {
            return sinLUT[MathUtils.round(x / Settings.SINCOS_LUT_PRECISION) % Settings.SINCOS_LUT_LENGTH];
        }
    }

    public static int abs(int x) {
        int y = x >> 31;
        return (x ^ y) - y;
    }

    public static int floor(float x) {
        if (Settings.FAST_FLOOR) {
            return fastFloor(x);
        } else {
            return (int) StrictMath.floor((double) x);
        }
    }

    private static int fastFloor(float x) {
        int y = (int) x;
        if (x < (float) y) {
            return y - 1;
        }
        return y;
    }

    public static int ceil(float x) {
        if (Settings.FAST_CEIL) {
            return fastCeil(x);
        } else {
            return (int) StrictMath.ceil((double) x);
        }
    }

    private static int fastCeil(float x) {
        int y = (int) x;
        if (x > (float) y) {
            return y + 1;
        }
        return y;
    }

    private static int round(float x) {
        if (Settings.FAST_ROUND) {
            return floor(x + .5f);
        } else {
            return StrictMath.round(x);
        }
    }

    /**
     * Rounds up the value to the nearest higher power^2 value.
     *
     * @param x
     * @return power^2 value
     */
    public static int ceilPowerOf2(int x) {
        int pow2 = 1;
        while (pow2 < x) {
            pow2 <<= 1;
        }
        return pow2;
    }

    public static float max(float a, float b) {
        return Math.max(a, b);
    }

    public static int max(int a, int b) {
        return Math.max(a, b);
    }

    public static float min(float a, float b) {
        return Math.min(a, b);
    }

    public static int min(int a, int b) {
        return Math.min(a, b);
    }

    public static float map(float val, float fromMin, float fromMax,
                            float toMin, float toMax) {
        float mult = (val - fromMin) / (fromMax - fromMin);
        float res = toMin + mult * (toMax - toMin);
        return res;
    }

    /**
     * Returns the closest value to 'a' that is in between 'low' and 'high'
     */
    public static float clamp(float a, float low, float high) {
        return max(low, min(a, high));
    }

    public static v2 clamp(v2 a, v2 low, v2 high) {
        v2 min = new v2();
        min.x = Math.min(a.x, high.x);
        min.y = Math.min(a.y, high.y);
        min.x = Math.max(low.x, min.x);
        min.y = Math.max(low.y, min.y);
        return min;
    }

    public static void clampToOut(v2 a, v2 low, v2 high, v2 dest) {
        dest.x = Math.min(a.x, high.x);
        dest.y = Math.min(a.y, high.y);
        dest.x = Math.max(low.x, dest.x);
        dest.y = Math.max(low.y, dest.y);
    }

    /**
     * Next Largest Power of 2: Given a binary integer value x, the next largest power of 2 can be
     * computed by a SWAR algorithm that recursively "folds" the upper bits into the lower bits. This
     * process yields a bit vector with the same most significant 1 as x, but all 1's below it. Adding
     * 1 to that value yields the next largest power of 2.
     */
    public static int nextPowerOfTwo(int x) {
        x |= x >> 1;
        x |= x >> 2;
        x |= x >> 4;
        x |= x >> 8;
        x |= x >> 16;
        return x + 1;
    }

    public static boolean isPowerOfTwo(int x) {
        return x > 0 && (x & x - 1) == 0;
    }

    public static float fastAtan2(float y, float x) {
        if (x == 0.0f) {
            if (y > 0.0f) return HALF_PI;
            if (y == 0.0f) return 0.0f;
            return -HALF_PI;
        }
        float atan;
        float z = y / x;
        if (Math.abs(z) < 1.0f) {
            atan = z / (1.0f + 0.28f * z * z);
            if (x < 0.0f) {
                if (y < 0.0f) return atan - PI;
                return atan + PI;
            }
        } else {
            atan = HALF_PI - z / (z * z + 0.28f);
            if (y < 0.0f) return atan - PI;
        }
        return atan;
    }

    public static float reduceAngle(float theta) {
        theta %= TWOPI;
        if (Math.abs(theta) > PI) {
            theta -= TWOPI;
        }
        if (Math.abs(theta) > HALF_PI) {
            theta = PI - theta;
        }
        return theta;
    }

    public static float randomFloat(float argLow, float argHigh) {
        return (float) Math.random() * (argHigh - argLow) + argLow;
    }

    public static float randomFloat(Random r, float argLow, float argHigh) {
        return r.nextFloat() * (argHigh - argLow) + argLow;
    }

    public static float distanceSquared(v2 v1, v2 v2) {
        float dx = (v1.x - v2.x);
        float dy = (v1.y - v2.y);
        return dx * dx + dy * dy;
    }

    public static float distance(v2 v1, v2 v2) {
        return (float) Math.sqrt((double) distanceSquared(v1, v2));
    }
}