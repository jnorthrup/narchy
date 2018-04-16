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
package org.oakgp;

import java.util.Arrays;

/**
 * Represents values assigned to variables.
 * <p>
 * Immutable.
 */
public final class Assignments {
    private final Object[] assignments;
    private final int hashCode;

    /**
     * @see #createAssignments(Node...)
     */
    public Assignments(Object... assignments) {
        this.assignments = assignments.clone();
        this.hashCode = Arrays.hashCode(assignments);
    }

    /**
     * Returns the value at the specified position in this {@code Assignments}.
     *
     * @param index index of the element to return
     * @return the value at the specified position in this {@code Assignments}
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public Object get(int index) {
        return assignments[index];
    }

    @Override
    public final int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Assignments && hashCode == o.hashCode() && Arrays.equals(this.assignments, ((Assignments) o).assignments));
    }

    @Override
    public String toString() {
        return Arrays.toString(assignments);
    }
}
