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

package com.metamx.collections.spatial.split;

import com.google.common.collect.Lists;
import com.metamx.collections.bitmap.BitmapFactory;
import com.metamx.collections.spatial.Node;
import com.metamx.collections.spatial.RTreeUtils;

import java.util.Arrays;
import java.util.List;

/**
 */
public abstract class GutmanSplitStrategy implements SplitStrategy {
    private final int minNumChildren;
    private final int maxNumChildren;
    private final BitmapFactory bf;

    protected GutmanSplitStrategy(int minNumChildren, int maxNumChildren, BitmapFactory b) {
        this.minNumChildren = minNumChildren;
        this.maxNumChildren = maxNumChildren;
        this.bf = b;
    }

    @Override
    public boolean needToSplit(Node node) {
        return (node.children.size() > maxNumChildren);
    }

    /**
     * This algorithm is from the original paper.
     * <p>
     * Algorithm Split. Divide a set of M+1 index entries into two groups.
     * <p>
     * S1. [Pick first entry for each group]. Apply Algorithm {@link #pickSeeds(java.util.List)} to choose
     * two entries to be the first elements of the groups. Assign each to a group.
     * <p>
     * S2. [Check if done]. If all entries have been assigned, stop. If one group has so few entries that all the rest
     * must be assigned to it in order for it to have the minimum number m, assign them and stop.
     * <p>
     * S3. [Select entry to assign]. Invoke Algorithm {@link #pickNext(java.util.List, com.metamx.collections.spatial.Node[])}
     * to choose the next entry to assign. Add it to the group whose covering rectangle will have to be enlarged least to
     * accommodate it. Resolve ties by adding the entry to the group smaller area, then to the one with fewer entries, then
     * to either. Repeat from S2.
     */
    @Override
    public Node[] split(Node node) {
        List<Node> children = Lists.newArrayList(node.children);
        Node[] seeds = pickSeeds(children);

        node.clear();
        node.addChild(seeds[0]);
        node.addToBitmapIndex(seeds[0]);

        Node group1 = new Node(
                Arrays.copyOf(seeds[1].min, seeds[1].min.length),
                Arrays.copyOf(seeds[1].max, seeds[1].max.length),
                Lists.newArrayList(seeds[1]),
                node.isLeaf,
                node.parent(),
                bf.makeEmptyMutableBitmap()
        );
        group1.addToBitmapIndex(seeds[1]);
        if (node.parent() != null) {
            node.parent().addChild(group1);
        }
        Node[] groups = {
                node, group1
        };

        RTreeUtils.enclose(groups);

        while (!children.isEmpty()) {
            for (Node group : groups) {
                if (group.children.size() + children.size() <= minNumChildren) {
                    for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
                        Node child = children.get(i);
                        group.addToBitmapIndex(child);
                        group.addChild(child);
                    }
                    RTreeUtils.enclose(groups);
                    return groups;
                }
            }

            Node nextToAssign = pickNext(children, groups);
            double group0ExpandedArea = RTreeUtils.getEnclosingArea(groups[0], nextToAssign);
            double group1ExpandedArea = RTreeUtils.getEnclosingArea(groups[1], nextToAssign);

            Node optimal;
            if (group0ExpandedArea < group1ExpandedArea) {
                optimal = groups[0];
            } else if (group0ExpandedArea == group1ExpandedArea) {
                optimal = groups[0].area() < groups[1].area() ? groups[0] : groups[1];
            } else {
                optimal = groups[1];
            }

            optimal.addToBitmapIndex(nextToAssign);
            optimal.addChild(nextToAssign);
            optimal.enclose();
        }

        return groups;
    }

    public abstract Node[] pickSeeds(List<Node> nodes);

    public abstract Node pickNext(List<Node> nodes, Node[] groups);
}
