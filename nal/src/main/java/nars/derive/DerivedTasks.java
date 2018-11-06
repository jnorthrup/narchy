package nars.derive;

import jcog.math.FloatRange;
import jcog.pri.bag.impl.PriArrayBag;
import jcog.pri.op.PriMerge;
import nars.NAR;
import nars.Task;
import nars.task.ITask;
import nars.task.NALTask;
import nars.task.util.TaskBagDrainer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface DerivedTasks extends PriMerge<Task, Task> {
    Task add(Task x, Derivation d);

    void commit(NAR n);

    @Override
    default float merge(Task pp, Task tt) {
        if (pp == tt)
            return 0;

        if (pp instanceof NALTask)
            ((NALTask) pp).priCauseMerge(tt);
        else
            pp.priMax(tt.pri()); //just without cause update

        if (pp.isCyclic() && !tt.isCyclic())
            pp.setCyclic(false);

        return 0; //TODO calculate

    }

    /**
     * TODO this is trivial. just input directly to NAR on add
     */
    abstract class DerivedTasksDirect implements DerivedTasks {

    }

    /** TODO
     *  when the concept index churn rate is low, use the DerivedTasksMap to conceptualize quickly
     *  when the concept index churn rate is high, use the DerivedTasksBag with controlled throughput rate
     *    acting as the original NoveltyBag did in OpenNARS
     *  using this the system can shift energy towards exploration (more conceptualization) OR
     *    towards more refined truth (more selectivity in task generation with regard to relatively
     *    stable set of concepts they would add to)
     */
    abstract class AdaptiveDerivedTasks {

    }

    /**
     * buffers derivations in a Map<> for de-duplication prior to a commit that flushes them as input to NAR
     * TODO find old implementation and re-implement this
     */
    class DerivedTasksMap implements DerivedTasks {
        private final Map<Task, Task> derivedTasks;

        public DerivedTasksMap(int initialCapacity) {
             derivedTasks = new ConcurrentHashMap<>(initialCapacity, 0.99f);
        }

        @Override
        public Task add(Task n, Derivation d) {
//            return derivedTasks.compute(x, (p, n)->{
//                merge(p, n);
//                return p;
//            });
            Task p = derivedTasks.putIfAbsent(n, n);
            if (p!=null) {
                merge(p, n);
                return p;
            }
            return n;
        }

        @Override
        public void commit(NAR n) {
            Iterator<Task> ii = derivedTasks.values().iterator();
            while (ii.hasNext()) {
                n.input(ii.next());
                ii.remove();
            }
        }
    }


    /**
     * buffers and deduplicates derivations in a Bag<Task,Task> allowing higher priority inputs to evict
     * lower priority inputs before they are flushed.  commits involve a multi-thread shareable drainer task
     * allowing multiple deriver threads to fill the bag, potentially deduplicating each's results,
     * while other thread(s) drain it in prioritized order as input to NAR.
     */
    class DerivedTasksBag implements DerivedTasks {

        //final static Logger logger = LoggerFactory.getLogger(DerivedTasksBag.class);

        final boolean inlineOrDeferredInput;
        /**
         * temporary buffer for derivations before input so they can be merged in case of duplicates
         */
        public final PriArrayBag<Task> tasks = new PriArrayBag<Task>(this, new HashMap());

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
        public final FloatRange DerivedTaskBagDrainRateLimit = new FloatRange(0.5f, 0, 1f);

        private final TaskBagDrainer derivedTasksDrainer = new TaskBagDrainer(tasks, true,
                (size, capacity) -> Math.min(size, Math.round(capacity * DerivedTaskBagDrainRateLimit.floatValue()))
        );


        /**
         * @capacity size of buffer for tasks that have been derived (and are being de-duplicated) but not yet input.
         * input may happen concurrently (draining the bag) while derivations are inserted from another thread.
         */
        public DerivedTasksBag(int capacity, float drainLimitInitial, boolean inlineOrDeferredInput) {
            this.inlineOrDeferredInput = inlineOrDeferredInput;
            this.DerivedTaskBagDrainRateLimit.set(drainLimitInitial);
            this.tasks.setCapacity(capacity);
        }

        @Override
        public Task add(Task x, Derivation d) {
            return tasks.put(x);
        }

        @Override
        public void commit(NAR nar) {
            int s = tasks.size();

            //System.out.println(this + " " + s);

            if (s > 0) {


                if (inlineOrDeferredInput) {
                    ITask.run(derivedTasksDrainer, nar); //inline
                } else {
                    nar.exe.execute(derivedTasksDrainer);
                }


            }
        }
    }

}
