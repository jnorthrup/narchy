package jcog.tree.rtree;

/** TODO generic contains, intersect, etc comparator ability via one method */
public interface Nodelike<T> {

    boolean contains(T x, HyperRegion b, Spatialization<T> model);

}
