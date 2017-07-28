package jcog.bag.impl;

import com.conversantmedia.util.concurrent.DisruptorBlockingQueue;
import jcog.pri.Prioritized;
import jcog.pri.op.PriMerge;
import jcog.util.QueueLock;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Random;

/** adds a QueueLock wrapping the putAsync methods */
public class ConcurrentCurveBag<X extends Prioritized> extends CurveBag<X> {

    private final QueueLock<X> toPut;

    public ConcurrentCurveBag(@NotNull PriMerge mergeFunction, @NotNull Map<X, X> map, Random rng, int cap) {
        super(mergeFunction, map, rng, cap);

        this.toPut = new QueueLock<X>(new DisruptorBlockingQueue(32), super::putAsync) {
            @Override
            protected void batchFinished(int batchSize) {
//                if (batchSize > 1) {
//                    System.out.println("batchsize="+batchSize);
//                }
                synchronized (items) {
                    ConcurrentCurveBag.super.ensureSorted();
                }
            }
        };
    }

    @Override
    protected void ensureSorted() {
        //sort elides until after batchFinished
    }

    @Override
    public void putAsync(@NotNull X b) {
        toPut.accept(b);
    }
}
