//package nars.index.concept;
//
//import com.google.common.collect.Iterables;
//import jcog.bloom.YesNoMaybe;
//import jcog.data.list.table.Table;
//import jcog.pri.PLink;
//import jcog.pri.PriCache;
//import jcog.pri.PriReference;
//import jcog.pri.bag.impl.ConcurrentArrayBag;
//import jcog.pri.op.PriMerge;
//import nars.IO;
//import nars.concept.Concept;
//import nars.concept.PermanentConcept;
//import nars.target.Term;
//import nars.target.Termed;
//import nars.target.Variable;
//import nars.truth.Truth;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.HashSet;
//import java.util.Iterator;
//import java.util.Set;
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.function.BiFunction;
//import java.util.function.Consumer;
//import java.util.stream.Stream;
//
//import static jcog.Texts.n2;
//import static jcog.pri.PriCache.Hold.SOFT;
//import static jcog.pri.PriCache.Hold.STRONG;
//
//public class PriMapConceptIndex extends MaplikeConceptIndex {
//
//
//
//    private final PriCache<Term, Concept> concepts;
//
//
//    /**
//     * how many items to visit during update
//     */
//
//    final int activeActive = 32;
//    final int activeGood = 32;
//    final int activeBad = 32;
//
//    static class EntryBag extends ConcurrentArrayBag<Term, PLink<Concept>> {
//
//        public EntryBag(PriMerge mergeFunction, int cap) {
//            super(mergeFunction, cap);
//        }
//
//        public String summary(String label) {
//            return label + " dist=" + n2(histogram(new float[8]));
//        }
//
//        @Nullable
//        @Override
//        public Term key(PLink<Concept> x) {
//            return x.id.target();
//        }
//    }
//
//
//    final EntryBag good = new EntryBag(PriMerge.max, activeGood);
//    final EntryBag bad = new EntryBag(PriMerge.max, activeBad);
//
//    public PriMapConceptIndex() {
//        super();
//        this.concepts = new PriCache<>() {
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//            @Override
//            protected float updateMemory(float used) {
//                System.err.println("memory: " + used + ", concepts=" + super.size());
//                return super.updateMemory(used);
//            }
//
//            final AtomicBoolean evicting = new AtomicBoolean(false);
//
//            @Override
//            public void evict(float strength) {
//                if (nar!=null)
//                nar.runLater(() -> {
//                    if (!evicting.compareAndSet(false, true))
//                        return;
//                    try {
//
//                        if (strength > 0) {
//
//                            if (strength > 0.95f) {
//                                System.gc();
//                            }
//
//                            int nv = bad.size();
//                            if (nv > 0) {
//                                int kill = Math.round(strength * nv);
//                                if (kill > 0) {
//
//                                    System.err.println("evicting " + kill + " victims (" + bad.size() + " remain;\ttotal concepts=" + super.size());
//                                    bad.pop(nar.random(), kill, (t) -> {
//                                        Concept x = t.id;
//                                        if (x != null)
//                                            removeGenocidally(x);
//                                    });
//
//                                }
//                            }
//                        }
//
//                        probeEvict(strength, probeRate);
//
//
//
//                        good.commit();
//
//                        bad.commit();
//
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    } finally {
//                        evicting.setAt(false);
//                    }
//                });
//            }
//
//            private Iterator<TLink<Term, Concept>> probe;
//
//            /**
//             * items per ms
//             */
//            float probeRate = 1f;
//
//            private void probeEvict(float evictPower, float itemsPerMS) {
//                if (concepts == null) return;
//                if (probe == null)
//                    probe = Iterables.cycle(concepts::linkIterator).iterator();
//
//                int num = Math.round(concepts.cleaner.periodMS.intValue() * itemsPerMS * evictPower);
//                for (int i = 0; probe.hasNext() && i < num; i++) {
//                    TLink<Term, Concept> next = probe.next();
//                    Concept c = next.get();
//                    if (c != null)
//                        update(c);
//                }
//
//            }
//
//
//            @Override
//            protected Hold mode(Term target, Concept v) {
//                if (v instanceof PermanentConcept)
//                    return STRONG;
//                else
//                    return SOFT;
//            }
//
//            @Override
//            protected void onRemove(Term target, Concept termed) {
//                assert (!(termed instanceof PermanentConcept));
//
//                PriMapConceptIndex.this.onRemove(termed);
//            }
//
//            /** terrorize the neighborhood graph of a killed victim
//             *  by spidering the victim's corpse for its associates
//             *  listed in its tasklinks/termlinks */
//            private void removeGenocidally(Concept concept) {
//
//                Set<Termed> neighbors = new HashSet<>();
//
//                Consumer<PriReference<Term>> victimCollector = (k) -> {
//                    Term t = k.get();
//                    if (t != null) {
//                        neighbors.addAt(t);
//                    }
//                };
//
//
//                concept.termlinks().forEach(victimCollector);
//
//                super.remove(concept.target());
//
//                neighbors.forEach(t -> {
//                    Concept c = super.get(t.target());
//                    if (c != null) {
//                        update(c);
//                    }
//                });
//
//            }
//
//        };
//
//    }
//
//    @Override
//    public int size() {
//        return concepts.size();
//    }
//
//    @Override
//    public @NotNull String summary() {
//        return concepts.size() + " concepts";
//    }
//
//    @Override
//    public Termed remove(@NotNull Term entry) {
//        concepts.remove(entry);
//        return null;
//    }
//
//    @Override
//    public Stream<Termed> stream() {
//        return concepts.values().stream().map(x -> (Termed)x); //HACK
//    }
//
//    @Nullable
//    @Override
//    public Termed get(Term x, boolean createIfMissing) {
//
//
//        if (x instanceof Variable)
//            return x;
//
//        if (createIfMissing) {
//
//            return concepts.compute(x, (target, u) -> (Concept) nar.conceptBuilder.apply(target, u));
//        } else {
//            return concepts.get(x);
//        }
//    }
//
//
//    @Deprecated
//    public static final BiFunction<? super Termed, ? super Termed, ? extends Concept> setOrReplaceNonPermanent = (prev, next) -> {
//        if (prev instanceof PermanentConcept && !(next instanceof PermanentConcept))
//            return (Concept) prev;
//        return (Concept) next;
//    };
//
//    @Override
//    public void setAt(@NotNull Term src, @NotNull Termed target) {
//        concepts.merge(src, (Concept) target, setOrReplaceNonPermanent);
//    }
//
//    @Override
//    public synchronized void clear() {
//
//        good.clear();
//        bad.clear();
//        concepts.clear();
//    }
//
//    /**
//     * victim pre-filter
//     */
//    private boolean victimizable(Concept x) {
//        return !(x instanceof PermanentConcept);
//    }
//
//    final YesNoMaybe<Concept> seenRecently = new YesNoMaybe<>((c) -> {
//        float value = value(c);
//
//        if (!good.isFull() || good.priMin() < value) {
//            good.putAsync(new PLink(c, value * 0.25f));
//        } else {
//
//            if (victimizable(c)) {
//                float antivalue = 1f / (1 + Math.max(0.05f, value) * 2);
//                if (!bad.isFull() || bad.priMin() < antivalue) {
//                    bad.putAsync(new PLink(c, antivalue));
//                }
//            }
//        }
//        return true;
//    }, c -> IO.termToBytes(c.target()), 1024, 0.01f);
//
//
//
//
//
//    protected void update(Concept c) {
//        seenRecently.test(c);
//    }
//
//    private float value(Concept c) {
//
//
//
//        float score = 0;
//
//
//
//
//        long now = nar.time();
//
//        if (c.op().beliefable) {
//            Truth bt = c.beliefs().truth(now, nar);
//            if (bt != null)
//                score += bt.conf();
//        }
//
//
//
//        if (c.op().goalable) {
//            Truth gt = c.goals().truth(now, nar);
//            if (gt != null)
//                score += gt.conf();
//        } else {
//            score = score * 2;
//        }
//
//
//
//
//
//
//
//        int complexity = c.complexity();
//
//        Table<?,nars.link.TaskLink> ta = c.tasklinks();
//        score += (ta.size() / (1f + ta.capacity())) / complexity;
//
//        Table<nars.target.Term,PriReference<Term>> te = c.termlinks();
//        score += (te.size() / (1f + te.capacity())) / complexity;
//
//        return score;
//    }
//
//    protected void forget(PriReference<Termed> x, Concept c, float amount) {
//
//        c.tasklinks().setCapacity(Math.round(c.tasklinks().capacity() * (1f - amount)));
//        c.termlinks().setCapacity(Math.round(c.termlinks().capacity() * (1f - amount)));
//
//        x.priMult(1f - amount);
//    }
//
//
//}
