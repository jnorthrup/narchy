/*
 * Copyright 2011 - 2015 Metamarkets Group Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metamx.collections.spatial.search;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;
import com.metamx.collections.spatial.ImmutablePoint;
import jcog.Util;

import java.nio.ByteBuffer;

/**
 */
public class RadiusBound extends RectangularBound {
    private static final byte CACHE_TYPE_ID = 0x01;
    private final float[] coords;
    private final float radius;

    @JsonCreator
    public RadiusBound(
            @JsonProperty("coords") float[] coords,
            @JsonProperty("radius") float radius,
            @JsonProperty("limit") int limit
    ) {
        super(getMinCoords(coords, radius), getMaxCoords(coords, radius), limit);

        this.coords = coords;
        this.radius = radius;
    }
    public RadiusBound(
            float[] coords,
            float radius
    ) {
        this(coords, radius, 0);
    }

    private static float[] getMinCoords(float[] coords, float radius) {
        float[] retVal = new float[coords.length];
        for (int i = 0; i < coords.length; i++) {
            retVal[i] = coords[i] - radius;
        }
        return retVal;
    }

    private static float[] getMaxCoords(float[] coords, float radius) {
        float[] retVal = new float[coords.length];
        for (int i = 0; i < coords.length; i++) {
            retVal[i] = coords[i] + radius;
        }
        return retVal;
    }

    @JsonProperty
    public float[] getCoords() {
        return coords;
    }

    @JsonProperty
    public float getRadius() {
        return radius;
    }

    @Override
    public boolean contains(float[] otherCoords) {
        double total = 0.0;
        for (int i = 0; i < coords.length; i++) {
            total += Util.sqr(otherCoords[i] - coords[i]);
        }

        return (total <= radius*radius);
    }

    @Override
    public Iterable<ImmutablePoint> filter(Iterable<ImmutablePoint> points) {
        return Iterables.filter(
                points,
                point -> contains(point.coord())
        );
    }

    @Override
    public byte[] getCacheKey() {
        final ByteBuffer minCoordsBuffer = ByteBuffer.allocate(coords.length * Floats.BYTES);
        minCoordsBuffer.asFloatBuffer().put(coords);
        final byte[] minCoordsCacheKey = minCoordsBuffer.array();
        final ByteBuffer cacheKey = ByteBuffer
                .allocate(1 + minCoordsCacheKey.length + Ints.BYTES + Floats.BYTES)
                .put(minCoordsCacheKey)
                .putFloat(radius)
                .putInt(getLimit())
                .put(CACHE_TYPE_ID);
        return cacheKey.array();
    }
}
