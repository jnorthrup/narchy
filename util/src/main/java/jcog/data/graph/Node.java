package jcog.data.graph;

import com.google.common.collect.Streams;

import java.io.PrintStream;
import java.util.stream.Stream;

public interface Node<N, E> {

    static <X,Y> FromTo<Node<Y,X>,X> edge(Node<Y, X> from, X what, Node<Y, X> to) {
        return new ImmutableDirectedEdge<>(from, what, to);
    }

    N id();

    Iterable<FromTo<Node<N,E>,E>> edges(boolean in, boolean out);


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
