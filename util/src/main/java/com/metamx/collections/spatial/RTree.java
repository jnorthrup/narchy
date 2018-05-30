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
import com.metamx.collections.bitmap.BitmapFactory;
import com.metamx.collections.bitmap.MutableBitmap;
import com.metamx.collections.bitmap.RoaringBitmapFactory;
import com.metamx.collections.spatial.split.LinearGutmanSplitStrategy;
import com.metamx.collections.spatial.split.SplitStrategy;

import java.util.Arrays;

/**
 * This RTree has been optimized to work with bitmap inverted indexes.
 * <p>
 * This code will probably make a lot more sense if you read:
 * http:
 */
public class RTree {
    public final int dim;
    public final SplitStrategy split;
    public final BitmapFactory bmp;
    private Node root;
    private int size;

    public RTree(BitmapFactory bmp) {
        this(0, new LinearGutmanSplitStrategy(0, 0, bmp), bmp);
    }

    public RTree(int dim) {
        this(dim,
            new LinearGutmanSplitStrategy(0, 4, RoaringBitmapFactory.DEFAULT),
            RoaringBitmapFactory.DEFAULT);
    }

    public RTree(int dim, SplitStrategy split, BitmapFactory bmp) {
        this.dim = dim;
        this.split = split;
        this.bmp = bmp;
        this.root = buildRoot(true);
    }

    /**
     * This description is from the original paper.
     * <p>
     * Algorithm Insert: Insert a new index entry E into an R-tree.
     * <p>
     * I1. [Find position for new record]. Invoke {@link #chooseLeaf(Node, Point)} to select
     * a leaf node L in which to place E.
     * <p>
     * I2. [Add records to leaf node]. If L has room for another entry, install E. Otherwise invoke
     * {@link SplitStrategy} split methods to obtain L and LL containing E and all the old entries of L.
     * <p>
     * I3. [Propagate changes upward]. Invoke {@link #adjust(Node, Node)} on L, also passing LL if a split was
     * performed.
     * <p>
     * I4. [Grow tree taller]. If node split propagation caused the root to split, create a new record whose
     * children are the two resulting nodes.
     *  @param coords - the coordinates of the entry
     * @param entry  - the integer to insert
     */
    public Point insert(float[] coords, int entry) {
        Preconditions.checkArgument(coords.length == dim);
        return insertInner(new Point(coords, entry, bmp));
    }

    public Point insert(float[] coords, MutableBitmap entry) {
        Preconditions.checkArgument(coords.length == dim);
        return insertInner(new Point(coords, entry));
    }


    public int size() {
        return size;
    }

    public Node root() {
        return root;
    }

    private Node buildRoot(boolean isLeaf) {
        float[] initMinCoords = new float[dim];
        Arrays.fill(initMinCoords, Float.NEGATIVE_INFINITY);
        float[] initMaxCoords = new float[dim];
        Arrays.fill(initMaxCoords, Float.POSITIVE_INFINITY);

        return new Node(initMinCoords, initMaxCoords, isLeaf, bmp);
    }

    private Point insertInner(Point point) {
        Node node = chooseLeaf(root, point);
        node.addChild(point);

        if (split.needToSplit(node)) {
            Node[] groups = split.split(node);
            adjust(groups[0], groups[1]);
        } else {
            adjust(node, null);
        }

        size++;
        return point;
    }


    /**
     * This description is from the original paper.
     * <p>
     * Algorithm ChooseLeaf. Select a leaf node in which to place a new index entry E.
     * <p>
     * CL1. [Initialize]. Set N to be the root node.
     * <p>
     * CL2. [Leaf check]. If N is a leaf, return N.
     * <p>
     * CL3. [Choose subtree]. If N is not a leaf, let F be the entry in N whose rectangle
     * FI needs least enlargement to include EI. Resolve ties by choosing the entry with the rectangle
     * of smallest area.
     * <p>
     * CL4. [Descend until a leaf is reached]. Set N to be the child node pointed to by Fp and repeated from CL2.
     *
     * @param node  - current node to evaluate
     * @param point - point to insert
     * @return - leafNode where point can be inserted
     */
    private static Node chooseLeaf(Node node, Point point) {
        while (true) {
            node.addToBitmapIndex(point);

            if (node.isLeaf) {
                return node;
            }

            double minCost = Double.MAX_VALUE;
            Node optimal = node.children.get(0);
            for (Node child : node.children) {
                double cost = RTreeUtils.getExpansionCost(child, point);
                if (cost < minCost) {
                    minCost = cost;
                    optimal = child;
                } else if (cost == minCost) {
                    
                    if (child.area() < optimal.area()) {
                        optimal = child;
                    }
                }
            }

            node = optimal;
        }
    }

    /**
     * This description is from the original paper.
     * <p>
     * AT1. [Initialize]. Set N=L. If L was split previously, set NN to be the resulting second node.
     * <p>
     * AT2. [Check if done]. If N is the root, stop.
     * <p>
     * AT3. [Adjust covering rectangle in parent entry]. Let P be the parent node of N, and let Ev(N)I be N's entry in P.
     * Adjust Ev(N)I so that it tightly encloses all entry rectangles in N.
     * <p>
     * AT4. [Propagate node split upward]. If N has a partner NN resulting from an earlier split, create a new entry
     * Ev(NN) with Ev(NN)p pointing to NN and Ev(NN)I enclosing all rectangles in NN. Add Ev(NN) to p is there is room.
     * Otherwise, invoke {@link SplitStrategy} split to product p and pp containing Ev(NN) and all p's old entries.
     *
     * @param n  - first node to adjust
     * @param nn - optional second node to adjust
     */
    private void adjust(Node n, Node nn) {
        
        if (n == root) {
            if (nn != null) {
                root = buildRoot(false);
                root.addChild(n);
                root.addChild(nn);
            }
            root.enclose();
            return;
        }

        boolean updateParent = n.enclose();

        if (nn != null) {
            nn.enclose();
            updateParent = true;

            if (split.needToSplit(n.parent())) {
                Node[] groups = split.split(n.parent());
                adjust(groups[0], groups[1]);
            }
        }

        if (n.parent() != null && updateParent) {
            adjust(n.parent(), null);
        }
    }

    public boolean remove(float[] coord, int entry) {
        Node contained = this.root().remove(coord, entry);
        if (contained!=null) {
            adjust(contained, null);
            size--;
            return true;
        }
        return false;
    }

    public void clear() {
        root.clear();
        size = 0;
    }
}
