package jcog.tree.rtree;

import jcog.Util;

abstract public class AbstractRNode<V> implements RNode<V> {

    public /*volatile*/ short size = 0;
    public /*volatile*/ HyperRegion bounds = null;

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
