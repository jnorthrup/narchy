/*
 * Copyright 2015 S. Webber
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.oakgp.function.math;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BigIntegerUtilsTest {
    private static final NumFunc<BigInteger> UTILS = BigIntegerFunc.the;

    @Test
    public void testAdd() {
        assertEquals(BigInteger.valueOf(9), UTILS.add(BigInteger.valueOf(7), BigInteger.valueOf(2)));
    }

    @Test
    public void testSubtract() {
        assertEquals(BigInteger.valueOf(5), UTILS.subtract(BigInteger.valueOf(7), BigInteger.valueOf(2)));
    }

    @Test
    public void testMultiply() {
        assertEquals(BigInteger.valueOf(14), UTILS.multiply(BigInteger.valueOf(7), BigInteger.valueOf(2)));
    }

    @Test
    public void testDivide() {
        assertEquals(BigInteger.valueOf(3), UTILS.divide(BigInteger.valueOf(7), BigInteger.valueOf(2)));
    }
}
