package nars.index.term;

import com.google.common.collect.Iterables;
import jcog.bag.Bag;
import jcog.bag.impl.ConcurrentArrayBag;
import jcog.bloom.YesNoMaybe;
import jcog.pri.PLink;
import jcog.pri.PriCache;
import jcog.pri.PriReference;
import jcog.pri.op.PriMerge;
import nars.IO;
import nars.Task;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.index.term.map.MaplikeTermIndex;
import nars.term.Term;
import nars.term.Termed;
import nars.term.var.Variable;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static jcog.Texts.n2;
import static jcog.pri.PriCache.Hold.SOFT;
import static jcog.pri.PriCache.Hold.STRONG;

public class PriMapTermIndex extends MaplikeTermIndex {

    //final IntCountsHistogram conceptScores = new IntCountsHistogram(1000, 2);

    private final PriCache<Term, Concept> concepts;


    /**
     * how many items to visit during update
     */

    final int activeActive = 32;
    final int activeGood = 32;
    final int activeBad = 32;

    static class EntryBag extends ConcurrentArrayBag<Term, PLink<Concept>> {

        public EntryBag(PriMerge mergeFunction, int cap) {
            super(mergeFunction, cap);
        }

        public String summary(String label) {
            return label + " dist=" + n2(histogram(new float[8]));
        }

        @Nullable
        @Override
        public Term key(PLink<Concept> x) {
            return x.get().term();
        }
    }

    //final EntryBag active = new EntryBag(PriMerge.replace, activeActive);
    final EntryBag good = new EntryBag(PriMerge.max, activeGood);
    final EntryBag bad = new EntryBag(PriMerge.max, activeBad);

    public PriMapTermIndex() {
        super();
        this.concepts = new PriCache<>() {


//            public final CountMinSketch hit = new CountMinSketch(1024,10);
//            private void hit(TLink<Term, Concept> y) {
//                int c = hit.countAndAdd(y.hash); //TODO use bigger has for therm
//                System.out.println(y.key + " " + c);
//            }

//            @Override
//            protected TLink<Term, Concept> getLink(Object x) {
//                TLink<Term, Concept> y = super.getLink(x);
//                if (y!=null) {
//                    update(y);
//                }
//                return y;
//            }


            @Override
            protected float updateMemory(float used) {
                System.err.println("memory: " + used + ", concepts=" + super.size());
                return super.updateMemory(used);
            }

            final AtomicBoolean evicting = new AtomicBoolean(false);

            @Override
            public void evict(float strength) {
                if (nar!=null) //HACK
                nar.runLater(() -> {
                    if (!evicting.compareAndSet(false, true))
                        return;
                    try {

                        if (strength > 0) {

                            if (strength > 0.95f) {
                                System.gc();
                            }

                            int nv = bad.size();
                            if (nv > 0) {
                                int kill = Math.round(strength * nv);
                                if (kill > 0) {

                                    System.err.println("evicting " + kill + " victims (" + bad.size() + " remain;\ttotal concepts=" + super.size());
                                    bad.pop(kill, (t) -> {
                                        Concept x = t.get();
                                        if (x != null)
                                            removeGenocidally(x);
                                    });

                                }
                            }
                        }

                        probeEvict(strength, probeRate);

                        //active.commit();

                        good.commit();
                        //System.out.println(good.summary("good"));
                        bad.commit();
                        //System.out.println(bad.summary("bad"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        evicting.set(false);
                    }
                });
            }

            private Iterator<TLink<Term, Concept>> probe;

            /**
             * items per ms
             */
            float probeRate = 1f;

            private void probeEvict(float evictPower, float itemsPerMS) {
                if (concepts == null) return;
                if (probe == null)
                    probe = Iterables.cycle(concepts::linkIterator).iterator();

                int num = Math.round(concepts.cleaner.periodMS.get() * itemsPerMS * evictPower);
                for (int i = 0; probe.hasNext() && i < num; i++) {
                    TLink<Term, Concept> next = probe.next();
                    Concept c = next.get();
                    if (c != null)
                        update(c);
                }

            }


            @Override
            protected Hold mode(Term term, Concept v) {
                if (v instanceof PermanentConcept)
                    return STRONG;
                else
                    return SOFT;
            }

            @Override
            protected void onRemove(Term term, Concept termed) {
                assert (!(termed instanceof PermanentConcept));
                //System.err.println("remove: " + term);
                PriMapTermIndex.this.onRemove(termed);
            }

            /** terrorize the neighborhood graph of a killed victim
             *  by spidering the victim's corpse for its associates
             *  listed in its tasklinks/termlinks */
            private void removeGenocidally(Concept concept) {

                Set<Termed> neighbors = new HashSet<>();

                Consumer<PriReference> victimCollector = (k) -> {
                    Termed t = (Termed) k.get();
                    if (t != null) {
                        neighbors.add(t);
                    }
                };

                concept.tasklinks().forEach(victimCollector);
                concept.termlinks().forEach(victimCollector);

                super.remove(concept.term());

                neighbors.forEach(t -> {
                    Concept c = super.get(t.term());
                    if (c != null) {
                        update(c);
                    }
                });

            }

        };

    }

