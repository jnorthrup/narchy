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
import org.oakgp.Arguments;
import org.oakgp.Assignments;
import org.oakgp.node.FnNode;
import org.oakgp.node.Node;
import org.oakgp.util.NodeSimplifier;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.oakgp.TestUtils.*;

public class ArithmeticExpressionSimplifierTest {
    private static final ArithmeticExpressionSimplifier SIMPLIFIER = IntFunc.the.simplifier;

    @Test
    public void testCombineWithChildNodes() {
        
        assertCombineWithChildNodes("3", "7", true, "10");
        assertCombineWithChildNodes("3", "7", false, "-4");

        
        assertCombineWithChildNodes("(+ 1 v0)", "7", true, "(+ 8 v0)");
        assertCombineWithChildNodes("(+ 1 v0)", "7", false, "(+ -6 v0)");
        assertCombineWithChildNodes("(+ 1 (- (- v0 9) 8))", "7", true, "(+ 8 (- (- v0 9) 8))");
        assertCombineWithChildNodes("(- 1 v0)", "7", true, "(- 8 v0)");
        assertCombineWithChildNodes("(- 1 v0)", "7", false, "(- -6 v0)");
        assertCombineWithChildNodes("(- 1 (- (- v0 9) 8))", "7", true, "(- 8 (- (- v0 9) 8))");

        
        assertCombineWithChildNodes("(+ 1 (- v0 9))", "v0", true, "(+ 1 (- (* 2 v0) 9))");
        assertCombineWithChildNodes("(+ 1 (- v0 9))", "v0", false, "(+ 1 (- 0 9))");

        
        assertCombineWithChildNodes("(* 3 v0)", "v0", true, "(* 4 v0)");
        assertCombineWithChildNodes("(* 3 v0)", "v0", false, "(* 2 v0)");
        assertCombineWithChildNodes("(* -3 v0)", "v0", true, "(* -2 v0)");
        assertCombineWithChildNodes("(* -3 v0)", "v0", false, "(* -4 v0)");

        
        assertCombineWithChildNodes("(* 3 v0)", "(* 7 v0)", true, "(* 10 v0)");
        assertCombineWithChildNodes("(* 3 v0)", "(* -7 v0)", true, "(* -4 v0)");
        assertCombineWithChildNodes("(* -3 v0)", "(* 7 v0)", true, "(* 4 v0)");
        assertCombineWithChildNodes("(* -3 v0)", "(* -7 v0)", true, "(* -10 v0)");
        assertCombineWithChildNodes("(* 3 v0)", "(* 7 v0)", false, "(* -4 v0)");
        assertCombineWithChildNodes("(* 3 v0)", "(* -7 v0)", false, "(* 10 v0)");
        assertCombineWithChildNodes("(* -3 v0)", "(* 7 v0)", false, "(* -10 v0)");
        assertCombineWithChildNodes("(* -3 v0)", "(* -7 v0)", false, "(* 4 v0)");

        
        assertCombineWithChildNodes("(+ 1 (- v0 9))", "v0", true, "(+ 1 (- (* 2 v0) 9))");
        assertCombineWithChildNodes("(+ 1 (- v0 9))", "v0", false, "(+ 1 (- 0 9))");
        assertCombineWithChildNodes("(+ 1 (* 2 v0))", "v0", true, "(+ 1 (* 3 v0))");
        assertCombineWithChildNodes("(+ 1 (* 2 v0))", "v0", false, "(+ 1 (* 1 v0))");
        assertCombineWithChildNodes("(+ 1 (* 2 v0))", "(* 3 v0)", true, "(+ 1 (* 5 v0))");
        assertCombineWithChildNodes("(+ 1 (* 2 v0))", "(* 3 v0)", false, "(+ 1 (* -1 v0))");
        assertCombineWithChildNodes("(+ 1 (- v0 9))", "(- v0 9)", true, "(+ 1 (* 2 (- v0 9)))");
        assertCombineWithChildNodes("(+ 1 (- 8 (- v0 9)))", "(- v0 9)", true, "(+ 1 (- 8 0))");
        assertCombineWithChildNodes("(+ 1 (- (- v0 9) 8))", "(- v0 9)", true, "(+ 1 (- (* 2 (- v0 9)) 8))");

        assertCannotCombineWithChildNodes("(- v0 9)", "(+ 1 (- v0 9))");
        assertCannotCombineWithChildNodes("(* 3 v0)", "v1");
        assertCannotCombineWithChildNodes("(* v0 v1)", "7");
    }

