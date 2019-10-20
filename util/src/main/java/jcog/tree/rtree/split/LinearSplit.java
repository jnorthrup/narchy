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
public class LinearSplit<X> implements Split<X> {

    public static final LinearSplit the = new LinearSplit();

    public LinearSplit() {

    }
    @Override
    public RNode<X> apply(X x, RLeaf<X> leaf, Spatialization<X> m) {



        final var MIN = 0;
        final var MAX = 1;
        final var NRANGE = 2;
        var data = leaf.data;
        var nD = m.bounds(data[0]).dim();
        var r = new int[nD][NRANGE][NRANGE];

        var separation = new double[nD];

        var size = leaf.size;
        for (var d = 0; d < nD; d++) {
            var rd = r[d];
            var iiMin = rd[MIN];
            var daa = m.bounds(data[iiMin[MIN]]).coord(d, false);
            var dba = m.bounds(data[iiMin[MAX]]).coord(d, false);
            var iiMax = rd[MAX];
            var dab = m.bounds(data[iiMax[MIN]]).coord(d, true);
            var dbb = m.bounds(data[iiMax[MAX]]).coord(d, true);

            for (var j = 1; j < size; j++) {

                var rj = m.bounds(data[j]);

                var rjMin = rj.coord(d, false);
                if (daa > rjMin) iiMin[MIN] = j;
                if (dba < rjMin) iiMin[MAX] = j;

                var rjMax = rj.coord(d, true);
                if (dab > rjMax) iiMax[MIN] = j;
                if (dbb < rjMax) iiMax[MAX] = j;
            }


            var width = Math.abs(
                    m.bounds(data[rd[MAX][MAX]]).coord(d, true) -
                    m.bounds(data[rd[MIN][MIN]]).coord(d, false)
            );

            separation[d] = Math.abs(
                    m.bounds(data[rd[MAX][MIN]]).coord(d, true) -
                    m.bounds(data[rd[MIN][MAX]]).coord(d, false)
            ) / width;
        }

        int r1Max = -1, r2Max = -1;
        var sepMax = Double.NEGATIVE_INFINITY;
        for (var d = 0; d < nD; d++) {
            if (separation[d] > sepMax) {
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
