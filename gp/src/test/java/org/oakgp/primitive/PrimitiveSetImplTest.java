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
package org.oakgp.primitive;

import org.junit.jupiter.api.Test;
import org.oakgp.NodeType;
import org.oakgp.function.Fn;
import org.oakgp.function.choice.If;
import org.oakgp.function.compare.*;
import org.oakgp.function.math.IntFunc;
import org.oakgp.node.ConstantNode;
import org.oakgp.node.VariableNode;
import org.oakgp.util.DummyRandom;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.oakgp.NodeType.*;
import static org.oakgp.TestUtils.*;
import static org.oakgp.util.Utils.intArrayType;

public class PrimitiveSetImplTest {
    

    private static final double VARIABLE_RATIO = .6;
    private static final ConstantNode[] CONSTANTS = {integerConstant(7), integerConstant(8), integerConstant(9)};
    private static final NodeType[] VARIABLE_TYPES = intArrayType(3);
    private static final Fn[] FUNCTIONS = new Fn[]{IntFunc.the.add, IntFunc.the.subtract,
            IntFunc.the.multiply, new If(integerType()), LessThan.create(integerType()), LessThanOrEqual.create(integerType()),
            new GreaterThan(integerType()), new GreaterThanOrEqual(integerType()), new Equal(integerType()), new NotEqual(integerType())};

    @Test
    public void testHasFunctions() {
        PrimitiveSet p = createWithFunctions(DummyRandom.EMPTY);
        assertTrue(p.hasFunctions(integerType()));
        assertFalse(p.hasFunctions(stringType()));
    }

    @Test
    public void testHasTerminals() {
        PrimitiveSet p = createWithTerminals(DummyRandom.EMPTY);
        assertTrue(p.hasTerminals(integerType()));
        assertFalse(p.hasTerminals(stringType()));
    }

    @Test
    public void testNextFunction() {
        Random mockRandom = mock(Random.class);
        
        given(mockRandom.nextInt(4)).willReturn(1, 0, 2, 1, 2, 0, 3);
        
        given(mockRandom.nextInt(6)).willReturn(1, 0, 5, 4);

        PrimitiveSet functionSet = createWithFunctions(mockRandom);

        
        assertSame(FUNCTIONS[1], functionSet.next(integerType()));
        assertSame(FUNCTIONS[0], functionSet.next(integerType()));
        assertSame(FUNCTIONS[2], functionSet.next(integerType()));
        assertSame(FUNCTIONS[1], functionSet.next(integerType()));
        assertSame(FUNCTIONS[2], functionSet.next(integerType()));
        assertSame(FUNCTIONS[0], functionSet.next(integerType()));
        assertSame(FUNCTIONS[3], functionSet.next(integerType()));

        assertSame(FUNCTIONS[5], functionSet.next(booleanType()));
        assertSame(FUNCTIONS[4], functionSet.next(booleanType()));
        assertSame(FUNCTIONS[9], functionSet.next(booleanType()));
        assertSame(FUNCTIONS[8], functionSet.next(booleanType()));
    }

    @Test
    public void testNextAlternativeFunction() {
        Random mockRandom = mock(Random.class);
        PrimitiveSet functionSet = createWithFunctions(mockRandom);

        given(mockRandom.nextInt(3)).willReturn(0, 1, 2, 0);
        given(mockRandom.nextInt(2)).willReturn(0, 1);
        assertSame(FUNCTIONS[1], functionSet.next(FUNCTIONS[0]));
        assertSame(FUNCTIONS[1], functionSet.next(FUNCTIONS[0]));
        assertSame(FUNCTIONS[2], functionSet.next(FUNCTIONS[0]));
        assertSame(FUNCTIONS[2], functionSet.next(FUNCTIONS[0]));

        given(mockRandom.nextInt(3)).willReturn(0, 1, 1, 2);
        given(mockRandom.nextInt(2)).willReturn(0, 1);
        assertSame(FUNCTIONS[0], functionSet.next(FUNCTIONS[1]));
        assertSame(FUNCTIONS[0], functionSet.next(FUNCTIONS[1]));
        assertSame(FUNCTIONS[2], functionSet.next(FUNCTIONS[1]));
        assertSame(FUNCTIONS[2], functionSet.next(FUNCTIONS[1]));

        given(mockRandom.nextInt(3)).willReturn(0, 1, 2, 2);
        given(mockRandom.nextInt(2)).willReturn(0, 1);
        assertSame(FUNCTIONS[0], functionSet.next(FUNCTIONS[2]));
        assertSame(FUNCTIONS[1], functionSet.next(FUNCTIONS[2]));
        assertSame(FUNCTIONS[0], functionSet.next(FUNCTIONS[2]));
        assertSame(FUNCTIONS[1], functionSet.next(FUNCTIONS[2]));
    }

    @Test
    public void testNextTerminal() {
        Random mockRandom = mock(Random.class);
        given(mockRandom.nextDouble()).willReturn(0.0, VARIABLE_RATIO, VARIABLE_RATIO + .01, .9, VARIABLE_RATIO - .01, .7);
        given(mockRandom.nextInt(3)).willReturn(1, 0, 2, 1, 0, 2);

        PrimitiveSet terminalSet = createWithTerminals(mockRandom);

        
        assertVariable(1, terminalSet.nextTerminal(integerType()));
        assertConstant(7, terminalSet.nextTerminal(integerType()));
        assertConstant(9, terminalSet.nextTerminal(integerType()));
        assertConstant(8, terminalSet.nextTerminal(integerType()));
        assertVariable(0, terminalSet.nextTerminal(integerType()));
        assertConstant(9, terminalSet.nextTerminal(integerType()));
    }

