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

import static org.oakgp.NodeType.integerType;

/**
 * Provides support for working with instances of {@code java.lang.Integer}.
 */
public final class IntFunc extends NumFunc<Integer> {
    /**
     * Singleton instance.
     */
    public static final IntFunc the = new IntFunc();

    /**
     * @see #the
     */
    private IntFunc() {
        super(integerType(), 0, 1, 2);
    }

    @Override
    protected Integer add(Integer i1, Integer i2) {
        return i1 + i2;
    }

    @Override
    protected Integer subtract(Integer i1, Integer i2) {
        return i1 - i2;
    }

    @Override
    protected Integer multiply(Integer i1, Integer i2) {
        return i1 * i2;
    }

    @Override
    protected Integer divide(Integer i1, Integer i2) {
        return i1 / i2;
    }
}
