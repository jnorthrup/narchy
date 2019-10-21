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
import java.util.function.IntToDoubleFunction;

/**
 * Guttmann's Quadratic split
 * <p>
 * Created by jcairns on 5/5/15.
 */
public class QuadraticSplit<X> implements Split<X> {

    /**  find the two bounds that are most wasteful */
    @Override public RNode<X> apply(X x, RLeaf<X> leaf, Spatialization<X> m) {

        double maxWaste = Double.NEGATIVE_INFINITY;
        short size = leaf.size;
        int r1 = -1, r2 = -1;
        X[] data = leaf.data;
        double[] COST = Util.map(new IntToDoubleFunction() {
            @Override
            public double applyAsDouble(int i) {
                return m.bounds(data[i]).cost();
            }
        }, new double[(int) size]); //cache
        for (int i = 0; i < (int) size -1; i++) {
            HyperRegion ii = m.bounds(data[i]);
            double iic = COST[i];
            Function<HyperRegion, HyperRegion> iiMbr =   ii.mbrBuilder();
            for (int j = i + 1; j < (int) size; j++) {
                HyperRegion jj = m.bounds(data[j]);
                HyperRegion ij = iiMbr.apply(jj);
                double jjc = COST[j];
                double ijc = (ij==ii ? iic : (ij ==jj ? jjc : ij.cost())); //assert(ijc >= iic && ijc >= iic);
                double waste = (ijc - iic) + (ijc - jjc);
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
