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


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;

/**
 */
public class RTreeUtils {
    private static final ObjectMapper jsonMapper = new ObjectMapper();

    public static double getEnclosingArea(Node a, Node b) {
        Preconditions.checkArgument(a.dim() == b.dim());

        double[] minCoords = new double[a.dim()];
        double[] maxCoords = new double[a.dim()];

        for (int i = 0; i < minCoords.length; i++) {
            minCoords[i] = Math.min(a.min[i], b.min[i]);
            maxCoords[i] = Math.max(a.max[i], b.max[i]);
        }

        double area = 1.0;
        for (int i = 0; i < minCoords.length; i++) {
            area *= (maxCoords[i] - minCoords[i]);
        }

        return area;
    }

    public static double getExpansionCost(Node node, Point point) {
        Preconditions.checkArgument(node.dim() == point.dim());

        if (node.contains(point.coords)) {
            return 0;
        }

        double expanded = 1.0;
        for (int i = 0; i < node.dim(); i++) {
            double min = Math.min(point.coords[i], node.min[i]);
            double max = Math.max(point.coords[i], node.min[i]);
            expanded *= (max - min);
        }

        return (expanded - node.area());
    }

    public static void enclose(Node[] nodes) {
        for (Node node : nodes) {
            node.enclose();
        }
    }

    public static Iterable<ImmutablePoint> getBitmaps(ImmutableRTree tree) {
        return depthFirstSearch(tree.root);
    }

    public static Iterable<ImmutablePoint> depthFirstSearch(ImmutableNode node) {
        return node.isLeaf ? Iterables.transform(
                node.children(),
                ImmutablePoint::new
        ) : Iterables.concat(
                Iterables.transform(
                        node.children(),
                        RTreeUtils::depthFirstSearch
                )
        );
    }

    public static void print(RTree tree) {
        System.out.printf("numDims : %d%n", tree.dim);
        try {
            printRTreeNode(tree.root(), 0);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public static void print(ImmutableRTree tree) {
        System.out.printf("numDims : %d%n", tree.dim);
        try {
            printNode(tree.root, 0);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public static void printRTreeNode(Node node, int level) throws Exception {
        System.out.printf(
                "%sminCoords: %s, maxCoords: %s, numChildren: %d, isLeaf:%s%n",
                makeDashes(level),
                jsonMapper.writeValueAsString(node.min),
                jsonMapper.writeValueAsString(
                        node.max
                ),
                node.children.size(),
                node.isLeaf
        );
        if (node.isLeaf) {
            for (Node child : node.children) {
                Point point = (Point) (child);
                System.out
                        .printf(
                                "%scoords: %s, conciseSet: %s%n",
                                makeDashes(level),
                                jsonMapper.writeValueAsString(point.coords),
                                point.bitmap
                        );
            }
        } else {
            level++;
            for (Node child : node.children) {
                printRTreeNode(child, level);
            }
        }
    }

    public static boolean verifyEnclose(Node node) {
        for (Node child : node.children) {
            for (int i = 0; i < node.dim(); i++) {
                if (child.min[i] < node.min[i]
                        || child.max[i] > node.max[i]) {
                    return false;
                }
            }
        }

        if (!node.isLeaf) {
            for (Node child : node.children) {
                if (!verifyEnclose(child)) {
                    return false;
                }
            }
        }

        return true;
    }

    public static boolean verifyEnclose(ImmutableNode node) {
        for (ImmutableNode child : node.children()) {
            for (int i = 0; i < node.dim; i++) {
                if (child.min()[i] < node.min()[i]
                        || child.max()[i] > node.max()[i]) {
                    return false;
                }
            }
        }

        if (!node.isLeaf) {
            for (ImmutableNode child : node.children()) {
                if (!verifyEnclose(child)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static void printNode(ImmutableNode node, int level) throws Exception {
        System.out.printf(
                "%sminCoords: %s, maxCoords: %s, numChildren: %d, isLeaf: %s%n",
                makeDashes(level),
                jsonMapper.writeValueAsString(node.min()),
                jsonMapper.writeValueAsString(
                        node.max()
                ),
                (int) node.size,
                node.isLeaf
        );
        if (node.isLeaf) {
            for (ImmutableNode immutableNode : node.children()) {
                ImmutablePoint point = new ImmutablePoint(immutableNode);
                System.out
                        .printf(
                                "%scoords: %s, conciseSet: %s%n",
                                makeDashes(level),
                                jsonMapper.writeValueAsString(point.coord()),
                                point.getImmutableBitmap()
                        );
            }
        } else {
            level++;
            for (ImmutableNode immutableNode : node.children()) {
                printNode(immutableNode, level);
            }
        }
    }

    private static String makeDashes(int level) {
        String retVal = "";
        for (int i = 0; i < level; i++) {
            retVal += "-";
        }
        return retVal;
    }
}
