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

import java.util.function.Function;

/**
 * Guttmann's Quadratic split
 * <p>
 * Created by jcairns on 5/5/15.
 */
public class QuadraticSplitLeaf<X> implements Split<X> {

    @Override
    public Node<X> split(X x, Leaf<X> leaf, Spatialization<X> m) {

        double minCost = Double.MIN_VALUE;
        short size = leaf.size;
        int r1Max = 0, r2Max = size - 1;
        X[] data = leaf.data;
        for (int i = 0; i < size; i++) {
            HyperRegion ii = m.bounds(data[i]);
            double iic = ii.cost();
            Function<HyperRegion, HyperRegion> iiMbr = ii.mbrBuilder();
            for (int j = i + 1; j < size; j++) {
                HyperRegion jj = m.bounds(data[j]);
                final double cost = iiMbr.apply(jj).cost() - (iic + jj.cost());
                if (cost > minCost) {
                    r1Max = i;
                    r2Max = j;
                    minCost = cost;
                }
            }
        }


        return newBranch(x, leaf, m, size, r1Max, r2Max, data);
    }



}
