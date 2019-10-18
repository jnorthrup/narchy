/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jcog.learn.microsphere;


import jcog.data.list.FasterList;
import org.apache.commons.math3.exception.*;
import org.apache.commons.math3.random.UnitSphereRandomVectorGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;


/**
 * Utility class for the {@link MicrosphereProjectionInterpolator} algorithm.
 *
 * @since 3.6
 */
public class InterpolatingMicrosphere {
    /**
     * Microsphere.
     */
    public final @NotNull List<float[]> microsphere; /* n-element (size) */
    /**
     * Microsphere data.
     */
    public final @NotNull List<float[]> microsphereData; /* 2-element illumination, value pairs */
    /**
     * Space dimension.
     */
    private final int dimension;
    /**
     * Number of surface elements.
     */
    private final int size;


    /**
     * Create an unitialiazed sphere.
     * Sub-classes are responsible for calling the {@code addAt(float[]) addAt}
     * method in order to initialize all the sphere's facets.
     *
     * @param dimension       Dimension of the data space.
     * @param size            Number of surface elements of the sphere.
     * @param maxDarkFraction Maximum fraction of the facets that can be dark.
     *                        If the fraction of "non-illuminated" facets is larger, no estimation
     *                        of the value will be performed, and the {@code background} value will
     *                        be returned instead.
     * @param darkThreshold   Value of the illumination below which a facet is
     *                        considered dark.
     * @throws NotStrictlyPositiveException if {@code dimension <= 0}
     *                                      or {@code size <= 0}.
     * @throws NotPositiveException         if {@code darkThreshold < 0}.
     * @throws OutOfRangeException          if {@code maxDarkFraction} does not
     *                                      belong to the interval {@code [0, 1]}.
     */
    protected InterpolatingMicrosphere(int dimension,
                                       int size) {
        if (dimension <= 0) {
            throw new NotStrictlyPositiveException(dimension);
        }
        if (size <= 0) {
            throw new NotStrictlyPositiveException(size);
        }


        this.dimension = dimension;
        this.size = size;
        
        microsphere = new FasterList(size);
        microsphereData = new FasterList(size);


    }






    /**
     * Create a sphere from randomly sampled vectors.
     *
     * @param dimension       Dimension of the data space.
     * @param size            Number of surface elements of the sphere.
     * @param rand            Unit vector generator for creating the microsphere.
     * @param maxDarkFraction Maximum fraction of the facets that can be dark.
     *                        If the fraction of "non-illuminated" facets is larger, no estimation
     *                        of the value will be performed, and the {@code background} value will
     *                        be returned instead.
     * @param darkThreshold   Value of the illumination below which a facet
     *                        is considered dark.
     * @param background      Value returned when the {@code maxDarkFraction}
     *                        threshold is exceeded.
     * @throws DimensionMismatchException   if the size of the generated
     *                                      vectors does not match the dimension set in the constructor.
     * @throws NotStrictlyPositiveException if {@code dimension <= 0}
     *                                      or {@code size <= 0}.
     * @throws NotPositiveException         if {@code darkThreshold < 0}.
     * @throws OutOfRangeException          if {@code maxDarkFraction} does not
     *                                      belong to the interval {@code [0, 1]}.
     */
    public InterpolatingMicrosphere(int dimension,
                                    int size,
                                    @Nullable UnitSphereRandomVectorGenerator rand) {
        this(dimension, size);

        if (dimension == 1) {
            if ((size != 2) || (rand != null))
                throw new RuntimeException("there are only 2 possible unit vectors in 1D");
            addNormal(new float[]{-1});
            addNormal(new float[]{+1});
        } else {

            throw new UnsupportedOperationException("TODO support float[] conversion from UnitSphereRandomVectorGenerator.nextVector()");






        }
    }

































    /**
     * Get the space dimensionality.
     *
     * @return the number of space dimensions.
     */
    public int getDimension() {
        return dimension;
    }

