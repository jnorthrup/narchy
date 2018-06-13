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
package org.oakgp.util;

import org.oakgp.NodeType;
import org.oakgp.node.Node;

public abstract class DummyNode implements Node {

    @Override
    public int size() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int depth() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeType returnType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public org.oakgp.node.NodeType nodeType() {
        throw new UnsupportedOperationException();
    }
}
