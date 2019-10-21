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

import jcog.sort.QuickSort;
import jcog.tree.rtree.*;
import org.eclipse.collections.api.block.function.primitive.IntToDoubleFunction;

import java.util.Arrays;

/**
 * Fast RTree split suggested by Yufei Tao taoyf@cse.cuhk.edu.hk
 * <p>
 * Perform an axial split
 * <p>
 * Created by jcairns on 5/5/15.
 */
public class AxialSplit<X> implements Split<X> {

    /** default stateless instance which can be re-used */
    public static final Split<?> the = new AxialSplit<>();

    public AxialSplit() { }

    @Override
    public RBranch<X> apply(X x, RLeaf<X> leaf, Spatialization<X> model) {


        HyperRegion rCombined = leaf.bounds.mbr(model.bounds(x));

        int nD = rCombined.dim();


        int axis = 0;
        double mostCost = Double.NEGATIVE_INFINITY;
        for (int d = 0; d < nD; d++) {

            double axisCost = rCombined.cost(d);
            if (axisCost > mostCost) {
                axis = d;
                mostCost = axisCost;
            }
        }


        int splitDimension = axis;

        short size = (short) ((int) leaf.size +1);
        X[] ld = leaf.data;
        X[] obj = Arrays.copyOf(ld, (int) size); //? X[] obj = (X[]) Array.newInstance(leaf.data.getClass(), size);
        obj[(int) size -1] = x;

//        double[] strength = new double[size];
//        for (int i = 0; i < size; i++) {
//            X li = i < size-1 ? ld[i] : x;
//            double c = model.bounds(li).center(splitDimension); //TODO secondary sort by range
//            strength[i] = -c;
//        }

        if ((int) size > 1) {
            QuickSort.sort(obj, 0, (int) size, new IntToDoubleFunction() {
                        @Override
                        public double valueOf(int i) {
                            return model.bounds(obj[i]).center(splitDimension);
                        }
                    }
            );
        }

        int splitN = (int) size /2 + ((((int) size & 1)!=0) ? 1 : 0);
        //TODO if size is odd, maybe l1Node should have the 1 extra element rather than l2Node as this will:
        RLeaf<X> l1Node = model.transfer(obj, 0, splitN);
        RLeaf<X> l2Node = model.transfer(obj, splitN, (int) size);

        //assert (l1Node.size()+l2Node.size() == size);

        //leaf.transfer(l1Node, l2Node, x, model);

        //assert (l1Node.size()+l2Node.size() == size);

        return model.newBranch(l1Node, l2Node);
    }




}
