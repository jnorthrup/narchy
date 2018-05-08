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
 *      http://www.apache.org/licenses/LICENSE-2.0
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
    public Node<T, ?> split(T t, Leaf<T> leaf, Spatialization<T> model) {


        HyperRegion r = leaf.bounds;

        final int nD = r.dim();

        // choose axis to split
        int axis = 0;
        double rangeD = r.range(0);
        for (int d = 1; d < nD; d++) {
            // split along the greatest range extent
            final double dr = r.rangeIfFinite(d, 0);
            if (dr > rangeD) {
                axis = d;
                rangeD = dr;
            }
        }

        // sort along split dimension
        final int splitDimension = axis;

        short size = leaf.size;
        IntDoublePair[] sorted = new IntDoublePair[size];
        double lastSpan = Double.NEGATIVE_INFINITY;
        T[] ld = leaf.data;
        for (int i = 0; i < size; i++) {
            double span = model.bounds(ld[i]).center(splitDimension);
            sorted[i] = pair(i, -span /* negative since the ArrayUtils.sort below is descending */);
            if (lastSpan==lastSpan) {
                if (span < lastSpan) {
                    lastSpan = Double.NaN; //set unsorted
                } else {
                    lastSpan = span;
                }
            }
        }
        if (lastSpan!=lastSpan) {
            if (size > 2) {
                ArrayUtils.sort(sorted, IntDoublePair::getTwo);
            } else {
                assert(size==2);
                //just swap entries
                IntDoublePair x = sorted[0];
                sorted[0] = sorted[1];
                sorted[1] = x;
            }
        }

//        final HyperRegion[] sortedMbr = HyperRegion.toArray(leaf.data, size, model.bounds);
//        Arrays.sort(sortedMbr, Comparator.comparingDouble(o -> o.center(splitDimension)));

        // divide sorted leafs
        final Leaf<T> l1Node = model.transfer(leaf, sorted, 0, size/2);
        final Leaf<T> l2Node = model.transfer(leaf, sorted, size / 2, size);

//        if ((l1Node.size()+l2Node.size() != size))
//            throw new RuntimeException("fix: leaf.contains(t)=" + leaf.contains(t, model.bounds(t), model));
        assert (l1Node.size()+l2Node.size() == size);

        leaf.transfer(l1Node, l2Node, t, model);

//        if ((l1Node.size()+l2Node.size() != size+1))
//            throw new RuntimeException("fix");
        assert (l1Node.size()+l2Node.size() == size+1);

        return model.newBranch(l1Node, l2Node);
    }




}
