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

import jcog.Util;
import org.oakgp.NodeType;
import org.oakgp.node.VariableNode;
import org.oakgp.util.Utils;

/**
 * Represents the range of possible variables to use during a genetic programming run.
 */
public final class VariableSet extends NodeSet<VariableNode> {

    private final VariableNode[] variables;


    public final static VariableSet Empty = new VariableSet(new VariableNode[] { });

    private VariableSet(VariableNode[] variables) {
        super(Utils.groupByType(variables));
        this.variables = variables;
    }

    /**
     * Constructs a variable set containing variables of the specified types.
     */
    public static VariableSet of(NodeType... variableTypes) {
        if (variableTypes.length == 0) return Empty;
        else return new VariableSet(Util.map(0, variableTypes.length,
                i -> new VariableNode(i, variableTypes[i]), VariableNode[]::new));
    }



    /**
     * Returns the {@code VariableNode} from this set that is associated with the specified ID.
     */
    public VariableNode get(int id) {
        return variables[id];
    }
}
