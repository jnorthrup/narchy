package jcog.data.graph.edge;

import jcog.data.graph.Node;
import jcog.data.graph.path.FromTo;
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
        this.hash = FromTo.hash(from, id, to);
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
        if (!(obj instanceof FromTo) || hash != obj.hashCode()) return false;
        FromTo ee = (FromTo) obj;
        return from.equals(ee.from()) && to.equals(ee.to()) && Objects.equals(id, ee.id());
    }

    public boolean isSelfLoop() {
        return from.equals(to);
    }

    @Override
    public String toString() {
        return from + " => " + id + " => " + to;
    }


}
