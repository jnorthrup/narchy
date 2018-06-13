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
package org.oakgp.evolve.crossover;

import org.oakgp.Arguments;
import org.oakgp.function.Fn;
import org.oakgp.node.FnNode;
import org.oakgp.node.Node;

import static org.oakgp.node.NodeType.areFunctions;
import static org.oakgp.node.NodeType.areTerminals;

final class CommonRegion {
    /**
     * Private constructor as all methods are static.
     */
    private CommonRegion() {
        
    }

    static Node crossoverAt(Node n1, Node n2, int crossOverPoint) {
        if (areFunctions(n1, n2)) {
            FnNode f1 = (FnNode) n1;
            FnNode f2 = (FnNode) n2;
            Arguments arguments = f1.args();
            int argCount = arguments.length();
            if (argCount == f2.args().length()) {
                int total = 0;
                for (int i = 0; i < argCount; i++) {
                    Node a1 = arguments.get(i);
                    Node a2 = f2.args().get(i);
                    int c = getNodeCount(a1, a2);
                    if (total + c > crossOverPoint) {
                        Fn f = f1.func();
                        return new FnNode(f,
                                arguments.replaceAt(i, crossoverAt(a1, a2, crossOverPoint - total)));
                    } else {
                        total += c;
                    }
                }
            }
        }

        return sameType(n1, n2) ? n2 : n1;
    }

    static int getNodeCount(Node n1, Node n2) {
        if (areFunctions(n1, n2)) {
            int total = sameType(n1, n2) ? 1 : 0;
            FnNode f1 = (FnNode) n1;
            FnNode f2 = (FnNode) n2;
            int argCount = f1.args().length();
            if (argCount == f2.args().length()) {
                for (int i = 0; i < argCount; i++) {
                    total += getNodeCount(f1.args().get(i), f2.args().get(i));
                }
            }
            return total;
        } else if (areTerminals(n1, n2)) {
            return sameType(n1, n2) ? 1 : 0;
        } else {
            
            return 0;
        }
    }

    private static boolean sameType(Node n1, Node n2) {
        return n1.returnType() == n2.returnType();
    }
}
