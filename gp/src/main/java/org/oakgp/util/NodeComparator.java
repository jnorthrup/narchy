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

import org.oakgp.node.ConstantNode;
import org.oakgp.node.FunctionNode;
import org.oakgp.node.Node;
import org.oakgp.node.NodeType;

import java.io.Serializable;
import java.util.Comparator;

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
            int i = o1.returnType().compareTo(o2.returnType());
            if (i != 0) return i;

            int iDepth = Integer.compare(o1.depth(), o2.depth());
            if (iDepth!=0) return iDepth;

            if (t1 == NodeType.CONSTANT) {
                ConstantNode c1 = (ConstantNode) o1;
                ConstantNode c2 = (ConstantNode) o2;
                int ii = c1.type.compareTo(c2.type);
                if (ii!=0) return ii;
                else return compareValue(c1.value, ((ConstantNode)o2).value);
            } else if (t1 == NodeType.FUNCTION) {
                FunctionNode f1 = (FunctionNode)o1;
                FunctionNode f2 = (FunctionNode)o2;
                
            }

            
            return o1.toString().compareTo(o2.toString());  

        } else if (t1 == NodeType.CONSTANT) {
            return -1;
        } else if (t2 == NodeType.CONSTANT) {
            return 1;
        } else if (t1 == NodeType.FUNCTION) {
            return 1;
        } else if (t2 == NodeType.FUNCTION) {
            return -1;
        } else {
            throw new IllegalStateException();
        }
    }

    static private int compareValue(Object x, Object y) {
        if (x instanceof Comparable) {
            return ((Comparable)x).compareTo(y);
        } else {
            int i = Integer.compare(x.hashCode(), y.hashCode());
            if (i!=0) return i;
            return x.toString().compareTo(y.toString());
        }
    }
}
