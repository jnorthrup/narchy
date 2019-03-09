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
    public Node<X> split(X x, Leaf<X> leaf, Spatialization<X> model) {




        
        double minCost = Double.MIN_VALUE;
        short size = leaf.size;
        int r1Max = 0, r2Max = size - 1;
        X[] data = leaf.data;
        for (int i = 0; i < size; i++) {
            HyperRegion ii = model.bounds(data[i]);
            double iic = ii.cost();
            Function<HyperRegion, HyperRegion> iiMbr = ii.mbrBuilder();
            for (int j = i + 1; j < size; j++) {
                HyperRegion jj = model.bounds(data[j]);
                final double cost = iiMbr.apply(jj).cost() - (iic + jj.cost());
                if (cost > minCost) {
                    r1Max = i;
                    r2Max = j;
                    minCost = cost;
                }
            }
        }


        final Leaf<X> l1Node = model.newLeaf();
        boolean[] dummy = new boolean[1];
        l1Node.add(data[r1Max], true, model, dummy);
        final Leaf<X> l2Node = model.newLeaf();
        dummy[0] = false;
        l2Node.add(data[r2Max], true, model, dummy);

        for (int i = 0; i < size; i++) {
            if ((i != r1Max) && (i != r2Max))
                leaf.transfer(l1Node, l2Node, data[i], model);
        }

        leaf.transfer(l1Node, l2Node, x, model);

        return model.newBranch(l1Node, l2Node);
    }

}
