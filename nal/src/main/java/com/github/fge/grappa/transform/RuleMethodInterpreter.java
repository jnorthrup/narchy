/*
 * Copyright (c) 2009-2010 Ken Wenzel and Mathias Doenitz
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.github.fge.grappa.transform;

import com.github.fge.grappa.exceptions.InvalidGrammarException;
import com.github.fge.grappa.rules.Rule;
import com.github.fge.grappa.transform.base.InstructionGraphNode;
import com.github.fge.grappa.transform.base.RuleMethod;
import com.google.common.base.Preconditions;
import jcog.data.list.FasterList;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;

import java.util.Arrays;
import java.util.List;

/**
 * dumb patch necessary to get it to work with ASM6
 */
public final class RuleMethodInterpreter extends BasicInterpreter {
    public static final BasicValue[] EmptyBasicValues = new BasicValue[0];
    final String errorMessage;
    private final RuleMethod method;
    private final List<Edge> additionalEdges = new FasterList<>();

    public RuleMethodInterpreter(RuleMethod method) {
        super(ASM7);
        this.method = method;
        this.errorMessage = "Internal error during analysis of rule method: " + method.name;
    }

    private static boolean isLabelOrJump(AbstractInsnNode node) {
        var t = node.getType();
        return t == AbstractInsnNode.LABEL || t == AbstractInsnNode.JUMP_INSN;
    }

    private static BasicValue unwrap(BasicValue resultValue) {
        return resultValue instanceof InstructionGraphNode
                ? ((InstructionGraphNode) resultValue).getResultValue()
                : resultValue;
    }

    @Override
    public BasicValue newValue(Type type) {
        var basicValue = super.newValue(type);
        if (basicValue == BasicValue.REFERENCE_VALUE)

            basicValue = new BasicValue(type);

        return basicValue;
    }

    @Override
    public BasicValue newOperation(AbstractInsnNode insn)
            throws AnalyzerException {
        return createNode(insn, super.newOperation(insn));
    }

    @Override
    public BasicValue copyOperation(AbstractInsnNode insn,
                                    BasicValue value)
            throws AnalyzerException {
        return createNode(insn, super.copyOperation(insn, value), value);
    }

    @Override
    public BasicValue unaryOperation(AbstractInsnNode insn,
                                     BasicValue value)
            throws AnalyzerException {
        return createNode(insn, super.unaryOperation(insn, null), value);
    }

    @Override
    public BasicValue binaryOperation(AbstractInsnNode insn,
                                      BasicValue value1, BasicValue value2)
            throws AnalyzerException {
        return createNode(insn, super.binaryOperation(insn, null, null), value1,
                value2);
    }

    @Override
    public BasicValue ternaryOperation(AbstractInsnNode insn,
                                       BasicValue v1, BasicValue v2, BasicValue v3)
            throws AnalyzerException {




        additionalEdges.add(new Edge(insn, findArrayCreatorPredecessor(v1)));
        return createNode(insn, super.ternaryOperation(insn, null, null, null),
                v1, v2, v3);
    }

    @Override
    public BasicValue naryOperation(AbstractInsnNode insn,
                                    List<? extends BasicValue> values)
            throws AnalyzerException {
        return createNode(insn, super.naryOperation(insn, null),
                values.toArray(EmptyBasicValues));
    }

    @Override
    public void returnOperation(AbstractInsnNode insn,
                                BasicValue value, BasicValue expected) {
        Preconditions.checkState(insn.getOpcode() == ARETURN);
        var type = Type.getType(Rule.class);
        Preconditions.checkState(unwrap(value).getType().equals(
                type));
        Preconditions.checkState(unwrap(expected).getType().equals(
                type));
        Preconditions.checkState(method.getReturnInstructionNode() == null);
        method.setReturnInstructionNode(createNode(insn, null, value));
    }

    private InstructionGraphNode createNode(AbstractInsnNode insn,
                                            BasicValue resultValue, BasicValue... prevNodes) {
        return method.setGraphNode(insn, unwrap(resultValue), Arrays.asList(
                prevNodes));
    }

    @Override
    public BasicValue merge(BasicValue v, BasicValue w) {


        return v;
    }

    public void newControlFlowEdge(int instructionIndex,
                                   int successorIndex) {
        var fromInsn
                = method.instructions.get(instructionIndex);
        var toInsn = method.instructions.get(successorIndex);
        if (isLabelOrJump(fromInsn) || isLabelOrJump(toInsn))
            additionalEdges.add(new Edge(fromInsn, toInsn));

    }

    private AbstractInsnNode findArrayCreatorPredecessor(BasicValue value) {

        if (!(value instanceof InstructionGraphNode))
            throw new InvalidGrammarException(errorMessage);

        var node = (InstructionGraphNode) value;

        while (true) {
            var opcode = node.getInstruction().getOpcode();

            if (opcode == ANEWARRAY || opcode == NEWARRAY
                    || opcode == MULTIANEWARRAY)
                break;

            var predecessors
                    = node.getPredecessors();

            if (predecessors.size() != 1)
                throw new InvalidGrammarException(errorMessage);

            node = predecessors.get(0);
        }

        return node.getInstruction();
    }

    public void finish() {

        for (var edge : additionalEdges) {
            var node = getGraphNode(edge.from);
            if (node == null)
                node = createNode(edge.from, null);
            var succ = getGraphNode(edge.to);
            if (succ == null)
                succ = createNode(edge.to, null);
            succ.addPredecessor(node);
        }
    }

    private InstructionGraphNode getGraphNode(AbstractInsnNode insn) {
        return method.getGraphNodes().get(method.instructions.indexOf(insn));
    }

    private static final class Edge {
        private final AbstractInsnNode from;
        private final AbstractInsnNode to;

        private Edge(AbstractInsnNode from, AbstractInsnNode to) {
            this.from = from;
            this.to = to;
        }
    }
}
