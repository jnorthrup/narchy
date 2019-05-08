package jcog.data.graph.path;

import org.jetbrains.annotations.Nullable;

/**
 * F represents the node type
 * X represents the edge type
 */
public interface FromTo<N, E> /* extends Triple<F,X,F> */ {

    N from();
    E id();
    N to();

    default N to(boolean outOrIn) {
        return outOrIn ? to() : from();
    }

    default N from(boolean outOrIn) {
        return outOrIn ? from() : to();
    }

    @Nullable
    default N other(N x) {
        N f = from(), t = to();
        //TODO maybe this needs to be .equals()
        if (f == x) return t;
        else if (t == x) return f;
        else
            throw new NullPointerException(); //return null;
    }

    default boolean loop() {
        return from().equals(to());
    }

}
