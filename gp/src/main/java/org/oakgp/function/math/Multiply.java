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
import org.oakgp.function.Fn;
import org.oakgp.node.FnNode;
import org.oakgp.node.Node;
import org.oakgp.node.NodeType;

import java.util.Arrays;

import static org.oakgp.node.NodeType.isConstant;

/**
 * Performs multiplication.
 */
public final class Multiply extends ArithmeticOperator {
    private final NumFunc<?> numberUtils;

    /**
     * @see NumFunc#getMultiply()
     */
    Multiply(NumFunc<?> numberUtils) {
        super(numberUtils.type);
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
        Node x = arguments.firstArg();
        Node y = arguments.secondArg();

         if (numberUtils.zero.equals(x)) {
            return numberUtils.zero;
        }
        if (numberUtils.zero.equals(y)) {
            return numberUtils.zero;
        }
        if (numberUtils.one.equals(x)) {
            return y;
        }
        if (numberUtils.one.equals(y)) {
            return x;
        }

        if (NodeType.func(y, "*")) {
            //verify commutive ordering
            Arguments yy = ((FnNode) y).args();
            Node[] mults = new Node[] { x, yy.get(0), yy.get(1) } ;
            Node[] original = mults.clone();
            Arrays.sort(mults);
            if (!Arrays.equals(original, mults)) {
                if (isConstant(mults[0]) && isConstant(mults[1])) {

                    return new FnNode(numberUtils.multiply, numberUtils.multiply(mults[0], mults[1]), mults[2] );
                } else {
                    return new FnNode(numberUtils.multiply, mults[0],
                            new FnNode(numberUtils.multiply, mults[1], mults[2]));
                }
            }

        }

        {
            if (isConstant(x) && numberUtils.isArithmeticExpression(y)) {
                FnNode fn = (FnNode) y;
                Fn f = fn.func();
                Arguments args = fn.args();
                Node fnArg1 = args.firstArg();
                Node fnArg2 = args.secondArg();
                if (isConstant(fnArg1)) {
                    if (numberUtils.isAddOrSubtract(f)) {
                        return new FnNode(f, numberUtils.multiply(x, fnArg1), new FnNode(this, x, fnArg2));
                    } else if (numberUtils.isMultiply(f)) {
                        return new FnNode(this, numberUtils.multiply(x, fnArg1), fnArg2);
                    } else if (numberUtils.isDivide(f)) {
                        
                        return null;
                    } else {
                        throw new IllegalArgumentException();
                    }
                } else if (numberUtils.isAddOrSubtract(f)) {
                    return new FnNode(f, new FnNode(this, x, fnArg1), new FnNode(this, x, fnArg2));
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
