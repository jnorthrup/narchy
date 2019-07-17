package jcog.sort;

import java.util.function.Consumer;

public interface TopFilter<X> extends Consumer<X>, Iterable<X> {

    boolean isEmpty();

    void clear();

    int size();
}
