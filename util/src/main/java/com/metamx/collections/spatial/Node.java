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
import com.google.common.collect.Lists;
import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;
import com.metamx.collections.bitmap.BitmapFactory;
import com.metamx.collections.bitmap.MutableBitmap;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

/**
 */
public class Node {
    public final float[] min;
    public final float[] max;

    public final List<Node> children;
    public final boolean isLeaf;
    public final MutableBitmap bmp;

    private Node parent;

    public Node(float[] min, float[] max, boolean isLeaf, BitmapFactory bitmapFactory) {
        this(
                min,
                max,
                Lists.newArrayList(),
                isLeaf,
                null,
                bitmapFactory.makeEmptyMutableBitmap()
        );
    }

    public Node(
            float[] min,
            float[] max,
            List<Node> children,
            boolean isLeaf,
            Node parent,
            MutableBitmap bmp
    ) {
        Preconditions.checkArgument(min.length == max.length);

        this.min = min;
        this.max = max;
        this.children = children;
        for (Node child : children) {
            child.setParent(this);
        }
        this.isLeaf = isLeaf;
        this.bmp = bmp;
        this.parent = parent;
    }

    public int dim() {
        return min.length;
    }

    public Node parent() {
        return parent;
    }

    private Node setParent(Node p) {
        parent = p;
        return this;
    }

    public void addChild(Node node) {
        children.add(node.setParent(this));
    }

    public double area() {
        return calculateArea();
    }

    public boolean contains(Node other) {
        Preconditions.checkArgument(dim() == other.dim());

        for (int i = 0; i < dim(); i++) {
            if (other.min[i] < min[i] || other.max[i] > max[i]) {
                return false;
            }
        }
        return true;
    }

    public boolean contains(float[] coords) {
        Preconditions.checkArgument(dim() == coords.length);

        for (int i = 0; i < dim(); i++) {
            if (coords[i] < min[i] || coords[i] > max[i]) {
                return false;
            }
        }
        return true;
    }

    public boolean enclose() {
        int d = dim();
        float[] minCoords = new float[d];
        Arrays.fill(minCoords, Float.POSITIVE_INFINITY);
        float[] maxCoords = new float[d];
        Arrays.fill(maxCoords, Float.NEGATIVE_INFINITY);

        for (int i1 = 0, childrenSize = children.size(); i1 < childrenSize; i1++) {
            Node child = children.get(i1);
            for (int i = 0; i < d; i++) {
                minCoords[i] = Math.min(child.min[i], minCoords[i]);
                maxCoords[i] = Math.max(child.max[i], maxCoords[i]);
            }
        }

        boolean retVal = false;
        if (!Arrays.equals(minCoords, min)) {
            System.arraycopy(minCoords, 0, min, 0, min.length);
            retVal = true;
        }
        if (!Arrays.equals(maxCoords, max)) {
            System.arraycopy(maxCoords, 0, max, 0, max.length);
            retVal = true;
        }

        return retVal;
    }

    public void addToBitmapIndex(Node node) {
        bmp.or(node.bmp);
    }

    public void clear() {
        children.clear();
        bmp.clear();
    }

    public int byteSize() {
        return ImmutableNode.HEADER_NUM_BYTES
                + 2 * dim() * Floats.BYTES
                + Ints.BYTES 
                + bmp.getSizeInBytes()
                + children.size() * Ints.BYTES;
    }

    int storeInByteBuffer(ByteBuffer buffer, int position) {
        buffer.position(position);
        buffer.putShort((short) (((isLeaf ? 0x1 : 0x0) << 15) | children.size()));
        for (float v : min) {
            buffer.putFloat(v);
        }
        for (float v : max) {
            buffer.putFloat(v);
        }
        byte[] bytes = bmp.toBytes();
        buffer.putInt(bytes.length);
        buffer.put(bytes);

        int pos = buffer.position();
        int childStartOffset = pos + children.size() * Ints.BYTES;
        for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
            buffer.putInt(pos, childStartOffset);
            childStartOffset = children.get(i).storeInByteBuffer(buffer, childStartOffset);
            pos += Ints.BYTES;
        }

        return childStartOffset;
    }

    private double calculateArea() {
        double area = 1.0;
        for (int i = 0; i < min.length; i++) {
            area *= (max[i] - min[i]);
        }
        return area;
    }

    /** returns the node which contained the entry, or null if not found */
    public Node remove(float[] coord, int entry) {
        if (contains(coord)) {

            if (bmp.remove(entry)) {
                for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
                    Node r = children.get(i);
                    if (r instanceof Point) {
                        if (((Point)r).bitmap.get(entry)) {
                            children.remove(i);
                            childrenSize--;
                            
                        }
                    }
                }
                return this;
            }

            if (children!=null) {
                for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
                    Node r = children.get(i).remove(coord, entry);
                    if (r != null)
                        return r;
                }
            }
        }
        return null;
    }
}
