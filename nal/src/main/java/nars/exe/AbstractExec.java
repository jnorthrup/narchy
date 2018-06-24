package nars.exe;

import nars.NAR;

import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

/**
 * unified executor
 */
abstract public class AbstractExec extends Exec {


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

}
