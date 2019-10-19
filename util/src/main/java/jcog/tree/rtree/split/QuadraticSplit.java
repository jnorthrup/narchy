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

import jcog.Util;
import jcog.tree.rtree.*;

import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Guttmann's Quadratic split
 * <p>
 * Created by jcairns on 5/5/15.
 */
public class QuadraticSplit<X> implements Split<X> {

    /**  find the two bounds that are most wasteful */
    @Override public RNode<X> apply(X x, RLeaf<X> leaf, Spatialization<X> m) {

        var maxWaste = Double.NEGATIVE_INFINITY;
        var size = leaf.size;
        int r1 = -1, r2 = -1;
        var data = leaf.data;
        var COST = Util.map(i -> m.bounds(data[i]).cost(), new double[size]); //cache
        for (var i = 0; i < size-1; i++) {
            var ii = m.bounds(data[i]);
            var iic = COST[i];
            var iiMbr =   ii.mbrBuilder();
            for (var j = i + 1; j < size; j++) {
                var jj = m.bounds(data[j]);
                var ij = iiMbr.apply(jj);
                var jjc = COST[j];
                var ijc = (ij==ii ? iic : (ij ==jj ? jjc : ij.cost())); //assert(ijc >= iic && ijc >= iic);
                var waste = (ijc - iic) + (ijc - jjc);
                if (waste > maxWaste) {
                    r1 = i;
                    r2 = j;
                    maxWaste = waste;
                }
            }
        }


        return newBranch(x, leaf, m, size, r1, r2, data);
    }



}
