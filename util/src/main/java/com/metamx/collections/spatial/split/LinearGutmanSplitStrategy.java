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

import com.metamx.collections.bitmap.BitmapFactory;
import com.metamx.collections.spatial.Node;

import java.util.List;

/**
 */
public class LinearGutmanSplitStrategy extends GutmanSplitStrategy {
    public LinearGutmanSplitStrategy(int minNumChildren, int maxNumChildren, BitmapFactory bf) {
        super(minNumChildren, maxNumChildren, bf);
    }

    /**
     * This algorithm is from the original paper.
     * <p>
     * Algorithm LinearPickSeeds. Select two entries to be the first elements of the groups.
     * <p>
     * LPS1. [Find extreme rectangles along all dimensions]. Along each dimension, find the entry whose rectangle has
     * the highest low side, and the one with the lowest high side. Record the separation.
     * <p>
     * LPS2. [Adjust for shape of the rectangle cluster]. Normalize the separations by dividing by the width of the
     * entire set along the corresponding dimension.
     * <p>
     * LPS3. [Select the most extreme pair]. Choose the pair with the greatest normalized separation along any dimension.
     *
     * @param nodes - nodes to choose from
     * @return - two groups representing the seeds
     */
    @Override
    public Node[] pickSeeds(List<Node> nodes) {
        int[] optimalIndices = new int[2];
        int numDims = nodes.get(0).dim();

        double bestNormalized = 0.0;
        for (int i = 0; i < numDims; i++) {
            float minCoord = Float.POSITIVE_INFINITY;
            float maxCoord = Float.NEGATIVE_INFINITY;
            float highestLowSide = Float.NEGATIVE_INFINITY;
            float lowestHighside = Float.POSITIVE_INFINITY;
            int highestLowSideIndex = 0;
            int lowestHighSideIndex = 0;

            int counter = 0;
            for (Node node : nodes) {
                minCoord = Math.min(minCoord, node.min[i]);
                maxCoord = Math.max(maxCoord, node.max[i]);

                if (node.min[i] > highestLowSide) {
                    highestLowSide = node.min[i];
                    highestLowSideIndex = counter;
                }
                if (node.max[i] < lowestHighside) {
                    lowestHighside = node.max[i];
                    lowestHighSideIndex = counter;
                }

                counter++;
            }
            double normalizedSeparation = (highestLowSideIndex == lowestHighSideIndex) ? -1.0 :
                    Math.abs((highestLowSide - lowestHighside) / (maxCoord - minCoord));
            if (normalizedSeparation > bestNormalized) {
                optimalIndices[0] = highestLowSideIndex;
                optimalIndices[1] = lowestHighSideIndex;
                bestNormalized = normalizedSeparation;
            }
        }

        
        if (bestNormalized == 0) {
            optimalIndices[0] = 0;
            optimalIndices[1] = 1;
        }

        int indexToRemove1 = Math.min(optimalIndices[0], optimalIndices[1]);
        int indexToRemove2 = Math.max(optimalIndices[0], optimalIndices[1]);
        return new Node[]{nodes.remove(indexToRemove1), nodes.remove(indexToRemove2 - 1)};
    }

    /**
     * This algorithm is from the original paper.
     * <p>
     * Algorithm LinearPickNext. PickNext simply choose any of the remaining entries.
     *
     * @param nodes  - remaining nodes
     * @param groups - the left and right groups
     * @return - the optimal selected node
     */
    @Override
    public Node pickNext(List<Node> nodes, Node[] groups) {
        return nodes.remove(0);
    }
}
