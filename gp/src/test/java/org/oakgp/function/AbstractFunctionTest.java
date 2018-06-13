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
package org.oakgp.function;

import org.junit.jupiter.api.Test;
import org.oakgp.Arguments;
import org.oakgp.Assignments;
import org.oakgp.Type;
import org.oakgp.node.ConstantNode;
import org.oakgp.node.FunctionNode;
import org.oakgp.node.Node;
import org.oakgp.primitive.VariableSet;
import org.oakgp.serialize.NodeReader;
import org.oakgp.util.NodeSimplifier;
import org.oakgp.util.Signature;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Observable;
import java.util.Observer;

import static org.junit.jupiter.api.Assertions.*;
import static org.oakgp.node.NodeType.isFunction;
import static org.oakgp.util.Utils.intArrayType;

public abstract class AbstractFunctionTest {
    private static final Type[] DEFAULT_VARIABLE_TYPES = intArrayType(100);

    private final Function[] functions;
    /**
     * Observable allows other objects to be notified of the tests that are run.
     * <p>
     * This is used to support the automatic creation of http:
     */
    private final Observable observable = new Observable() {
        @Override
        public void notifyObservers(Object arg) {
            super.setChanged();
            super.notifyObservers(arg);
        }
    };

    protected AbstractFunctionTest() {
        functions = getFunctionSet();
    }

    protected abstract Function getFunction();

    @Test
    public abstract void testEvaluate();

    @Test
    public abstract void testCanSimplify();

    @Test
    public abstract void testCannotSimplify();

    @Test
    public void testSignatureReused() {
        Function function = getFunction();
        assertNotNull(function.sig());
        assertSame(function.sig(), function.sig());
    }

    @Test
    public void testDisplayNameValid() {
        String displayName = getFunction().name();
        assertTrue(NodeReader.isValidDisplayName(displayName));
    }

    protected Function[] getFunctionSet() {
        return new Function[]{getFunction()};
    }

    protected void cannotSimplify(String input, Type... variableTypes) {
        FunctionNode node = readFunctionNode(input, variableTypes);
        assertSame(node, NodeSimplifier.simplify(node));
    }

    void addObserver(Observer o) {
        observable.addObserver(o);
    }

    private FunctionNode readFunctionNode(String input, Type... variableTypes) {
        return readFunctionNode(input, VariableSet.createVariableSet(variableTypes));
    }

    private FunctionNode readFunctionNode(String input, VariableSet variableSet) {
        FunctionNode functionNode = (FunctionNode) readNode(input, variableSet);
        assertSame(getFunction().getClass(), functionNode.func().getClass());
        return functionNode;
    }

    private Node readNode(String input, VariableSet variableSet) {
        try (NodeReader nodeReader = new NodeReader(input, functions, new ConstantNode[0], variableSet)) {
            return nodeReader.readNode();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public EvaluateExpectation evaluate(String input) {
        return new EvaluateExpectation(input);
    }

    public SimplifyExpectation simplify(String input) {
        return new SimplifyExpectation(input);
    }

    static class Notification {
        final FunctionNode input;
        final ConstantNode[] assignedValues;
        final Object output;

        private Notification(FunctionNode input, ConstantNode[] assignedValues, Object output) {
            this.input = input;
            this.assignedValues = assignedValues;
            this.output = output;
        }
    }

    protected class EvaluateExpectation {
        private final String input;
        private ConstantNode[] assignedValues = {};

        private EvaluateExpectation(String input) {
            this.input = input;
        }

        public EvaluateExpectation assigned(ConstantNode... assignedValues) {
            this.assignedValues = assignedValues;
            return this;
        }

        public void to(Object expectedResult) {
            Type[] variableTypes = toVariableTypes(assignedValues);
            FunctionNode functionNode = readFunctionNode(input, variableTypes);
            Assignments assignments = toAssignments(assignedValues);
            
            assertEquals(expectedResult, functionNode.eval(assignments));
            assertEquals(expectedResult, functionNode.eval(assignments));
            observable.notifyObservers(new Notification(functionNode, assignedValues, expectedResult));
        }

        private Assignments toAssignments(ConstantNode[] constants) {
            Object[] values = new Object[constants.length];
            for (int i = 0; i < constants.length; i++) {
                values[i] = constants[i].eval(null);
            }
            return new Assignments(values);
        }

        private Type[] toVariableTypes(ConstantNode[] constants) {
            Type[] types = new Type[constants.length];
            for (int i = 0; i < constants.length; i++) {
                types[i] = constants[i].returnType();
            }
            return types;
        }
    }

    protected class SimplifyExpectation {
        private final String input;
        private Type[] variableTypes = DEFAULT_VARIABLE_TYPES;
        private FunctionNode inputNode;
        private Node simplifiedNode;

        public SimplifyExpectation(String input) {
            this.input = input;
        }

        public SimplifyExpectation with(Type... variableTypes) {
            this.variableTypes = variableTypes;
            return this;
        }

        public SimplifyExpectation to(String expected) {
            VariableSet variableSet = VariableSet.createVariableSet(variableTypes);

            Node expectedNode = readNode(expected, variableSet);
            inputNode = readFunctionNode(input, variableSet);
            simplifiedNode = NodeSimplifier.simplify(inputNode);

            
            assertEquals(expectedNode, simplifiedNode);
            assertSame(inputNode.returnType(), simplifiedNode.returnType());

            if (isFunction(simplifiedNode)) {
                
                
                FunctionNode fn = (FunctionNode) simplifiedNode;
                Arguments fnArguments = fn.args();
                Signature fnSignature = fn.func().sig();

                assertSame(fn.returnType(), fnSignature.returnType());
                assertSameArgumentTypes(fnArguments, fnSignature);
            }

            
            assertEquals(NodeSimplifier.simplify(inputNode), NodeSimplifier.simplify(inputNode));

            return this;
        }

        private void assertSameArgumentTypes(Arguments args, Signature signature) {
            assertEquals(args.length(), signature.size());
            for (int i = 0; i < signature.size(); i++) {
                assertSame(args.get(i).returnType(), signature.argType(i));
            }
        }

        public SimplifyExpectation verify(Object... values) {
            Assignments assignments = new Assignments(values);
            Object expectedOutcome = inputNode.eval(assignments);
            Object actualOutcome = simplifiedNode.eval(assignments);
            assertEquals(expectedOutcome, actualOutcome);
            return this;
        }

        public void verifyAll(Object[][] values) {
            for (Object[] a : values) {
                verify(a);
            }
        }
    }
}
