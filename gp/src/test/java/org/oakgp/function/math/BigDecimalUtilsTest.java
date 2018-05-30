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

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BigDecimalUtilsTest {
    private static final NumFunc<BigDecimal> UTILS = BigDecimalFunc.the;

    @Test
    public void testAdd() {
        assertEquals(BigDecimal.valueOf(9), UTILS.add(BigDecimal.valueOf(7), BigDecimal.valueOf(2)));
    }

    @Test
    public void testSubtract() {
        assertEquals(BigDecimal.valueOf(5), UTILS.subtract(BigDecimal.valueOf(7), BigDecimal.valueOf(2)));
    }

    @Test
    public void testMultiply() {
        assertEquals(BigDecimal.valueOf(14), UTILS.multiply(BigDecimal.valueOf(7), BigDecimal.valueOf(2)));
    }

    @Test
    public void testDivide() {
        assertEquals(BigDecimal.valueOf(3.5), UTILS.divide(BigDecimal.valueOf(7), BigDecimal.valueOf(2)));
    }
}
