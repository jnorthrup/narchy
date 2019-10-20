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


        var rCombined = leaf.bounds.mbr(model.bounds(x));

        var nD = rCombined.dim();


        var axis = 0;
        var mostCost = Double.NEGATIVE_INFINITY;
        for (var d = 0; d < nD; d++) {

            var axisCost = rCombined.cost(d);
            if (axisCost > mostCost) {
                axis = d;
                mostCost = axisCost;
            }
        }


        var splitDimension = axis;

        var size = (short) (leaf.size+1);
        var ld = leaf.data;
        var obj = Arrays.copyOf(ld, size); //? X[] obj = (X[]) Array.newInstance(leaf.data.getClass(), size);
        obj[size-1] = x;

//        double[] strength = new double[size];
//        for (int i = 0; i < size; i++) {
//            X li = i < size-1 ? ld[i] : x;
//            double c = model.bounds(li).center(splitDimension); //TODO secondary sort by range
//            strength[i] = -c;
//        }

        if (size > 1) {
            QuickSort.sort(obj, 0, size, (IntToDoubleFunction) i ->
                model.bounds(obj[i]).center(splitDimension)
            );
        }

        var splitN = size/2 + (((size & 1)!=0) ? 1 : 0);
        //TODO if size is odd, maybe l1Node should have the 1 extra element rather than l2Node as this will:
        var l1Node = model.transfer(obj, 0, splitN);
        var l2Node = model.transfer(obj, splitN, size);

        //assert (l1Node.size()+l2Node.size() == size);

        //leaf.transfer(l1Node, l2Node, x, model);

        //assert (l1Node.size()+l2Node.size() == size);

        return model.newBranch(l1Node, l2Node);
    }




}
