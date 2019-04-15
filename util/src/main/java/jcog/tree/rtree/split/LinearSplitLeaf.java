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
public class LinearSplitLeaf<X> implements Split<X> {

    @Override
    public Node<X> split(X x, Leaf<X> leaf, Spatialization<X> m) {



        final int MIN = 0;
        final int MAX = 1;
        final int NRANGE = 2;
        X[] data = leaf.data;
        final int nD = m.bounds(data[0]).dim();
        final int[][][] r = new int[nD][NRANGE][NRANGE];

        final double[] separation = new double[nD];

        short size = leaf.size;
        for (int d = 0; d < nD; d++) {
            final int[][] rd = r[d];
            int[] iiMin = rd[MIN];
            double daa = m.bounds(data[iiMin[MIN]]).coord(d, false);
            double dba = m.bounds(data[iiMin[MAX]]).coord(d, false);
            int[] iiMax = rd[MAX];
            double dab = m.bounds(data[iiMax[MIN]]).coord(d, true);
            double dbb = m.bounds(data[iiMax[MAX]]).coord(d, true);

            for (int j = 1; j < size; j++) {

                HyperRegion rj = m.bounds(data[j]);

                double rjMin = rj.coord(d, false);
                if (daa > rjMin) iiMin[MIN] = j;
                if (dba < rjMin) iiMin[MAX] = j;

                double rjMax = rj.coord(d, true);
                if (dab > rjMax) iiMax[MIN] = j;
                if (dbb < rjMax) iiMax[MAX] = j;
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

        int r1Max = -1, r2Max = -1;
        double sepMax = Double.NEGATIVE_INFINITY;
        for (int d = 0; d < nD; d++) {
            if (sepMax < separation[d]) {
                sepMax = separation[d];
                r1Max = r[d][MAX][MIN];
                r2Max = r[d][MIN][MAX];
            }
        }

        if (r1Max == r2Max) {
            r1Max = 0;
            r2Max = size - 1;
        }

        return newBranch(x, leaf, m, size, r1Max, r2Max, data);

    }


}
