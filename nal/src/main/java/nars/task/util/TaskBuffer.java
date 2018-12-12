package nars.task.util;

import jcog.data.list.FasterList;
import jcog.math.FloatRange;
import jcog.math.IntRange;
import jcog.pri.bag.Bag;
import jcog.pri.bag.impl.ArrayBag;
import jcog.pri.bag.impl.PriArrayBag;
import jcog.pri.op.PriMerge;
import nars.Task;
import nars.control.CauseMerge;
import nars.task.ITask;
import nars.task.NALTask;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static nars.Op.*;

/** regulates a flow of supplied tasks to a target consumer */
abstract public class TaskBuffer implements Consumer<Task> {



    /** returns the input task, or the existing task if a pending duplicate was present */
    public abstract Task add(Task x);

    public abstract void commit(long now, int dur, Consumer<Task> target);

    public abstract void clear();

    /** known or estimated number of tasks present */
    abstract public int size();

    public final IntRange capacity = new IntRange(0, 0, 4*1024);

    /** calculate or estimate current capacity, as a value between 0 and 100% [0..1.0] */
    public final float volume() {
        return size() / capacity.floatValue();
    }

    @Override
    public final void accept(Task task) {
        add(task);
    }

    //TODO
    //final AtomicLong in = new AtomicLong(), out = new AtomicLong(), drop = new AtomicLong(), merge = new AtomicLong();


    public final float merge(Task pp, Task tt) {
        if (pp == tt)
            return 0;

        if (pp instanceof NALTask)
            ((NALTask) pp).priCauseMerge(tt, CauseMerge.AppendUnique);
        else
            pp.priMax(tt.pri()); //just without cause update

        if (pp.isCyclic() && !tt.isCyclic())
            pp.setCyclic(false);

        return 0; //TODO calculate

    }

    /**
     * TODO this is trivial. just input directly to NAR on add
     */
    public static abstract class TaskBufferDirect extends TaskBuffer {
        @Override
        public int size() {
            return 0;
        }
    }

    /** TODO
     *  when the concept index churn rate is low, use the Map to conceptualize quickly
     *  when the concept index churn rate is high, use the Bag with controlled throughput rate
     *    acting as the original NoveltyBag did in OpenNARS
     *  using this the system can shift energy towards exploration (more conceptualization) OR
     *    towards more refined truth (more selectivity in task generation with regard to relatively
     *    stable set of concepts they would add to)
     */
    public static abstract class AdaptiveTaskBuffer extends TaskBuffer {

    }

    /**
     * buffers in a Map<> for de-duplication prior to a commit that flushes them as input to NAR
     * does not obey adviced capacity
     * TODO find old implementation and re-implement this
     */
    public static class MapTaskBuffer extends TaskBuffer {
        private final Map<Task, Task> tasks;

        public MapTaskBuffer(int initialCapacity) {
            capacity.set(initialCapacity);
             tasks = new ConcurrentHashMap<>(initialCapacity, 0.99f);
        }

        @Override
        public void clear() {
            tasks.clear();
        }

        @Override
        public int size() {
            return tasks.size();
        }

        @Override
        public Task add(Task n) {
            Task p = tasks.putIfAbsent(n, n);
            if (p!=null) {
                merge(p, n);
                return p;
            }
            return n;
        }

        /** TODO time-sensitive */
        @Override public void commit(long now, int dur, Consumer<Task> target) {
            Iterator<Task> ii = tasks.values().iterator();
            while (ii.hasNext()) {
                target.accept(ii.next());
                ii.remove();
            }
        }
    }


    /**
     * buffers and deduplicates in a Bag<Task,Task> allowing higher priority inputs to evict
     * lower priority inputs before they are flushed.  commits involve a multi-thread shareable drainer task
     * allowing multiple inputting threads to fill the bag, potentially deduplicating each's results,
     * while other thread(s) drain it in prioritized order as input to NAR.
     */
    public static class BagTasksBuffer extends TaskBuffer {


        /**
         * temporary buffer before input so they can be merged in case of duplicates
         */
        public final Bag<Task, Task> tasks =
                new PriArrayBag<Task>(PriMerge.max,
                        //new HashMap()
                        new UnifiedMap()
                ) {
                    @Override
                    protected float merge(Task existing, Task incoming) {
                        return BagTasksBuffer.this.merge(existing, incoming);
                    }
                };

        //new HijackBag...

        @Override
        public void clear() {
            tasks.clear();
        }

        @Override
        public int size() {
            return tasks.size();
        }

        //            @Override
//            protected boolean fastMergeMaxReject() {
//                return true;
//            }

