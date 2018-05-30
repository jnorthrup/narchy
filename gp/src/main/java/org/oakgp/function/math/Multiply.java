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
import org.oakgp.function.Function;
import org.oakgp.node.FunctionNode;
import org.oakgp.node.Node;

import static org.oakgp.node.NodeType.isConstant;
import static org.oakgp.util.NodeComparator.NODE_COMPARATOR;

/**
 * Performs multiplication.
 */
final class Multiply extends ArithmeticOperator {
    private final NumFunc<?> numberUtils;

    /**
     * @see NumFunc#getMultiply()
     */
    Multiply(NumFunc<?> numberUtils) {
        super(numberUtils.getType());
        this.numberUtils = numberUtils;
    }

    @Override
    public final boolean argsSorted() {
        return true;
    }

    /**
     * Returns the result of multiplying the two elements of the specified arguments.
     *
     * @return the result of multiplying {@code arg1} and {@code arg2}
     */
    @Override
    protected Object evaluate(Node arg1, Node arg2, Assignments assignments) {
        if (arg1.equals(numberUtils.one))
            return arg2.eval(assignments);
        if (arg2.equals(numberUtils.one))
            return arg1.eval(assignments);
        return numberUtils.multiply(arg1, arg2, assignments).eval(null);
    }

    @Override
    public Node simplify(Arguments arguments) {
        Node arg1 = arguments.firstArg();
        Node arg2 = arguments.secondArg();

        if (NODE_COMPARATOR.compare(arg1, arg2) > 0) {
            
            
            return new FunctionNode(this, arg2, arg1);
        } else if (numberUtils.zero.equals(arg1)) {
            
            
            return numberUtils.zero;
        } else if (numberUtils.zero.equals(arg2)) {
            
            throw new IllegalArgumentException("arg1 " + arg1 + " arg2 " + arg2);
        } else if (numberUtils.one.equals(arg1)) {
            
            
            return arg2;
        } else if (numberUtils.one.equals(arg2)) {
            
            throw new IllegalArgumentException("arg1 " + arg1 + " arg2 " + arg2);
        } else {
            if (isConstant(arg1) && numberUtils.isArithmeticExpression(arg2)) {
                FunctionNode fn = (FunctionNode) arg2;
                Function f = fn.func();
                Arguments args = fn.args();
                Node fnArg1 = args.firstArg();
                Node fnArg2 = args.secondArg();
                if (isConstant(fnArg1)) {
                    if (numberUtils.isAddOrSubtract(f)) {
                        return new FunctionNode(f, numberUtils.multiply(arg1, fnArg1), new FunctionNode(this, arg1, fnArg2));
                    } else if (numberUtils.isMultiply(f)) {
                        return new FunctionNode(this, numberUtils.multiply(arg1, fnArg1), fnArg2);
                    } else if (numberUtils.isDivide(f)) {
                        
                        return null;
                    } else {
                        throw new IllegalArgumentException();
                    }
                } else if (numberUtils.isAddOrSubtract(f)) {
                    return new FunctionNode(f, new FunctionNode(this, arg1, fnArg1), new FunctionNode(this, arg1, fnArg2));
                }
            }

            return null;
        }
    }

    @Override
    public String name() {
        return "*";
    }
}
