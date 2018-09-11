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

import jcog.tree.rtree.util.Stats;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Created by jcairns on 4/30/15.
 */
public interface Node<X> extends Nodelike<X> {
    /**
     * @return boolean - true if this node is a leaf
     */
    boolean isLeaf();

    Stream<X> streamValues();

    /** iterate leaves only */
    default java.util.Iterator<X> iterateValues() {
        return streamValues().iterator();
    }

    Stream<Node<X>> streamNodes();

    /** the values only in this specific branch or leaf, regardless of type */
    Stream<?> streamLocal();

    /** the values only in this specific branch or leaf, regardless of type */
    default java.util.Iterator<?> iterateLocal() {
        return streamLocal().iterator();
    }

    /** iterate nodes only */
    default java.util.Iterator<Node<X>> iterateNodes() {
        return streamNodes().iterator();
    }




    /**
     * @return Rect - the bounding rectangle for this node
     */
    /*@NotNull */HyperRegion bounds();

    /**
     * Add t to the index
     *  @param x      - value to add to index
     * @param parent - the callee which is the parent of this instance.
     *                  if parent is null, indicates it is in the 'merge attempt' stage
     *                  if parent is non-null, in the 'insertion attempt' stage
     * @param model
     * @param added
     * @return null if Leaf merged it with existing item
     */
    Node<X> add(/*@NotNull */X x, @Nullable Nodelike<X> parent, /*@NotNull */Spatialization<X> model, boolean[] added);

    /**
     * Remove t from the index
     * @param x      - value to remove from index
     * @param xBounds - the bounds of t which may not necessarily need to be the same as the bounds as model might report it now; for removing a changing value
     * @param model
     * @param removed
     */
    Node<X> remove(X x, HyperRegion xBounds, Spatialization<X> model, boolean[] removed);

    /**
     * update an existing t in the index
     * @param told - old index to be updated
     * @param tnew - value to update old index to
     * @param model
     */
    Node<X> replace(X told, X tnew, Spatialization<X> model);




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
    void forEach(Consumer<? super X> consumer);

    boolean AND(Predicate<X> p);

    boolean OR(Predicate<X> p);

    /**
     * Consumer "accepts" every node in the given rect
     *
     * @param rect     - limiting rect
     * @param t
     * @param model
     * @return whether to continue visit
     */
    boolean intersecting(HyperRegion rect, Predicate<X> t, Spatialization<X> model);

    boolean containing(HyperRegion rect, Predicate<X> t, Spatialization<X> model);





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
    Node<X> instrument();






















}
