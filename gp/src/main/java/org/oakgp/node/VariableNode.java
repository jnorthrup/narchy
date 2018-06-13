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

/**
 * Represents a variable.
 * <p>
 * The result of evaluating this node will vary based on the {@link Assignments} specified.
 */
public final class VariableNode extends TerminalNode {
    public final int id;
    public final NodeType type;
    public final int hashCode;

    /**
     * Constructs a new {@code VariableNode} with the specified ID.
     *
     * @param id   represents the index to specify when getting the value for this variable from an {@link Assignments}
     * @param type the {@code Type} that the values represented by this node are of
     */
    public VariableNode(int id, NodeType type) {
        this.id = id;
        this.type = type;
        
        
        this.hashCode = (id + 1) * 997;
    }

    /**
     * Returns the ID associated with the variable.
     * <p>
     * The ID indicates the index to specify when getting the value for this variable from an {@link Assignments}.
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the value assigned to this {@code VariableNode} by the specified {@code Assignments}.
     *
     * @return the value stored in {@code Assignments} at the index specified by the ID of this {@code VariableNode}
     */
    @Override
    public Object eval(Assignments assignments) {
        return assignments.get(id);
    }

    @Override
    public NodeType returnType() {
        return type;
    }

    @Override
    public final org.oakgp.node.NodeType nodeType() {
        return org.oakgp.node.NodeType.VARIABLE;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    /**
     * Two VariableNode references are only "equal" if they refer to the same instance.
     */
    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public String toString() {
        return "v" + id;
    }
}
