package jcog.bag.impl;

import jcog.Util;
import jcog.pri.Priority;
import jcog.pri.op.PriMerge;
import jcog.util.QueueLock;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Random;
import java.util.function.IntConsumer;

/**
 * adds a QueueLock wrapping the putAsync methods
 */
public class ConcurrentCurveBag<X extends Priority> extends CurveBag<X> {

    private final QueueLock<X> toPut;

    public ConcurrentCurveBag(@NotNull PriMerge mergeFunction, @NotNull Map<X, X> map, Random rng, int cap) {
        super(mergeFunction, map, rng, cap);

        IntConsumer afterBatch = null; //assumes the bag will be periodically / manually commit()'d
//                (batchSize) -> {
//            commit();
////            if (mustSort) {
////                synchronized (items) {
////                    super.ensureSorted();
////                }
////            }
//        };
        this.toPut = new QueueLock<X>(Util.blockingQueue(cap*2), super::putAsync, afterBatch);
    }

    @Override
    protected void ensureSorted() {
        //sort elides until after batchFinished
    }

    @Override
    public void putAsync(X b) {
        toPut.accept(b);
    }


}
