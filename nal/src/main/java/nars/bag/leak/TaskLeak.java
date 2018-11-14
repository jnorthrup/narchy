package nars.bag.leak;

import jcog.event.Off;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import jcog.pri.bag.Bag;
import jcog.pri.bag.Sampler;
import jcog.pri.bag.impl.PLinkArrayBag;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.exe.Causable;
import nars.term.Term;
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
public abstract class TaskLeak extends Causable {



    final TaskSource source;

    /**
     * if empty, listens for all
     */
    private final byte[] puncs;

    protected TaskLeak(@Nullable NAR n, byte... puncs) {
        this(new TaskTableSource(), n, puncs);

    }
    protected TaskLeak(int capacity, @Nullable NAR n, byte... puncs) {
        this(
                new BaggedTaskEvents(//new FastPutProxyBag<>(
                        new PLinkArrayBag<>(Param.taskMerge, capacity)
                        //Runtime.getRuntime().availableProcessors()*128)
                )
                , n, puncs
        );
    }

    TaskLeak(TaskSource src, @Nullable NAR n, byte... puncs) {
        super();

        this.puncs = puncs;

        this.source = src;

        if (n!=null)
            n.on(this);
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
        Off off = source.start(this, nar);
        if (off!=null)
            on(off);
    }

    @Override
    public final void clear() {
        source.clear();
    }

    @Override
    protected void next(NAR nar, BooleanSupplier kontinue) {
        source.next(kontinue, nar, this::leak);
    }

    public static abstract class TaskSource {
        protected FloatFunction<Task> pri;

        public void clear() {

        }

        @Nullable abstract public Off starting(TaskLeak t, NAR n);

        public abstract void next(BooleanSupplier kontinue, NAR nar, Consumer<Task> each);

        public Off start(TaskLeak t, NAR nar) {
            this.pri = t::priFiltered; //not t::pri
            return starting(t, nar);
        }
    }

    /** adds task event's to a bag, leaks elements from the bag by its .sample() methods */
    public static class BaggedTaskEvents extends TaskSource  {
        protected final Bag<Task, PriReference<Task>> bag;
        @Nullable Consumer<PriReference<Task>> bagUpdateFn = null;

        public BaggedTaskEvents(Bag<Task, PriReference<Task>> bag) {
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
        public void next(BooleanSupplier kontinue, NAR nar, Consumer<Task> each) {
            if (!bag.commit(bagUpdateFn).isEmpty()) {
                Random rng = nar.random();
                bag.sample(rng, (PriReference<Task> v) -> {
                    Task t = v.get();
                    if (t.isDeleted())
                        return Sampler.SampleReaction.Remove;

                    each.accept(t);

                    return kontinue.getAsBoolean() ? Sampler.SampleReaction.Remove : Sampler.SampleReaction.RemoveAndStop;
                });
            } else {
//        if (bag.isEmpty())
//            sleepRemainderOfCycle();
            }
        }
    }



    /** samples tasks from the
     *     task tables of active concepts
     *     //tasklink bag of active concepts
     *  TODO configurable "burst" size per visited concept
     * */
    public static class TaskTableSource extends TaskSource {

        private Predicate<Term> termFilter;
        private Predicate<Task> taskFilter;
        private Random rng;

        private byte[] puncs;
        int nextPunc = 0;
        private NAR nar;

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
        public void next(BooleanSupplier kontinue, NAR nar, Consumer<Task> each) {
            nar.attn.concepts.sample(rng, (Predicate<Concept>)(c)->{

                if (c == null) return false; //TODO can this even happen

                if (termFilter.test(c.term())) { //TODO check for impl filters which assume the term is from a Task, ex: dt!=XTERNAL but would be perfectly normal for a concept's term
                    Task x = sample(c);
                    if (x!=null)
                        each.accept(x);
                }
                return kontinue.getAsBoolean();
            });
        }

        /** TODO abstract */
        protected long[] focus() {
            long now = nar.time();
            int dur = nar.dur();
            return new long[] {
                    //now - dur/2, now + dur/2
                    now - dur, now + dur
            };
        }

        @Nullable private Task sample(Concept c) {
            long[] when = focus();

            Term ct = c.term();
            boolean hasTemporal = ct.hasAny(Op.Temporal);

            byte[] p = this.puncs;
            for (int i = 0; i < p.length; i++) {
                Task x = c.table(p[nextPunc]).sample(when[0], when[1], null, t ->{
                    Term tt = t.term();
                    if (hasTemporal && !ct.equals(tt))
                        if (!termFilter.test(tt)) //re-test
                            return false;

                    return taskFilter.test(t);
                }, nar);
                if (x!=null)
                    return x;
                nextPunc++;
                if (nextPunc == p.length)
                    nextPunc = 0;
            }
            return null;
        }
    }
}
