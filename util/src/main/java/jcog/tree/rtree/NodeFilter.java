//package jcog.tree.rtree;
//
//import java.util.Collections;
//import java.util.IdentityHashMap;
//import java.util.Set;
//
///**
// * allows one or more iterators to filter duplicate node visits
// * TODO improve */
//public final class NodeFilter<X> {
//
//    private Set<Node<X>> already;
//    private final int capacity;
//
//    public NodeFilter(int capacity){
//        this.capacity = capacity;
//    }
//
//    public boolean tryVisit(Node n) {
//        if (already==null)
//            this.already = Collections.newSetFromMap(new IdentityHashMap<>(capacity));
//
//        return already.add(n);
//    }
//}
