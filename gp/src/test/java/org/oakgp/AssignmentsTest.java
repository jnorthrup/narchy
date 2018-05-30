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
package org.oakgp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AssignmentsTest {
    @Test
    public void test() {
        int x = 9;
        int y = 7;
        Object[] values = {x, y};
        Assignments assignments = new Assignments(values);
        assertEquals(x, assignments.get(0));
        assertEquals(y, assignments.get(1));

        
        values[0] = 10;
        assertEquals(x, assignments.get(0));
        assertEquals(y, assignments.get(1));

        assertArrayIndexOutOfBoundsException(assignments, -1);
        assertArrayIndexOutOfBoundsException(assignments, 2);
    }

    @Test
    public void testEqualsAndHashCode() {
        Assignments a1 = new Assignments("hello", true, 42);
        Assignments a2 = new Assignments("hello", true, 42);
        assertEquals(a1, a1);
        assertEquals(a1.hashCode(), a2.hashCode());
        assertEquals(a1, a2);
    }

    @Test
    public void testNotEquals() {
        Assignments a = new Assignments("hello", true, 42);

        
        assertNotEquals(a, new Assignments(42, true, "hello"));

        
        assertNotEquals(a, new Assignments("hello", true, 43));

        
        assertNotEquals(a, new Assignments("hello", true));

        
        assertNotEquals(a, new Assignments("hello", true, 42, 42));
    }

    @Test
    public void testToString() {
        Assignments assignments = new Assignments("hello", true, 42);
        assertEquals("[hello, true, 42]", assignments.toString());
    }

    private void assertArrayIndexOutOfBoundsException(Assignments assignments, int index) {
        try {
            assignments.get(index);
            fail("");
        } catch (ArrayIndexOutOfBoundsException e) {
            
        }
    }
}