    @Override
    public int size() {
        return concepts.size();
    }

    @Override
    public @NotNull String summary() {
        return concepts.size() + " concepts";
    }

    @Override
    public void remove(@NotNull Term entry) {
        concepts.remove(entry);
    }

    @Override
    public Stream<? extends Termed> stream() {
        return concepts.values().stream();
    }

    @Nullable
    @Override
    public Termed get(Term x, boolean createIfMissing) {

        //assert(!(x instanceof Variable)): "variables should not be stored in index";
        if (x instanceof Variable)
            return x;

        if (createIfMissing) {
            //HACK when switching to Term,Concept use 'conceptBuilder' itself not this adapter
            return concepts.compute(x, (term, u) -> (Concept) conceptBuilder.apply(term, u));
        } else {
            return concepts.get(x);
        }
    }


    @Deprecated
    public static final BiFunction<? super Termed, ? super Termed, ? extends Concept> setOrReplaceNonPermanent = (prev, next) -> {
        if (prev instanceof PermanentConcept && !(next instanceof PermanentConcept))
            return (Concept) prev;
        return (Concept) next;
    };

    @Override
    public void set(@NotNull Term src, @NotNull Termed target) {
        concepts.merge(src, (Concept) target, setOrReplaceNonPermanent);
    }

    @Override
    public synchronized void clear() {
        //active.clear();
        good.clear();
        bad.clear();
        concepts.clear();
    }

    /**
     * victim pre-filter
     */
    private boolean victimizable(Concept x) {
        if (x instanceof PermanentConcept)
            return false;

        return true;
    }

    final YesNoMaybe<Concept> seenRecently = new YesNoMaybe<>((c) -> {
        float value = value(c);

        if (!good.isFull() || good.priMin() < value) {
            good.putAsync(new PLink(c, value * 0.25f));
        } else {

            if (victimizable(c)) {
                float antivalue = 1f / (1 + Math.max(0.05f, value) * 2);
                if (!bad.isFull() || bad.priMin() < antivalue) {
                    bad.putAsync(new PLink(c, antivalue));
                }
            }
        }
        return true;
    }, c -> IO.termToBytes(c.term()), 1024, 0.01f);

//    static final ToDoubleFunction<Task> taskScore =
//            //t -> t.evi(now, dur);
//            Truthed::conf;

    protected void update(Concept c) {
        seenRecently.test(c);
    }

    private float value(Concept c) {

        //float maxAdmit = -victims.minAdmission();

        float score = 0;

//        score += ((float) c.beliefs().stream().mapToDouble(taskScore).average().orElse(0));
//        score += ((float) c.goals().stream().mapToDouble(taskScore).average().orElse(0));

        long now = nar.time();

        if (c.op().beliefable) {
            Truth bt = c.beliefs().truth(now, nar); //,nar.dur()*8 /* dilated duration perspective */);
            if (bt != null)
                score += bt.conf();
        }

        //if (score > maxAdmit) return Float.NaN; //sufficient, early EXIT

        if (c.op().goalable) {
            Truth gt = c.goals().truth(now, nar);
            if (gt != null)
                score += gt.conf();
        } else {
            score = score * 2; //double any belief score, ie. for ==>
        }

        // link value is divided by the complexity,
        // representing increassed link 'maintenance cost'
        // involved in terms with higher complexity
        // that were ultimately necessary to
        // form and supporting the beliefs and goals counted above,
        // (they are not divided like this)
        int complexity = c.complexity();

        Bag<Task, PriReference<Task>> ta = c.tasklinks();
        score += (ta.size() / (1f + ta.capacity())) / complexity;

        Bag<Term, PriReference<Term>> te = c.termlinks();
        score += (te.size() / (1f + te.capacity())) / complexity;

        return score;
    }

    protected void forget(PriReference<Termed> x, Concept c, float amount) {
        //shrink link bag capacity in proportion to the forget amount
        c.tasklinks().setCapacity(Math.round(c.tasklinks().capacity() * (1f - amount)));
        c.termlinks().setCapacity(Math.round(c.termlinks().capacity() * (1f - amount)));

        x.priMult(1f - amount);
    }


}
