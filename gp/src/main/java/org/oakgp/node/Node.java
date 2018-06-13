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

import org.oakgp.Assignments;
import org.oakgp.NodeType;

import static org.oakgp.util.NodeComparator.NODE_COMPARATOR;

/**
 * A node represents a single point of a tree structure.
 */
public interface Node extends Comparable<Node> {

    Node[] EmptyArray = new Node[0];

    /**
     * Returns the result of evaluating this {@code Node} using the values of the specified {@code Assignments}.
     *
     * @param assignments represents the values to assign to any variables used in the evaluation of this {@code Node}
     * @return the result of evaluating this {@code Node} using the values of the specified {@code Assignments}
     */
    <T> T eval(Assignments assignments);

    /**
     * Returns the total number of nodes represented by this {@code Node} - including any child-nodes.
     */
    int size();

    /**
     * Returns the height of this {@code Node}.
     * <p>
     * The height of a node is the number of edges on the longest downward path between that node and a leaf.
     */
    int depth();

    /**
     * Returns the {@code Type} of this {@code Node}.
     * <p>
     * This indicates the type of the value returned when {@code #evaluate(Assignments)} is called on this {@code Node}.
     */
    NodeType returnType();

    /**
     * Returns the {@code NodeType} of this {@code Node}.
     * <p>
     * This can be used to determine if the node is a function, constant or variable node.
     */
    @Deprecated org.oakgp.node.NodeType nodeType();

    /** default impl: not entirely */
    @Override default int compareTo(Node o) {
        return NODE_COMPARATOR.compare(this, o);
    }



}
