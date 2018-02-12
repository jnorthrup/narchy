package nars.index.term;

import com.google.common.collect.Streams;
import jcog.tree.radix.MyConcurrentRadixTree;
import nars.NAR;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * concurrent radix tree index
 */
public class TreeConceptIndex extends ConceptIndex implements Consumer<NAR> {

    float maxFractionThatCanBeRemovedAtATime = 0.05f;
    float descentRate = 0.5f;
    //int iterationLimit = -1; //TODO tune iterationLimit by the overflow amount

    @NotNull
    public final TermTree concepts;

    int sizeLimit;

    public TreeConceptIndex(int sizeLimit) {

        this.concepts = new TermTree() {

            @Override
            public boolean onRemove(Termed r) {
                if (r instanceof Concept) {
                    Concept c = (Concept) r;
                    if (removeable(c)) {
                        onRemoval((Concept) r);
                        return true;
                    } else {
                        return false;
                    }
                }
                return false;
            }
        };
        this.sizeLimit = sizeLimit;

//        Thread t = new Thread(this, this.toString() + "_Forget");
//        //t.setPriority(Thread.MAX_PRIORITY - 1);
//        t.setUncaughtExceptionHandler((whichThread, e) -> {
//            logger.error("Forget: {}", e);
//            e.printStackTrace();
//            System.exit(1);
//        });
//        t.start();

    }

    @Override
    public Stream<Termed> stream() {
        return Streams.stream( concepts.iterator() );
    }

    @Override
    public void start(@NotNull NAR nar) {
        super.start(nar);

        nar.onCycle(this);
    }

    //    //1. decide how many items to remove, if any
//    //2. search for items to meet this quota and remove them
//    @Override
//    public void run() {
//
//        while (true) {
//
//            try {
//                Thread.sleep(updatePeriodMS);
//            } catch (InterruptedException e) {
//            }
//
//            if (nar != null)
//                forgetNextLater();
//        }
//
//    }
//
//    private void forgetNextLater() {
//        nar.runLater(this::forgetNext);
//    }

    private void forgetNext() {

        int sizeBefore = sizeEst();

        int overflow = sizeBefore - sizeLimit;

        if (overflow < 0)
            return;


        int maxConceptsThatCanBeRemovedAtATime = (int) Math.max(1, sizeBefore * maxFractionThatCanBeRemovedAtATime);

        if (overflow < maxConceptsThatCanBeRemovedAtATime)
            return;

        concepts.acquireWriteLock();
        try {
            MyConcurrentRadixTree.SearchResult s = null;

            while (/*(iterationLimit-- > 0) &&*/ ((sizeEst() - sizeLimit) > maxConceptsThatCanBeRemovedAtATime)) {

                Random rng = nar.random();

                MyConcurrentRadixTree.Node subRoot = volumeWeightedRoot(rng);

                if (s == null)
                    s = concepts.random(subRoot, descentRate, rng);

                MyConcurrentRadixTree.Node f = s.found;

                if (f != null && f != subRoot) {
                    int subTreeSize = concepts.sizeIfLessThan(f, maxConceptsThatCanBeRemovedAtATime);

                    if (subTreeSize > 0) {
                        //long preBatch = sizeEst();
                        concepts.removeHavingAcquiredWriteLock(s, true);
                        //if (logger.isDebugEnabled())
                          //  logger.info("  Forgot {}", preBatch - sizeEst());
                    }

                    s = null; //restart
                }

            }
        } finally {
            concepts.releaseWriteLock();
        }

//        int sizeAfter = sizeEst();
//        logger.info("Forgot {} Concepts", sizeBefore - sizeAfter);

    }

    /**
     * since the terms are sorted by a volume-byte prefix, we can scan for removals in the higher indices of this node
     */
    private MyConcurrentRadixTree.Node volumeWeightedRoot(@NotNull Random rng) {

        List<MyConcurrentRadixTree.Node> l = concepts.root.getOutgoingEdges();
        int levels = l.size();

        //HEURISTIC: x^n sharp curve
        float r = rng.nextFloat();
        r = (r * r); //r^2
        //r = (r * r); //r^4
        //r = (r * r); //r^8

        return l.get( Math.round((levels - 1) * (1 - r)) );
    }

