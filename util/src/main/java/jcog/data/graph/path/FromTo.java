package jcog.data.graph.path;

import org.jetbrains.annotations.Nullable;

public interface FromTo<F, X> /* extends Triple<F,X,F> */ {

    F from();
    X id();
    F to();

    default F to(boolean outOrIn) {
        return outOrIn ? to() : from();
    }

    default F from(boolean outOrIn) {
        return outOrIn ? from() : to();
    }

    @Nullable
    default F other(F x) {
        F f = from(), t = to();
        if (f == x) return t;
        else if (t == x) return f;
        else return null;
    }

    default boolean loop() {
        return from().equals(to());
    }

}
