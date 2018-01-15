package nars.exe;

import com.google.common.util.concurrent.MoreExecutors;
import jcog.Texts;
import jcog.Util;
import jcog.exe.BusyPool;
import jcog.math.MutableInteger;
import jcog.math.random.XoRoShiRo128PlusRandom;
import nars.NAR;
import nars.Task;

import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * uses a common forkjoin pool for execution
 */
public class PoolMultiExec extends AbstractExec {

    static final int IDLE_PERIOD_MS = 50;

    private final int qSize;
    BusyPool pool;

    private final Revaluator revaluator;

    /** TODO make this adjust in realtime */
    private final MutableInteger threads = new MutableInteger();

    private Focus focus;
    private Consumer exe;


    public PoolMultiExec(Revaluator revaluator, int capacity, int qSize) {
        this(revaluator, Runtime.getRuntime().availableProcessors() - 1, capacity, qSize);
    }

    protected PoolMultiExec(Revaluator r, int threads, int capacity, int qSize) {
        super(capacity);
        this.revaluator = r;
        this.threads.set(threads);
        this.qSize = qSize;
        this.exe = this::executeInline;
    }

    @Override
    public void execute(Runnable async) {
        exe.accept(async);
    }

    @Override
    public void execute(Object t) {
        if (t instanceof Task /*|| isWorker(Thread.currentThread())*/) {
            executeInline(t);
        } else {
            exe.accept(t);
        }
    }

    @Override
    public synchronized void start(NAR nar) {

        this.focus = new Focus(nar, revaluator);

        super.start(nar);

        this.pool = new BusyPool(threads.intValue(),
                Util.blockingQueue(qSize)
                //new ArrayBlockingQueue(qSize)
                ) {

            @Override
            protected BusyPool.Worker newWorker(Queue<Runnable> q) {
                return new Worker(q) {

                    final Random rng = new XoRoShiRo128PlusRandom(System.nanoTime());

                    //long runUntil = System.currentTimeMillis();
                    long lastCycle = Long.MIN_VALUE;

                    protected boolean endCondition() {
                        return nar.time() == lastCycle && q.isEmpty();
                        //&& System.currentTimeMillis() <= runUntil;
                    }

                    @Override
                    protected void run(Object next) {
                        executeInline(next);
                    }

                    @Override
                    protected void idle(long timeSinceLastBusyNS) {

                        int loopMS = nar.loop.periodMS.intValue();
                        if (loopMS < 0) {
                            loopMS = IDLE_PERIOD_MS;
                        }
                        long dutyMS = Math.round(nar.loop.throttle.floatValue() * loopMS);
                        //System.out.println(this + " " + Texts.timeStr(timeSinceLastBusyNS) + " since busy, " + Texts.timeStr(dutyMS*1E6) + " loop time" );
                        if (dutyMS > 0) {

                            double t = nar.loop.jiffy.doubleValue() * dutyMS / 1000.0;

                            //runUntil = System.currentTimeMillis() + dutyMS;

                            lastCycle = nar.time();
                            focus.runDeadline(
                                    t,
                                    this::endCondition,
                                    rng, nar);

                        }

                    }
                };
            }
        };
        this.exe = pool::queue;
    }

    @Override
    public synchronized void stop() {
        exe = this::executeInline;
        super.stop();
        pool.shutdownNow();
        pool = null;
        focus = null;

    }

    @Override
    public boolean concurrent() {
        return true;
    }


}
