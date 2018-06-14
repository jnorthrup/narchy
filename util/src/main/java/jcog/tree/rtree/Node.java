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

import jcog.list.FasterList;
import jcog.tree.rtree.util.Stats;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Created by jcairns on 4/30/15.
 * @param L the leaf type, ie. the type which will be found at the edges and which characterizes the type of the tree this may be used within
 * @param V the type of child values, which will either be Node<L,?> for Branch, or L for leaves
 */
public interface Node<L, V> extends Nodelike<L> {
    /**
     * @return boolean - true if this node is a leaf
     */
    boolean isLeaf();

    Stream<L> stream();

    default java.util.Iterator<L> iterator() {
        return stream().iterator();
    }

    Stream<V> streamNodes();
    //{
      //  return IntStream.range(0, size()).mapToObj(this::get);
    //}

    java.util.Iterator<V> iterateNodes();


    default FasterList<V> toList() {
        int s = size();
        FasterList f = new FasterList(s);
        Object[] ff = f.array();
        for (int i = 0; i < s; i++) {
            ff[i] = get(i);
        }
        return f;
    }

    /** gets contained child i */
    V get(int i);

    /**
     * @return Rect - the bounding rectangle for this node
     */
    /*@NotNull */HyperRegion bounds();

    /**
     * Add t to the index
     *  @param l      - value to add to index
     * @param parent - the callee which is the parent of this instance.
     *                  if parent is null, indicates it is in the 'merge attempt' stage
     *                  if parent is non-null, in the 'insertion attempt' stage
     * @param model
     * @param added
     * @return null if Leaf merged it with existing item
     */
    Node<L, ?> add(/*@NotNull */L l, @Nullable Nodelike<L> parent, /*@NotNull */Spatialization<L> model, boolean[] added);

    /**
     * Remove t from the index
     * @param l      - value to remove from index
     * @param xBounds - the bounds of t which may not necessarily need to be the same as the bounds as model might report it now; for removing a changing value
     * @param model
     * @param removed
     */
    Node<L, ?> remove(L l, HyperRegion xBounds, Spatialization<L> model, boolean[] removed);

    /**
     * update an existing t in the index
     * @param told - old index to be updated
     * @param tnew - value to update old index to
     * @param model
     */
    Node<L, ?> replace(L told, L tnew, Spatialization<L> model);




    /**
     * The number of entries in the node
     *
     * @return entry count
     */
    int size();

    /**
     * Consumer "accepts" every node in the entire index
     *
     * @param consumer
     */
    void forEach(Consumer<? super L> consumer);

    boolean AND(Predicate<L> p);

    boolean OR(Predicate<L> p);

    /**
     * Consumer "accepts" every node in the given rect
     *
     * @param rect     - limiting rect
     * @param t
     * @param model
     * @return whether to continue visit
     */
    boolean intersecting(HyperRegion rect, Predicate<L> t, Spatialization<L> model);

    boolean containing(HyperRegion rect, Predicate<L> t, Spatialization<L> model);





    /**
     * Recurses over index collecting stats
     *
     * @param stats - Stats object being populated
     * @param depth - current depth in tree
     */
    void collectStats(Stats stats, int depth);

    /**
     * Visits node, wraps it in an instrumented node, (see CounterNode)
     *
     * @return instrumented node wrapper
     */
    Node<L, ?> instrument();






















}
