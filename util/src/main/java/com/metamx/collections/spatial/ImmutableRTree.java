/*
 * Copyright 2011 - 2015 Metamarkets Group Inc.
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

package com.metamx.collections.spatial;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import com.metamx.collections.bitmap.BitmapFactory;
import com.metamx.collections.bitmap.ImmutableBitmap;
import com.metamx.collections.spatial.search.Bound;
import com.metamx.collections.spatial.search.GutmanSearchStrategy;
import com.metamx.collections.spatial.search.SearchStrategy;
import jcog.util.ArrayUtils;

import java.nio.ByteBuffer;

/**
 * An immutable representation of an {@link RTree} for spatial indexing.
 */
public class ImmutableRTree {
    public  static final byte VERSION = 0x0;
    public  final int dim;
    public final ImmutableNode root;
    public final ByteBuffer data;
    public  final SearchStrategy defaultSearchStrategy = new GutmanSearchStrategy();

    static final ByteBuffer empty = ByteBuffer.wrap(ArrayUtils.EMPTY_BYTE_ARRAY);

    public ImmutableRTree() {
        this.dim = 0;
        this.data = empty;
        this.root = null;
    }
    public ImmutableRTree(ByteBuffer data, BitmapFactory bitmapFactory) {
        final int initPosition = data.position();
        Preconditions.checkArgument(data.get(0) == VERSION, "Mismatching versions");
        this.dim = data.getInt(1 + initPosition) & 0x7FFF;
        this.data = data;
        this.root = new ImmutableNode(dim, initPosition, 1 + Ints.BYTES, data, bitmapFactory);
    }

    final static ImmutableRTree EMPTY = new ImmutableRTree();

    public static ImmutableRTree toImmutable(RTree rTree) {
        if (rTree.size() == 0) {
            return EMPTY;
        }

        ByteBuffer buffer = ByteBuffer.wrap(new byte[calcNumBytes(rTree)]);

        buffer.put(VERSION);
        buffer.putInt(rTree.dim);
        rTree.root().storeInByteBuffer(buffer, buffer.position());
        buffer.position(0);
        return new ImmutableRTree(buffer.asReadOnlyBuffer(), rTree.bmp);
    }

    private static int calcNumBytes(RTree tree) {
        int total = 1 + Ints.BYTES; 

        total += calcNodeBytes(tree.root());

        return total;
    }

    private static int calcNodeBytes(Node node) {
        int total = 0;

        
        total += node.byteSize();

        
        for (Node child : node.children) {
            total += node.isLeaf ? child.byteSize() : calcNodeBytes(child);
        }

        return total;
    }

    public int size() {
        return data.capacity();
    }

    public Iterable<ImmutableBitmap> search(Bound bound) {
        return search(defaultSearchStrategy, bound);
    }

    public Iterable<ImmutableBitmap> search(SearchStrategy strategy, Bound bound) {
        return bound.getNumDims() == dim ? strategy.search(root, bound) : ImmutableList.of();
    }

    public byte[] toBytes() {
        ByteBuffer buf = ByteBuffer.allocate(data.capacity());
        buf.put(data.asReadOnlyBuffer());
        return buf.array();
    }

    public int compareTo(ImmutableRTree other) {
        return this.data.compareTo(other.data);
    }
}
