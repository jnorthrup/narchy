package jcog.exe.realtime;

import jcog.TODO;
import jcog.Texts;
import jcog.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * Hash Wheel Timer, as per the paper:
 * <p>
 * Hashed and hierarchical timing wheels:
 * http:
 * <p>
 * More comprehensive slides, explaining the paper can be found here:
 * http:
 * <p>
 * Hash Wheel timer is an approximated timer that allows performant execution of
 * larger amount of tasks with better performance compared to traditional scheduling.
 *
 * @author Oleksandr Petrov
 */
public class HashedWheelTimer implements ScheduledExecutorService, Runnable {

    static final int THREAD_PRI =
            //Thread.MAX_PRIORITY;
            Thread.NORM_PRIORITY;

    /**
     * how many epochs can pass while empty before the thread attempts to end (going into a re-activatable sleep mode)
     */
    static final int SLEEP_EPOCHS = 1024;

    final transient private boolean daemon = false;


    private final WheelModel model;

    public final static Logger logger = LoggerFactory.getLogger(HashedWheelTimer.class);


    public final long resolution;
    public final int wheels;

    private Thread loop;
    private final Executor executor;
    private final WaitStrategy waitStrategy;

    private final AtomicInteger cursor = new AtomicInteger(-1);

    /**
     * Create a new {@code HashedWheelTimer} using the given timer resolution and wheelSize. All times will
     * rounded up to the closest multiple of this resolution.
     *
     * @param res          resolution of this timer in NANOSECONDS
     * @param numWheels    size of the Ring Buffer supporting the Timer, the larger the wheel, the less the lookup time is
     *                     for sparse timeouts. Sane default is 512.
     * @param waitStrategy strategy for waiting for the next tick
     */
    public HashedWheelTimer(WheelModel model, WaitStrategy waitStrategy) {
        this(model, waitStrategy,
                Executors.newFixedThreadPool(1));
    }


    /**
     * Create a new {@code HashedWheelTimer} using the given timer resolution and wheelSize. All times will
     * rounded up to the closest multiple of this resolution.
     *
     * @param res       resolution of this timer in NANOSECONDS
     * @param numWheels size of the Ring Buffer supporting the Timer, the larger the wheel, the less the lookup time is
     *                  for sparse timeouts. Sane default is 512.
     * @param strategy  strategy for waiting for the next tick
     * @param exec      Executor instance to submit tasks to
     */
    public HashedWheelTimer(WheelModel model, WaitStrategy strategy, Executor exec) {
        this.waitStrategy = strategy;

        this.resolution = model.resolution;

        this.executor = exec;

        this.model = model;
        this.wheels = model.wheels;
    }


    private static Callable<?> constantlyNull(Runnable r) {
        return () -> {
            r.run();
            return null;
        };
    }

    static final int SHUTDOWN = Integer.MIN_VALUE;

    @Override
    public void run() {

        logger.info("{} restart {}", this, System.currentTimeMillis());

        model.restart(this);

        int c, empties;

        long deadline = System.nanoTime();

        do {

            empties = 0;

            while ((c = cursor.getAndAccumulate(wheels, (cc, w) -> (cc + 1) % w)) >= 0) {

                if (model.run(c, this) == 0) {
                    if (empties++ >= wheels * SLEEP_EPOCHS)
                        break;

                } else
                    empties = 0;


                //await();
                deadline = await(deadline);
            }
        }
        while (cursor() >= 0 && !model.isEmpty() && !cursor.compareAndSet(c, -1));

        loop = null;

        logger.info("{} {} {}", this, c == SHUTDOWN ? "off" : "sleep", System.currentTimeMillis());


    }

//    private void await() {
//        try {
//            waitStrategy.waitUntil(System.nanoTime() + resolution);
//        } catch (InterruptedException e) {
//            logger.error("interrupted: {}", e);
//            shutdownNow();
//        }
//    }

