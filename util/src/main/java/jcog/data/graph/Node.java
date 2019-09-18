package jcog.data.graph;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import jcog.Util;
import jcog.data.graph.edge.ImmutableDirectedEdge;
import jcog.data.graph.path.FromTo;
import jcog.data.list.FasterList;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.google.common.collect.Iterables.transform;

public interface Node<N, E> {

    N id();

    static <X,Y> FromTo<Node<Y,X>,X> edge(Node<Y, X> from, X what, Node<Y, X> to) {
        return new ImmutableDirectedEdge<>(from, what, to);
    }

    Iterable<FromTo<Node<N,E>,E>> edges(boolean in, boolean out);

    /** warning this buffers the iteration */
    default Iterable<FromTo<Node<N,E>,E>> edges(boolean in, boolean out, @Nullable Predicate<FromTo<Node<N,E>,E>> filter, @Nullable Comparator<FromTo<Node<N,E>,E>> sorter) {
        FasterList<FromTo<Node<N, E>, E>> l = new FasterList(edgeIterator(in, out));
        int s = l.size();
        if (s == 0)
            return Util.emptyIterable;

        if (filter!=null) {
            if (l.removeIf(filter.negate())) {
                s = l.size();
                if (s == 0)
                    return Util.emptyIterable;
            }
        }

        if (sorter!=null && s>1) {
            l.sortThis(sorter);
        }

        return l;
    }

    default Iterable<FromTo<Node<N,E>,E>> edges(boolean in, boolean out, @Nullable Predicate<FromTo<Node<N,E>,E>> filter) {
        Iterable<FromTo<Node<N, E>, E>> l = edges(in, out);
        if (l instanceof Collection && ((Collection)l).isEmpty())
            return Util.emptyIterable;
        else
            return filter != null ? Iterables.filter(l, filter::test) : l;
    }

    default Iterator<FromTo<Node<N,E>,E>> edgeIterator(boolean in, boolean out) { return edges(in, out).iterator(); }

    /** TODO Iterator version of this, like edges and edgesIterator */
    default Iterable<? extends Node<N,E>> nodes(boolean in, boolean out) {
        if (!in && !out)
            return Collections.EMPTY_LIST;

        Function<FromTo<Node<N, E>, E>, Node<N, E>> other = x -> x.other(this);
        Iterable<Node<N, E>> i = in ? transform(edges(true, false), other) : null;
        Iterable<Node<N, E>> o = out ? transform(edges(false,true), other) : null;
        if (in && out)
            return Iterables.concat(i, o);
        else if (in)
            return i;
        else //if (out)
            return o;
    }

    default Stream<FromTo<Node<N,E>,E>> streamIn() {
        return Streams.stream(edges(true, false));
    }

    default Stream<FromTo<Node<N,E>,E>> streamOut() {
        return Streams.stream(edges(false, true));
    }

    default Stream<FromTo<Node<N,E>,E>> stream() {
        return Streams.stream(edges(true, true));
    }

    default void print(PrintStream out) {
        out.println(this);
        stream().forEach(e -> {
            out.println("\t" + e);
        });
    }

    default int edgeCount(boolean in, boolean out) {
        return (int) ((in ? streamIn().count() : 0) + (out ? streamOut().count() : 0));
    }



}
