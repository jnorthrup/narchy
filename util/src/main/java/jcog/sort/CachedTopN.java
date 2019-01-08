//package jcog.sort;
//
//import jcog.data.set.ArrayHashSet;
//import jcog.pri.AtomicPri;
//import jcog.pri.NLink;
//import org.eclipse.collections.api.block.function.primitive.FloatFunction;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.Collection;
//
///**
// * with set of entries to merge (filter) duplicates
// */
//public class CachedTopN<X> extends ArrayHashSet<NLink<X>>  {
//
//    private FloatFunction<X> rank;
//
//    public CachedTopN(int limit, FloatFunction<X> rank) {
//        this(new NLink[limit], rank);
//    }
//
//    public CachedTopN(NLink<X>[] target, FloatFunction<X> rank) {
//        super(new TopN<>(target, FloatRank.the(AtomicPri::pri)));
//        rank(rank);
//    }
//
//    public CachedTopN<X> clear(FloatFunction<X> rank) {
//        clear();
//        return rank(rank);
//    }
//
//    public CachedTopN<X> rank(FloatFunction<X> rank) {
//        //assert(isEmpty());
//        this.rank = rank;
//        return this;
//    }
//
//    /** implementations can filter unique inputs before ranking them here */
//    public boolean valid(X x) {
//        return true;
//    }
//
//    @Override
//    public boolean add(NLink<X> element) {
//        throw new UnsupportedOperationException("use accept");
//    }
//
//    @Override
//    public boolean addAll(Collection<? extends NLink<X>> c) {
//        throw new UnsupportedOperationException("use accept");
//    }
//
//    public final boolean accept(X x) {
//
//        if (!contains(x) && valid(x)) {
//            float r = rank.floatValueOf(x);
//            if (r > ((TopN)list).minValueIfFull())
//                super.add(new NLink<>(x, r));
//        }
//        return true;
//    }
//
//
//
//    public float pri(int y) {
//        return get(y).pri();
//    }
//
//    public boolean isEmpty() {
//        return size() == 0;
//    }
//
//
//    /** what % to remain; ex: rate of 25% removes the lower 75% */
//    public void removePercentage(float below, boolean ofExistingOrCapacity) {
//        ((TopN)list).removePercentage(below,ofExistingOrCapacity);
//    }
//
//    @Nullable
//    public X pop() {
//        NLink<X> n = ((TopN<NLink<X>>) list).pop();
//        return n != null ? n.get() : null;
//    }
//
//
////    public Set<X> removePercentageToSet(float below) {
////        assert(below >= 0 && below <= 1.0f);
////        int belowIndex = (int) Math.floor(size() * below);
////        if (belowIndex == size)
////            return Set.of();
////
////        int toRemove = size - belowIndex;
////        Set<X> removed = new HashSet();
////        for (int i = 0; i < toRemove; i++) {
////            removed.add(removeLast());
////        }
////        return removed;
////    }
//
//
//}