    private int sizeEst() {
        return concepts.sizeEst();
    }

//    /** relative capacitance; >0 = over-capacity, <0 = under-capacity */
//    private float capacitance() {
//        int s = sizeEst();
//        //if (s > sizeLimit) {
//        return s - sizeLimit;
//        //}
//        //return 0;
//    }

    private static boolean removeable(Concept c) {
        return !(c instanceof PermanentConcept);
    }


    @Override
    public @Nullable Termed get(Term t, boolean createIfMissing) {
        TermKey k = TermTree.key(t);

        return createIfMissing ? _get(k, t) : _get(k);
    }

    protected @Nullable Termed _get(TermKey k) {
        return concepts.get(k);
    }

    protected @NotNull Termed _get(TermKey k, Term finalT) {
        return concepts.putIfAbsent(k, () -> nar.conceptBuilder.apply(finalT, null));
    }

    @NotNull
    public static TermKey key(Term t) {
        return TermTree.key(t);
    }


    @Override
    public void set(Term src, Termed target) {

        @NotNull TermKey k = key(src);

        concepts.acquireWriteLock();
        try {
            Termed existing = concepts.get(k); //TODO cache ref to where this resolved to accelerate the put() below
            if (existing!=target && !(existing instanceof PermanentConcept)) {
                concepts.put(k, target);
            }
        } finally {
            concepts.releaseWriteLock();
        }
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("yet");
    }

    @Override
    public void forEach(@NotNull Consumer<? super Termed> c) {
        concepts.forEach(c);
    }

    @Override
    public int size() {
        return concepts.size(); //WARNING may be slow
    }


    @Override
    public @NotNull String summary() {
        //return ((nar.random.nextFloat() < SIZE_UPDATE_PROB) ? (this.lastSize = size()) : ("~" + lastSize)) + " terms";
        return concepts.sizeEst() + " concepts";
    }


    @Override
    public void remove(@NotNull Term entry) {
        TermKey k = key(entry);
        Termed result = concepts.get(k);
        if (result != null) {
            concepts.remove(k);
        }
    }


    protected void onRemoval(@NotNull Concept value) {
        onRemove(value);
    }

    @Override
    public void accept(NAR eachFrame) {
        forgetNext();
    }


//    /**
//     * Tree-index with a front-end "L1" non-blocking hashmap cache
//     */
//    public static class L1TreeIndex extends TreeTermIndex {
//
//        @NotNull
//        private final HijacKache<Term, Termed> L1;
//
//        public L1TreeIndex(ConceptBuilder conceptBuilder, int sizeLimit, int cacheSize, int reprobes) {
//            super(conceptBuilder, sizeLimit);
//            this.L1 = new HijacKache<>(cacheSize, reprobes);
//        }
//
//        @Override
//        public @Nullable Termed get(@NotNull Term t, boolean createIfMissing) {
//
//            Object o = L1.computeIfAbsent2(t,
//                    createIfMissing ?
//                            ttt -> super.get(ttt, true) :
//                            ttt -> {
//                                Termed v = super.get(ttt, false);
//                                if (v == null)
//                                    return L1; //this will result in null at the top level, but the null will not be stored in L1 itself
//                                return v;
//                            }
//            );
//
//            if (o instanceof Termed)
//                return (Termed) o;
//            else if (createIfMissing) { //HACK try again: this should be handled by computeIfAbsent2, not here
//                L1.miss++;
//                return super.get(t, true);
//            } else {
//                return null;
//            }
//        }
//
//        @Override
//        public @NotNull String summary() {
//            return super.summary() + "\t, L1:" + L1.summary(true);
//        }
//
//        @Override
//        protected void onRemoval(Concept r) {
//            super.onRemoval(r);
//            L1.remove(r);
//        }
//    }
}
