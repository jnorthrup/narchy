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
package org.oakgp.primitive;

import org.oakgp.Type;
import org.oakgp.function.Function;
import org.oakgp.node.Node;
import org.oakgp.util.GPRandom;

import java.util.List;

/**
 * Represents the range of possible functions and terminal nodes to use during a genetic programming run.
 */
public final class PrimitiveSetImpl implements PrimitiveSet {
    private final FunctionSet functionSet;
    private final ConstantSet constantSet;
    private final VariableSet variableSet;
    private final GPRandom random;
    private final double ratioVariables;

    /**
     * Constructs a new primitive set consisting of the specified components.
     *
     * @param functionSet    the set of possible functions to use in the construction of programs
     * @param constantSet    the set of possible constants to use in the construction of programs
     * @param variableSet    the set of possible variables to use in the construction of programs
     * @param random         used to randomly select components to use in the construction of programs
     * @param ratioVariables a value in the range 0 to 1 (inclusive) which specifies the proportion of terminal nodes that should represent variables, rather than constants
     */
    public PrimitiveSetImpl(FunctionSet functionSet, ConstantSet constantSet, VariableSet variableSet, GPRandom random, double ratioVariables) {
        this.functionSet = functionSet;
        this.constantSet = constantSet;
        this.variableSet = variableSet;
        this.random = random;
        this.ratioVariables = ratioVariables;
    }

    @Override
    public boolean hasTerminals(Type type) {
        return variableSet.getByType(type) != null || constantSet.getByType(type) != null;
    }

    @Override
    public boolean hasFunctions(Type type) {
        return functionSet.getByType(type) != null;
    }

    /**
     * Returns a randomly selected terminal node.
     *
     * @return a randomly selected terminal node
     */
    @Override
    public Node nextTerminal(Type type) {
        boolean doCreateVariable = doCreateVariable();
        Node next = nextTerminal(type, doCreateVariable);
        if (next == null) {
            next = nextTerminal(type, !doCreateVariable);
        }
        if (next == null) {
            throw new IllegalArgumentException("No terminals of type: " + type);
        } else {
            return next;
        }
    }

    private Node nextTerminal(Type type, boolean doCreateVariable) {
        List<? extends Node> possibilities = doCreateVariable ? variableSet.getByType(type) : constantSet.getByType(type);
        return randomlySelectAlternative(null, possibilities);
    }

    /**
     * Returns a randomly selected terminal node that is not the same as the specified {@code Node}.
     *
     * @param current the current {@code Node} that the returned result should be an alternative to (i.e. not the same as)
     * @return a randomly selected terminal node that is not the same as the specified {@code Node}
     */
    @Override
    public Node nextAlternativeTerminal(Node current) {
        boolean doCreateVariable = doCreateVariable();
        Node next = nextAlternativeNode(current, doCreateVariable);
        if (next == current) {
            return nextAlternativeNode(current, !doCreateVariable);
        } else {
            return next;
        }
    }

    private boolean doCreateVariable() {
        return random.nextDouble() < ratioVariables;
    }

    private Node nextAlternativeNode(Node current, boolean doCreateVariable) {
        Type type = current.returnType();
        List<? extends Node> possibilities =
                doCreateVariable ? variableSet.getByType(type) : constantSet.getByType(type);
        return randomlySelectAlternative(current, possibilities);
    }

    /**
     * Returns a randomly selected {@code Function} of the specified {@code Type}.
     *
     * @param type the required return type of the {@code Function}
     * @return a randomly selected {@code Function} with a return type of {@code type}
     */
    @Override
    public Function nextFunction(Type type) {
        List<Function> typeFunctions = functionSet.getByType(type);
        if (typeFunctions == null) {
            throw new IllegalArgumentException("No functions with return type: " + type);
        }
        int index = nextInt(typeFunctions.size());
        return typeFunctions.get(index);
    }

    /**
     * Returns a randomly selected {@code Function} that is not the same as the specified {@code Function}.
     *
     * @param current the current {@code Function} that the returned result should be an alternative to (i.e. not the same as)
     * @return a randomly selected {@code Function} that is not the same as the specified {@code Function}
     */
    @Override
    public Function nextAlternativeFunction(Function current) {
        List<Function> functions = functionSet.getBySignature(current.sig());
        return randomlySelectAlternative(current, functions);
    }

    private <C, P extends C> C randomlySelectAlternative(C currentVersion, List<P> possibilities) {
        if (possibilities == null) {
            return currentVersion;
        }

        int possibilitiesSize = possibilities.size();
        int randomIndex = nextInt(possibilitiesSize);
        C next = possibilities.get(randomIndex);
        if (next == currentVersion) {
            if (possibilitiesSize == 1) {
                return currentVersion;
            } else {
                int secondRandomIndex = nextInt(possibilitiesSize - 1);
                return possibilities.get(secondRandomIndex + ((secondRandomIndex >= randomIndex) ? 1 : 0));
            }
        } else {
            return next;
        }
    }

    private int nextInt(int bound) {
        return bound == 1 ? 0 : random.nextInt(bound);
    }
}
