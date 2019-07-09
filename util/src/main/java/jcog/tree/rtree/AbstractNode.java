package jcog.tree.rtree;

import jcog.Util;

abstract public class AbstractNode<V> implements Node<V> {

    public /*volatile*/ short size;
    public /*volatile*/ HyperRegion bounds;

    protected final void grow(HyperRegion b) {
        HyperRegion x = this.bounds;
        this.bounds = x != null ? Util.maybeEqual(x, x.mbr(b)) : b;
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
