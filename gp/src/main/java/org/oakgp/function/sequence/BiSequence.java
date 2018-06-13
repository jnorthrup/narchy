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
package org.oakgp.function.sequence;

import org.oakgp.Arguments;
import org.oakgp.Assignments;
import org.oakgp.util.Signature;
import org.oakgp.node.FunctionNode;
import org.oakgp.node.Node;
import org.oakgp.util.Void;

import static org.oakgp.util.Void.*;

/**
 * Executes two nodes in sequence.
 */
public class BiSequence implements AbstractSequence {

    public static final BiSequence BISEQUENCE = new BiSequence();

    static final Signature BiSig = new Signature(VOID_TYPE, VOID_TYPE, VOID_TYPE);

    protected BiSequence() {
    }

    @Override
    public Signature sig() {
        return BiSig;
    }

    @Override
    public Void evaluate(Arguments arguments, Assignments assignments) {
        arguments.evalEach(assignments);;
        return Void.VOID;
    }

    @Override
    public Node simplify(Arguments arguments) {
        Node x = arguments.firstArg();
        Node y = arguments.secondArg();
        if (isVoid(x)) {
            return y;
        } else if (isVoid(y)) {
            return x;
        } else if (isMutex(x, y)) {
            return VOID_CONSTANT;
        } else if (isBiSequence(x)) {
            Arguments firstArgArgs = ((FunctionNode) x).args();
            return createTriSequence(firstArgArgs.firstArg(), firstArgArgs.secondArg(), y);
        } else if (isBiSequence(y)) {
            Arguments secondArgArgs = ((FunctionNode) y).args();
            return createTriSequence(x, secondArgArgs.firstArg(), secondArgArgs.secondArg());
        } else {
            return null;
        }
    }

    private boolean isBiSequence(Node firstArg) {
        FunctionNode fn = (FunctionNode) firstArg;
        return fn.func()==this;
    }

    @Deprecated private Node createTriSequence(Node arg1, Node arg2, Node arg3) {
        return new FunctionNode(TriSequence.TRISEQUENCE, arg1, arg2, arg3);
    }
}
