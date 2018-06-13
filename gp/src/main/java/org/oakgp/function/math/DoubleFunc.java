/*
 * Copyright 2015 S. Webber
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
package org.oakgp.function.math;

import static org.oakgp.NodeType.doubleType;

/**
 * Provides support for working with instances of {@code java.lang.Double}.
 */
public final class DoubleFunc extends NumFunc<Double> {
    /**
     * Singleton instance.
     */
    public static final DoubleFunc the = new DoubleFunc();

    /**
     * @see #the
     */
    private DoubleFunc() {
        super(doubleType(), 0d, 1d, 2d);
    }

    @Override
    protected Double add(Double i1, Double i2) {
        return i1 + i2;
    }

    @Override
    protected Double subtract(Double i1, Double i2) {
        return i1 - i2;
    }

    @Override
    protected Double multiply(Double i1, Double i2) {
        return i1 * i2;
    }

    @Override
    protected Double divide(Double i1, Double i2) {
        return i1 / i2;
    }
}
