package jcog.tree.rtree;

abstract public class AbstractNode<V> implements Node<V> {

    public short size;
    public HyperRegion bounds;

    protected final void grow(HyperRegion tb) {
        HyperRegion bounds = this.bounds;
        this.bounds = bounds != null ? bounds.mbr(tb) : tb;
    }

    protected final void grow(Node node) {
        grow(node.bounds());
    }

    @Override
    public final HyperRegion bounds() {
        return bounds;
    }


    @Override
    public final int size() {
        return size;
    }

}
