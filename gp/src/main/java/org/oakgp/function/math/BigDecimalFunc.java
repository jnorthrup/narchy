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

import java.math.BigDecimal;

import static org.oakgp.NodeType.bigDecimalType;

/**
 * Provides support for working with instances of {@code java.math.BigDecimal}.
 */
public final class BigDecimalFunc extends NumFunc<BigDecimal> {
    /**
     * Singleton instance.
     */
    public static final BigDecimalFunc the = new BigDecimalFunc();

    /**
     * @see #the
     */
    private BigDecimalFunc() {
        super(bigDecimalType(), BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.valueOf(2));
    }

    @Override
    protected BigDecimal add(BigDecimal i1, BigDecimal i2) {
        return i1.add(i2);
    }

    @Override
    protected BigDecimal subtract(BigDecimal i1, BigDecimal i2) {
        return i1.subtract(i2);
    }

    @Override
    protected BigDecimal multiply(BigDecimal i1, BigDecimal i2) {
        return i1.multiply(i2);
    }

    @Override
    protected BigDecimal divide(BigDecimal i1, BigDecimal i2) {
        return i1.divide(i2);
    }
}
