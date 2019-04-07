package nars.task.util;

import jcog.TODO;
import jcog.math.FloatRange;
import jcog.math.IntRange;
import jcog.pri.OverflowDistributor;
import jcog.pri.PriMap;
import jcog.pri.Prioritizable;
import jcog.pri.bag.Bag;
import jcog.pri.bag.impl.BufferedBag;
import jcog.pri.bag.impl.PriArrayBag;
import jcog.pri.op.PriMerge;
import jcog.pri.op.PriReturn;
import nars.Param;
import nars.Task;
import nars.control.CauseMerge;
import nars.control.channel.ConsumerX;
import nars.exe.Exec;

import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

/**
 * regulates a flow of supplied tasks to a target consumer
 * TODO some of this can be moved to util/
 */
abstract public class PriBuffer<T extends Prioritizable> implements Consumer<T> {


    public final IntRange capacity = new IntRange(0, 0, 4 * 1024);

    /**
     * returns
     * true if the implementation will manage async target suppliying,
     * false if it needs periodic flushing
     */
    abstract public boolean async(ConsumerX<T> target);

    /**
     * returns the input task, or the existing task if a pending duplicate was present
     */
    public abstract T put(T x);

    //public abstract void commit(long now, Consumer<ITask> target);
    public abstract void commit(long now, ConsumerX<? super T> target);

    public abstract void clear();

    /**
     * known or estimated number of tasks present
     */
    abstract public int size();

    /**
     * calculate or estimate current capacity, as a value between 0 and 100% [0..1.0]
     */
    public final float load() {
        return size() / capacity.floatValue();
    }

    //TODO
    //final AtomicLong in = new AtomicLong(), out = new AtomicLong(), drop = new AtomicLong(), merge = new AtomicLong();

    @Override
    public final void accept(T task) {
        put(task);
    }

    abstract public float priMin();

    /**
     * pass-thru, no buffering
     */
    public static class DirectPriBuffer<T extends Prioritizable> extends PriBuffer<T> {

        private Consumer<T> each = null;

        public DirectPriBuffer() {
        }

        @Override
        public T put(T x) {
            each.accept(x);
            if (x.isDeleted())
                return null;
            else
                return x;
        }

        @Override
        public boolean async(ConsumerX<T> target) {
            each = target;
            return true;
        }

        @Override
        public void commit(long now, ConsumerX<? super T> target) {

        }

        @Override
        public void clear() {

        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public float priMin() {
            return 0;
        }
    }

    /**
     * TODO
     * when the concept index churn rate is low, use the Map to conceptualize quickly
     * when the concept index churn rate is high, use the Bag with controlled throughput rate
     * acting as the original NoveltyBag did in OpenNARS
     * using this the system can shift energy towards exploration (more conceptualization) OR
     * towards more refined truth (more selectivity in task generation with regard to relatively
     * stable set of concepts they would add to)
     */
    public static abstract class AdaptiveTaskBuffer extends PriBuffer {

    }

    /**
     * buffers in a Map<> for de-duplication prior to a commit that flushes them as input to NAR
     * does not obey adviced capacity
     * TODO find old implementation and re-implement this
     */
    public static class MapTaskBuffer extends PriBuffer<Task> {

        private final Map<Task, Task> tasks;

        public MapTaskBuffer(int initialCapacity) {
            capacity.set(initialCapacity);
            tasks = PriMap.newMap(true);
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
        public float priMin() {
            throw new TODO();
        }

        @Override
        public final Task put(Task n) {
            Task p = tasks.putIfAbsent(n, n);
            if (p != null) {
                Task.merge(p, n);
                return p;
            } else
                return n;
        }

        /**
         * TODO time-sensitive
         */
        @Override
        public void commit(long now, ConsumerX<? super Task> target) {
            Iterator<Task> ii = tasks.values().iterator();
            while (ii.hasNext()) {
                target.accept(ii.next());
                ii.remove();
            }
        }

        @Override
        public boolean async(ConsumerX<Task> target) {
            return false;
        }
    }


    /**
     * buffers and deduplicates in a Bag<Task,Task> allowing higher priority inputs to evict
     * lower priority inputs before they are flushed.  commits involve a multi-thread shareable drainer task
     * allowing multiple inputting threads to fill the bag, potentially deduplicating each's results,
     * while other thread(s) drain it in prioritized order as input to NAR.
     */
    public static class BagTaskBuffer extends PriBuffer<Task> {

        /**
         * temporary buffer before input so they can be merged in case of duplicates
         */
        public final Bag<Task, Task> tasks;

        {
            final PriMerge merge = Param.tasklinkMerge;
            tasks = new BufferedBag.SimpleBufferedBag<Task,Task>(new PriArrayBag<>(merge, 0) {

                /** merge in the pre-buffer */
                @Override protected float merge(Task existing, Task incoming, float incomingPri) {
                    return Task.merge(existing, incoming, merge, CauseMerge.Append, PriReturn.Overflow, true);
                }

                @Override
                        protected int histogramBins() {
                            return 0; /* since sampling is not used */
                        }
                    },
                    new PriMap<Task>(merge) {
                        /** merge in the post-buffer */
                        @Override protected void merge(Prioritizable existing, Task incoming, float pri, PriMerge merge, OverflowDistributor<Task> overflow) {
                            Task.merge((Task)existing, incoming, merge, CauseMerge.Append, PriReturn.Post, true);
                        }
                    }
            );
        }
//                new PriArrayBag<ITask>(PriMerge.max, new HashMap()
//                        //new UnifiedMap()
//                ) {
//                    @Override
//                    protected float merge(ITask existing, ITask incoming) {
//                        return TaskBuffer.merge(existing, incoming);
//                    }
//                };

