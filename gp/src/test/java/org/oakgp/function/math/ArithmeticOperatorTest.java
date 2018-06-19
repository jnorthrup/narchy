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

import org.junit.jupiter.api.Test;
import org.oakgp.Assignments;
import org.oakgp.NodeType;
import org.oakgp.function.Fn;
import org.oakgp.node.Node;
import org.oakgp.util.Signature;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.oakgp.NodeType.integerType;
import static org.oakgp.TestUtils.readNode;
import static org.oakgp.function.math.ArithmeticExpressionSimplifier.assertEvaluateToSameResult;
import static org.oakgp.util.NodeSimplifier.simplify;

public class ArithmeticOperatorTest {
    private static final File TEST_DATA_FILE;
    static {
        File f;
        try {
            f = new File(ArithmeticOperator.class.getClassLoader().getResource("ArithmeticOperatorTest.txt").toURI());
        } catch (URISyntaxException e) {
            f = null;
            e.printStackTrace();
        }
        TEST_DATA_FILE = f;
    }

    private static final int TEST_DATA_SIZE = 10000;

    @Test
    public void testGetSignature() {
        Fn f = new ArithmeticOperator(NodeType.integerType()) {
            @Override
            protected Object evaluate(Node arg1, Node arg2, Assignments assignments) {
                throw new UnsupportedOperationException();
            }
        };
        Signature signature = f.sig();
        assertSame(integerType(), signature.returnType());
        assertEquals(2, signature.size());
        assertSame(integerType(), signature.argType(0));
        assertSame(integerType(), signature.argType(1));
    }

    @Test
    public void testSimplification() throws IOException {
        List<String> tests = getTestData();
        assertEquals(TEST_DATA_SIZE, tests.size());
        long inputNodeCtr = 0;
        long outputNodeCtr = 0;
        long start = System.currentTimeMillis();
        for (String test : tests) {
            Node input = readNode(test);
            Node output = simplify(input);
            assertEvaluateToSameResult(input, output);
            inputNodeCtr += input.size();
            outputNodeCtr += output.size();
        }
        long end = System.currentTimeMillis();
        System.out.println("from " + inputNodeCtr + " to " + outputNodeCtr + " in " + (end - start) + "ms");
    }

    private List<String> getTestData() throws IOException {
        if (!TEST_DATA_FILE.exists()) {
            
            throw new RuntimeException("Not found: " + TEST_DATA_FILE.getAbsolutePath());
        }
        return Files.readAllLines(TEST_DATA_FILE.toPath());
    }

    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
}
