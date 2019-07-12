package nars.task.util;

import jcog.TODO;
import jcog.data.list.FasterList;
import jcog.math.FloatRange;
import jcog.math.IntRange;
import jcog.pri.PriMap;
import jcog.pri.Prioritizable;
import jcog.pri.bag.Bag;
import jcog.pri.bag.impl.BufferedBag;
import jcog.pri.bag.impl.PriArrayBag;
import jcog.pri.op.PriMerge;
import jcog.pri.op.PriReturn;
import jcog.util.ConsumerX;
import nars.NAL;
import nars.NAR;
import nars.Task;
import nars.control.CauseMerge;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * regulates a flow of supplied tasks to a target consumer
 * TODO some of this can be moved to util/
 */
abstract public class PriBuffer<T extends Prioritizable> implements Consumer<T> {




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
    public abstract void commit(ConsumerX<T> target, NAR n);

    public abstract void clear();

    /**
     * known or estimated number of tasks present
     */
    abstract public int size();

    abstract public int capacity();

    /**
     * estimate current utilization: size/capacity (as a value between 0 and 100%)
     */
    public final float load() {
        return ((float)size()) / capacity();
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
        public final T put(T x) {
            each.accept(x);
            return x.isDeleted() ? null : x;
        }

        @Override
        public boolean async(ConsumerX<T> target) {
            each = target;
            return true;
        }

        @Override
        public void commit(ConsumerX<T> target, NAR n) {

        }

        @Override
        public void clear() {

        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public final int capacity() {
            return 1;
        }

        @Override
        public float priMin() {
            return 0;
        }
    }

//    /**
//     * TODO
//     * when the concept index churn rate is low, use the Map to conceptualize quickly
//     * when the concept index churn rate is high, use the Bag with controlled throughput rate
//     * acting as the original NoveltyBag did in OpenNARS
//     * using this the system can shift energy towards exploration (more conceptualization) OR
//     * towards more refined truth (more selectivity in task generation with regard to relatively
//     * stable set of concepts they would add to)
//     */
//    public static abstract class AdaptiveTaskBuffer extends PriBuffer {
//
//    }

    /**
     * buffers in a Map<> for de-duplication prior to a commit that flushes them as input to NAR
     * does not obey adviced capacity
     * TODO find old implementation and re-implement this
     */
    public static class MapTaskBuffer<X extends Task> extends PriBuffer<X> {

        final AtomicLong hit = new AtomicLong(0), miss = new AtomicLong(0);

        private final Map<X, X> tasks;

        public MapTaskBuffer() {
            tasks = PriMap.newMap(true);
        }

        @Override
        public int capacity() {
            return Integer.MAX_VALUE;
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
        public final X put(X n) {
            X p = tasks.putIfAbsent(n, n);
            if (p != null) {
                Task.merge(p, n);
                hit.incrementAndGet();
                return p;
            } else {
                miss.incrementAndGet();
                return n;
            }
        }

        /**
         * TODO time-sensitive
         */
        @Override
        public void commit(ConsumerX<X> target, NAR n) {
            Iterator<X> ii = tasks.values().iterator();
            while (ii.hasNext()) {
                X r = ii.next();
                ii.remove();
                target.accept(r);
            }
        }

        @Override
        public boolean async(ConsumerX<X> target) {
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

        public final IntRange capacity = new IntRange(0, 0, 4 * 1024);

        /**
         * perceptual valve
         * dilation factor
         * input rate
         * tasks per cycle
         */
        public final FloatRange valve = new FloatRange(0.5f, 0, 1);

        private transient long prev = Long.MIN_VALUE;
        /**
         * temporary buffer before input so they can be merged in case of duplicates
         */
        public final Bag<Task, Task> tasks;

        @Override
        public int capacity() {
            return capacity.intValue();
        }

        {
            final PriMerge merge = NAL.tasklinkMerge;
            tasks = new BufferedBag.SimpleBufferedBag<>(new PriArrayBag<>(merge, 0) {
                @Override
                protected int histogramBins(int s) {
                    return 0; //disabled
                }

                /**
                 * merge in the pre-buffer
                 */
                @Override
                protected float merge(Task existing, Task incoming, float incomingPri) {
                    return Task.merge(existing, incoming, merge, CauseMerge.Append, PriReturn.Overflow, true);
                }

            },
                    new PriMap<>(merge) {
                        /**
                         * merge in the post-buffer
                         */
                        @Override
                        protected float merge(Prioritizable existing, Task incoming, float pri, PriMerge merge) {
                            return Task.merge((Task) existing, incoming, merge, CauseMerge.Append, PriReturn.Delta, true);
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
        public void commit(ConsumerX<Task> target, NAR nar) {

            long now = nar.time();
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
                        //one at a time
                        b.pop(null, n, target);
                    } else {
                        //batch
                        int remain = n;
                        int nEach = (int) Math.ceil(((float) remain) / c);

                        Consumer<FasterList<Task>> targetBatched = (batch) -> batch.forEach(target);

                        for (int i = 0; i < c && remain > 0; i++) {
                            int asked = Math.min(remain, nEach);
                            remain -= asked;
                            target.input(b, target, asked, nar.exe,
                                targetBatched,
                                NAL.PRE_SORT_TASK_INPUT_BATCH ? Task.sloppySorter : null
                                );
                        }
                    }

                }
            }


        }


        /**
         * TODO abstract
         */
        protected int batchSize(long dt) {
            return Math.max(1, (int) Math.ceil(dt * capacity() * valve.floatValue()));

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
