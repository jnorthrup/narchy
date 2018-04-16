//package nars.index;
//
//import nars.concept.AtomConcept;
//import nars.concept.CompoundConcept;
//import nars.concept.Concept;
//import nars.nar.util.DefaultConceptBuilder;
//import nars.term.Termed;
//import nars.term.container.TermContainer;
//import org.cache2k.Cache2kBuilder;
//import org.cache2k.CacheManager;
//import org.cache2k.core.CacheManagerImpl;
//import org.cache2k.core.Entry;
//import org.cache2k.integration.CacheLoader;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.Random;
//import java.util.function.Consumer;
//import java.util.stream.StreamSupport;
//
///**
// * http://cache2k.org/#Integrating_cache2k_in_your_project
// */
//public class Cache2kIndex extends MaplikeIndex {
//
//    @NotNull
//    final ClockProPlusCache data;
//
//    public Cache2kIndex(long cap, @NotNull Random rng) {
//        super(new DefaultConceptBuilder(rng));
//
//        CacheConfiguration cfg = Cache2kBuilder.forUnknownTypes().
//                name("termindex").
//                //refreshAhead(true).
//                        keyType(Object.class).
//                        valueType(Object.class).
//                        eternal(true)
//                .storeByReference(true).
//                        entryCapacity(cap).
//                //keepDataAfterExpired(true).
//                toConfiguration();
//
//        ClockProPlusCache h = new ClockProPlusCache() {
//
//
//            @Override
//            protected void evictEntry(Entry e) {
//                Object o = e.getValue();
//
//                super.evictEntry(e);
//
//                if (isSpecial(o)) {
//                    //reinsert? HACK this should be done as a special case in the loader
//                    put(o, o);
//                } else {
//                    if (o instanceof Concept)
//                        ((Concept)o).delete();
//                }
//            }
//
//            //trying to copy ClockProPlus64
//            @Override
//            protected void recordHit(Entry e) {
//                super.recordHit(e); //HACK
//            }
//        };
//        h.setCacheManager((CacheManagerImpl) CacheManager.getInstance());
//        h.setCacheConfig(cfg);
//        h.setLoader(new NullLoader());
//        h.init();
//        data = h;
//
//
//    }
//
//    @Override
//    public @NotNull String summary() {
//        return size() + " concepts+subterms";
//    }
//
//    @Override
//    public void clear() {
//        data.clear();
//    }
//
//    @Override
//    public int size() {
//        return data.getTotalEntryCount();
//    }
//
//    @Override
//    public int subtermsCount() {
//        return 0;
//    }
//
//    @Override
//    public void remove(Termed entry) {
//        data.remove(entry);
//    }
//
//    @Override
//    public @Nullable Termed get(@NotNull Termed x) {
//        return (Termed) data.get(x);
//    }
//
//
//    @Override
//    public void set(@NotNull Termed src, Termed target) {
//
//        data.put(src, target);
//        if (isSpecial(src)) {
//            //custom concept impl, hardwire
//
//        }
//    }
//
//    public boolean isSpecial(@NotNull Object src) {
//        return (src instanceof CompoundConcept) && (src.getClass()!=CompoundConcept.class)
//                ||
//                (src instanceof AtomConcept) //includes operators
//                ;
//    }
//
//    @Override
//    protected TermContainer put(TermContainer src) {
//        if (data.putIfAbsent(src, src)) {
//            return src;
//        }
//        return (TermContainer) data.get(src); //HACK
//    }
//
//    @Override
//    public void forEach(@NotNull Consumer<? super Termed> cc) {
//        StreamSupport.stream(data.spliterator(), false).filter(x -> x instanceof Termed).
//                forEach(x -> cc.accept((Termed) x));
//    }
//
//    private static class NullLoader extends CacheLoader {
//        @Nullable
//        @Override
//        public Object load(Object o) throws Exception {
//            return null;
//        }
//    }
//
////    @Override
////    public void onEntryExpired(Cache cache, CacheEntry cacheEntry) {
////        System.out.println("expire: " + cacheEntry);
////    }
////
////    @Override
////    public void onEntryRemoved(Cache cache, CacheEntry cacheEntry) {
////        System.out.println("removed: " + cacheEntry);
////    }
//}
