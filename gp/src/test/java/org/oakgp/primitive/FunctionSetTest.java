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
import org.oakgp.function.Function;
import org.oakgp.function.Signature;
import org.oakgp.function.choice.If;
import org.oakgp.function.classify.IsNegative;
import org.oakgp.function.classify.IsPositive;
import org.oakgp.function.classify.IsZero;
import org.oakgp.function.coll.Count;
import org.oakgp.function.compare.*;
import org.oakgp.function.hof.Filter;
import org.oakgp.function.hof.Reduce;
import org.oakgp.function.math.IntFunc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.oakgp.TestUtils.assertUnmodifiable;
import static org.oakgp.Type.*;

public class FunctionSetTest {
    private static final Function ADD = IntFunc.the.add;
    private static final Function SUBTRACT = IntFunc.the.subtract;
    private static final Function MULTIPLY = IntFunc.the.getMultiply();

    private static FunctionSet createFunctionSet() {
        return new FunctionSet(
                
                ADD, SUBTRACT, MULTIPLY,
                
                LessThan.create(integerType()), LessThanOrEqual.create(integerType()), new GreaterThan(integerType()), new GreaterThanOrEqual(integerType()),
                new Equal(integerType()), new NotEqual(integerType()),
                
                new If(integerType()),
                
                new Reduce(integerType()), new Filter(integerType()), new org.oakgp.function.hof.Map(integerType(), booleanType()),
                
                new IsPositive(), new IsNegative(), new IsZero(),
                
                new Count(integerType()), new Count(booleanType()));
    }

    @Test
    public void testGetByType() {
        IsZero isZero = new IsZero();
        FunctionSet functionSet = new FunctionSet(ADD, SUBTRACT, MULTIPLY, isZero);

        List<Function> integers = functionSet.getByType(integerType());
        assertEquals(3, integers.size());
        assertSame(ADD, integers.get(0));
        assertSame(SUBTRACT, integers.get(1));
        assertSame(MULTIPLY, integers.get(2));

        List<Function> booleans = functionSet.getByType(booleanType());
        assertEquals(1, booleans.size());
        assertSame(isZero, booleans.get(0));

        assertNull(functionSet.getByType(stringType()));
    }

    @Test
    public void assertGetByTypeUnmodifiable() {
        FunctionSet functionSet = createFunctionSet();
        List<Function> integers = functionSet.getByType(integerType());
        assertUnmodifiable(integers);
    }

    @Test
    public void testGetBySignature() {
        Count countIntegerArray = new Count(integerType());
        Count countBooleanArray = new Count(booleanType());
        FunctionSet functionSet = new FunctionSet(ADD, SUBTRACT, countIntegerArray, countBooleanArray);

        
        assertEquals(4, functionSet.getByType(integerType()).size());

        List<Function> integers = functionSet.getBySignature(new Signature(integerType(), integerType(), integerType()));
        assertEquals(2, integers.size());
        assertSame(ADD, integers.get(0));
        assertSame(SUBTRACT, integers.get(1));

        List<Function> integerArrays = functionSet.getBySignature(new Signature(integerType(), integerArrayType()));
        assertEquals(1, integerArrays.size());
        assertSame(countIntegerArray, integerArrays.get(0));

        List<Function> booleanArrays = functionSet.getBySignature(new Signature(integerType(), booleanArrayType()));
        assertEquals(1, booleanArrays.size());
        assertSame(countBooleanArray, booleanArrays.get(0));

        assertNull(functionSet.getBySignature(new Signature(stringType(), integerType(), integerType())));
    }

    @Test
    public void assertGetBySignatureUnmodifiable() {
        FunctionSet functionSet = createFunctionSet();
        List<Function> integers = functionSet.getByType(integerType());
        assertUnmodifiable(integers);
    }
}
