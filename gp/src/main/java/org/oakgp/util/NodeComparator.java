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
package org.oakgp.util;

import org.oakgp.node.*;

import java.io.Serializable;
import java.util.Comparator;

import static org.oakgp.node.NodeType.CONSTANT;
import static org.oakgp.node.NodeType.FUNCTION;

/**
 * An implementation of {@code Comparator} for comparing instances of {@link Node}.
 */
public final class NodeComparator implements Comparator<Node>, Serializable {
    /**
     * Singleton instance.
     */
    public static final NodeComparator NODE_COMPARATOR = new NodeComparator();

    /**
     * Private constructor to force use of {@link #notify()}.
     */
    private NodeComparator() {
        
    }

    @Override
    public int compare(Node o1, Node o2) {
        if (o1.equals(o2))
            return 0;

        NodeType t1 = o1.nodeType();
        NodeType t2 = o2.nodeType();

        if (t1 == t2) {
            switch (t1) {
                case VARIABLE:
                    int i = Integer.compare(
                            ((VariableNode)o1).id,
                            ((VariableNode)o2).id
                    );
                    if (i!=0)
                        return i;
                    break;
            }
            int i = o1.returnType().compareTo(o2.returnType());
            if (i != 0) return i;

            if (t1 == FUNCTION) {
                //only function (non-terminal) nodes have depth > 1
                int iDepth = Integer.compare(o1.depth(), o2.depth());
                if (iDepth != 0) return iDepth;

                int iName = ((FunctionNode)o1).func().name().compareTo(
                        ((FunctionNode)o2).func().name()
                );
                if (iName != 0) return iName;
            }

            if (t1 == CONSTANT) {
                return compareValue(((ConstantNode)o1).value, ((ConstantNode)o2).value);
            }


            return o1.toString().compareTo(o2.toString());

        } else if (t1 == CONSTANT) {
            return -1;
        } else if (t2 == CONSTANT) {
            return 1;
        } else if (t1 == FUNCTION) {
            return 1;
        } else if (t2 == FUNCTION) {
            return -1;
        } else {
            throw new IllegalStateException();
        }
    }

    static private int compareValue(Object x, Object y) {
        if (x.equals(y))
            return 0;
        if (x instanceof Comparable) {
            return ((Comparable)x).compareTo(y);
        } else {
            int i = Integer.compare(x.hashCode(), y.hashCode());
            if (i!=0) return i;
            return x.toString().compareTo(y.toString());
        }
    }
}
