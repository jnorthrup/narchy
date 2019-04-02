package jcog.tree.rtree;

abstract public class AbstractNode<V> implements Node<V> {

    public short size;
    public HyperRegion bounds;

    protected final void  grow(HyperRegion tb) {
        HyperRegion bounds = this.bounds;
        this.bounds = bounds != null ? bounds.mbr(tb) : tb;
    }

    protected final void grow(Node node) {
        grow(node.bounds());
    }

    @Override
    public final  HyperRegion bounds() {

//        //TEMPORARY
//        if (this instanceof Branch) {
//            HyperRegion actualBounds = null;
//            Iterator<Node<V>> ii = this.streamNodes().iterator();
//            while (ii.hasNext()) {
//                HyperRegion iib = ii.next().bounds();
//                if (actualBounds == null) actualBounds = iib;
//                else actualBounds = actualBounds.mbr(iib);
//            }
//            if (!Objects.equals(actualBounds, bounds))
//                throw new WTF();
//        }

        return bounds;
    }


    @Override
    public final int size() {
        return size;
    }

}
