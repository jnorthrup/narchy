package jcog.sort;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface TopFilter<X> extends Consumer<X>, Iterable<X> {

    boolean add(X x);

    @Nullable X pop();

    boolean isEmpty();

    void clear();

    int size();
}
