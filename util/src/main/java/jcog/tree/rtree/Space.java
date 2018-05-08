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
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import jcog.list.FasterList;
import jcog.tree.rtree.util.Stats;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Created by jcovert on 12/30/15.
 */
public interface Space<T> extends Nodelike<T> {

//    int DEFAULT_MIN_M = 2;
//    int DEFAULT_MAX_M = 8;
//    Spatialization.DefaultSplits DEFAULT_SPLIT_TYPE = Spatialization.DefaultSplits.AXIAL;

//    /**
//     * Create an R-Tree with default values for m, M, and split type
//     *
//     * @param spatializer - Builder implementation used to create HyperRects out of T's
//     * @param <T>         - The store type of the bound
//     * @return SpatialSearch - The spatial search and index structure
//     */
//    static <T> Space<T> rTree(final Function<T, HyperRegion> spatializer) {
//        return new RTree<>(spatializer, DEFAULT_MIN_M, DEFAULT_MAX_M, DEFAULT_SPLIT_TYPE);
//    }

//    /**
//     * Create an R-Tree with specified values for m, M, and split type
//     *
//     * @param builder   - Builder implementation used to create HyperRects out of T's
//     * @param minM      - minimum number of entries per node of this tree
//     * @param maxM      - maximum number of entries per node of this tree (exceeding this causes node split)
//     * @param splitType - type of split to use when M+1 entries are added to a node
//     * @param <T>       - The store type of the bound
//     * @return SpatialSearch - The spatial search and index structure
//     */
//    static <T> Space<T> rTree(final Function<T, HyperRegion> builder, final int minM, final int maxM, final Spatialization.DefaultSplits splitType) {
//        return new RTree<>(builder, minM, maxM, splitType);
//    }
//
//    /**
//     * Create a protected R-Tree with default values for m, M, and split type
//     *
//     * @param builder - Builder implementation used to create HyperRects out of T's
//     * @param <T>     - The store type of the bound
//     * @return SpatialSearch - The spatial search and index structure
//     */
//    static <T> Spatialized<T> lockingRTree(final Function builder) {
//        return new LockingRTree<>(rTree(builder), new ReentrantReadWriteLock(true));
//    }

//    /**
//     * Create a protected R-Tree with specified values for m, M, and split type
//     *
//     * @param builder   - Builder implementation used to create HyperRects out of T's
//     * @param minM      - minimum number of entries per node of this tree
//     * @param maxM      - maximum number of entries per node of this tree (exceeding this causes node split)
//     * @param splitType - type of split to use when M+1 entries are added to a node
//     * @param <T>       - The store type of the bound
//     * @return SpatialSearch - The spatial search and index structure
//     */
//    static <T> SpatialSearch<T> lockingRTree(final RectBuilder<T> builder, final int minM, final int maxM, final RTree.Split splitType) {
//        return new LockingRTree<>(rTree(builder, minM, maxM, splitType), new ReentrantReadWriteLock(true));
//    }



    Spatialization<T> model();


    /**
     * Update entry in tree
     *  @param told - Entry to update
     * @param tnew - Entry to update it to
     */
    boolean replace(final T told, final T tnew);

    /**
     * Get the number of entries in the tree
     *
     * @return entry count
     */
    int size();

    boolean OR(Predicate<T> o);
    boolean AND(Predicate<T> o);
    void forEach(Consumer<? super T> consumer);

//    default boolean intersecting(HyperRegion rect, Consumer<T> consumer) {
//        return intersecting(rect, (x) -> {
//            consumer.accept(x);
//            return true;
//        });
//    }

    /** continues finding intersecting regions until the predicate returns false */
    void whileEachIntersecting(HyperRegion rect, Predicate<T> t);

    /** continues finding containing regions until the predicate returns false */
    void whileEachContaining(HyperRegion rect, Predicate<T> t);

    /**
     * Search for entries intersecting given bounding rect
     *
     * @param rect - Bounding rectangle to use for querying
     * @param t    - Array to store found entries
     * @return Number of results found
     */
    @Deprecated
    int containedToArray(final HyperRegion rect, final T[] t);


    Stats stats();

    default boolean isEmpty() {
        return size() == 0;
    }

    void clear();

    /**
     * Add the data entry to the SpatialSearch structure
     *
     * @param t Data entry to be added
     * @return whether the item was added, or false if it wasn't (ex: duplicate or some other prohibition)
     */
    boolean add(final T t);

    /** adds, deferred if necessary until un-busy */
    default void addAsync(T t) {
        throw new UnsupportedOperationException();
    }

    /**
     * Remove the data entry from the SpatialSearch structure
     *
     * @param x Data entry to be removed
     * @return whether the item was added, or false if it wasn't (ex: duplicate or some other prohibition)
     */
    boolean remove(/*@NotNull*/ final T x);


    /** removes, deferred if necessary until un-busy */
    default void removeAsync(T x) {
        throw new UnsupportedOperationException();
    }

//    default RTreeCursor<T> cursor(HyperRegion start) {
//        return new RTreeCursor<>(this).in(start);
//    }

    Node<T, ?> root();

//    void intersectingNodes(HyperRegion start, Predicate<Node<T, ?>> eachWhile);

    HyperRegion bounds(T task);

    Stream<T> stream();

    default Iterator<T> iterator() {
        int s = size();
        if (s ==0)
            return Collections.emptyIterator();

        //HACK
        List<T> snapshot = new FasterList(s);
        forEach(snapshot::add);
        return snapshot.iterator();

//        return new Iterator<T>() {
//            List<Node> stack = new FasterList();
//            {
//                stack.add(root());
//            }
//            Iterator<>
//
//            @Override
//            public boolean hasNext() {
//
//                return false;
//            }
//
//            @Override
//            public T next() {
//                return null;
//            }
//        };
    }

    default List<T> asList() {
        int s = size();
        List<T> l = new FasterList<>(s);
        iterator().forEachRemaining(l::add);
        return l;
    }


    boolean contains(T t);

}
