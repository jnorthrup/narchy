package nars.task.util;

import jcog.math.FloatRange;
import jcog.math.IntRange;
import jcog.pri.bag.impl.PriArrayBag;
import jcog.pri.op.PriMerge;
import nars.NAR;
import nars.Task;
import nars.control.CauseMerge;
import nars.task.ITask;
import nars.task.NALTask;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

abstract public class TaskBuffer  {



    public abstract Task add(Task x);

    public abstract void commit(NAR n);

    /** known or estimated number of tasks present */
    abstract public int size();

    public final IntRange capacity = new IntRange(0, 0, 4*1024);

    /** calculate or estimate current capacity, as a value between 0 and 100% [0..1.0] */
    public final float volume() {
        return size() / capacity.floatValue();
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

        @Override
        public void commit(NAR n) {
            Iterator<Task> ii = tasks.values().iterator();
            while (ii.hasNext()) {
                n.input(ii.next());
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

        final boolean inlineOrDeferredInput;
        /**
         * temporary buffer before input so they can be merged in case of duplicates
         */
        public final PriArrayBag<Task> tasks =
                new PriArrayBag<Task>(PriMerge.max, new HashMap()) {
                    @Override
                    protected float merge(Task existing, Task incoming) {
                        return BagTasksBuffer.this.merge(existing, incoming);
                    }
                };
                //new HijackBag...

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




        /** max percent of capacity allowed input */
        public final FloatRange drainRate = new FloatRange(0.5f, 0, 1f);

        private final TaskBagDrainer tasksDrainer = new TaskBagDrainer(tasks, true,
                (size, capacity) -> Math.min(size, Math.round(capacity * drainRate.floatValue()))
        );


        public BagTasksBuffer(int capacity, float rate) {
            this(capacity, rate, true);
        }


        /**
         * @capacity size of buffer for tasks that have been input (and are being de-duplicated) but not yet input.
         * input may happen concurrently (draining the bag) while inputs are inserted from another thread.
         */
        public BagTasksBuffer(int capacity, float rate, boolean inlineOrDeferredInput) {
            this.capacity.set(capacity);
            this.inlineOrDeferredInput = inlineOrDeferredInput;
            this.drainRate.set(rate);
            this.tasks.setCapacity(capacity);
        }

        @Override
        public Task add(Task x) {
            return tasks.put(x);
        }

        @Override
        public void commit(NAR nar) {

            tasks.setCapacity(capacity.intValue());

            int s = tasks.size();

            //System.out.println(this + " " + s);

            if (s > 0) {


                if (inlineOrDeferredInput) {
                    ITask.run(tasksDrainer, nar); //inline
                } else {
                    nar.exe.execute(tasksDrainer);
                }


            }
        }
    }

}
