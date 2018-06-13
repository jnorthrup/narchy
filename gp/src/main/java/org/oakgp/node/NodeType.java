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

import org.jetbrains.annotations.Nullable;
import org.oakgp.function.Function;

/**
 * Defines the node types used to construct tree structures.
 *
 * @see Node#nodeType()
 */
public enum NodeType {
    /**
     * Indicates that a node is a function node.
     *
     * @see FunctionNode
     */
    FUNCTION,
    /**
     * Indicates that a node is a constant node.
     *
     * @see ConstantNode
     */
    CONSTANT,
    /**
     * Indicates that a node is a variable node.
     *
     * @see VariableNode
     */
    VARIABLE;

    /**
     * Returns {@code true} if both of the specified nodes are function nodes, else {@code false}.
     */
    public static boolean areFunctions(Node n1, Node n2) {
        return isFunction(n1) && isFunction(n2);
    }

    /**
     * Returns {@code true} if the specified node is a function node, else {@code false}.
     */
    public static boolean isFunction(Node n) {
        return n.nodeType() == FUNCTION;
    }

    @Nullable
    public static org.oakgp.function.Function func(Node n) {
        return n.nodeType() == FUNCTION ? ((FunctionNode)n).func() : null;
    }


    /**
     * Returns {@code true} if both of the specified nodes are terminal nodes, else {@code false}.
     */
    public static boolean areTerminals(Node n1, Node n2) {
        return isTerminal(n1) && isTerminal(n2);
    }

    /**
     * Returns {@code true} if the specified node is a terminal node, else {@code false}.
     */
    public static boolean isTerminal(Node n) {
        return n.nodeType() != FUNCTION;
    }

    /**
     * Returns {@code true} if the specified node is a constant node, else {@code false}.
     */
    public static boolean isConstant(Node n) {
        return n.nodeType() == CONSTANT;
    }

    /**
     * Returns {@code true} if the specified node is a variable node, else {@code false}.
     */
    public static boolean isVariable(Node n) {
        return n.nodeType() == VARIABLE;
    }

    public static boolean func(Node arg1, String fName) {
        Function f = func(arg1);
        if (f!=null) {
            return f.name().equals(fName);
        }
        return false;
    }
}
