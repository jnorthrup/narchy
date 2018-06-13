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
package org.oakgp.node;

import org.junit.jupiter.api.Test;
import org.oakgp.Arguments;
import org.oakgp.Assignments;
import org.oakgp.function.Fn;
import org.oakgp.util.Signature;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.oakgp.TestUtils.*;
import static org.oakgp.NodeType.integerType;
import static org.oakgp.function.math.IntFunc.the;

public class FnNodeTest {
    @Test
    public void testConstructors() {
        Fn function = the.multiply;
        ConstantNode arg1 = integerConstant(42);
        VariableNode arg2 = createVariable(0);

        
        FnNode n1 = new FnNode(function, arg1, arg2);

        
        Arguments arguments = new Arguments(new Node[]{arg1, arg2});
        FnNode n2 = new FnNode(function, arguments);

        
        assertEquals(n1, n2);
    }

    @Test
    public void testEvaluate() {
        Fn function = the.multiply;
        Arguments arguments = new Arguments(new Node[]{integerConstant(42), createVariable(0)});
        FnNode functionNode = new FnNode(function, arguments);

        assertSame(function, functionNode.func());
        assertEquals(arguments, functionNode.args());

        Assignments assignments = new Assignments(3);
        assertEquals(126, functionNode.eval(assignments));
    }

    @Test
    public void testCountAndHeight() {
        assertCountAndHeight("(* 7 7)", 3, 2);
        assertCountAndHeight("(* (+ 8 9) 7)", 5, 3);
        assertCountAndHeight("(* 7 (+ 8 9))", 5, 3);
        assertCountAndHeight("(zero? (+ (* 4 5) (- 6 (+ 7 8))))", 10, 5);
        assertCountAndHeight("(zero? (+ (- 6 (+ 7 8)) (* 4 5)))", 10, 5);
        assertCountAndHeight("(if (zero? v0) v1 v2)", 5, 3);
        assertCountAndHeight("(if (zero? v0) v1 (+ v0 (* v1 v2)))", 9, 4);
        assertCountAndHeight("(if (zero? v0) (+ v0 (* v1 v2)) v1)", 9, 4);
    }

    private void assertCountAndHeight(String expression, int nodeCount, int height) {
        Node n = readNode(expression);
        assertEquals(nodeCount, n.size());
        assertEquals(height, n.depth());
    }

    @Test
    public void testGetType() {
        FnNode n = createFunctionNode();
        assertSame(integerType(), n.returnType());
    }

    @Test
    public void testEqualsAndHashCode1() {
        final FnNode n1 = createFunctionNode();
        final FnNode n2 = createFunctionNode();
        assertNotSame(n1, n2); 
        assertEquals(n1, n1);
        assertEquals(n1.hashCode(), n2.hashCode());
        assertEquals(n1, n2);
        assertEquals(n2, n1);
    }

    @Test
    public void testEqualsAndHashCode2() {
        Node n1 = readNode("(* 288 v1)");
        Node n2 = readNode("(* 288 v1)");
        assertNotSame(n1, n2); 
        assertEquals(n1, n1);
        assertEquals(n1, n2);
        assertEquals(n2, n1);
        assertEquals(n1.hashCode(), n2.hashCode());
    }

    @Test
    public void testNotEquals() {
        Fn add = the.add;

        final FnNode n = new FnNode(add, createVariable(0), integerConstant(7));

        
        assertEquals(n, new FnNode(add, createVariable(0), integerConstant(7)));


        Fn multiply = the.multiply;
        assertNotEquals(n, new FnNode(multiply, createVariable(0), integerConstant(7)));

        
        assertNotEquals(n, new FnNode(add, createVariable(1), integerConstant(7)));

        
        assertNotEquals(n, new FnNode(add, createVariable(0), integerConstant(6)));







        
        assertNotEquals(n, new FnNode(add, createVariable(0), integerConstant(7), integerConstant(7)));

        
        assertNotEquals(n, new FnNode(add, createVariable(0)));

        
        assertNotEquals(n, new FnNode(add));

        
        assertNotEquals(n, integerConstant(7));

        
        assertNotEquals(n, new Object());

        assertFalse(n.equals(null));
    }

    /**
     * Tests that for two {@code FunctionNode} instances to be considered equal they must share the same instance of {@code Function} (i.e. it is not enough for
     * them to have separate instances of the same {@code Function} class).
     */
    @Test
    public void testEqualityRequiresSameFunctionInstance() {
        class DummyFn implements Fn {
            @Override
            public Object evaluate(Arguments arguments, Assignments assignments) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Signature sig() {
                throw new UnsupportedOperationException();
            }
        }

        Fn f1 = new DummyFn();
        Fn f2 = new DummyFn();
        Arguments arguments = new Arguments(new Node[]{integerConstant(1)});
        FnNode fn1 = new FnNode(f1, arguments);
        FnNode fn2 = new FnNode(f2, arguments);

        assertSame(f1.getClass(), f2.getClass());
        assertNotEquals(fn1, fn2);
    }

    @Test
    public void testHashCode() {
        
        
        
        
        
        
        
        
        
        

        
        
        
        
        
        
        
        
        

        
        Node n1 = readNode("(- (- (* -1 v3) 0) (- 13 v1))");
        Node n2 = readNode("(- (- (* -1 v3) 13) (- 0 v1))");
        assertNotEquals(n1.hashCode(), n2.hashCode());
    }

    @Test
    public void testLargeNumberOfArguments() {
        Node[] args = new Node[1000];
        for (int i = 0; i < args.length; i++) {
            args[i] = integerConstant(i);
        }

        FnNode n = new FnNode(mock(Fn.class), args);

        assertEquals(args.length, n.args().length());
        for (int i = 0; i < args.length; i++) {
            assertSame(args[i], n.args().get(i));
        }
    }

    /**
     * Returns representation of: {@code (x*y)+z+1}
     */
    private FnNode createFunctionNode() {
        return new FnNode(the.add, new FnNode(the.multiply, createVariable(0), createVariable(1)), new FnNode(
                the.add, createVariable(2), integerConstant(1)));
    }
}
