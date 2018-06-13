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
package org.oakgp.function.choice;

import org.oakgp.Arguments;
import org.oakgp.Assignments;
import org.oakgp.Type;
import org.oakgp.function.Function;
import org.oakgp.node.FunctionNode;
import org.oakgp.node.Node;
import org.oakgp.node.walk.NodeWalk;
import org.oakgp.util.Utils;
import org.oakgp.util.Signature;
import java.util.function.Predicate;

import static java.lang.Boolean.TRUE;
import static org.oakgp.Type.booleanType;
import static org.oakgp.node.NodeType.isConstant;

/**
 * A selection operator that uses a boolean expression to determine which code to evaluate.
 * <p>
 * Expects three arguments:
 * <ol>
 * <li>Conditional statement.</li>
 * <li>Value to evaluate to if the conditional statement is {@code true}.</li>
 * <li>Value to evaluate to if the conditional statement is {@code false}.</li>
 * </ol>
 */
public final class If implements Function {
    private static final int TRUE_IDX = 1;
    private static final int FALSE_IDX = 2;

    private final Signature signature;

    /**
     * Constructs a selection operator that returns values of the specified type.
     */
    public If(Type type) {
        signature = new Signature(type, booleanType(), type, type);
    }

    private static int outcomeIndex(Arguments arguments, Assignments assignments) {
        return TRUE.equals(arguments.firstArg().eval(assignments)) ? TRUE_IDX : FALSE_IDX;
    }

    @Override
    public Object evaluate(Arguments arguments, Assignments assignments) {
        int index = outcomeIndex(arguments, assignments);
        return arguments.get(index).eval(assignments);
    }

    @Override
    public Signature sig() {
        return signature;
    }

    @Override
    public Node simplify(Arguments args) {
        Node trueBranch = args.secondArg();
        Node falseBranch = args.thirdArg();
        if (trueBranch.equals(falseBranch)) {
            return trueBranch;
        }

        Node condition = args.firstArg();
        if (isConstant(condition)) {
            return outcomeIndex(args, null) == TRUE_IDX ?
                    trueBranch : falseBranch;
        }

        Predicate<Node> criteria = n -> n.equals(condition);
        Node simplifiedTrueBranch = NodeWalk.replaceAll(trueBranch, criteria, n -> Utils.TRUE_NODE);
        Node simplifiedFalseBranch = NodeWalk.replaceAll(falseBranch, criteria, n -> Utils.FALSE_NODE);
        if (trueBranch != simplifiedTrueBranch || falseBranch != simplifiedFalseBranch) {
            return new FunctionNode(this, condition, simplifiedTrueBranch, simplifiedFalseBranch);
        } else {
            return null;
        }
    }
}
