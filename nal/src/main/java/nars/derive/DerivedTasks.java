package nars.derive;

import jcog.pri.bag.impl.PriArrayBag;
import jcog.pri.op.PriMerge;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.task.NALTask;
import nars.task.util.TaskBagDrainer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

public interface DerivedTasks {
    Task add(Task x, Derivation d);
    void commit(NAR n);

    BiFunction<Task, Task, Task> DERIVATION_MERGE = (pp, tt) -> {
        pp.priMax(tt.pri());
        if (pp instanceof NALTask)
            ((NALTask) pp).priCauseMerge(tt);
        if (pp.isCyclic() && !tt.isCyclic()) {

            pp.setCyclic(false);
        }
        return pp;
    };

    /** TODO this is trivial. just input directly to NAR on add */
    abstract class DerivedTasksDirect implements DerivedTasks {

    }

    /**
     * buffers derivations in a Map<> for de-duplication prior to a commit that flushes them as input to NAR
     * TODO find old implementation and re-implement this */
    abstract class DerivedTasksMap implements DerivedTasks {
        private final Map<Task, Task> derivedTasks = new LinkedHashMap<>(4096,0.9f);
    }


    /**
     * buffers and deduplicates derivations in a Bag<Task,Task> allowing higher priority inputs to evict
     * lower priority inputs before they are flushed.  commits involve a multi-thread shareable drainer task
     * allowing multiple deriver threads to fill the bag, potentially deduplicating each's results,
     * while other thread(s) drain it in prioritized order as input to NAR.
      */
    class DerivedTasksBag implements DerivedTasks {

        //final static Logger logger = LoggerFactory.getLogger(DerivedTasksBag.class);

        /**
         * temporary buffer for derivations before input so they can be merged in case of duplicates
         */
        final PriArrayBag<Task> tasks = new PriArrayBag<>(PriMerge.max, new ConcurrentHashMap<>()) {

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


        };

        private final TaskBagDrainer derivedTasksDrainer = new TaskBagDrainer(tasks, true,
                (size, capacity) -> {
                    return Math.min(size, Math.round(capacity * Param.DerivedTaskBagDrainRateLimit));
                }
        );

        public DerivedTasksBag(int capacity) {
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

                tasks.commit(null /* no forget */);

                nar.input(derivedTasksDrainer);
            }
        }
    }

}