        //new HijackBag...
        /**
         * perceptual valve
         * dilation factor
         * input rate
         * tasks per cycle
         */
        public final FloatRange valve = new FloatRange(1, 0, 512);
        private transient long prev = Long.MIN_VALUE;

        /**
         * @capacity size of buffer for tasks that have been input (and are being de-duplicated) but not yet input.
         * input may happen concurrently (draining the bag) while inputs are inserted from another thread.
         */
        public BagTaskBuffer(int capacity, float rate) {
            this.capacity.set(capacity);
            this.valve.set(rate);
            this.tasks.setCapacity(capacity);
        }

        @Override
        public float priMin() {
            return tasks.isFull() ? tasks.priMin() : 0;
        }

        @Override
        public void clear() {
            tasks.clear();
        }

//        final AtomicBoolean busy = new AtomicBoolean(false);

        @Override
        public int size() {
            return tasks.size();
        }

        @Override
        public final boolean async(ConsumerX<Task> target) {
            return false;
        }

        @Override
        public Task put(Task x) {
            return tasks.put(x);
        }

        @Override
        public void commit(long now, ConsumerX<? super Task> target) {

            if (prev == Long.MIN_VALUE)
                prev = now - 1;

            long dt = now - prev;

            prev = now;

            Bag<Task, Task> b = this.tasks;

            b.setCapacity(capacity.intValue());
            b.commit(null);

            int s = b.size();
            if (s != 0) {
                int n = Math.min(s, batchSize(dt));
                if (n > 0) {
                    //TODO target.input(tasks, n, target.concurrency());

                    int c = target.concurrency();
                    if (c <= 1) {
                        b.pop(null, n, target::accept);
                    } else {
                        int remain = n;
                        int nEach = (int) Math.ceil(((float) remain) / c);
                        for (int i = 0; i < c && remain > 0; i++) {
                            ((Exec) target).input(b, Math.min(remain, nEach));
                            remain -= nEach;
                        }
                    }

                }
            }


        }


        /**
         * TODO abstract
         */
        protected int batchSize(long dt) {
            return 1 /* iff dt==0 */ + (int) Math.ceil(dt * valve.floatValue());

            //rateControl.apply(tasks.size(), tasks.capacity());

//            float v = valve.floatValue();
//            if (v < ScalarValue.EPSILON)
//                return 0;
//            return Math.max(1, Math.round(
//                    //dt * v * tasks.capacity()
//                    v * tasks.capacity()
//            ));
        }


    }


//    public static class BagPuncTasksBuffer extends TaskBuffer {
//
//        public final TaskBuffer belief, goal, question, quest;
//        private final TaskBuffer[] ALL;
//
//        public BagPuncTasksBuffer(int capacity, float rate) {
//            belief = new BagTaskBuffer(capacity, rate);
//            goal = new BagTaskBuffer(capacity, rate);
//            question = new BagTaskBuffer(capacity, rate);
//            quest = new BagTaskBuffer(capacity, rate);
//
//            ALL = new TaskBuffer[]{belief, goal, question, quest};
//
//            this.capacity.set(capacity);
//        }
//
//        @Override
//        public boolean async(ConsumerX<Prioritizable> target) {
//            return false;
//        }
//
//        @Override
//        public float priMin() {
//            float q = question.priMin();
//            if (q < ScalarValue.EPSILON) return 0;
//            float qq = quest.priMin();
//            if (qq < ScalarValue.EPSILON) return 0;
//            float b = belief.priMin();
//            if (b < ScalarValue.EPSILON) return 0;
//            float g = goal.priMin();
//            if (g < ScalarValue.EPSILON) return 0;
//            return Math.min(Math.min(Math.min(b, g), q), qq);
//        }
//
//        private TaskBuffer buffer(byte punc) {
//            switch (punc) {
//                case BELIEF:
//                    return belief;
//                case GOAL:
//                    return goal;
//                case QUESTION:
//                    return question;
//                case QUEST:
//                    return quest;
//                default:
//                    return null;
//            }
//        }
//
//        @Override
//        public void clear() {
//            for (TaskBuffer x : ALL)
//                x.clear();
//        }
//
//        @Override
//        public <T extends ITask> T put(T x) {
//            return buffer(x.punc()).put(x);
//        }
//
//        @Override
//        public void commit(long now, ConsumerX<Prioritizable> target) {
//            //TODO parallelize option
//
//            int c = Math.max(1, capacity.intValue() / ALL.length);
//            for (TaskBuffer x : ALL) {
//                x.capacity.set(c);
//                x.commit(now, target);
//            }
//        }
//
//        @Override
//        public int size() {
//            int s = 0;
//            for (TaskBuffer x : ALL)
//                s += x.size();
//            return s;
//        }
//    }
}
