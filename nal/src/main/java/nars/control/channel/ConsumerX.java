package nars.control.channel;

import jcog.data.iterator.ArrayIterator;
import jcog.data.list.FasterList;
import jcog.pri.Prioritizable;
import jcog.pri.bag.Sampler;
import jcog.pri.bag.impl.ArrayBag;
import jcog.pri.bag.impl.BufferedBag;
import nars.Param;
import nars.Task;
import nars.exe.Exec;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Stream;

/** recipient of instances: in collections, iterators, streams, or individually.
 * */
@FunctionalInterface public interface ConsumerX<X extends Prioritizable> extends Consumer<X> {

    default void acceptAll(Iterable<? extends X> xx) {
        if (xx instanceof Collection)
            acceptAll((Collection)xx);
        else
            acceptAll(xx.iterator());
    }

    default void acceptAll(Collection<? extends X> xx) {
        switch (xx.size()) {
            case 0: return;
            case 1: {
                if (xx instanceof List) {
                    accept(((List<X>) xx).get(0));
                    return;
                }
                break;
            }
        }
        acceptAll(xx.iterator());
    }

    default void acceptAll(Iterator<? extends X> xx) {
        xx.forEachRemaining(this);
    }

    default void acceptAll(Stream<? extends X> x) {
        x.forEach(this);
    }

    default void acceptAll(List<? extends X> x) {
        if (x.size() == 1) {
            accept(x.get(0));
        } else {
            acceptAll((Collection)x);
        }
    }

    default void acceptAll(X[] xx) {
        switch (xx.length) {
            case 0:  break;
            case 1:  accept(xx[0]); break;
            default: acceptAll(ArrayIterator.iterator(xx)); break;
        }
    }

    /** override for multithreading hints */
    default int concurrency() {
        return 1;
    }

    ThreadLocal<FasterList> drainBuffer = ThreadLocal.withInitial(FasterList::new);

//    void input(Bag<ITask, ITask> b, What target, int min);
    /** asynchronously drain N elements from a bag as input */
    default void input(Sampler<? extends X> taskSampler, ConsumerX<? super X> target, int max, Executor exe, Consumer<FasterList<X>> runner) {
        Sampler b;
        if  (taskSampler instanceof BufferedBag)
            b = ((BufferedBag) taskSampler).bag;
        else
            b = taskSampler;

        exe.execute(() -> {

            FasterList batch = Exec.drainBuffer.get();

            if (b instanceof ArrayBag) {
                boolean blocking = true;
                ((ArrayBag) b).popBatch(max, blocking, batch::add);
            } else {
                b.pop(null, max, batch::add); //per item.. may be slow
            }

            int bs = batch.size();
            if (bs > 0) {

                try {

                    if (bs > 2) {
                        if (Param.PRE_SORT_TASK_INPUT_BATCH) {
                            batch.sortThis(Task.sloppySorter);
                        }
                    }

                    runner.accept(batch);

                } finally {
                    batch.clear();
                }
            }
        });

    }

}
