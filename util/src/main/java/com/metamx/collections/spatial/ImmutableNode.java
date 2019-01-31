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

import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;
import com.metamx.collections.bitmap.BitmapFactory;
import com.metamx.collections.bitmap.ImmutableBitmap;

import java.nio.ByteBuffer;
import java.util.Iterator;

/**
 * Byte layout:
 * Header
 * 0 to 1 : the MSB is a boolean flag for isLeaf, the next 15 bits represent the number of children of a node
 * Body
 * 2 to 2 + numDims * Floats.BYTES : minCoordinates
 * 2 + numDims * Floats.BYTES to 2 + 2 * numDims * Floats.BYTES : maxCoordinates
 * concise setAt
 * rest (children) : Every 4 bytes is storing an offset representing the position of a child.
 * <p>
 * The child offset is an offset from the initialOffset
 */
public class ImmutableNode {
    public static final int HEADER_NUM_BYTES = 2;

    public final int dim;
    public final int initialOffset;
    public final int offsetFromInitial;

    public final short size;
    public final boolean isLeaf;
    public final int childrenOffset;

    public final ByteBuffer data;

    public final BitmapFactory bmp;

    public ImmutableNode(
            int dim,
            int initialOffset,
            int offsetFromInitial,
            ByteBuffer data,
            BitmapFactory bitmapFactory
    ) {
        this.bmp = bitmapFactory;
        this.dim = dim;
        this.initialOffset = initialOffset;
        this.offsetFromInitial = offsetFromInitial;
        short header = data.getShort(initialOffset + offsetFromInitial);
        this.isLeaf = (header & 0x8000) != 0;
        this.size = (short) (header & 0x7FFF);
        final int sizePosition = initialOffset + offsetFromInitial + HEADER_NUM_BYTES + 2 * dim * Floats.BYTES;
        int bitmapSize = data.getInt(sizePosition);
        this.childrenOffset = initialOffset
                + offsetFromInitial
                + HEADER_NUM_BYTES
                + 2 * dim * Floats.BYTES
                + Ints.BYTES
                + bitmapSize;

        this.data = data;
    }

    public ImmutableNode(
            int dim,
            int initialOffset,
            int offsetFromInitial,
            short size,
            boolean leaf,
            ByteBuffer data,
            BitmapFactory bitmapFactory
    ) {
        this.bmp = bitmapFactory;
        this.dim = dim;
        this.initialOffset = initialOffset;
        this.offsetFromInitial = offsetFromInitial;
        this.size = size;
        this.isLeaf = leaf;
        final int sizePosition = initialOffset + offsetFromInitial + HEADER_NUM_BYTES + 2 * dim * Floats.BYTES;
        int bitmapSize = data.getInt(sizePosition);
        this.childrenOffset = initialOffset
                + offsetFromInitial
                + HEADER_NUM_BYTES
                + 2 * dim * Floats.BYTES
                + Ints.BYTES
                + bitmapSize;

        this.data = data;
    }

    public float[] min() {
        return coord(initialOffset + offsetFromInitial + HEADER_NUM_BYTES);
    }

    public float[] max() {
        return coord(initialOffset + offsetFromInitial + HEADER_NUM_BYTES + dim * Floats.BYTES);
    }

    public ImmutableBitmap getImmutableBitmap() {
        final int sizePosition = initialOffset + offsetFromInitial + HEADER_NUM_BYTES + 2 * dim * Floats.BYTES;
        int numBytes = data.getInt(sizePosition);
        data.position(sizePosition + Ints.BYTES);
        ByteBuffer tmpBuffer = data.slice();
        tmpBuffer.limit(numBytes);
        return bmp.mapImmutableBitmap(tmpBuffer.asReadOnlyBuffer());
    }

    public Iterable<ImmutableNode> children() {
        return () -> new Iterator<ImmutableNode>() {
            private volatile int count;

            @Override
            public boolean hasNext() {
                return (count < size);
            }

            @Override
            public ImmutableNode next() {
                if (isLeaf) {
                    return new ImmutablePoint(
                            dim,
                            initialOffset,
                            data.getInt(childrenOffset + (count++) * Ints.BYTES),
                            data,
                            bmp
                    );
                }
                return new ImmutableNode(
                        dim,
                        initialOffset,
                        data.getInt(childrenOffset + (count++) * Ints.BYTES),
                        data,
                        bmp
                );
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    private float[] coord(int offset) {

        final ByteBuffer readOnlyBuffer = data.asReadOnlyBuffer();
        readOnlyBuffer.position(offset);
        final float[] retVal = new float[dim];
        readOnlyBuffer.asFloatBuffer().get(retVal);

        return retVal;
    }
}
