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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class FunctionsTest {
    /**
     * Confirms that each implementation of {@code Function} has exactly one corresponding {@code AbstractFunctionTest} instance.
     */
    @Disabled
    @Test
    public void test() throws Exception {
        List<Class<?>> functionClasses = SubClassFinder.find(Fn.class, "src/main/java");
        List<Class<?>> functionTestClasses = SubClassFinder.find(AbstractFnTest.class, "src/test/java");
        for (Class<?> functionTest : functionTestClasses) {
            AbstractFnTest t = (AbstractFnTest) functionTest.newInstance();
            Class<?> functionClass = t.getFunction().getClass();
            assertTrue(functionClasses.contains(functionClass), "Tested more than once: " + functionClass);
            functionClasses.remove(functionClass);
        }
        assertTrue(functionClasses.isEmpty(), "Not tested " + functionClasses.toString());
    }
}
