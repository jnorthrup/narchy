package jcog.tree.rtree;

@FunctionalInterface public interface Split<T> {
    Node<T> split(T t, Leaf<T> leaf, Spatialization<T> model);
}
