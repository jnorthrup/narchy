package nars.exe;

import nars.NAR;

import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

/**
 * unified executor
 */
abstract public class AbstractExec extends Exec {

    private final int concurrency, concurrencyMax;

    AbstractExec(int concurrency, int concurrencyMax) {
        super();
        this.concurrency = concurrency;
        this.concurrencyMax = concurrencyMax; //TODO this will be a value like Runtime.getRuntime().availableProcessors() when concurrency can be adjusted dynamically
    }

    @Override
    public void execute(Runnable async) {
        if (concurrent()) {
            ForkJoinPool.commonPool().execute(async);
        } else {
            async.run();
        }
    }

    public void execute(Consumer<NAR> r) {
        if (concurrent()) {
            ForkJoinPool.commonPool().execute(() -> r.accept(nar));
        } else {
            r.accept(nar);
        }
    }

    @Override
    public boolean concurrent() {
        return concurrency > 1;
    }

    @Override
    public final int concurrency() {
        return concurrency;
    }

    @Override
    public final int concurrencyMax() {
        return concurrencyMax;
    }

}
