package jcog.exe.util;

import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RunnableFuture;

/**
 * from: ForkJoinPool.java
 */
public abstract class RunnableForkJoin extends ForkJoinTask<Void> implements RunnableFuture<Void> {
    @Override public final Void getRawResult() {
        return null;
    }

    @Override public final void setRawResult(Void v) {
    }

    @Override
    public abstract boolean exec();

    public final void run() {
        invoke();
    }
}
