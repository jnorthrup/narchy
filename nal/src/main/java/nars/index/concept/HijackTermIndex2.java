//package nars.index.term;
//
//import jcog.Util;
//import jcog.bag.impl.hijack.PLinkHijackBag;
//import jcog.memoize.HijackMemoize;
//import jcog.pri.PLink;
//import jcog.pri.PriReference;
//import nars.NAR;
//import nars.Param;
//import nars.concept.Concept;
//import nars.concept.PermanentConcept;
//import nars.index.term.map.MaplikeTermIndex;
//import nars.term.Term;
//import nars.term.Termed;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.concurrent.atomic.AtomicReferenceArray;
//import java.util.function.Consumer;
//
///**
// * Created by me on 2/20/17.
// */
//public class HijackTermIndex2 extends MaplikeTermIndex implements Runnable {
//
//    private final PLinkHijackBag<Termed> table;
//    //private final Map<Term,Termed> permanent = new ConcurrentHashMap<>(1024);
//
//    private boolean running;
//
//    private final long updatePeriodMS;
//
//    /** current update index */
//    private int visit;
//
//    /** how many items to visit during update */
//    private final int updateBatchSize;
//    private final float initial = 0.5f;
//    private final float getBoost = 0.02f;
//    private final float forget = 0.05f;
//
//    public HijackTermIndex2(int capacity, int reprobes) {
//        super();
//
//        updateBatchSize = 4096; //1 + (capacity / (reprobes * 2));
//        updatePeriodMS = 100;
//
//        this.table = new HijackMemoize<Term,Termed>((t)->{
//            return this.conceptBuilder.apply(t);
//        }, capacity, reprobes);
//
//    }
//
//    @Override
//    public void start(NAR nar) {
//        super.start(nar);
//    }
//
//    @Override
//    public void commit(Concept c) {
//        get(c.term(), false); //get boost
//    }
//
//    @Override
//    public @Nullable Termed get(@NotNull Term key, boolean createIfMissing) {
//        @Nullable PriReference<Termed> x = table.get(key);
//        if (x != null) {
//            x.priAdd(getBoost);
//            return x.get(); //cache hit
//        } else {
//
//            if (createIfMissing) {
//                Termed kc = conceptBuilder.apply(key);
//                if (kc!=null) {
//                    PriReference<Termed> inserted = table.put(new PLink<>(kc, initial));
//                    if (inserted != null) {
//                        return kc;
////                        Termed ig = inserted.get();
////                        if (ig.term().equals(kc.term()))
////                            return ig;
//                    } else {
//                        return null;
//                    }
//                }
//            }
//        }
//
//        return null;
//    }
//
//
//    @Override
//    public void set(@NotNull Term src, Termed target) {
//        remove(src);
//        PriReference<Termed> inserted = table.put(new PLink<>(target, 1f));
//        if (inserted == null && target instanceof PermanentConcept) {
//            throw new RuntimeException("unresolvable hash collision between PermanentConcepts: " + target);
//        }
//    }
//
//    @Override
//    public void clear() {
//        table.clear();
//    }
//
//    @Override
//    public void forEach(@NotNull Consumer<? super Termed> c) {
//        //TODO make sure this doesnt visit a term twice appearing in both tables but its ok for now
//        table.forEachKey(c);
//        //permanent.values().forEach(c);
//    }
//
//    @Override
//    public int size() {
//        return table.size(); /** approx since permanent not considered */
//    }
//
//    @Override
//    public @NotNull String summary() {
//        return table.size() + " concepts"; // (" + permanent.size() + " permanent)";
//    }
//
//    @Override
//    public void remove(@NotNull Term entry) {
//        table.remove(entry);
//        //permanent.remove(entry);
//    }
//
//    @Override
//    public void run() {
//        while (running) {
//
//            AtomicReferenceArray<PriReference<Termed>> tt = table.map.get();
//
//            int c = tt.length();
//
//            int visit = this.visit;
//            int n = updateBatchSize;
//
//            for (int i = 0; i < n; i++ , visit++) {
//
//                if (visit >= c) visit = 0;
//
//                PriReference<Termed> x = tt.get(visit);
//                if (x!=null)
//                    update(x);
//            }
//
//            this.visit = visit;
//
//            //Util.pause(updatePeriodMS);
//            Util.sleep(updatePeriodMS);
//        }
//    }
//
//    protected void update(PriReference<Termed> x) {
//
//        //TODO better update function based on Concept features
//        Termed c = x.get();
//        if (!(c instanceof PermanentConcept)) {
//            float decayRate = c.complexity() / ((float)Param.COMPOUND_VOLUME_MAX);
//                    // / (1f + c.beliefs().priSum() + c.goals().priSum());
//            x.priMult(1f - forget * Util.unitize(decayRate));
//        }
//    }
//
//}