    private void assertCombineWithChildNodes(String first, String second, boolean isPos, String expected) {
        Node result = SIMPLIFIER.combineWithChildNodes(readNode(first), readNode(second), isPos);
        assertNodeEquals(expected, result);
    }

    private void assertCannotCombineWithChildNodes(String first, String second) {
        assertNull(SIMPLIFIER.combineWithChildNodes(readNode(first), readNode(second), true));
        assertNull(SIMPLIFIER.combineWithChildNodes(readNode(first), readNode(second), false));
    }

    @Test
    public void testSimplify() {
        assertSimplify("(+ 1 1)", "2");
        assertSimplify("(- 1 1)", "0");

        assertAdditionSimplification("v0", "(+ 1 v0)", "(+ 1 (* 2 v0))");

        assertAdditionSimplification("v0", "(+ v1 (+ v1 (+ v0 9)))", "(+ 9 (+ (* 2 v0) (* 2 v1)))");

        assertAdditionSimplification("v1", "(+ v1 (+ v1 (+ v0 9)))", "(+ 9 (+ v0 (* 3 v1)))");

        assertAdditionSimplification("v0", "(* 1 v0)", "(* 2 v0)");

        assertSimplify("(- 1 1)", "0");

        assertSimplify("(+ v0 (- 1 v0))", "1");

        assertSimplify("(- v0 (- v1 (- v1 (- v0 9))))", "9");

        assertAdditionSimplification("9", "(+ v0 3)", "(+ 12 v0)");

        assertAdditionSimplification("9", "(- v0 3)", "(+ 6 v0)");

        assertSimplify("(- 4 (- v1 (- v0 9)))", "(- (- v0 5) v1)");
        assertSimplify("(- 4 (- v1 (+ v0 9)))", "(- (+ 13 v0) v1)");

        assertSimplify("(- (+ 4 v0) 3)", "(+ 1 v0)");
        assertSimplify("(- (- v0 1) v1)", "(- (- v0 1) v1)");

        assertSimplify("(- (- v0 1) (- v0 1))", "0");
        assertSimplify("(- (+ v0 1) (+ v0 1))", "0");
        assertSimplify("(+ (- v0 1) (- v0 1))", "(- (* 2 v0) 2)");
        assertSimplify("(+ (+ v0 1) (+ v0 1))", "(+ 2 (* 2 v0))");
        assertSimplify("(- (+ v0 1) (- v0 1))", "2");

        assertSimplify("(- v0 (- v1 (- v0 9)))", "(- (- (* 2 v0) 9) v1)");


    }

    private void assertAdditionSimplification(String firstArg, String secondArg, String expectedOutput) {
        assertSimplify("(+ " + firstArg + ' ' + secondArg + ')', expectedOutput);
    }

    private void assertSimplify(String input, String expectedOutput) {
        FnNode in = readFunctionNode(input);
        Arguments args = in.args();
        Node simplifiedVersion = NodeSimplifier.simplify(in); 
        assertNodeEquals(expectedOutput, simplifiedVersion);
        if (!simplifiedVersion.equals(in)) {
            int[][] assignedValues = {{0, 0}, {1, 21}, {2, 14}, {3, -6}, {7, 3}, {-1, 9}, {-7, 0}};
            for (int[] assignedValue : assignedValues) {
                Assignments assignments = new Assignments(assignedValue[0], assignedValue[1]);
                if (!in.eval(assignments).equals(simplifiedVersion.eval(assignments))) {
                    throw new RuntimeException(expectedOutput);
                }
            }
        }
    }

    private Optional<Node> simplify(FnNode in, Arguments args) {
        return Optional.ofNullable(SIMPLIFIER.simplify(in.func(), args.firstArg(), args.secondArg()));
    }

    @Test
    public void testEvaluateToSameResultSuccess() {
        Node a = readNode("(* 7 (+ 1 2))");
        Node b = readNode("(+ 9 12)");
        ArithmeticExpressionSimplifier.assertEvaluateToSameResult(a, b);
    }

    @Test
    public void testEvaluateToSameResultFailure() {
        Node a = readNode("(* 7 (- 1 2))");
        Node b = readNode("(+ 9 12)");
        try {
            ArithmeticExpressionSimplifier.assertEvaluateToSameResult(a, b);
            fail("");
        } catch (IllegalArgumentException e) {
            assertEquals("(* 7 (- 1 2)) = -7 (+ 9 12) = 21", e.getMessage());
        }
    }
}
