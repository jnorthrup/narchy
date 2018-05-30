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

import org.oakgp.Arguments;
import org.oakgp.function.Function;
import org.oakgp.node.ConstantNode;
import org.oakgp.node.FunctionNode;
import org.oakgp.node.Node;

import java.util.HashSet;
import java.util.Set;

import static org.oakgp.node.NodeType.isConstant;
import static org.oakgp.node.NodeType.isFunction;

/**
 * Attempts to reduce the size of tree structures without altering their functionality.
 * <p>
 * This can be done by replacing expressions with constant values or removing redundant branches. e.g. The expression:
 * <p>
 * <pre>
 * (+ 7 (* 3 6))
 * </pre>
 * <p>
 * can be simplified to the value:
 * <p>
 * <pre>
 * 25
 * </pre>
 * <p>
 * <b>Note:</b> relies on {@link org.oakgp.function.Function#isPure()} to identify if a function is referentially transparent and therefore suitable for
 * replacement with the result of evaluating it.
 */
public final class NodeSimplifier {
    private static final int MAX_RETRIES = 100;

    /**
     * Private constructor as all methods are static.
     */
    private NodeSimplifier() {
        
    }

    /**
     * Attempts to reduce the size of the specified tree structures without altering its functionality.
     * <p>
     * Simplification can occur by replacing expressions with constant values (e.g. replacing {@code (+ 1 1)} with {@code 2}) or removing redundant branches
     * (e.g. replacing {@code (if (< 2 3) (+ v0 v1) (* v0 v1)) with {@code (+ v0 v1)}.
     *
     * @param input the node to attempt to simplify.
     * @return the result of attempting to simplify {@code input}.
     * @see org.oakgp.function.Function#simplify(Arguments)
     */
    public static Node simplify(Node input) {
        int ctr = 0;
        Set<Node> s = new HashSet<>();
        Node previous;
        Node output = input;
        do {
            previous = output;
            output = simplifyOnce(output);

            
            
            if (!output.equals(previous) && !s.add(output)) {
                return output;
            }
            
            if (ctr++ > MAX_RETRIES) {
                throw new IllegalArgumentException(input.toString());
            }
        } while (isFunction(output) && !output.equals(previous));
        return output;
    }

    private static Node simplifyOnce(Node input) {
        if (isFunction(input)) {
            return simplifyFunctionNode((FunctionNode) input);
        } else {
            return input;
        }
    }

    private static Node simplifyFunctionNode(final FunctionNode input) {
        

        
        Arguments inputArgs = input.args();
        Node[] simplifiedArgs = new Node[inputArgs.length()];
        boolean haveAnyArgumentsBeenSimplified = false;
        boolean areAllArgumentsConstants = true;
        for (int i = 0; i < simplifiedArgs.length; i++) {
            Node originalArg = inputArgs.get(i);
            simplifiedArgs[i] = simplifyOnce(originalArg);
            if (originalArg != simplifiedArgs[i]) {
                haveAnyArgumentsBeenSimplified = true;
            }
            if (!isConstant(simplifiedArgs[i])) {
                areAllArgumentsConstants = false;
            }
        }

        
        Arguments arguments;
        FunctionNode output;
        Function f = input.func();
        if (haveAnyArgumentsBeenSimplified) {
            output = new FunctionNode(f, Arguments.get(f, simplifiedArgs));
            arguments = output.args();
        } else {
            arguments = inputArgs;
            output = input;
        }

        
        
        
        
        if (areAllArgumentsConstants && f.isPure()) {
            return new ConstantNode(output.eval(null), output.returnType());
        }

        
        Node simplifiedByFunctionVersion = f.simplify(arguments);
        if (simplifiedByFunctionVersion == null) {
            return output;
        } else {
            return simplifiedByFunctionVersion;
        }
    }
}
