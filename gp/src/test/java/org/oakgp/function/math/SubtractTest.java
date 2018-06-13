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

import org.oakgp.function.AbstractFnTest;
import org.oakgp.function.Fn;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.oakgp.NodeType.integerType;

public class SubtractTest extends AbstractFnTest {
    @Override
    protected Subtract getFunction() {
        return IntFunc.the.subtract;
    }

    @Override
    public void testEvaluate() {
        
        evaluate("(- 3 21)").to(-18);

        
        evaluate("(- 3L 21L)").to(-18L);

        
        evaluate("(- 3I 21I)").to(BigInteger.valueOf(-18));

        
        evaluate("(- 3.0 21.0)").to(-18d);

        
        evaluate("(- 3D 21D)").to(BigDecimal.valueOf(-18));
    }

    @Override
    public void testCanSimplify() {
        Object[][] assignedValues = {{0, 0}, {0, 1}, {1, 0}, {1, 21}, {2, 14}, {3, -6}, {7, 3}, {-1, 9}, {-7, 0}};

        
        simplify("(- 8 3)").to("5").verifyAll(assignedValues);

        
        simplify("(- v0 0)").to("v0").verifyAll(assignedValues);

        
        simplify("(- v0 v0)").to("0").verifyAll(assignedValues);

        
        simplify("(- 0 (- v0 v1))").to("(- v1 v0)").verifyAll(assignedValues);

        
        simplify("(- v0 -7)").to("(+ 7 v0)").verifyAll(assignedValues);

        
        simplify("(- 1 (+ 1 v0))").to("(- 0 v0)").verifyAll(assignedValues);
        simplify("(- 6 (+ 4 v0))").to("(- 2 v0)").verifyAll(assignedValues);

        
        simplify("(- 1 (- 7 v0))").to("(- v0 6)").verifyAll(assignedValues);
        simplify("(- 1 (- 1 v0))").to("v0").verifyAll(assignedValues);
        simplify("(- 6 (- 4 v0))").to("(+ 2 v0)").verifyAll(assignedValues);
        simplify("(- 1 (- 0 v0))").to("(+ 1 v0)").verifyAll(assignedValues);

        
        simplify("(- 0 (* -3 v0))").to("(* 3 v0)").verifyAll(assignedValues);
        simplify("(- 7 (* -3 v0))").to("(+ 7 (* 3 v0))").verifyAll(assignedValues);

        
        simplify("(- (+ 1 v0) (+ 2 v1))").to("(- v0 (+ 1 v1))").verifyAll(assignedValues);
        simplify("(- (+ 1 v0) (+ 2 v0))").to("-1").verifyAll(assignedValues);

        
        simplify("(- (+ 1 v0) (- 12 v1))").to("(- v0 (- 11 v1))").verifyAll(assignedValues);
        simplify("(- (+ 1 v0) (- 12 v0))").to("(- (* 2 v0) 11)").verifyAll(assignedValues);

        simplify("(- (+ 1 v0) v0)").to("1").verifyAll(assignedValues);

        simplify("(- (- (- (* 2 v0) 9) v1) v1)").to("(- (- (* 2 v0) 9) (* 2 v1))").verifyAll(assignedValues);
        simplify("(- (- (+ (* 2 v0) 9) v1) v1)").to("(- (+ 9 (* 2 v0)) (* 2 v1))").verifyAll(assignedValues);

        simplify("(- (- (- (* 2 v0) 9) v1) v1)").to("(- (- (* 2 v0) 9) (* 2 v1))").verifyAll(assignedValues);

        simplify("(- 5 (- (- (+ (* 2 v0) (* 2 v1)) 1) (- v0 2)))").to("(- 4 (+ v0 (* 2 v1)))").verifyAll(assignedValues);
        simplify("(- (* 2 v0) (- v1 v0))").to("(- (* 3 v0) v1)").verifyAll(assignedValues);
        simplify("(- (* -2 v0) (- v1 v0))").to("(- (* -1 v0) v1)").verifyAll(assignedValues);

        
        
        
        simplify("(- (- 5 (- (- (+ (* 2 v0) (* 2 v1)) 1) (- v1 2))) (* 2 v1))").to("(- (- 4 (* 2 v0)) (* 3 v1))").verifyAll(assignedValues);

        simplify("(- (+ 9 (- v0 (+ 9 v1))) (- 8 v1))").to("(- v0 8)").verifyAll(assignedValues);
        simplify("(- (+ 9 (- v0 (+ 9 v1))) (- v1 v0))").to("(- (* 2 v0) (* 2 v1))").verifyAll(assignedValues);

        
        simplify("(- v0 (- v1 (+ (* 2 v0) (* -2 v1))))").to("(+ (* 3 v0) (* -3 v1))").verifyAll(assignedValues);

        simplify("(- v1 (- (* 2 v0) v1))").to("(- (* 2 v1) (* 2 v0))").verifyAll(assignedValues);
        simplify("(- v1 (- 4 (+ v1 (* 2 v0))))").to("(- (+ (* 2 v1) (* 2 v0)) 4)").verifyAll(assignedValues);

        simplify("(- (+ 9 (- v0 (+ 9 v1))) (- 8 v1))").to("(- v0 8)").verifyAll(assignedValues);
        simplify("(- (+ 9 (- v0 (+ 9 v1))) (- v1 v0))").to("(- (* 2 v0) (* 2 v1))").verifyAll(assignedValues);

        simplify("(- (- v0 v1) (- v1 v0))").to("(- (* 2 v0) (* 2 v1))").verifyAll(assignedValues);

        simplify("(- 0 (* 2 v0))").to("(* -2 v0)").verifyAll(assignedValues);

        simplify("(- 0 (* -162 v0))").to("(* 162 v0)").verifyAll(assignedValues);

        
        
        
        
    }

    @Override
    public void testCannotSimplify() {
        cannotSimplify("(- v0 1)", integerType());
        cannotSimplify("(- 0 v0)", integerType());
    }

    @Override
    protected Fn[] getFunctionSet() {
        return new Fn[]{getFunction(), IntFunc.the.add, IntFunc.the.multiply, LongFunc.the.subtract,
                DoubleFunc.the.subtract, BigIntegerFunc.the.subtract, BigDecimalFunc.the.subtract};
    }
}
