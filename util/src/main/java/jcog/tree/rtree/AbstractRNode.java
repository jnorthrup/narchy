package jcog.tree.rtree;

import jcog.Util;
import jcog.data.iterator.ArrayIterator;

import java.util.Iterator;
import java.util.stream.Stream;

abstract public class AbstractRNode<V,D> implements RNode<V> {

    public /*volatile*/ short size = 0;
    public /*volatile*/ HyperRegion bounds = null;
    public final D[] data;

    protected AbstractRNode(D[] data) {
        this.data = data;
    }

    @Override
    public final Stream<D> streamLocal() {
        return ArrayIterator.streamNonNull(data, size); //TODO null-terminator iterator eliding 'size'
    }
    @Override
    public final Iterator<D> iterateLocal() {
        return ArrayIterator.iterateN(data, size);
    }


    @Override
    public final D get(int i) {
        return data[i];
    }

    protected final void grow(HyperRegion y) {
        HyperRegion x = this.bounds;
        this.bounds = x != null ? Util.maybeEqual(x, x.mbr(y)) : y;
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
