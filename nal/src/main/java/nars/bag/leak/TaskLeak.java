package nars.bag.leak;

import jcog.event.Off;
import jcog.pri.PLink;
import jcog.pri.PriBuffer;
import jcog.pri.PriReference;
import jcog.pri.bag.Bag;
import jcog.pri.bag.Sampler;
import jcog.pri.bag.impl.BufferedBag;
import jcog.pri.bag.impl.PriReferenceArrayBag;
import jcog.pri.op.PriMerge;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.concept.Concept;
import nars.control.How;
import nars.link.TaskLink;
import nars.term.Term;
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
        this(new TableSource(), n, puncs);

    }
    protected TaskLeak(int capacity, @Nullable NAR n, byte... puncs) {
        this(capacity, PriMerge.max, n, puncs);
    }

    protected TaskLeak(int capacity, PriMerge merge, @Nullable NAR n, byte... puncs) {
        this(
                new BufferSource(
                        new BufferedBag.SimpleBufferedBag<>(
                                new PriReferenceArrayBag<>(merge, capacity, PriBuffer.newMap(false)),
                                new PriBuffer<>(merge))
                )
                , n, puncs
        );
    }

    TaskLeak(TaskSource src, @Nullable NAR n, byte... puncs) {
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
    abstract protected float leak(Task next);

    @Override
    protected void starting(NAR nar) {
        super.starting(nar);

        on(nar.eventClear.on(this::clear));

        Off off = source.start(this, nar);
        if (off!=null)
            on(off);
    }

    @Override
    public void next(NAR nar, BooleanSupplier kontinue) {
        volMax = nar.termVolumeMax.intValue();
        source.next(this::leak, kontinue, nar);
    }

    public void clear() {
        source.clear();
    }

    public static abstract class TaskSource {
        protected FloatFunction<Task> pri;

        public void clear() {

        }

        @Nullable abstract public Off starting(TaskLeak t, NAR n);

        public abstract void next(Consumer<Task> each, BooleanSupplier kontinue, NAR nar);

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
        public void next(Consumer<Task> each, BooleanSupplier kontinue, NAR nar) {
            if (!bag.commit(bagUpdateFn).isEmpty()) {
                bag.sample(nar.random(), (PriReference<Task> v) -> {
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
    public static class TableSource extends TaskSource {

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
        public void next(Consumer<Task> each, BooleanSupplier kontinue, NAR nar) {

            when = focus();

            nar.attn.sample(rng, (Predicate<? super TaskLink>)(c)->{

                if (c == null) return false; //TODO can this even happen

                Concept cc = nar.concept(c.from());
                Term ct = cc.term();

                if (ct.hasAny(Op.Temporal) || termFilter.test(cc.term())) { //TODO check for impl filters which assume the target is from a Task, ex: dt!=XTERNAL but would be perfectly normal for a concept's target
                    Task x = sample(cc);
                    if (x!=null)
                        each.accept(x);
                }

                return kontinue.getAsBoolean();
            });
        }

        /** TODO abstract */
        protected When focus() {
            long now = nar.time();
            int dur = nar.dur();
            return new When(now - dur, now + dur, dur, nar);
        }

        @Nullable private Task sample(Concept c) {


            Term ct = c.term();
            boolean hasTemporal = ct.hasAny(Op.Temporal);

            byte[] p = this.puncs;
            for (int i = 0; i < p.length; i++) {
                Task x = c.table(p[nextPunc]).match(when, null, (Task t) ->{
                    Term tt = t.term();
                    if (!hasTemporal || termFilter.test(tt)) //test temporal containing target as this was not done when testing concept
                        return false;

                    return taskFilter.test(t);
                }).task();

                if (++nextPunc == p.length)
                    nextPunc = 0;

                if (x!=null)
                    return x;
            }
            return null;
        }
    }
}
