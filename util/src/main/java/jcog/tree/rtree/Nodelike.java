package jcog.tree.rtree;

public interface Nodelike<T> {

    boolean contains(T x, HyperRegion b, Spatialization<T> model);

}
