package nars.op;

import jcog.bloom.StableBloomFilter;
import jcog.event.Off;
import jcog.pri.PLink;
import jcog.pri.PriMap;
import jcog.pri.PriReference;
import jcog.pri.bag.Bag;
import jcog.pri.bag.Sampler;
import jcog.pri.bag.impl.BufferedBag;
import jcog.pri.bag.impl.PriReferenceArrayBag;
import jcog.pri.op.PriMerge;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.attention.TaskLinkWhat;
import nars.attention.What;
import nars.concept.Concept;
import nars.control.How;
import nars.link.TaskLink;
import nars.term.Term;
import nars.term.Terms;
import nars.time.When;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static nars.Op.*;


/**
 * interface for controlled draining of a bag
 * "leaky bucket" model
 */
public abstract class TaskLeak extends How {



    final TaskSource source;

    /**
     * if empty, listens for all
     */
    private final byte[] puncs;

    transient protected int volMax;

    protected TaskLeak(@Nullable NAR n, byte... puncs) {
        this(new TaskLinksSource(), n, puncs);

    }
    protected TaskLeak(int capacity, @Nullable NAR n, byte... puncs) {
        this(capacity, PriMerge.max, n, puncs);
    }

    protected TaskLeak(int capacity, PriMerge merge, @Nullable NAR n, byte... puncs) {
        this(
                new BufferSource(
                        new BufferedBag.SimpleBufferedBag<>(
                                new PriReferenceArrayBag<>(merge, capacity, PriMap.newMap(false)),
                                new PriMap<>(merge))
                )
                , n, puncs
        );
    }

    TaskLeak(TaskSource src, @Deprecated @Nullable NAR n, byte... puncs) {
        super();

        this.puncs = puncs;

        this.source = src;

        if (n!=null)
            n.start(this);
    }

    /**
     * an adjusted priority of the task for its insertion to the leak bag
     */
    @Deprecated  /* TODO move to TaskEventSource */ protected float pri(Task t) {
        return t.priElseZero();
    }

    /** assumes the input is of the expected punctuation type(s) */
    @Deprecated  /* TODO move to TaskEventSource */ public final float priFiltered(Task t) {
        if (filter(t.term()) && filter(t))
            return pri(t);
        return Float.NaN;
    }

    protected boolean filter(Task next) {
        return true;
    }
    protected boolean filter(Term term) {
        return true;
    }

    /**
     * returns how much of the input was consumed; 0 means nothing, 1 means 100%
     */
    abstract protected float leak(Task next, What what);

    @Override
    protected void starting(NAR nar) {
        super.starting(nar);

        whenDeleted(nar.eventClear.on(this::clear));

        Off off = source.start(this, nar);
        if (off!=null)
            whenDeleted(off);
    }

    @Override
    public void next(What w, BooleanSupplier kontinue) {
        volMax = w.nar.termVolMax.intValue();
        source.next((next) -> leak(next, w), kontinue, w);
    }

    public void clear() {
        source.clear();
    }

    public static abstract class TaskSource {
        protected FloatFunction<Task> pri;

        public void clear() {

        }

        @Nullable abstract public Off starting(TaskLeak t, NAR n);

        public abstract void next(Consumer<Task> each, BooleanSupplier kontinue, What w);

        public Off start(TaskLeak t, NAR nar) {
            this.pri = t::priFiltered; //not t::pri
            return starting(t, nar);
        }
    }

    /**
     * TODO merge with TaskBuffer
     * adds task event's to a bag, leaks elements from the bag by its .sample() methods */
    public static class BufferSource extends TaskSource  {
        protected final Bag<Task, PriReference<Task>> bag;
        @Nullable Consumer<PriReference<Task>> bagUpdateFn = null;

        public BufferSource(Bag<Task, PriReference<Task>> bag) {
            this.bag = bag;
        }

        private void accept(Task t) {

            float p = pri.floatValueOf(t);
            if (p == p)
                bag.putAsync(new PLink<>(t, p));

        }

        @Override
        public void clear() {
            bag.clear();
        }

        @Override
        public Off starting(TaskLeak t, NAR n) {
            return n.onTask(this::accept, t.puncs);
        }

        @Override
        public void next(Consumer<Task> each, BooleanSupplier kontinue, What w) {
            if (!bag.commit(bagUpdateFn).isEmpty()) {
                bag.sample(w.nar.random(), (PriReference<Task> v) -> {
                    Task t = v.get();
                    if (t.isDeleted())
                        return Sampler.SampleReaction.Remove;

                    each.accept(t);

                    return kontinue.getAsBoolean() ? Sampler.SampleReaction.Remove : Sampler.SampleReaction.RemoveAndStop;
                });
            }
        }
    }



    /** samples tasks from the
     *     task tables of active concepts
     *     //tasklink bag of active concepts
     *  TODO configurable "burst" size per visited concept
     * */
    public static class TaskLinksSource extends TaskSource {

        private Predicate<Term> termFilter;
        private Predicate<Task> taskFilter;
        private Random rng;

        private byte[] puncs;
        int nextPunc = 0;
        private NAR nar;
        When when;

        @Override
        public @Nullable Off starting(TaskLeak t, NAR n) {
            this.termFilter = t::filter;
            this.taskFilter = t::filter;
            this.rng = n.random();
            this.puncs = t.puncs;
            this.nar = n;
            if (puncs == null || puncs.length == 0)
                puncs = new byte[] { BELIEF, QUESTION, GOAL, QUEST };
            return null;
        }

        @Override
        public void next(Consumer<Task> each, BooleanSupplier kontinue, What w) {

            when = focus(w.dur());

            StableBloomFilter<Task> filter = Terms.newTaskBloomFilter(rng, ((TaskLinkWhat) w).links.links.size());

            w.sampleUnique(rng, (Predicate<? super TaskLink>)(c)->{

                Term xt = c.from();
                if (termFilter.test(xt)) { //TODO check for impl filters which assume the target is from a Task, ex: dt!=XTERNAL but would be perfectly normal for a concept's target
                    Concept cc = nar.conceptualizeDynamic(xt);
                    if (cc!=null) {
                        Task x = sample(cc);
                        if (x != null && filter.addIfMissing(x))
                            each.accept(x);
                    }
                }

                return kontinue.getAsBoolean();
            });
        }

        /** TODO abstract */
        protected When focus(float dur) {
            long now = nar.time();
            return new When(Math.round(now - dur/2), Math.round(now + dur/2), dur, nar);
        }

        /** TODO use TaskLink as the Task resolver not this custom impl */
        @Deprecated @Nullable private Task sample(Concept c) {
            Term ct = c.term();
            boolean hasTemporal = ct.hasAny(Op.Temporal);

            byte[] p = this.puncs;
            for (int i = 0; i < p.length; i++) {
                Task x = c.table(p[nextPunc]).match(when, null, (Task t) ->{
                    Term tt = t.term();
                    if (hasTemporal && !ct.equals(tt) && !termFilter.test(tt)) //test again for temporal containing target as this was not done when testing concept
                        return false;

                    return taskFilter.test(t);
                }, false);

                if (x!=null)
                    return x;
            }
            return null;
        }
    }
}
