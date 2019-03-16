package jcog.data.graph;

import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import jcog.WTF;
import jcog.data.graph.path.FromTo;

import java.io.PrintStream;
import java.util.stream.Stream;

import static com.google.common.collect.Iterables.transform;

public interface Node<N, E> {

    static <X,Y> FromTo<Node<Y,X>,X> edge(Node<Y, X> from, X what, Node<Y, X> to) {
        return new ImmutableDirectedEdge<>(from, what, to);
    }

    N id();

    Iterable<FromTo<Node<N,E>,E>> edges(boolean in, boolean out);

    default Iterable<? extends Node<N,E>> nodes(boolean in, boolean out) {
        Iterable<Node<N, E>> i = in ? transform(edges(true, false), x -> x.other(this)) : null;
        Iterable<Node<N, E>> o = out ? transform(edges(false,true), x->x.other(this)) : null;
        if (in && out)
            return Iterables.concat(i, o);
        else if (in)
            return i;
        else if (out)
            return o;
        else
            throw new WTF();
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