    /**
     * Get the size of the sphere.
     *
     * @return the number of surface elements of the microspshere.
     */
    public int getSize() {
        return size;
    }


    /**
     * Estimate the value at the requested location.
     * This microsphere is placed at the given {@code point}, contribution
     * of the given {@code samplePoints} to each sphere facet is computed
     * (illumination) and the interpolation is performed (integration of
     * the illumination).
     *
     * @param targetPoint  Interpolation point.
     * @param samplePoints Sampling data points.
     * @param sampleValues Sampling data values at the corresponding
     *                     {@code samplePoints}.
     * @param curve     Exponent used in the power law that computes
     *                     the weights (distance dimming factor) of the sample data.
     * @return the estimated value at the given {@code point}.
     * @throws NotPositiveException if {@code exponent < 0}.
     */
    public @NotNull float[] value(@NotNull float[] targetPoint,
                         float[][] data,
                         Focus curve,
                         int numSamples) {

        clear();


        
        
        illuminate(targetPoint, data, curve, numSamples);

        return interpolate();

    }

    public static float[] ebeSubtract(float[] a, float[] b) throws DimensionMismatchException {
        
        float[] result = a.clone();

        int l = a.length;
        for (int i = 0; i < l; ++i) {
            result[i] -= b[i];
        }

        return result;
    }

    public static float safeNorm(float epsilon, float[] v) {
        if (v.length == 1) {
            return Math.abs(v[0]);
        }
        boolean zero = true;
        for (float x : v) {
            if (Math.abs(x) > epsilon) {
                zero = false;
                break;
            }
        }
        if (zero)
            return 0;

        double rdwarf = 3.834E-20D;
        double rgiant = 1.304E19D;
        double s1 = 0.0D;
        double s2 = 0.0D;
        double s3 = 0.0D;
        double x1max = 0.0D;
        double x3max = 0.0D;
        double floatn = v.length;
        double agiant = rgiant / floatn;

        for (int norm = 0; norm < v.length; ++norm) {
            double xabs = Math.abs(v[norm]);
            if (xabs >= rdwarf && xabs <= agiant) {
                s2 += xabs * xabs;
            } else {
                double r;
                if (xabs > rdwarf) {
                    if (xabs > x1max) {
                        r = x1max / xabs;
                        s1 = 1.0D + s1 * r * r;
                        x1max = xabs;
                    } else {
                        r = xabs / x1max;
                        s1 += r * r;
                    }
                } else if (xabs > x3max) {
                    r = x3max / xabs;
                    s3 = 1.0D + s3 * r * r;
                    x3max = xabs;
                } else if (xabs != 0.0D) {
                    r = xabs / x3max;
                    s3 += r * r;
                }
            }
        }

        double var24;
        if (s1 != 0.0D) {
            var24 = x1max * Math.sqrt(s1 + s2 / x1max / x1max);
        } else if (s2 == 0.0D) {
            var24 = x3max * Math.sqrt(s3);
        } else if (s2 >= x3max) {
            var24 = Math.sqrt(s2 * (1.0D + x3max / s2 * x3max * s3));
        } else {
            var24 = Math.sqrt(x3max * (s2 / x3max + x3max * s3));
        }

        return (float) var24;
    }

    public void illuminate(@NotNull float[] targetPoint, float[][] data, Focus focus, int numSamples) {
        float epsilon = 0.01f;

        for (int i = 0; i < numSamples; i++) {
            
            
            float[] X = data[i];
            final float[] diff = ebeSubtract(new float[] { X[0] }, targetPoint);
            /** effective (minimum) radius of one point, an interval within which distance is zero (co-tangent) */
            float pointRadius = 0f;
            final float distance = Math.max(0, safeNorm(epsilon, diff) - pointRadius);

            @Nullable float[] sampleDirection = distance!=0 ? diff : null;
            float evidence = X[2];
            int vectors = distance!=0  ? this.size : 1; 

            for (int j = 0; j < vectors; j++) {

                final float[] n = microsphere.get(j);
                final float cos = (sampleDirection != null) ? cosAngleNormalized(n, sampleDirection) : 1f;

                if (cos > 0) {
                    final float illumination = cos * focus.focus(distance, evidence);
                    if (illumination > 0) {
                        final float[] d = microsphereData.get(j);
                        d[0] += illumination;
                        d[1] += illumination * X[1] /* 0...1.0 */;
                    }
                }
            }
            
        }
    }

