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

import org.oakgp.function.Function;
import org.oakgp.node.Node;

import java.util.Arrays;
import java.util.List;

/**
 * Represents the arguments of a function.
 * <p>
 * Immutable.
 */
public class Arguments {
    private final Node[] args;
    private final int hashCode;
    public final int depth;

    /**
     * @see #createArguments(Node...)
     */
    public Arguments(Node... args) {
        this.args = args;
        this.hashCode = Arrays.hashCode(args);
        this.depth = calcDepth(args);
    }

    public static Arguments get(Function f, Node... aa) {
        if (aa.length > 1 && f.argsSorted()) {
            aa = aa.clone();
            Arrays.sort(aa);
            return new SortedArguments(aa);
        } else {
            return new Arguments(aa);
        }
    }

    public static Arguments get(Function f, List<Node> a) {
        if (f.argsSorted() && a.size() > 1) {
            Node[] aa = a.toArray(Node.EmptyArray);
            Arrays.sort(aa);
            return new SortedArguments(aa);
        } else {
            return new Arguments(a);
        }

    }

    public Arguments sorted() {
        if (length() < 2)
            return this;

        Node[] a = args.clone();
        Arrays.sort(a);
        if (Arrays.equals(args, a))
            return new SortedArguments(args); 
        else
            return new SortedArguments(a);
    }

    public static class SortedArguments extends Arguments {

        SortedArguments(Node[] knownToBeSorted) {
            super(knownToBeSorted);
        }

        @Override
        public Arguments sorted() {
            return this;
        }
    }

    public Arguments(List<? extends Node> args) {
        this(args.toArray(new Node[0]));
    }

    static int calcDepth(Node[] arguments) {
        int height = 0;
        int n = arguments.length;
        for (Node argument : arguments) {
            height = Math.max(height, argument.depth());
        }
        return height + 1;
    }


    /**
     * Returns the {@code Node} at the specified position in this {@code Arguments}.
     *
     * @param arg index of the element to return
     * @return the {@code Node} at the specified position in this {@code Arguments}
     * @throws ArrayIndexOutOfBoundsException if the index is out of range (<tt>index &lt; 0 || index &gt;= getArgCount()</tt>)
     */
    public Node get(int arg) {
        return args[arg];
    }

    /**
     * Returns the first argument in this {@code Arguments}.
     */
    public Node firstArg() {
        return args[0];
    }

    /**
     * Returns the second argument in this {@code Arguments}.
     */
    public Node secondArg() {
        return args[1];
    }

    /**
     * Returns the third argument in this {@code Arguments}.
     */
    public Node thirdArg() {
        return args[2];
    }

    /**
     * Returns the number of elements in this {@code Arguments}.
     *
     * @return the number of elements in this {@code Arguments}
     */
    public final int length() {
        return args.length;
    }

    /**
     * Returns a new {@code Arguments} resulting from replacing the existing {@code Node} at position {@code index} with {@code replacement}.
     *
     * @param index       the index of the {@code Node} that needs to be replaced.
     * @param replacement the new {@code Node} that needs to be store.
     * @return A new {@code Arguments} derived from this {@code Arguments} by replacing the element at position {@code index} with {@code replacement}.
     */
    public Arguments replaceAt(int index, Node replacement) {
        if (args[index].equals(replacement))
            return this;
        Node[] clone = args.clone();
        clone[index] = replacement;
        return new Arguments(clone);
    }

    /**
     * Returns a new {@code Arguments} resulting from switching the node located at index {@code index1} with the node located at index {@code index2}.
     *
     * @param index1 the index in this {@code Arguments} of the first {@code Node} to be swapped.
     * @param index2 the index in this {@code Arguments} of the second {@code Node} to be swapped.
     * @return A new {@code Arguments} resulting from switching the node located at index {@code index1} with the node located at index {@code index2}.
     */
    public Arguments swap(int index1, int index2) {
        if (index1==index2)
            return this;
        Node[] clone = args.clone();
        clone[index1] = args[index2];
        clone[index2] = args[index1];
        return new Arguments(clone);
    }

    @Override
    public final int hashCode() {
        return hashCode;
    }

    @Override
    public final boolean equals(Object o) {
        return this == o  ||
                (hashCode == o.hashCode() &&
                (o instanceof Arguments && Arrays.equals(this.args, ((Arguments) o).args)));
    }

    @Override
    public String toString() {
        return Arrays.toString(args);
    }


}
