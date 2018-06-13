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
package org.oakgp.function.compare;

import org.oakgp.Arguments;
import org.oakgp.NodeType;
import org.oakgp.node.FnNode;
import org.oakgp.node.Node;

import static org.oakgp.util.NodeComparator.NODE_COMPARATOR;

/**
 * Determines if two objects are not equal.
 * <p>
 * <b>Note:</b> Equality is checked using {@code Comparable#compareTo(Object)} rather than {@code Object#equals(Object)}.
 */
public final class NotEqual extends ComparisonOperator {
    /**
     * Constructs a function that compares two arguments of the specified type.
     */
    public NotEqual(NodeType type) {
        super(type, false);
    }

    @Override
    protected boolean evaluate(int diff) {
        return diff != 0;
    }

    @Override
    public Node simplify(Arguments arguments) {
        Node simplifiedVersion = super.simplify(arguments);
        if (simplifiedVersion == null && NODE_COMPARATOR.compare(arguments.firstArg(), arguments.secondArg()) > 0) {
            
            return new FnNode(this, arguments.secondArg(), arguments.firstArg());
        } else {
            return simplifiedVersion;
        }
    }

    @Override
    public String name() {
        return "!=";
    }
}
