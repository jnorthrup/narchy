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
public class LinearSplitLeaf<T> implements Split<T> {

    @Override
    public Node<T> split(T t, Leaf<T> leaf, Spatialization<T> m) {



        final int MIN = 0;
        final int MAX = 1;
        final int NRANGE = 2;
        T[] data = leaf.data;
        final int nD = m.bounds(data[0]).dim();
        final int[][][] r = new int[nD][NRANGE][NRANGE];

        final double[] separation = new double[nD];

        short size = leaf.size;
        for (int d = 0; d < nD; d++) {
            int[][] rd = r[d];
            for (int j = 1; j < size; j++) {
                int[][] ii = rd;

                HyperRegion rj = m.bounds(data[j]);

                double rjMin = rj.coord(d, false);
                int[] iiMin = ii[MIN];
                if (m.bounds(data[iiMin[MIN]]).coord(d, false) > rjMin) iiMin[MIN] = j;
                if (m.bounds(data[iiMin[MAX]]).coord(d, false) < rjMin) iiMin[MAX] = j;

                double rjMax = rj.coord(d, true);
                int[] iiMax = ii[MAX];
                if (m.bounds(data[iiMax[MIN]]).coord(d, true) > rjMax) iiMax[MIN] = j;
                if (m.bounds(data[iiMax[MAX]]).coord(d, true) < rjMax) iiMax[MAX] = j;
            }


            final double width = Math.abs(
                    m.bounds(data[rd[MAX][MAX]]).coord(d, true) -
                    m.bounds(data[rd[MIN][MIN]]).coord(d, false)
            );

            separation[d] = Math.abs(
                    m.bounds(data[rd[MAX][MIN]]).coord(d, true) -
                    m.bounds(data[rd[MIN][MAX]]).coord(d, false)
            ) / width;
        }

        int r1Ext = -1, r2Ext = -1;
        double sepMax = Double.NEGATIVE_INFINITY;
        for (int d = 0; d < nD; d++) {
            if (sepMax < separation[d]) {
                sepMax = separation[d];
                r1Ext = r[d][MAX][MIN];
                r2Ext = r[d][MIN][MAX];
            }
        }

        if (r1Ext == r2Ext) {
            r1Ext = 0;
            r2Ext = size - 1;
        }

        final Leaf<T> l1Node = m.newLeaf();
        boolean[] dummy = new boolean[1];
        l1Node.add(data[r1Ext], true, m, dummy);

        final Leaf<T> l2Node = m.newLeaf();
        dummy[0] = false;
        l2Node.add(data[r2Ext], true, m, dummy);

        for (int i = 0; i < size; i++) {
            if ((i != r1Ext) && (i != r2Ext))
                leaf.transfer(l1Node, l2Node, data[i], m);
        }

        leaf.transfer(l1Node, l2Node, t, m);

        return m.newBranch(l1Node, l2Node);
    }


}