    /**
     * TODO call System.nanoTime() less by passing now,then as args to the wait interfce
     */
    private long await(long deadline) {

        deadline += resolution;
        long now = System.nanoTime();
        long sleepTimeNanos = deadline - now;

        if (sleepTimeNanos > 0) {
            //System.out.println(Texts.timeStr(sleepTimeNanos) + " sleep");

            try {
                waitStrategy.waitUntil(deadline);
            } catch (InterruptedException e) {
                logger.error("interrupted: {}", e);
                shutdownNow();
            }

            //TODO check after sleep?
            //long wake = System.nanoTime();


        } else {

            float lagThreshold = wheels; //in resolutions
            if (sleepTimeNanos < -resolution * lagThreshold) {
                //fell behind more than N resolutions, adjust
                deadline = now;
            }
        }
        return deadline;
    }

    @Override
    public TimedFuture<?> submit(Runnable runnable) {
        return schedule((TimedFuture) new Soon.Run(runnable));
    }

    public final <D> TimedFuture<D> schedule(TimedFuture<D> r) {
        if (r.state()==TimedFuture.Status.CANCELLED)
            throw new RuntimeException("scheduling an already cancelled task");

        boolean ok = model.accept(r, this);
        if (!ok)
            reject(r);

        assertRunning();
        return r;
    }

    protected <X> void reject(TimedFuture<X> r) {
        logger.error("spill {}", r);
        r.cancel(false);
    }