        //            @Override
//            public Task put(Task incoming, NumberX overflow) {
//                //fast merge intercept: avoids synchronization in normal insert procedure
//                Task existing = map.get(incoming);
//                if (existing!=null) {
//                    DERIVATION_MERGE.apply(existing, incoming);
//                    //TODO sort if order changed
//                    return existing;
//                }
//
//
//                return super.put(incoming, overflow);
//            }
//
//            /** returning null elides lookup which was already performed on the intercept */
//            @Override protected Task getExisting(Task key) {
//                return null;
//            }
//            @Override
//            protected float merge(Task existing, Task incoming) {
//                throw new UnsupportedOperationException("should not reach here");
//            }


        /**
         * perceptual valve
         * dilation factor
         * input rate
         * proportional to # of capacities/durations of time
         */
        public final FloatRange valve = new FloatRange(0.5f, 0, 1f);

        final AtomicBoolean busy = new AtomicBoolean(false);

        static final ThreadLocal<FasterList<ITask>> batch = ThreadLocal.withInitial(FasterList::new);

        private transient long prev = Long.MIN_VALUE;

        float commitDurs = 4;

        float durRemain = commitDurs;

        /**
         * @capacity size of buffer for tasks that have been input (and are being de-duplicated) but not yet input.
         * input may happen concurrently (draining the bag) while inputs are inserted from another thread.
         */
        public BagTasksBuffer(int capacity, float rate) {
            this.capacity.set(capacity);
            this.valve.set(rate);
            this.tasks.setCapacity(capacity);
        }

        @Override
        public Task add(Task x) {
            return tasks.put(x);
        }

        @Override
        public void commit(long now, int dur, Consumer<Task> target) {

            if (!busy.compareAndSet(false, true))
                return; //an operation is in-progress

            try {

                if (prev == Long.MIN_VALUE)
                    prev = now - 1;

                long dt = now - prev;
                if (dt == 0)
                    return;

                prev = now;

                tasks.setCapacity(capacity.intValue());

                int s = tasks.size();
                if (s == 0)
                    return;



                float dDur = (float) (((double) dt) / dur);

                durRemain -= dDur;
                if (durRemain < 0) {
                    commitDur();
                    durRemain = Math.max(0, (durRemain + commitDurs));
                }

                if (!tasks.isEmpty()) {
                    int n = batchSize(dDur);
                    if (n > 0) {

                        if (tasks instanceof ArrayBag) {
                            FasterList<ITask> batch = BagTasksBuffer.batch.get();
                            ((ArrayBag) tasks).popBatch(n, batch);
                            if (!batch.isEmpty()) {
                                batch.forEach(target);
                                batch.clear();
                            }

                        } else {
                            tasks.pop(null, n, target); //per item.. may be slow
                        }
                    }
                }

            } finally {
                busy.set(false);
            }

        }

        protected void commitDur() {
            tasks.commit(null /* no forget */);
        }

        /**  TODO abstract */
        protected int batchSize(float dtDurs) {
            //rateControl.apply(tasks.size(), tasks.capacity());
            return Math.max(1,Math.round(dtDurs * valve.floatValue() * tasks.capacity()));
        }
    }



    public static class BagPuncTasksBuffer extends TaskBuffer {

        public final TaskBuffer belief, goal, question, quest;
        private final TaskBuffer[] ALL;

        public BagPuncTasksBuffer(int capacity, float rate) {
            belief = new BagTasksBuffer(capacity, rate) {
                @Override
                protected void commitDur() {
                    //valve.set(...)
                    super.commitDur();
                }
            };
            goal = new BagTasksBuffer(capacity, rate);
            question = new BagTasksBuffer(capacity, rate);
            quest = new BagTasksBuffer(capacity, rate);

            ALL = new TaskBuffer[] {belief, goal, question, quest};

            this.capacity.set(capacity);
        }

        private TaskBuffer buffer(byte punc) {
            switch (punc) {
                case BELIEF:
                    return belief;
                case GOAL:
                    return goal;
                case QUESTION:
                    return question;
                case QUEST:
                    return quest;
                default:
                    return null;
            }
        }

        @Override
        public void clear() {
            for (TaskBuffer x : ALL)
                x.clear();
        }

        @Override
        public Task add(Task x) {
            return buffer(x.punc()).add(x);
        }

        @Override
        public void commit(long now, int dur, Consumer<Task> target) {
            //TODO parallelize

            int c = Math.max(1, capacity.intValue() / ALL.length);
            for (TaskBuffer x : ALL) {
                x.capacity.set(c);
                x.commit(now, dur, target);
            }
        }


        @Override
        public int size() {
            int s = 0;
            for (TaskBuffer x : ALL)
                s += x.size();
            return s;
        }
    }
}
