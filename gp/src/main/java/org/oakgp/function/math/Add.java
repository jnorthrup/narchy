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
package org.oakgp.function.math;

import org.oakgp.Arguments;
import org.oakgp.Assignments;
import org.oakgp.node.FunctionNode;
import org.oakgp.node.Node;

import static org.oakgp.node.NodeType.isConstant;
import static org.oakgp.node.NodeType.isFunction;
import static org.oakgp.util.NodeComparator.NODE_COMPARATOR;

/**
 * Performs addition.
 */
final class Add extends ArithmeticOperator {
    private final NumFunc<?> numberUtils;
    private final ArithmeticExpressionSimplifier simplifier;

    /**
     * @see NumFunc#getAdd()
     */
    Add(NumFunc<?> numberUtils) {
        super(numberUtils.getType());
        this.numberUtils = numberUtils;
        this.simplifier = numberUtils.getSimplifier();
    }

    @Override
    public final boolean argsSorted() {
        return true;
    }

    /**
     * Returns the result of adding the two elements of the specified arguments.
     *
     * @return the result of adding {@code arg1} and {@code arg2}
     */
    @Override
    protected Object evaluate(Node arg1, Node arg2, Assignments assignments) {
        return numberUtils.add(arg1, arg2, assignments).eval(null);
    }

    @Override
    public Node simplify(Arguments arguments) {
        Node arg1 = arguments.firstArg();
        Node arg2 = arguments.secondArg();

        if (NODE_COMPARATOR.compare(arg1, arg2) > 0) {
            
            
            return new FunctionNode(this, arg2, arg1);
        } else if (numberUtils.zero.equals(arg1)) {
            
            
            return arg2;
        } else if (numberUtils.zero.equals(arg2)) {
            
            throw new IllegalArgumentException("arg1 " + arg1 + " arg2 " + arg2);
        } else if (arg1.equals(arg2)) {
            
            
            return numberUtils.multiplyByTwo(arg1);
        } else if (isConstant(arg1) && numberUtils.isNegative(arg1)) {
            
            
            return new FunctionNode(numberUtils.getSubtract(), arg2, numberUtils.negateConstant(arg1));
        } else if (isConstant(arg2) && numberUtils.isNegative(arg2)) {
            
            
            
            
            throw new IllegalArgumentException("arg1 " + arg1 + " arg2 " + arg2);
        } else if (isConstant(arg1) && isFunction(arg2)) {
            FunctionNode fn2 = (FunctionNode) arg2;
            if (isConstant(fn2.args().firstArg()) && numberUtils.isAddOrSubtract(fn2.func())) {
                return new FunctionNode(fn2.func(), numberUtils.add(arg1, fn2.args().firstArg()), fn2.args().secondArg());
            }
        }

        return simplifier.simplify(this, arg1, arg2);
    }

    @Override
    public String name() {
        return "+";
    }
}
