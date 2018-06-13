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

import static org.oakgp.function.sequence.BiSequence.BISEQUENCE;
import static org.oakgp.util.Void.VOID_TYPE;
import static org.oakgp.util.Void.isVoid;

/**
 * Executes three nodes in sequence.
 */
public class TriSequence implements AbstractSequence {

    public static final TriSequence TRISEQUENCE = new TriSequence(BISEQUENCE);

    static final Signature TriSig = new Signature(VOID_TYPE, VOID_TYPE, VOID_TYPE, VOID_TYPE);

    final BiSequence bi;

    public TriSequence() {
        this(BISEQUENCE);
    }

    public TriSequence(BiSequence bi) {
        this.bi = bi;
        if (bi != BISEQUENCE) {
            //TODO register with that function as a supplier of TriSequences
        }
    }

    @Override
    public Signature sig() {
        return TriSig;
    }

    @Override
    public Void evaluate(Arguments arguments, Assignments assignments) {
        arguments.evalEach(assignments);
        return Void.VOID;
    }

    @Override
    public Node simplify(Arguments arguments) {
        Node first = arguments.firstArg();
        Node second = arguments.secondArg();
        Node third = arguments.thirdArg();
        if (isVoid(first)) {
            return createBiSequence(second, third);
        } else if (isVoid(second)) {
            return createBiSequence(first, third);
        } else if (isVoid(third)) {
            return createBiSequence(first, second);
        } else if (isMutex(first, second)) {
            return third;
        } else if (isMutex(second, third)) {
            return first;
        } else {
            return null;
        }
    }

    @Override
    public boolean isMutex(Node firstArg, Node secondArg) {
        return bi.isMutex(firstArg, secondArg);
    }

    private Node createBiSequence(Node arg1, Node arg2) {
        return new FunctionNode(bi, arg1, arg2);
    }
}
