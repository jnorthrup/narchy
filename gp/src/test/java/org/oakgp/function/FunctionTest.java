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
package org.oakgp.function;

import org.junit.jupiter.api.Test;
import org.oakgp.Arguments;
import org.oakgp.Assignments;
import org.oakgp.Type;
import org.oakgp.util.Signature;

import static org.junit.jupiter.api.Assertions.*;

public class FunctionTest {
    @Test
    public void testSimplify() {
        Function o = new Function() {
            @Override
            public Signature sig() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Object evaluate(Arguments arguments, Assignments assignments) {
                throw new UnsupportedOperationException();
            }
        };
        assertNull(o.simplify(null));
    }

    @Test
    public void testIsPure() {
        assertTrue(new DummyFunction().isPure());
    }

    @Test
    public void testGetDisplayName() {
        assertEquals("dummyfunction", new DummyFunction().name());
    }

    @Test
    public void testGetDisplayNameBooleanReturnType() {
        assertEquals("booleandummyfunction?", new IsBooleanDummyFunction().name());
    }
}

class DummyFunction implements Function {
    @Override
    public Signature sig() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object evaluate(Arguments arguments, Assignments assignments) {
        throw new UnsupportedOperationException();
    }
}

class IsBooleanDummyFunction implements Function {
    @Override
    public Signature sig() {
        return new Signature(Type.booleanType(), Type.integerType());
    }

    @Override
    public Object evaluate(Arguments arguments, Assignments assignments) {
        throw new UnsupportedOperationException();
    }
}
