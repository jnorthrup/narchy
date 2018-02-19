package jcog.data.graph;

import jcog.Util;
import org.jetbrains.annotations.Nullable;

/**
 * immutable directed edge with cached hashcode
 */
public class ImmutableDirectedEdge<N, E>  {

    private final int hash;
    public final E id;
    public final NodeGraph.Node<N, E> from;
    public final NodeGraph.Node<N, E> to;

    public ImmutableDirectedEdge(NodeGraph.Node<N, E> from, NodeGraph.Node<N, E> to, E id) {
        this.id = id;
        this.from = from;
        this.to = to;
        this.hash = Util.hashCombine(id.hashCode(), from.hashCode(), to.hashCode());
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (hash!=obj.hashCode() || !(obj instanceof ImmutableDirectedEdge)) return false;
        ImmutableDirectedEdge ee = (ImmutableDirectedEdge) obj;
        return from == ee.from && to == ee.to && id.equals(ee.id);
    }

    public boolean isSelfLoop() {
        return from == to;
    }

    @Override
    public String toString() {
        return from + " => " + id + " => " + to;
    }

    public NodeGraph.Node<N,E> to(boolean outOrIn) {
        return outOrIn ? to : from;
    }

    public NodeGraph.Node<N,E> from(boolean outOrIn) {
        return outOrIn ? from : to;
    }

    @Nullable
    public NodeGraph.Node<N,E> other(NodeGraph.Node<N,E> x) {
        if (from == x) return to;
        else if (to == x) return from;
        else return null;
    }
}
