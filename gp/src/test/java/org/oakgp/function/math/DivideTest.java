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

import org.oakgp.function.AbstractFunctionTest;
import org.oakgp.function.Function;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.oakgp.Type.integerType;

public class DivideTest extends AbstractFunctionTest {
    @Override
    protected Divide getFunction() {
        return IntFunc.the.getDivide();
    }

    @Override
    public void testEvaluate() {
        
        evaluate("(/ 21 3)").to(7);
        evaluate("(/ 5 2)").to(2);
        evaluate("(/ 12 1)").to(12);
        evaluate("(/ 12 12)").to(1);

        
        evaluate("(/ 21L 3L)").to(7L);
        evaluate("(/ 5L 2L)").to(2L);

        
        evaluate("(/ 21I 3I)").to(BigInteger.valueOf(7));
        evaluate("(/ 5I 2I)").to(BigInteger.valueOf(2));

        
        evaluate("(/ 21.0 3.0)").to(7d);
        evaluate("(/ 5.0 2.0)").to(2.5d);

        
        evaluate("(/ 21D 3D)").to(BigDecimal.valueOf(7));
        evaluate("(/ 5D 2D)").to(BigDecimal.valueOf(2.5));

        
        evaluate("(/ 12 0)").to(1);
        evaluate("(/ 12L 0L)").to(1L);
        evaluate("(/ 12I 0I)").to(BigInteger.valueOf(1));
        evaluate("(/ 12.0 0.0)").to(1d);
        evaluate("(/ 12D 0D)").to(BigDecimal.valueOf(1));
    }

    @Override
    public void testCanSimplify() {
        simplify("(/ 4 0)").to("1");
        simplify("(/ 4 1)").to("4");
        simplify("(/ 4 -1)").to("-4");
        simplify("(/ -4 -1)").to("4");
        simplify("(/ v0 -1)").to("(- 0 v0)");
    }

    @Override
    public void testCannotSimplify() {
        cannotSimplify("(/ 1 v0)", integerType());
    }

    @Override
    protected Function[] getFunctionSet() {
        return new Function[]{getFunction(), IntFunc.the.subtract, LongFunc.the.getDivide(), DoubleFunc.the.getDivide(),
                BigIntegerFunc.the.getDivide(), BigDecimalFunc.the.getDivide()};
    }
}
