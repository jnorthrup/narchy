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

public class MultiplyTest extends AbstractFunctionTest {
    @Override
    protected Multiply getFunction() {
        return IntFunc.the.getMultiply();
    }

    @Override
    public void testEvaluate() {
        
        evaluate("(* 3 21)").to(63);

        
        evaluate("(* 3L 21L)").to(63L);

        
        evaluate("(* 3I 21I)").to(BigInteger.valueOf(63));

        
        evaluate("(* 3.0 21.0)").to(63d);

        
        evaluate("(* 3D 21D)").to(BigDecimal.valueOf(63));
    }

    @Override
    public void testCanSimplify() {
        
        simplify("(* 8 3)").to("24");

        
        simplify("(* 2 v0)").to("(* 2 v0)");
        simplify("(* v0 2)").to("(* 2 v0)");
        simplify("(* v0 v1)").to("(* v0 v1)");
        simplify("(* v1 v0)").to("(* v0 v1)");

        
        simplify("(* v1 0)").to("0");
        simplify("(* 0 v1)").to("0");

        
        simplify("(* v1 1)").to("v1");
        simplify("(* 1 v1)").to("v1");

        
        simplify("(* 3 (- 0 (+ 1 v0)))").to("(- (* -3 v0) 3)");

        simplify("(* 3 (+ 9 v0))").to("(+ 27 (* 3 v0))");
        simplify("(* 3 (* 9 v0))").to("(* 27 v0)");
        simplify("(* 3 (- 9 v0))").to("(- 27 (* 3 v0))");

        
    }

    @Override
    public void testCannotSimplify() {
        cannotSimplify("(* 2 v0)", integerType());
        cannotSimplify("(* -1 v0)", integerType());
        cannotSimplify("(* v0 v1)", integerType(), integerType());
    }

    @Override
    protected Function[] getFunctionSet() {
        return new Function[]{getFunction(), IntFunc.the.add, IntFunc.the.subtract, LongFunc.the.getMultiply(),
                DoubleFunc.the.getMultiply(), BigIntegerFunc.the.getMultiply(), BigDecimalFunc.the.getMultiply()};
    }
}
