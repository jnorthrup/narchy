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

import static org.oakgp.node.NodeType.isConstant;

/**
 * Performs subtraction.
 */
public final class Subtract extends ArithmeticOperator {
    private final NumFunc<?> numberUtils;
    private final ArithmeticExpressionSimplifier simplifier;

    /**
     * @see NumFunc#getSubtract()
     */
    Subtract(NumFunc<?> numberUtils) {
        super(numberUtils.type);
        this.numberUtils = numberUtils;
        this.simplifier = numberUtils.simplifier;
    }

    /**
     * Returns the result of subtracting the second element of the specified arguments from the first.
     *
     * @return the result of subtracting {@code arg2} from {@code arg1}
     */
    @Override
    protected Object evaluate(Node arg1, Node arg2, Assignments assignments) {
        return numberUtils.subtract(arg1, arg2, assignments).eval(null);
    }

    @Override
    public Node simplify(Arguments arguments) {
        Node x = arguments.firstArg();
        Node y = arguments.secondArg();

        if (x.equals(y)) {
            return numberUtils.zero;
        }
        if (numberUtils.zero.equals(y)) {
            return x;
        }
        if (numberUtils.zero.equals(x) && numberUtils.isSubtract(y)) {
            FnNode fn2 = (FnNode) y;
            Arguments fn2Arguments = fn2.args();
            return new FnNode(this, fn2Arguments.secondArg(), fn2Arguments.firstArg());
        }
        if (isConstant(y) && numberUtils.isNegative(y)) {
            return new FnNode(numberUtils.add, numberUtils.negate(y), x);
        }


        {
            if (numberUtils.isArithmeticExpression(y)) {
                FnNode fn = (FnNode) y;
                Fn f = fn.func();
                Arguments args = fn.args();
                Node fnArg1 = args.firstArg();
                Node fnArg2 = args.secondArg();
                if (numberUtils.isMultiply(f) && isConstant(fnArg1)) {
                    if (numberUtils.zero.equals(x)) {

                        return new FnNode(f, numberUtils.negateConstant(fnArg1), fnArg2);
                    } else if (numberUtils.isNegative(fnArg1)) {

                        return new FnNode(numberUtils.add, x, new FnNode(f, numberUtils.negateConstant(fnArg1), fnArg2));
                    }
                } else if (numberUtils.isAdd(f) && numberUtils.zero.equals(x)) {

                    return new FnNode(f, numberUtils.negate(fnArg1), numberUtils.negate(fnArg2));
                } else if (numberUtils.isSubtract(fn) && isConstant(x) && isConstant(fnArg1)) {
                    if (numberUtils.zero.equals(x)) {

                        throw new IllegalArgumentException();
                    } else if (numberUtils.zero.equals(fnArg1)) {

                        return new FnNode(numberUtils.add, x, fnArg2);
                    } else {

                        return new FnNode(numberUtils.add, numberUtils.subtract(x, fnArg1), fnArg2);
                    }
                }
            }

            //(- (- a b) (- c d))
            //  == (a - b) - (c - d) == (a - b) + (d - c) == (a + d) -b  -c =
            // (a+d) - (b + c) = (+ a ( - d ( + b c )
            if (NodeType.func(x, "-") && NodeType.func(y, "-")) {
                Arguments xx = ((FnNode) x).args();
                Node a = xx.firstArg();
                Node b = xx.secondArg();

                Arguments yy = ((FnNode) y).args();
                Node c = yy.firstArg();
                Node d = yy.secondArg();

                if (a.compareTo(d) > 0) {
                    Node t = d;
                    d = a;
                    a = t;
                }
                if (b.compareTo(c) > 0) {
                    Node t = c;
                    c = b;
                    b = t;
                }
                // (a+d) - (b + c) = (+ a ( - d ( + b c )
//                return new FunctionNode(numberUtils.subtract,
//                        new FunctionNode(numberUtils.addAt, a, d),
//                        new FunctionNode(numberUtils.addAt, b, c));
                return new FnNode(numberUtils.add, a,
                        new FnNode(numberUtils.subtract, d,
                                new FnNode(numberUtils.add, b, c)
                        )
                );
            }

            return simplifier.simplify(this, x, y);
        }

    }

    @Override
    public String name() {
        return "-";
    }
}
