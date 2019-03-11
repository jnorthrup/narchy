package jcog.data.graph;

import jcog.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * immutable directed edge with cached hashcode
 */
public class ImmutableDirectedEdge<N, E> implements FromTo<Node<N,E>, E> {

    public final Node<N,E> from, to;
    @Nullable private final E id;
    private final int hash;

    public ImmutableDirectedEdge(Node<N, E> from, @Nullable E id, Node<N, E> to) {
        this.hash = Util.hashCombine((id!=null ? id.hashCode() : 0), from.hashCode(), to.hashCode());
        this.id = id;
        this.from = from;
        this.to = to;
    }

    @Override
    public final Node<N,E> from() {
        return from;
    }

    @Override
    public final E id() {
        return id;
    }

    @Override
    public final Node<N,E> to() {
        return to;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (hash!=obj.hashCode() || !(obj instanceof FromTo)) return false;
        ImmutableDirectedEdge ee = (ImmutableDirectedEdge) obj;
        return from == ee.from && to == ee.to && Objects.equals(id, ee.id);
    }

    public boolean isSelfLoop() {
        return from.equals(to);
    }

    @Override
    public String toString() {
        return from + " => " + id + " => " + to;
    }


}
