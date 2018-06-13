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

import static org.oakgp.NodeType.longType;

/**
 * Provides support for working with instances of {@code java.lang.Long}.
 */
public final class LongFunc extends NumFunc<Long> {
    /**
     * Singleton instance.
     */
    public static final LongFunc the = new LongFunc();

    /**
     * @see #INTEGER_UTILS
     */
    private LongFunc() {
        super(longType(), 0L, 1L, 2L);
    }

    @Override
    protected Long add(Long i1, Long i2) {
        return i1 + i2;
    }

    @Override
    protected Long subtract(Long i1, Long i2) {
        return i1 - i2;
    }

    @Override
    protected Long multiply(Long i1, Long i2) {
        return i1 * i2;
    }

    @Override
    protected Long divide(Long i1, Long i2) {
        return i1 / i2;
    }
}
