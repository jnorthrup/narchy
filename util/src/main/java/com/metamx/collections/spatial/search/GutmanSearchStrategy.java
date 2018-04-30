/*
 * Copyright 2011 - 2015 Metamarkets Group Inc.
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

package com.metamx.collections.spatial.search;

import com.google.common.collect.Iterables;
import com.metamx.collections.bitmap.ImmutableBitmap;
import com.metamx.collections.spatial.ImmutableNode;
import com.metamx.collections.spatial.ImmutablePoint;

/**
 */
public class GutmanSearchStrategy implements SearchStrategy {
    @Override
    public Iterable<ImmutableBitmap> search(ImmutableNode node, Bound bound) {
        if (bound.getLimit() > 0) {
            return Iterables.transform(
                    breadthFirstSearch(node, bound),
                    ImmutableNode::getImmutableBitmap
            );
        }

        return Iterables.transform(
                depthFirstSearch(node, bound),
                ImmutableNode::getImmutableBitmap
        );
    }

    public static Iterable<ImmutablePoint> depthFirstSearch(ImmutableNode node, final Bound bound) {
        return node.isLeaf ? bound.filter(
                Iterables.transform(
                        node.children(),
                        ImmutablePoint::new
                )
        ) : Iterables.concat(
                Iterables.transform(
                        Iterables.filter(
                                node.children(),
                                bound::overlaps
                        ),
                        child -> depthFirstSearch(child, bound)
                )
        );
    }

    public static Iterable<ImmutableNode> breadthFirstSearch(
            ImmutableNode node,
            final Bound bound
    ) {
        if (node.isLeaf) {
            return Iterables.filter(
                    node.children(),
                    immutableNode -> bound.contains(immutableNode.min())
            );
        }
        return breadthFirstSearch(node.children(), bound, 0);
    }

    public static Iterable<ImmutableNode> breadthFirstSearch(
            Iterable<ImmutableNode> nodes,
            final Bound bound,
            int total
    ) {
        Iterable<ImmutableNode> points = Iterables.concat(
                Iterables.transform(
                        Iterables.filter(
                                nodes,
                                immutableNode -> immutableNode.isLeaf
                        ),
                        immutableNode -> Iterables.filter(
                                immutableNode.children(),
                                m -> bound.contains(m.min())
                        )
                )
        );

        Iterable<ImmutableNode> overlappingNodes = Iterables.filter(
                nodes,
                immutableNode -> !immutableNode.isLeaf && bound.overlaps(immutableNode)
        );

        int totalPoints = Iterables.size(points);
        int totalOverlap = Iterables.size(overlappingNodes);

        return totalOverlap == 0 || (totalPoints + totalOverlap + total) >= bound.getLimit() ? Iterables.concat(
                points,
                overlappingNodes
        ) : Iterables.concat(
                points,
                breadthFirstSearch(
                        Iterables.concat(
                                Iterables.transform(
                                        overlappingNodes,
                                        ImmutableNode::children
                                )
                        ),
                        bound,
                        totalPoints
                )
        );
    }
}
