package jcog.tree.rtree.split;

/*
 * #%L
 * Conversant RTree
 * ~~
 * Conversantmedia.com © 2016, Conversant, Inc. Conversant® is a trademark of Conversant, Inc.
 * ~~
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import jcog.tree.rtree.*;

/**
 * Guttmann's Linear split
 * <p>
 * Created by jcairns on 5/5/15.
 */
public final class LinearSplitLeaf<T> implements Split<T> {

    @Override
    public Node<T> split(T t, Leaf<T> leaf, Spatialization<T> model) {



        final int MIN = 0;
        final int MAX = 1;
        final int NRANGE = 2;
        T[] data = leaf.data;
        final int nD = model.bounds(data[0]).dim();
        final int[][][] rIndex = new int[nD][NRANGE][NRANGE];

        final double[] separation = new double[nD];

        short size = leaf.size;
        for (int d = 0; d < nD; d++) {


            for (int j = 1; j < size; j++) {
                int[][] rd = rIndex[d];

                HyperRegion rj = model.bounds(data[j]);
                double rjMin = rj.coord(d, false);
                if (model.bounds(data[rd[MIN][MIN]]).coord(d, false) > rjMin) {
                    rd[MIN][MIN] = j;
                }

                if (model.bounds(data[rd[MIN][MAX]]).coord(d, false) < rjMin) {
                    rd[MIN][MAX] = j;
                }

                double rjMax = rj.coord(d, true);
                if (model.bounds(data[rd[MAX][MIN]]).coord(d, true) > rjMax) {
                    rd[MAX][MIN] = j;
                }

                if (model.bounds(data[rd[MAX][MAX]]).coord(d, true) < rjMax) {
                    rd[MAX][MAX] = j;
                }
            }


            final double width = model.bounds(data[rIndex[d][MAX][MAX]]).
                    distance(model.bounds(data[rIndex[d][MIN][MIN]]), d, true, false);


            separation[d] = model.bounds(data[rIndex[d][MAX][MIN]]).distance(model.bounds(data[rIndex[d][MIN][MAX]]), d, true, false) / width;
        }

        int r1Ext = rIndex[0][MAX][MIN], r2Ext = rIndex[0][MIN][MAX];
        double highSep = separation[0];
        for (int d = 1; d < nD; d++) {
            if (highSep < separation[d]) {
                highSep = separation[d];
                r1Ext = rIndex[d][MAX][MIN];
                r2Ext = rIndex[d][MIN][MAX];
            }
        }

        if (r1Ext == r2Ext) {

            r1Ext = 0;
            r2Ext = size - 1;
        }

        final Leaf<T> l1Node = model.newLeaf();
        final Leaf<T> l2Node = model.newLeaf();

        boolean[] dummy = new boolean[1];
        l1Node.add(data[r1Ext], true, model, dummy);
        dummy[0] = false;
        l2Node.add(data[r2Ext], true, model, dummy);

        for (int i = 0; i < size; i++) {
            if ((i != r1Ext) && (i != r2Ext)) {
                leaf.transfer(l1Node, l2Node, data[i], model);
            }
        }

        leaf.transfer(l1Node, l2Node, t, model);

        return model.newBranch(l1Node, l2Node);
    }


}
