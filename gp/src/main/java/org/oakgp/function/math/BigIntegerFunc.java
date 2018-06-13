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

import java.math.BigInteger;

import static org.oakgp.NodeType.bigIntegerType;

/**
 * Provides support for working with instances of {@code java.math.BigInteger}.
 */
public final class BigIntegerFunc extends NumFunc<BigInteger> {
    /**
     * Singleton instance.
     */
    public static final BigIntegerFunc the = new BigIntegerFunc();

    /**
     * @see #the
     */
    private BigIntegerFunc() {
        super(bigIntegerType(), BigInteger.ZERO, BigInteger.ONE, BigInteger.valueOf(2));
    }

    @Override
    protected BigInteger add(BigInteger i1, BigInteger i2) {
        return i1.add(i2);
    }

    @Override
    protected BigInteger subtract(BigInteger i1, BigInteger i2) {
        return i1.subtract(i2);
    }

    @Override
    protected BigInteger multiply(BigInteger i1, BigInteger i2) {
        return i1.multiply(i2);
    }

    @Override
    protected BigInteger divide(BigInteger i1, BigInteger i2) {
        return i1.divide(i2);
    }
}
