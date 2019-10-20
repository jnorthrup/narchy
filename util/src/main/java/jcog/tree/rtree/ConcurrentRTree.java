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
import jcog.util.LambdaStampedLock;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.*;
import java.util.stream.Stream;

/**
 * Created by jcovert on 12/30/15.
 */
@SuppressWarnings("TooBroadScope")
public class ConcurrentRTree<X> extends LambdaStampedLock implements Space<X> {

    protected final RTree<X> tree;

    public ConcurrentRTree(final RTree<X> tree) {
        super();
        this.tree = tree;
    }

    @Override
    public final boolean contains(final X x) {
        return testWith(RTree::contains, x);
    }

    @Override
    public boolean OR(final Predicate<X> o) {
        return testWith((tr,oo)->tr.root().OR(oo), o);
    }

    @Override
    public boolean AND(final Predicate<X> o) {
        return testWith((tr,oo)->tr.root().AND(oo), o);
    }

    /**
     * Blocking locked search
     *
     * @param rect - HyperRect to search
     * @param t    - array to hold results
     * @return number of entries found
     */
    @Deprecated  @Override
    public int containedToArray(final HyperRegion rect, final X[] t) {
        return read(() -> tree.containedToArray(rect, t));
    }

    /** TODO encapsulate. should not be exposed */
    @Deprecated @Override
    public final RNode<X> root() {
        return tree.root();
    }

    /**
     * Blocking locked addAt
     *
     * @param x - entry to addAt
     */
    @Override
    public RInsertion<X> insert(final X x) {
        final long l = writeLock();
        try {
            return tree.insert(x);
        } finally {
            unlockWrite(l);
        }
    }

    /**
     * prefer this instead of add() in multithread environments, because it elides what might ordinarily involve a lock wait
     */
    @Override
    public void addAsync(final X t) {
        add(t);
    }

    @Override
    public void removeAsync(final X t) {
        remove(t);
    }


    /** TODO read -> write lock */
    @Override public boolean remove(final X x) {
        final long l = writeLock();
        try {
            return tree.remove(x);
        } finally {
            unlockWrite(l);
        }
    }

    public void removeAll(final Iterable<? extends X> t) {
        write(() -> {
            for (final X x : t) {
                remove(x);
            }
        });
    }

    public final void readIfNonEmpty(final Consumer<RTree<X>> x) {
        if (size() == 0) return;
        read(x);
    }

    public final void read(final Consumer<RTree<X>> x) {
        final long l = readLock();
        try {
            x.accept(tree);
        } finally {
            unlockRead(l);
        }
    }


    /**
     * doesnt lock, use at your own risk
     */
    protected final void readDirect(final Consumer<Space<X>> x) {
        x.accept(tree);
    }

    public final void write(final Consumer<Space<X>> x) {
        final long l = writeLock();
        try {
            x.accept(tree);
        } finally {
            unlockWrite(l);
        }
    }

    public final boolean write(final Predicate<Space<X>> x) {
        final long l = writeLock();
        try {
            return x.test(tree);
        } finally {
            unlockWrite(l);
        }
    }

    public final <Y> Y write(final Function<Space<X>,Y> x) {
        final long l = writeLock();
        try {
            return x.apply(tree);
        } finally {
            unlockWrite(l);
        }
    }

    public final <A,Y> Y writeWith(final A a, final BiFunction<Space<X>,A,Y> x) {
        final long l = writeLock();
        try {
            return x.apply(tree, a);
        } finally {
            unlockWrite(l);
        }
    }

    public final void readOptimistic(final Consumer<Space<X>> x) {
        readOptimistic(() -> x.accept(tree));
    }

    @Override
    public final HyperRegion bounds(final X task) {
        return tree.bounds(task);
    }


    /**
     * Blocking locked update
     *
     * @param told - entry to update
     * @param tnew - entry with new value
     * TODO read -> write lock
     */
    @Override public final boolean replace(final X told, final X tnew) {
        final long l = writeLock();
        try {
            return tree.replace(told, tnew);
        } finally {
            unlockWrite(l);
        }
    }

    @Override
    public final int size() {
        return tree.size();
    }

    @Override
    public void clear() {
        write(tree::clear);
    }

    @Override
    public final void forEach(final Consumer<? super X> consumer) {
        final long l = readLock();
        try {
            tree.forEach(consumer);
        } finally {
            unlockRead(l);
        }
    }

    public final void forEachOptimistic(final Consumer<? super X> consumer) {
        readOptimistic(() -> tree.forEach(consumer));
    }

    public <Y> void readWith(final BiConsumer<RTree<X>,Y> readProcedure, final Y x) {
        final long stamp = readLock();
        try {
            readProcedure.accept(tree, x);
        } finally {
            unlockRead(stamp);
        }
    }


    public boolean test(final Predicate<RTree<X>> p) {
        final long stamp = readLock();
        try {
            return p.test(tree);
        } finally {
            unlockRead(stamp);
        }
    }

    public <Y> boolean testWith(final BiPredicate<RTree<X>,Y> p, final Y y) {
        final long stamp = readLock();
        try {
            return p.test(tree, y);
        } finally {
            unlockRead(stamp);
        }
    }

    @Override
    public boolean containsWhile(final HyperRegion rect, final Predicate<X> t) {
        read(() -> tree.containsWhile(rect, t));
        return false;
    }

    @Override
    public boolean intersectsWhile(final HyperRegion rect, final Predicate<X> t) {
        //Predicates.compose(t,)
        read(() -> tree.intersectsWhile(rect, t));
        return false;
    }

    /**
     * warning: not locked
     */
    @Override
    public Stream<X> stream() {
        return root().streamValues().filter(Objects::nonNull);
    }

    /**
     * warning: not locked
     */
    public Stream<RNode<X>> streamNodes() {
        return root().streamNodesRecursively().filter(Objects::nonNull);
    }

    /**
     * warning: not locked
     */
    @Override
    public Iterator<X> iterator() {
        return stream().iterator();
    }

    @Override
    public Stats stats() {
        return read(tree::stats);
    }

    @Override
    public String toString() {
        return tree.toString();
    }

    @Override
    public boolean contains(final X x, final HyperRegion b, final Spatialization<X> model) {
        final long l = readLock();
        try {
            return tree.contains(x, b, model);
        } finally {
            unlockRead(l);
        }
    }


    public final Spatialization<X> model() {
        return tree.model;
    }
}