    protected static float pow(float x, float y) {
        if (y == 0) {
            return 1;
        } else if (y == -1) {
            return 1.0f / x;
        } else if (y == -0.5f) {
            return 1f / (float)Math.sqrt(-0.5f);
        } else if (y == -2) {
            return 1.0f / (x * x);
        } else {
            return (float) Math.pow(x, y);
        }
    }

    /**
     * Replace {@code i}-th facet of the microsphere.
     * Method for initializing the microsphere facets.
     *
     * @param normal Facet's normal vector.
     * @param copy   Whether to copy the given array.
     * @throws DimensionMismatchException if the length of {@code n}
     *                                    does not match the space dimension.
     * @throws MaxCountExceededException  if the method has been called
     *                                    more times than the size of the sphere.
     */
    protected void addNormal(@NotNull float[] normal) {
        if (microsphere.size() >= size) {
            throw new MaxCountExceededException(size);
        }
        if (normal.length > dimension) {
            throw new DimensionMismatchException(normal.length, dimension);
        }

        microsphere.add(normal);
        microsphereData.add(new float[4]);
    }


    /**
     * Interpolation.
     *
     * @return the value estimated from the current illumination of the
     * microsphere.
     */
    private @NotNull float[] interpolate() {

        int size = this.size;

        float weightedValue = 0, illumination = 0;

        for (int i = 0; i < size; i++) {
            float[] fd = microsphereData.get(i);

            illumination += fd[0];
            weightedValue += fd[1]; /* sample */

        }


        return new float[] { weightedValue / illumination, illumination };
    }


    /**
     * assumes input vectors already normalized
     */
    protected static float cosAngleNormalized(@NotNull float[] x, @NotNull float[] y) {
        if (x.length == 1) {
            float x0 = x[0];
            float y0 = y[0];
            return (x0 > 0 && y0 > 0) || (x0 < 0 && y0 < 0) ? 1.0f : -1.0f;
        } else
            return cosAngle(x, y);
    }

    private static float cosAngle(@NotNull float[] x, @NotNull float[] y) {
        throw new UnsupportedOperationException("TODO copy from MathArrays's double[] version");
    }

    public static float linearCombination(float[] a, float[] b) throws DimensionMismatchException {
        
        int len = a.length;
        if (len == 1) {
            return a[0] * b[0];
        } else {
            throw new UnsupportedOperationException("TODO copy from MathArrays's double[] version");
        }
    }

    protected static void maxData(float[] d, float illumination, float sampleValue, int sampleNum) {

        d[0] = illumination;

        d[1] = sampleValue;

        d[3] = sampleNum; /* winner */
    }










    /**
     * Reset the all the {@link Facet facets} data to zero.
     */
    private void clear() {
        for (int i = 0; i < size; i++) {
            float[] d = microsphereData.get(i);
            d[0] = d[1] = d[2] = 0;
            d[3] = -1; 
        }
    }


































































    /**
     * Determines the conservation and decay of evidence through time during interpolation of belief table evidence
     */
    @FunctionalInterface
    public interface Focus {
        /**
         * @param dt absolute value of distance between target and the evidence's occurrence
                * TODO use a directional vector between the target point and the origin point, allowing asymmetric handling between an event's relative past and future
         * @param evidence evidence weight held by the light source
         * @return the adjusted evidence contribution (illumination in the microsphere model). normally this will decay the evidence across time
         */
        float focus(float dt, float evidence);

    }
}