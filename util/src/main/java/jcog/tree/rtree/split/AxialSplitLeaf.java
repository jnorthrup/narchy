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
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.tuple.primitive.IntDoublePair;

import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

/**
 * Fast RTree split suggested by Yufei Tao taoyf@cse.cuhk.edu.hk
 * <p>
 * Perform an axial split
 * <p>
 * Created by jcairns on 5/5/15.
 */
public final class AxialSplitLeaf<T> implements Split<T> {

    /** default stateless instance which can be re-used */
    public static final Split<?> the = new AxialSplitLeaf<>();

    public AxialSplitLeaf() { }

    @Override
    public Node<T> split(T t, Leaf<T> leaf, Spatialization<T> model) {


        HyperRegion r = leaf.bounds;

        final int nD = r.dim();

        
        int axis = 0;
        double rangeD = Double.NEGATIVE_INFINITY;
        for (int d = 0; d < nD; d++) {
            
            final double dr = r.cost(d);
            if (dr > rangeD) {
                axis = d;
                rangeD = dr;
            }
        }

        
        final int splitDimension = axis;

        short size = leaf.size;
        IntDoublePair[] sorted = new IntDoublePair[size];
        double lastSpan = Double.NEGATIVE_INFINITY;
        T[] ld = leaf.data;
        for (int i = 0; i < size; i++) {
            double span = model.bounds(ld[i]).center(splitDimension);
            sorted[i] = pair(i, -span /* negative since the ArrayUtils.sort below is descending */);
            if (lastSpan==lastSpan) {
                lastSpan = span < lastSpan ? Double.NaN : span;
            }
        }
        if (lastSpan!=lastSpan) {
            if (size > 2) {
                ArrayUtils.sort(sorted, IntDoublePair::getTwo);
            } else {
                assert(size==2);
                
                IntDoublePair x = sorted[0];
                sorted[0] = sorted[1];
                sorted[1] = x;
            }
        }




        
        final Leaf<T> l1Node = model.transfer(leaf, sorted, 0, size/2);
        final Leaf<T> l2Node = model.transfer(leaf, sorted, size / 2, size);



        assert (l1Node.size()+l2Node.size() == size);

        leaf.transfer(l1Node, l2Node, t, model);



        assert (l1Node.size()+l2Node.size() == size+1);

        return model.newBranch(l1Node, l2Node);
    }




}
