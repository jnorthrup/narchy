/*
 * Copyright 2015 S. Webber
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import org.oakgp.node.ConstantNode;
import org.oakgp.node.FunctionNode;
import org.oakgp.node.Node;

import static org.oakgp.node.NodeType.*;
import static org.oakgp.util.NodeComparator.NODE_COMPARATOR;

final class ArithmeticExpressionSimplifier {
    private static final boolean SANITY_CHECK = false;

    private final NumFunc<?> numberUtils;

    ArithmeticExpressionSimplifier(NumFunc<?> numberUtils) {
        this.numberUtils = numberUtils;
    }

    /**
     * Returns {@code true} if the specified nodes can be combined into a single node.
     * <p>
     * Two constants (even if they have different values) can be combined. e.g. {@code 9} and {@code 12} can be combined to form {@code 21}
     * </p>
     * <p>
     * Any two node that are {@code equal} can be combined. e.g. {@code v0} and {@code v0} can be combined to form {@code (* 2 v0)}, {@code (- 8 v0)} and
     * {@code (- 8 v0)} can be combined to form {@code (* 2 (- 8 v0))}
     * </p>
     */
    private static boolean isSuitableForCombining(Node currentNode, Node nodeToReplace) {
        if (isConstant(nodeToReplace)) {
            return isConstant(currentNode);
        } else {
            return nodeToReplace.equals(currentNode);
        }
    }

    private static void sanityCheck(Runnable r) {
        // TODO remove this method - only here to sanity check input during development
        if (SANITY_CHECK) {
            r.run();
        }
    }

    private static void assertSameClass(Node currentNode, Node nodeToReplace) {
        if (nodeToReplace.getClass() != currentNode.getClass()) {
            throw new IllegalArgumentException(nodeToReplace.getClass().getName() + ' ' + currentNode.getClass().getName());
        }
    }

    /**
     * Asserts that the specified nodes evaluate to the same results.
     *
     * @param first  the node to compare to {@code second}
     * @param second the node to compare to {@code first}
     * @throws IllegalArgumentException if the specified nodes evaluate to different results
     */
    static void assertEvaluateToSameResult(Node first, Node second) {
        Object[] assignedValues = {2, 14, 4, 9, 7};
        Assignments assignments = new Assignments(assignedValues);
        Object firstResult = first.eval(assignments);
        Object secondResult = second.eval(assignments);
        if (!firstResult.equals(secondResult)) {
            throw new IllegalArgumentException(first + " = " + firstResult + ' ' + second + " = " + secondResult);
        }
    }

    /**
     * @return {@code null} if it was not possible to simplify the expression.
     */
    Node simplify(Function function, Node firstArg, Node secondArg) {
        sanityCheck(() -> {
            assertAddOrSubtract(function);
            assertArgumentsOrdered(function, firstArg, secondArg);
        });

        return functionSimplified(function, firstArg, secondArg);
    }

    private Node functionSimplified(Function f, Node firstArg, Node secondArg) {
        if (f.argsSorted() && secondArg.compareTo(firstArg) < 0) {
            Node x = firstArg;
            firstArg = secondArg;
            secondArg = x;
        }

        if (f instanceof ArithmeticOperator) {
            if (isConstant(firstArg) && isConstant(secondArg)) {
                return new ConstantNode(
                        ((ArithmeticOperator) f).evaluate(
                                firstArg, secondArg, null
                        ), f.sig().returnType()
                );
            }
        }

        boolean isPos = numberUtils.isAdd(f);
        if (areFunctions(firstArg, secondArg)) {

            NodePair p = removeFromChildNodes(firstArg, secondArg, isPos);
            if (p != null) {
                return new FunctionNode(f, p.nodeThatHasBeenReduced, p.nodeThatHasBeenExpanded);
            }
            p = removeFromChildNodes(secondArg, firstArg, isPos);
            if (p != null) {
                return new FunctionNode(f, p.nodeThatHasBeenExpanded, p.nodeThatHasBeenReduced);
            }
        } else if (isFunction(firstArg)) {
            return combineWithChildNodes(firstArg, secondArg, isPos);
        } else if (isFunction(secondArg)) {
            // 3, (+ (* 12 v2) 30) -> (+ (* 12 v2) 33)
            Node tmp = combineWithChildNodes(secondArg, firstArg, isPos);
            if (tmp != null && numberUtils.isSubtract(f)) {
                // 3, (- (* 12 v2) 30) -> (- (* 12 v2) 33) -> (0 - (- (* 12 v2) 33))
                return new FunctionNode(f, numberUtils.zero, tmp);
            } else {
                return tmp;
            }
        }

        return null;
    }


    /**
     * Returns the result of removing the second argument from the first argument.
     *
     * @param nodeToWalk   tree structure to walk and remove the node from
     * @param nodeToRemove the node to remove from {@code nodeToWalk}
     * @param isPos        {@code true} to indicate that {@code nodeToRemove} should be removed from {@code nodeToWalk}, else {@code false} to indicate that
     *                     {@code nodeToAdd} should be added to {@code nodeToWalk}
     * @return {@code null} if it was not possible to remove (@code nodeToRemove} from {@code nodeToWalk}
     */
    private NodePair removeFromChildNodes(final Node nodeToWalk, final Node nodeToRemove, final boolean isPos) {
        if (numberUtils.isArithmeticExpression(nodeToWalk)) {
            FunctionNode fn = (FunctionNode) nodeToWalk;
            Function f = fn.func();
            Node firstArg = fn.args().firstArg();
            Node secondArg = fn.args().secondArg();
            if (numberUtils.isMultiply(f) && isFunction(nodeToRemove)) {
                FunctionNode x = (FunctionNode) nodeToRemove;
                Arguments a = x.args();
                if (numberUtils.isMultiply(x) && isConstant(firstArg) && isConstant(a.firstArg()) && secondArg.equals(a.secondArg())) {
                    ConstantNode result;
                    if (isPos) {
                        result = numberUtils.add(a.firstArg(), firstArg);
                    } else {
                        result = numberUtils.subtract(a.firstArg(), firstArg);
                    }
                    Node tmp = new FunctionNode(f, result, secondArg);
                    return new NodePair(numberUtils.zero, tmp);
                }

                Node tmp = combineWithChildNodes(nodeToRemove, nodeToWalk, isPos);
                if (tmp != null) {
                    return new NodePair(numberUtils.zero, tmp);
                }
            }

            boolean isSubtract = numberUtils.isSubtract(f);
            if (numberUtils.isAdd(f) || isSubtract) {
                NodePair p = removeFromChildNodes(firstArg, nodeToRemove, isPos);
                if (p != null) {
                    NodePair p2 = removeFromChildNodes(secondArg, p.nodeThatHasBeenExpanded, isSubtract != isPos);
                    if (p2 == null) {
                        return new NodePair(new FunctionNode(f, p.nodeThatHasBeenReduced, secondArg), p.nodeThatHasBeenExpanded);
                    } else {
                        return new NodePair(new FunctionNode(f, p.nodeThatHasBeenReduced, p2.nodeThatHasBeenReduced), p2.nodeThatHasBeenExpanded);
                    }
                }
                p = removeFromChildNodes(secondArg, nodeToRemove, isSubtract != isPos);
                if (p != null) {
                    return new NodePair(new FunctionNode(f, firstArg, p.nodeThatHasBeenReduced), p.nodeThatHasBeenExpanded);
                }
            }
        } else if (!numberUtils.zero.equals(nodeToWalk)) {
            Node tmp = combineWithChildNodes(nodeToRemove, nodeToWalk, isPos);
            if (tmp != null) {
                return new NodePair(numberUtils.zero, tmp);
            }
        }
        return null;
    }

    /**
     * Returns the result of merging the second argument into the first argument.
     *
     * @param nodeToWalk tree structure to walk and remove the node to
     * @param nodeToAdd  the node to remove from {@code nodeToWalk}
     * @param isPos      {@code true} to indicate that {@code nodeToAdd} should be added to {@code nodeToWalk}, else {@code false} to indicate that {@code nodeToAdd}
     *                   should be subtracted from {@code nodeToWalk}
     * @return {@code null} if it was not possible to merge (@code nodeToAdd} into {@code nodeToWalk}
     */
    Node combineWithChildNodes(final Node nodeToWalk, final Node nodeToAdd, final boolean isPos) {
        if (isSuitableForCombining(nodeToWalk, nodeToAdd)) {
            return combine(nodeToWalk, nodeToAdd, isPos);
        }
        if (!numberUtils.isArithmeticExpression(nodeToWalk)) {
            return null;
        }

        FunctionNode currentFunctionNode = (FunctionNode) nodeToWalk;
        Node firstArg = currentFunctionNode.args().firstArg();
        Node secondArg = currentFunctionNode.args().secondArg();
        Function f = currentFunctionNode.func();
        boolean isAdd = numberUtils.isAdd(f);
        boolean isSubtract = numberUtils.isSubtract(f);
        if (isAdd || isSubtract) {
            boolean recursiveIsPos = isPos;
            if (isSubtract) {
                recursiveIsPos = !isPos;
            }
            if (isSuitableForCombining(firstArg, nodeToAdd)) {
                return new FunctionNode(f, combine(firstArg, nodeToAdd, isPos), secondArg);
            } else if (isSuitableForCombining(secondArg, nodeToAdd)) {
                return new FunctionNode(f, firstArg, combine(secondArg, nodeToAdd, recursiveIsPos));
            }
            Node tmp = combineWithChildNodes(firstArg, nodeToAdd, isPos);
            if (tmp != null) {
                return new FunctionNode(f, tmp, secondArg);
            }
            tmp = combineWithChildNodes(secondArg, nodeToAdd, recursiveIsPos);
            if (tmp != null) {
                return new FunctionNode(f, firstArg, tmp);
            }
        } else if (numberUtils.isMultiply(f) && isConstant(firstArg) && secondArg.equals(nodeToAdd)) {
            ConstantNode multiplier;
            if (isPos) {
                multiplier = numberUtils.increment(firstArg);
            } else {
                multiplier = numberUtils.decrement(firstArg);
            }
            return new FunctionNode(f, multiplier, nodeToAdd);
        } else if (isMultiplyingTheSameValue(nodeToWalk, nodeToAdd)) {
            return combineMultipliers(nodeToWalk, nodeToAdd, isPos);
        }

        return null;
    }

    /**
     * Returns a node that is the result of combining the two specified nodes.
     * <p>
     * e.g. {@code 9} and {@code 12} can be combined to form {@code 21}, {@code v0} and {@code v0} can be combined to form {@code (* 2 v0)}
     * </p>
     *
     * @param isPos {@code true} to indicate that {@code second} should be added to {@code first}, else {@code false} to indicate that {@code second} should be
     *              subtracted from {@code first}
     */
    private Node combine(Node first, Node second, boolean isPos) {
        sanityCheck(() -> assertSameClass(first, second));

        if (isConstant(second)) {
            if (isPos) {
                return numberUtils.add(first, second);
            } else {
                return numberUtils.subtract(first, second);
            }
        } else {
            if (isPos) {
                return numberUtils.multiplyByTwo(second);
            } else {
                return numberUtils.zero;
            }
        }
    }

    /**
     * Returns {@code true} if both of the specified nodes represent multiplication of the same value by a constant.
     * <p>
     * Examples of arguments that would return true: {@code (* 3 v0), (* 7 v0)} or {@code (* 1 v0), (* -8 v0)}
     * </p>
     * <p>
     * Examples of arguments that would return false: {@code (* 3 v0), (+ 7 v0)} or {@code (* 1 v0), (* -8 v1)}
     * </p>
     */
    private boolean isMultiplyingTheSameValue(Node n1, Node n2) {
        if (areFunctions(n1, n2)) {
            FunctionNode f1 = (FunctionNode) n1;
            FunctionNode f2 = (FunctionNode) n2;
            if (numberUtils.isMultiply(f1) && numberUtils.isMultiply(f2) && isConstant(f1.args().firstArg()) && isConstant(f2.args().firstArg())
                    && f1.args().secondArg().equals(f2.args().secondArg())) {
                return true;
            }
        }
        return false;
    }

    /**
     * e.g. arguments: {@code (* 3 v0), (* 7 v0)} would produce: {@code (* 10 v0)}
     */
    private Node combineMultipliers(Node n1, Node n2, boolean isPos) {
        FunctionNode f1 = (FunctionNode) n1;
        FunctionNode f2 = (FunctionNode) n2;
        ConstantNode result;
        if (isPos) {
            result = numberUtils.add(f1.args().firstArg(), f2.args().firstArg());
        } else {
            result = numberUtils.subtract(f1.args().firstArg(), f2.args().firstArg());
        }
        return new FunctionNode(f1.func(), result, f1.args().secondArg());
    }

    private void assertAddOrSubtract(Function f) {
        if (!numberUtils.isAddOrSubtract(f)) {
            throw new IllegalArgumentException(f.getClass().getName());
        }
    }

    private void assertArgumentsOrdered(Function f, Node firstArg, Node secondArg) {
        if (!numberUtils.isSubtract(f) && NODE_COMPARATOR.compare(firstArg, secondArg) > 0) {
            throw new IllegalArgumentException("arg1 " + firstArg + " arg2 " + secondArg);
        }
    }

    private static class NodePair {
        private final Node nodeThatHasBeenReduced;
        private final Node nodeThatHasBeenExpanded;

        NodePair(Node nodeThatHasBeenReduced, Node nodeThatHasBeenExpanded) {
            this.nodeThatHasBeenReduced = nodeThatHasBeenReduced;
            this.nodeThatHasBeenExpanded = nodeThatHasBeenExpanded;
        }
    }
}