    protected final boolean reschedule(TimedFuture<?> r) {
        assertRunning();
        int c = cursor();
        if (c >= 0) {
            int offset = r.offset(model.resolution);
            if (!model.reschedule(idx(c + offset), r)) {
                reject(r);
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * equivalent to model.idx() since its wheels is equal
     */
    public final int idx(int cursor) {
        return cursor % wheels;
    }

    public int cursor() {
        return cursor.getOpaque();
    }

//    public final int idxCursor(int delta) {
//        return idx(cursor() + delta);
//    }

    @Override
    public ScheduledFuture<?> schedule(Runnable runnable,
                                   long delayNS,
                                   TimeUnit timeUnit) {
        return scheduleOneShot(NANOSECONDS.convert(delayNS, timeUnit), constantlyNull(runnable));
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delayNS, TimeUnit timeUnit) {
        return scheduleOneShot(NANOSECONDS.convert(delayNS, timeUnit), callable);
    }

    @Override
    public FixedRateTimedFuture scheduleAtFixedRate(Runnable runnable, long delayNS, long periodNS, TimeUnit unit) {
        return scheduleFixedRate(NANOSECONDS.convert(periodNS, unit), NANOSECONDS.convert(delayNS, unit),
                runnable);
    }

    public void scheduleAtFixedRate(FixedRateTimedFuture f, long period, TimeUnit unit) {
        f.init(NANOSECONDS.convert(period, unit), resolution, wheels);
        reschedule(f);
    }

    @Override
    public FixedDelayTimedFuture<?> scheduleWithFixedDelay(Runnable runnable, long initialDelay, long delay, TimeUnit unit) {
        return scheduleFixedDelay(NANOSECONDS.convert(delay, unit),
                NANOSECONDS.convert(initialDelay, unit),
                constantlyNull(runnable));
    }

    @Override
    public String toString() {
        return String.format("HashedWheelTimer { Buffer Size: %d, Resolution: %s }",
                wheels,
                Texts.timeStr(resolution));
    }

    /**
     * Executor Delegate, invokes immediately bypassing the timer's ordered scheduling.
     * Use submit for invokeLater-like behavior
     */
    @Override
    public final void execute(Runnable r) {
        executor.execute(r);
    }

    @Override
    public void shutdown() {
        cursor.set(Integer.MIN_VALUE);
        if (executor instanceof ExecutorService)
            ((ExecutorService) this.executor).shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        cursor.set(Integer.MIN_VALUE);
        if (executor instanceof ExecutorService)
            return ((ExecutorService) this.executor).shutdownNow();
        else
            return List.of();
    }

    @Override
    public boolean isShutdown() {
        return cursor() >= 0 &&
                (!(executor instanceof ExecutorService) || ((ExecutorService) this.executor).isShutdown());
    }

    @Override
    public boolean isTerminated() {
        return cursor() >= 0 &&
                (!(executor instanceof ExecutorService) || ((ExecutorService) this.executor).isTerminated());
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return
                (!(executor instanceof ExecutorService) || ((ExecutorService) this.executor).awaitTermination(timeout, unit));
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return ((ExecutorService) this.executor).submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return ((ExecutorService) this.executor).submit(task, result);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return ((ExecutorService) this.executor).invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout,
                                         TimeUnit unit) throws InterruptedException {
        return ((ExecutorService) this.executor).invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return ((ExecutorService) this.executor).invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout,
                           TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return ((ExecutorService) this.executor).invokeAny(tasks, timeout, unit);
    }


    /**
     * INTERNALS
     */
    private <V> ScheduledFuture<V> scheduleOneShot(long delayNS, Callable<V> callable) {

        if (delayNS <= resolution/2) {
            //immediate
            ImmediateFuture<V> f = new ImmediateFuture<>(callable);
            executor.execute(f);
            return f;
        } else if (delayNS < resolution) {
            //round-up
            delayNS = resolution;
        }


        long cycleLen = wheels * resolution;
        int rounds = (int) (delayNS / cycleLen);
        int firstFireOffset = Util.longToInt( delayNS - rounds * cycleLen );

        return schedule(new OneTimedFuture(Math.max(1, firstFireOffset), rounds, callable));
    }


    public FixedRateTimedFuture scheduleFixedRate(long recurringTimeout,
                                                  long firstDelay,
                                                  Runnable callable) {


        FixedRateTimedFuture r = new FixedRateTimedFuture(0, callable,
                recurringTimeout, resolution, wheels);

        return scheduleFixedRate(recurringTimeout, firstDelay, r);
    }

    private FixedRateTimedFuture scheduleFixedRate(long recurringTimeout, long firstDelay, FixedRateTimedFuture r) {
        assert (recurringTimeout >= resolution) : "Cannot schedule tasks for amount of time less than timer precision.";

        if (firstDelay > 0) {
            scheduleOneShot(firstDelay, () -> {
                schedule(r);
                return null;
            });
        } else {
            schedule(r);
        }

        return r;
    }

    private <V> FixedDelayTimedFuture<V> scheduleFixedDelay(long recurringTimeout,
                                                            long firstDelay,
                                                            Callable<V> callable) {
        assert (recurringTimeout >= resolution) : "Cannot schedule tasks for amount of time less than timer precision.";


        FixedDelayTimedFuture<V> r = new FixedDelayTimedFuture<>(
                callable,
                recurringTimeout, resolution, wheels,
                this::schedule);

        if (firstDelay > resolution) {
            scheduleOneShot(firstDelay, () -> {
                schedule(r);
                return null;
            });
        } else {
            schedule(r);
        }

        return r;
    }


    void assertRunning() {
        if (cursor.compareAndSet(-1, 0)) {
            this.loop = new Thread(this, HashedWheelTimer.class.getSimpleName() + '_' + hashCode());
            this.loop.setDaemon(daemon);
            this.loop.setPriority(THREAD_PRI);
            this.loop.start();
        }
    }

    @FunctionalInterface
    public interface WaitStrategy {

        /**
         * Yielding wait strategy.
         * <p>
         * Spins in the loop, until the deadline is reached. Releases the flow control
         * by means of Thread.yield() call. This strategy is less precise than BusySpin
         * one, but is more scheduler-friendly.
         */
        WaitStrategy YieldingWait = (deadline) -> {
            Thread t = null;
            while (deadline >= System.nanoTime()) {
                Thread.yield();
                if ((t == null ? (t = Thread.currentThread()) : t).isInterrupted())
                    throw new InterruptedException();
            }
        };
        /**
         * BusySpin wait strategy.
         * <p>
         * Spins in the loop until the deadline is reached. In a multi-core environment,
         * will occupy an entire core. Is more precise than Sleep wait strategy, but
         * consumes more resources.
         */
        WaitStrategy BusySpinWait = (long deadline) -> {
            Thread t = null;
            while (deadline >= System.nanoTime()) {
                if ((t == null ? (t = Thread.currentThread()) : t).isInterrupted())
                    throw new InterruptedException();
            }
        };
        /**
         * Sleep wait strategy.
         * <p>
         * Will release the flow control, giving other threads a possibility of execution
         * on the same processor. Uses less resources than BusySpin wait, but is less
         * precise.
         */
        WaitStrategy SleepWait = (long deadline) -> {
            long sleepNS = deadline - System.nanoTime();
            Util.sleepNS(sleepNS);
        };

        /**
         * Wait until the given deadline, deadlineNanoseconds
         *
         * @param deadlineNanoseconds deadline to wait for, in milliseconds
         */
        void waitUntil(long deadlineNanoseconds) throws InterruptedException;


    }

    public int size() {
        return model.size();
    }

    /** scheduler implementation */
    abstract static class WheelModel {
        public final int wheels;
        public final long resolution;

        protected WheelModel(int wheels, long resolution) {
            this.wheels = wheels;
            this.resolution = resolution;
        }

        public final void out(TimedFuture<?> x, HashedWheelTimer timer) {
            schedule(x, timer.cursor(), timer);
        }

        public final int idx(int cursor) {
            return cursor % wheels;
        }

        /**
         * returns how approximately how many entries were in the wheel at start.
         * used in part to determine if the entire wheel is empty.
         *
         */
        abstract public int run(int wheel, HashedWheelTimer timer);

        /** return false if unable to schedule */
        abstract public boolean accept(TimedFuture<?> r, HashedWheelTimer hashedWheelTimer);

        /** return false if unable to reschedule */
        abstract public boolean reschedule(int wheel, TimedFuture r);

        public final boolean schedule(TimedFuture r, int c, HashedWheelTimer timer) {
            int offset = r.offset(resolution);
//            if (offset <= 0)
//                System.out.println(r);
            if (offset > -1 || r.isPeriodic()) {
                if (!reschedule(idx(c + offset), r))
                    return false;
            } else {
                timer.execute(r);
            }
            return false;
        }

        /**
         * estimated number of tasks currently in the wheel
         */
        abstract public int size();

        /**
         * allows the model to interrupt the wheel before it decides to sleep
         */
        abstract public boolean isEmpty();

        public void restart(HashedWheelTimer h) {

        }
    }

    private static class ImmediateFuture<V> implements ScheduledFuture<V>, Runnable {
        private Callable<V> callable;
        private Object result = this;

        public ImmediateFuture(Callable<V> callable) {
            this.callable = callable;
        }

        @Override
        public long getDelay(TimeUnit timeUnit) {
            return 0;
        }

        @Override
        public int compareTo(Delayed delayed) {
            throw new TODO();
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            Callable<V> c = callable;
            if (c != null) {
                result = null;
                callable = null;
                return true;
            }
            return false;
        }

        @Override
        public boolean isCancelled() {
            return result==this && callable==null;
        }

        @Override
        public boolean isDone() {
            return result!=this;
        }

        @Override
        public V get() {
            Object r = this.result;
            return r == this ? null : (V) r;
        }

        @Override
        public V get(long l, TimeUnit timeUnit)  {
            return AbstractTimedCallable.poll(this, l, timeUnit);
        }

        @Override
        public void run() {
            try {
                result = callable.call();
            } catch (Exception e) {
                result = e;
            }
        }
    }
}
