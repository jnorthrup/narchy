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

import org.oakgp.NodeType;
import org.oakgp.function.Fn;
import org.oakgp.node.Node;

import java.util.Random;

/**
 * Represents the range of possible functions and terminal nodes to use during a genetic programming run.
 */
public final class PrimitiveSetImpl implements PrimitiveSet {
    private final FnSet functionSet;
    private final NodeSet constantSet;
    private final VariableSet variableSet;
    private final Random random;
    private final double varCreationProbability;

    /**
     * Constructs a new primitive set consisting of the specified components.
     *
     * @param functionSet    the set of possible functions to use in the construction of programs
     * @param constantSet    the set of possible constants to use in the construction of programs
     * @param variableSet    the set of possible variables to use in the construction of programs
     * @param random         used to randomly select components to use in the construction of programs
     * @param varCreationProbability a value in the range 0 to 1 (inclusive) which specifies the proportion of terminal nodes that should represent variables, rather than constants
     */
    public PrimitiveSetImpl(FnSet functionSet, NodeSet constantSet, VariableSet variableSet, Random random, double varCreationProbability) {
        this.functionSet = functionSet;
        this.constantSet = constantSet;
        this.variableSet = variableSet;
        this.random = random;
        this.varCreationProbability = varCreationProbability;
    }

    @Override
    public boolean hasTerminals(NodeType type) {
        return variableSet.hasType(type) || constantSet.hasType(type);
    }

    @Override
    public boolean hasFunctions(NodeType type) {
        return functionSet.hasType(type);
    }

    /**
     * Returns a randomly selected terminal node.
     *
     * @return a randomly selected terminal node
     */
    @Override
    public Node nextTerminal(NodeType type) {
        boolean doCreateVariable = shouldCreateVariable();
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

    private Node nextTerminal(NodeType type, boolean createVariable) {

        return (Node) (createVariable ? variableSet : constantSet)
                .randomAlternate(type, random);

    }

    /**
     * Returns a randomly selected terminal node that is not the same as the specified {@code Node}.
     *
     * @param current the current {@code Node} that the returned result should be an alternative to (i.e. not the same as)
     * @return a randomly selected terminal node that is not the same as the specified {@code Node}
     */
    @Override
    public Node nextTerminal(Node current) {
        boolean doCreateVariable = shouldCreateVariable();
        Node next = next(current, doCreateVariable);
        return next == current ? next(current, !doCreateVariable) : next;
    }

    private boolean shouldCreateVariable() {
        return random.nextDouble() < varCreationProbability;
    }

    private Node next(Node current, boolean doCreateVariable) {
        return (Node) (doCreateVariable ? variableSet : constantSet)
                .randomAlternate(current.returnType(), current, random);
    }

    /**
     * Returns a randomly selected {@code Function} of the specified {@code Type}.
     *
     * @param type the required return type of the {@code Function}
     * @return a randomly selected {@code Function} with a return type of {@code type}
     */
    @Override
    public Fn next(NodeType type) {
        Fn f= functionSet.random(type, random);
        if (f == null) {
            throw new IllegalArgumentException("No functions with return type: " + type);
        }
        return f;
    }

    /**
     * Returns a randomly selected {@code Function} that is not the same as the specified {@code Function}.
     *
     * @param current the current {@code Function} that the returned result should be an alternative to (i.e. not the same as)
     * @return a randomly selected {@code Function} that is not the same as the specified {@code Function}
     */
    @Override
    public Fn next(Fn current) {
        return functionSet.randomAlternate(current, random);
    }

}
