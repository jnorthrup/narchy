package jcog.tree.rtree;

abstract public class AbstractNode<V> implements Node<V> {

    public short size;
    public HyperRegion bounds;

    protected final void grow(HyperRegion tb) {
        HyperRegion bounds = this.bounds;
        HyperRegion nextBounds = bounds != null ? bounds.mbr(tb) : tb;
        if (bounds != nextBounds)
            this.bounds = nextBounds;
    }

    protected void grow(Node node) {
        HyperRegion bounds = this.bounds;
        HyperRegion nextBounds = bounds.mbr(node.bounds());
        if (bounds != nextBounds)
            this.bounds = nextBounds;
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
