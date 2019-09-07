package jcog.util;

import jcog.data.list.FasterList;
import jcog.pri.Prioritizable;
import jcog.pri.bag.Sampler;
import jcog.pri.bag.impl.ArrayBag;
import jcog.pri.bag.impl.BufferedBag;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
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
        switch (x.size()) {
            case 0:
                return;
            case 1:
                accept(x.get(0));
                break;
            default:
                acceptAll((Collection) x);
                break;
        }
    }

    default void acceptAll(X[] xx) {
        for (X x : xx)
            accept(x);
    }

    /** override for multithreading hints */
    default int concurrency() {
        return 1;
    }

    ThreadLocal<FasterList> drainBuffer = ThreadLocal.withInitial(FasterList::new);

//    void input(Bag<ITask, ITask> b, What target, int min);
    /** asynchronously drain N elements from a bag as input */
    default void input(Sampler<? extends X> taskSampler, int max, Executor exe, Consumer<FasterList<X>> runner, @Nullable Comparator<X> batchSorter) {
        Sampler b;
        if  (taskSampler instanceof BufferedBag)
            b = ((BufferedBag) taskSampler).bag;
        else
            b = taskSampler;

        exe.execute(() -> {

            FasterList batch = drainBuffer.get();

            if (b instanceof ArrayBag) {
                ((ArrayBag) b).popBatch(max, true, batch::add);
            } else {
                b.pop(null, max, batch::add); //per item.. may be slow
            }

            int bs = batch.size();
            if (bs > 0) {

                try {

                    if (bs > 2 && batchSorter!=null) {
                        batch.sortThis(batchSorter);
                    }

                    runner.accept(batch);

                } finally {
                    batch.clear();
                }
            }
        });

    }

}