    @Test
    public void testNextAlternativeConstant() {
        Random mockRandom = mock(Random.class);
        given(mockRandom.nextDouble()).willReturn(VARIABLE_RATIO); 

        PrimitiveSet terminalSet = createWithTerminals(mockRandom);

        given(mockRandom.nextInt(3)).willReturn(0, 1, 2, 0);
        given(mockRandom.nextInt(2)).willReturn(0, 1);
        assertConstant(8, terminalSet.nextTerminal(CONSTANTS[0]));
        assertConstant(8, terminalSet.nextTerminal(CONSTANTS[0]));
        assertConstant(9, terminalSet.nextTerminal(CONSTANTS[0]));
        assertConstant(9, terminalSet.nextTerminal(CONSTANTS[0]));

        given(mockRandom.nextInt(3)).willReturn(0, 1, 1, 2);
        given(mockRandom.nextInt(2)).willReturn(0, 1);
        assertConstant(7, terminalSet.nextTerminal(CONSTANTS[1]));
        assertConstant(7, terminalSet.nextTerminal(CONSTANTS[1]));
        assertConstant(9, terminalSet.nextTerminal(CONSTANTS[1]));
        assertConstant(9, terminalSet.nextTerminal(CONSTANTS[1]));

        given(mockRandom.nextInt(3)).willReturn(0, 1, 2, 2);
        given(mockRandom.nextInt(2)).willReturn(0, 1);
        assertConstant(7, terminalSet.nextTerminal(CONSTANTS[2]));
        assertConstant(8, terminalSet.nextTerminal(CONSTANTS[2]));
        assertConstant(7, terminalSet.nextTerminal(CONSTANTS[2]));
        assertConstant(8, terminalSet.nextTerminal(CONSTANTS[2]));

        given(mockRandom.nextInt(3)).willReturn(2, 0, 1);
        assertConstant(9, terminalSet.nextTerminal(createVariable(9)));
        assertConstant(7, terminalSet.nextTerminal(createVariable(9)));
        assertConstant(8, terminalSet.nextTerminal(createVariable(9)));
    }

    @Test
    public void testNextAlternativeVariable() {
        Random mockRandom = mock(Random.class);
        given(mockRandom.nextDouble()).willReturn(VARIABLE_RATIO - .01); 

        NodeSet constantSet = NodeSet.byType(CONSTANTS);
        VariableSet variableSet = VariableSet.of(VARIABLE_TYPES);
        PrimitiveSet terminalSet = new PrimitiveSetImpl(null, constantSet, variableSet, mockRandom, VARIABLE_RATIO);

        given(mockRandom.nextInt(3)).willReturn(0, 1, 2, 0);
        given(mockRandom.nextInt(2)).willReturn(0, 1);
        VariableNode firstVariable = variableSet.get(0);
        assertVariable(1, terminalSet.nextTerminal(firstVariable));
        assertVariable(1, terminalSet.nextTerminal(firstVariable));
        assertVariable(2, terminalSet.nextTerminal(firstVariable));
        assertVariable(2, terminalSet.nextTerminal(firstVariable));

        given(mockRandom.nextInt(3)).willReturn(0, 1, 1, 2);
        given(mockRandom.nextInt(2)).willReturn(0, 1);
        VariableNode secondVariable = variableSet.get(1);
        assertVariable(0, terminalSet.nextTerminal(secondVariable));
        assertVariable(0, terminalSet.nextTerminal(secondVariable));
        assertVariable(2, terminalSet.nextTerminal(secondVariable));
        assertVariable(2, terminalSet.nextTerminal(secondVariable));

        given(mockRandom.nextInt(3)).willReturn(0, 1, 2, 2);
        given(mockRandom.nextInt(2)).willReturn(0, 1);
        VariableNode thirdVariable = variableSet.get(2);
        assertVariable(0, terminalSet.nextTerminal(thirdVariable));
        assertVariable(1, terminalSet.nextTerminal(thirdVariable));
        assertVariable(0, terminalSet.nextTerminal(thirdVariable));
        assertVariable(1, terminalSet.nextTerminal(thirdVariable));

        given(mockRandom.nextInt(3)).willReturn(2, 0, 1);
        ConstantNode constantNode = integerConstant(9);
        assertVariable(2, terminalSet.nextTerminal(constantNode));
        assertVariable(0, terminalSet.nextTerminal(constantNode));
        assertVariable(1, terminalSet.nextTerminal(constantNode));
    }

    private PrimitiveSet createWithTerminals(Random random) {
        NodeSet constantSet = NodeSet.byType(CONSTANTS);
        VariableSet variableSet = VariableSet.of(VARIABLE_TYPES);
        return new PrimitiveSetImpl(null, constantSet, variableSet, random, VARIABLE_RATIO);
    }

    private PrimitiveSet createWithFunctions(Random random) {
        return new PrimitiveSetImpl(new FnSet(FUNCTIONS), null, null, random, .1);
    }
}
