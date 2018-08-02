package jcog.data.graph;

import com.google.common.collect.Streams;

import java.io.PrintStream;
import java.util.stream.Stream;

public interface Node<N, E> {

    <X> X id();

    Iterable<FromTo<Node<N,E>,E>> edges(boolean in, boolean out);


    default Stream<FromTo<Node<N,E>,E>> streamIn() {
        return Streams.stream(edges(true, false));
    }

    default Stream<FromTo<Node<N,E>,E>> streamOut() {
        return Streams.stream(edges(false, true));
    }

    default void print(PrintStream out) {
        out.println(this);
        streamOut().forEach(e -> {
            out.println("\t" + e);
        });
    }

}
