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

import org.oakgp.NodeType;
import org.oakgp.function.Fn;
import org.oakgp.node.Node;

public class DummyPrimitiveSet implements PrimitiveSet {
    @Override
    public boolean hasTerminals(NodeType type) {
        return true;
    }

    @Override
    public boolean hasFunctions(NodeType type) {
        return true;
    }

    @Override
    public Node nextTerminal(NodeType type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Node nextTerminal(Node current) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Fn next(NodeType type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Fn next(Fn current) {
        throw new UnsupportedOperationException();
    }
}
