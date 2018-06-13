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
import org.oakgp.node.NodeType;

import java.util.Arrays;

import static org.oakgp.node.NodeType.isConstant;
import static org.oakgp.node.NodeType.isFunction;

/**
 * Performs addition.
 */
public final class Add extends ArithmeticOperator {
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
        Node x = arguments.firstArg();
        Node y = arguments.secondArg();

        if (numberUtils.zero.equals(x)) {
            return y;
        }
        if (numberUtils.zero.equals(y)) {
            //throw new IllegalArgumentException("arg1 " + arg1 + " arg2 " + arg2);
            return x;
        }


        if (x.equals(y)) {
            return numberUtils.multiplyByTwo(x);
        }
//
        if (isConstant(x) && numberUtils.isNegative(x)) {


            return new FunctionNode(numberUtils.subtract, y, numberUtils.negateConstant(x));
        }
//        } else if (isConstant(arg2) && numberUtils.isNegative(arg2)) {
//
//
//
//
//            throw new IllegalArgumentException("arg1 " + arg1 + " arg2 " + arg2);
//        } 
        if (isConstant(x) && isFunction(y)) {
            FunctionNode fn2 = (FunctionNode) y;
            if (isConstant(fn2.args().firstArg()) && numberUtils.isAddOrSubtract(fn2.func())) {
                return new FunctionNode(fn2.func(), numberUtils.add(x, fn2.args().firstArg()), fn2.args().secondArg());
            }
        }
        if (NodeType.func(y, "+")) {
            //verify commutive ordering
            Arguments yy = ((FunctionNode) y).args();
            Node[] adds = new Node[] { x, yy.get(0), yy.get(1) } ;
            Node[] original = adds.clone();
            Arrays.sort(adds);
            if (!Arrays.equals(original, adds)) {
                if (isConstant(adds[0]) && isConstant(adds[1])) {
                    return new FunctionNode(numberUtils.add, numberUtils.add(adds[0], adds[1]), adds[2] );
                } else {
                    return new FunctionNode(numberUtils.add, adds[0],
                            new FunctionNode(numberUtils.add, adds[1], adds[2]));
                }
            }

        }


        //(+ (- a b) (- c d))
        //  == (a - b) + (c - d) == (a + c) - (b + d) == (+ a (- c (+ b d )))
        if (NodeType.func(x, "-") && NodeType.func(y, "-")) {
            Arguments xx = ((FunctionNode) x).args();
            Node a = xx.firstArg();
            Node b = xx.secondArg();

            Arguments yy = ((FunctionNode) y).args();
            Node c = yy.firstArg();
            Node d = yy.secondArg();

//            return new FunctionNode(numberUtils.subtract,
//                    new FunctionNode(numberUtils.add, a, c),
//                    new FunctionNode(numberUtils.add, b, d)
//            );

            if (a.compareTo(c) > 0) {
                Node t = c;
                c = a;
                a = t;
            }
            if (b.compareTo(d) > 0) {
                Node t = d;
                d = b;
                b = t;
            }

            //(+ a (- c (+ b d )))
            return new FunctionNode(numberUtils.add, a,
                    new FunctionNode(numberUtils.subtract, c,
                        new FunctionNode(numberUtils.add, b, d)
                    )
            );

        }



        return simplifier.simplify(this, x, y);
    }

    @Override
    public String name() {
        return "+";
    }
}
