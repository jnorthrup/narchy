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
package org.oakgp.function.compare;

import org.junit.jupiter.api.Test;
import org.oakgp.NodeType;
import org.oakgp.function.Fn;
import org.oakgp.util.Signature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.oakgp.NodeType.booleanType;

public class ComparisonOperatorTest {
    @Test
    public void testGetSignature() {
        NodeType type = NodeType.type("ComparisonOperatorTest");
        Fn f = new ComparisonOperator(type, true) {
            @Override
            protected boolean evaluate(int diff) {
                throw new UnsupportedOperationException();
            }
        };
        Signature signature = f.sig();
        assertSame(booleanType(), signature.returnType());
        assertEquals(2, signature.size());
        assertSame(type, signature.argType(0));
        assertSame(type, signature.argType(1));
    }
}
