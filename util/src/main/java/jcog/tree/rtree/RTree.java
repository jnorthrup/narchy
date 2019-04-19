package jcog.tree.rtree;

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


import jcog.data.atomic.MetalAtomicIntegerFieldUpdater;
import jcog.tree.rtree.util.CounterNode;
import jcog.tree.rtree.util.Stats;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * <p>Data structure to make range searching more efficient. Indexes multi-dimensional information
 * such as geographical coordinates or rectangles. Groups information and represents them with a
 * minimum bounding rectangle (mbr). When searching through the tree, any query that does not
 * intersect an mbr can ignore any data entries in that mbr.</p>
 * <p>More information can be @see <a href="https:
 * <p>
 * Created by jcairns on 4/30/15.</p>
 */
public class RTree<T> implements Space<T> {

    private static final MetalAtomicIntegerFieldUpdater<RTree> SIZE = new MetalAtomicIntegerFieldUpdater(RTree.class, "_size");

    private volatile Node<T> root;

    private volatile int _size;

    public final Spatialization<T> model;

    public RTree(int max, Split<T> splitType) {
        this((x-> (HyperRegion) x), max, splitType);
    }

    public RTree(@Nullable Function<T, HyperRegion> spatialize, final int mMax, final Split<T> splitType) {
        this(new Spatialization<>(spatialize, splitType, mMax));
    }

    public RTree(Spatialization<T> model) {
        this.model = model;
        clear();
    }

    @Override
    public Stream<T> stream() {
        return root.streamValues();
    }

    @Override
    public final Iterator<T> iterator() {
        return root.iterateValues();
    }

    @Override
    public void clear() {
        SIZE.updateAndGet(this, (sizeBeforeClear) -> {
            if (sizeBeforeClear > 0 || root == null)
                this.root = model.newLeaf();
            return 0;
        });
    }

    @Override
    public boolean OR(Predicate<T> o) {
        return root().OR(o);
    }

    @Override
    public boolean AND(Predicate<T> o) {
        return root().AND(o);
    }

    /**
     * TODO handle duplicate items (ie: dont increase entryCount if exists)
     */
    @Override
    public boolean add(/*@NotNull*/ final T t) {


        RInsertion<T> i = new RInsertion<>(t, true, model);
        Node<T> nextRoot = root.add(i);

        if (i.added()) {

            //assert(nextRoot!=null);
            this.root = nextRoot;
            //this.root = nextRoot!=null ? nextRoot : model.newLeaf();
            SIZE.getAndIncrement(this);


            //TEMPORARY
//            if (Iterators.size(iterator())!=size()) {
//                boolean[] added2 = new boolean[1];
//                Node<T> nextRoot2 = root.add(t, true, model, added2);
//                throw new WTF("inconsistent");
//            }


            return true;
        }


        return false;
    }


    /**
     * @param xBounds - the bounds of t which may not necessarily need to be the same as the bounds as model might report it now; for removing a changing value
     */
    @Override
    public boolean remove(final T x) {
        int before = SIZE.get(this);
        if (before == 0)
            return false;
        HyperRegion bx = model.bounds(x);
        if (!root.bounds().contains(bx))
            return false;

        boolean[] removed = new boolean[1];
        @Nullable Node<T> nextRoot = root.remove(x, bx, model, removed);
        if (removed[0]) {

            SIZE.getAndDecrement(this);

            root = nextRoot!=null ? nextRoot : model.newLeaf();

            return true;
        }
        return false;
    }

    @Override
    public boolean replace(final T told, final T tnew) {

        if (told == tnew) {
            return true;
        }

        if (model.bounds(told).equals(model.bounds(tnew))) {

            root = root.replace(told, model.bounds(told), tnew, model);
            return true;
        } else {

            boolean removed = remove(told);
            if (removed) {
                boolean added = add(tnew);
                if (!added)
                    throw new UnsupportedOperationException("error adding " + tnew);
                return true;
            } else {
                return false;
            }
        }
    }


    /**
     * @return number of data entries stored in the RTree
     */
    @Override
    public final int size() {
        return SIZE.getOpaque(this);
    }


    @Override
    public void forEach(Consumer<? super T> consumer) {
        if (root != null)
            root.forEach(consumer);
    }

    @Override
    public boolean intersectsWhile(HyperRegion rect, Predicate<T> t) {
         return root.intersecting(rect, t, model);
    }

    @Override
    public boolean containsWhile(HyperRegion rect, final Predicate<T> t) {
        return root.containing(rect, t, model);
    }

    /**
     * returns how many items were filled
     */
    @Override
    @Deprecated public int containedToArray(HyperRegion rect, final T[] t) {
        final int[] i = {0};
        root.containing(rect, (x) -> {
            t[i[0]++] = x;
            return i[0] < t.length;
        }, model);
        return i[0];
    }

    @Deprecated public Set<T> containedToSet(HyperRegion rect) {
        int s = size();
        Set<T> t = new HashSet(s);
        root.containing(rect, x -> {
            t.add(x);
            return true;
        }, model);
        return t;
    }


    void instrumentTree() {
        root = root.instrument();
        CounterNode.searchCount = 0;
        CounterNode.bboxEvalCount = 0;
    }

    @Override
    public Stats stats() {
        Stats stats = new Stats();
        stats.setType(model);
        stats.setMaxFill(model.max);

        root.collectStats(stats, 0);
        return stats;
    }


    @Override
    public String toString() {
        return getClass().getSimpleName() + "[size=" + size() + ']';
    }

    @Override
    public Node<T> root() {
        return this.root;
    }

    @Override
    public boolean contains(T x, HyperRegion b, Spatialization<T> model) {
        return root.contains(x, b, model);
    }

    public boolean contains(T t) {
        return contains(t, model.bounds(t), model);
    }

    @Override
    public HyperRegion bounds(T x) {
        return model.bounds(x);
    }


}
