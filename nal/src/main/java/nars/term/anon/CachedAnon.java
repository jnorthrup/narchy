//package nars.term.anon;
//
//import nars.term.Term;
//import nars.term.atom.Atomic;
//import nars.util.term.transform.CachedTermTransform;
//import nars.util.term.transform.DirectTermTransform;
//import nars.util.term.transform.TermTransform;
//import org.eclipse.collections.impl.map.mutable.UnifiedMap;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.Map;
//
///** TODO implement these as CachedTransform-wrapped sub-implementations of the Anon.GET and PUT transforms, each with their own cache */
//public class CachedAnon extends Anon {
//
//    private Map<Term,Term> externCache;
//    private DirectTermTransform.CachedDirectTermTransform internCache;
//
//    public CachedAnon(int capacity, int internCacheSize) {
//        super(capacity);
//        this.internCache.resize(internCacheSize);
//    }
//
//    @Override
//    public boolean rollback(int uniques) {
//        if (uniques == 0) {
//            clear();
//            return true;
//        }
//        if (super.rollback(uniques)) {
//            if (externCache!=null)
//                externCache.clear();
//
//            return true;
//        }
//        return false;
//    }
//
//    @Override
//    public void clear() {
//        if (externCache!=null)
//            externCache.clear();
//
//        super.clear();
//    }
//
//
//    protected TermTransform newPut() {
//        return internCache = new DirectTermTransform.CachedDirectTermTransform(0) {
//            @Override
//            public final @Nullable Term transformAtomic(Atomic atomic) {
//                return put(atomic);
//            }
//
//            @Override
//            public boolean eval() {
//                return false;
//            }
//        };
//    }
//
//    protected TermTransform newGet() {
//        if (cacheGet()) {
//            if (externCache == null)
//                externCache = new UnifiedMap<>();
//
//            return new CachedTermTransform(super.newGet(), externCache);
//        } else {
//            return super.newGet();
//        }
//    }
//
//
//    protected boolean cacheGet() {
//        return true;
//    }
//
//}
