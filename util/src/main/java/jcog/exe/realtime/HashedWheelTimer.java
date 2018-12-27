package jcog.exe.realtime;

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


    private boolean daemon = false;

    public int size() {
        return model.size();
    }


    abstract static class WheelModel {
        public final int wheels;
        public final long resolution;

        protected WheelModel(int wheels, long resolution) {
            this.wheels = wheels;
            this.resolution = resolution;
        }

        public final int idx(int cursor) {
            return cursor % wheels;
        }

        /** returns how approximately how many entries were in the wheel at start.
         * used in part to determine if the entire wheel is empty*/
        abstract public int run(int wheel, HashedWheelTimer timer);

        abstract public void schedule(TimedFuture<?> r);

        abstract public void reschedule(int wheel, TimedFuture r);

        public final void schedule(TimedFuture r, int c, HashedWheelTimer timer) {
            int offset = r.getOffset(resolution);
            if (offset>-1 || r.isPeriodic()) {
                reschedule(idx(c + offset + 1 ), r);
            } else {
                timer.execute(r); 
            }
        }

        /** number of tasks currently in the wheel */
        abstract public int size();

        /** allows the model to interrupt the wheel before it decides to sleep */
        abstract public boolean canExit();

        public void restart(HashedWheelTimer h) {

        }
    }

    private final WheelModel model;

    public final static Logger logger = LoggerFactory.getLogger(HashedWheelTimer.class);




    /** how many epochs can pass while empty before the thread attempts to end (going into a re-activatable sleep mode) */
    static final int SLEEP_EPOCHS = 128;

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


    private static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw new IllegalArgumentException(message);
        }
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

            while ((c = cursor.getAndAccumulate(wheels, (cc,w) -> (cc + 1) % w)) >= 0) {

                if (model.run(c, this) == 0) {
                    if (empties++ >= wheels * SLEEP_EPOCHS) {
                        break; 
                    }
                } else
                    empties = 0;



                deadline = await(deadline);
            }
        }
        while (cursor.getOpaque() >= 0 && !model.canExit() && !cursor.compareAndSet(c, -1));

        loop = null;

        logger.info("{} {} {}", this, c == SHUTDOWN ? "off" : "sleep", System.currentTimeMillis());


    }

    /** TODO call System.nanoTime() less by passing now,then as args to the wait interfce */
    private long  await(long deadline) {

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

        } else {
            float lagThreshold = 1; //in resolutions
            if (sleepTimeNanos < -resolution * lagThreshold) {
                //fell behind more than N resolutions, adjust
                deadline = now + resolution;
            }
        }
        return deadline;
    }

    @Override public TimedFuture<?> submit(Runnable runnable) {
        return schedule((TimedFuture) new Soon.Run(runnable));
    }

    public final <D> TimedFuture<D> schedule(TimedFuture<D> r) {
        model.schedule(r);
        assertRunning();
        return r;
    }
    protected final void _schedule(TimedFuture<?> r) {
        int c = cursor.get();
        if (c >= 0) {
            model.reschedule(idx(c + r.getOffset(model.resolution) + 1), r);
            assertRunning();
        }
    }

    /** equivalent to model.idx() since its wheels is equal */
    public final int idx(int cursor) {
        return cursor % wheels;
    }

    @Override
    public TimedFuture<?> schedule(Runnable runnable,
                                   long period,
                                   TimeUnit timeUnit) {
        return scheduleOneShot(NANOSECONDS.convert(period, timeUnit),
                constantlyNull(runnable));
    }

    @Override
    public <V> TimedFuture<V> schedule(Callable<V> callable, long period, TimeUnit timeUnit) {
        return scheduleOneShot(NANOSECONDS.convert(period, timeUnit),
                callable);
    }

    @Override
    public FixedRateTimedFuture scheduleAtFixedRate(Runnable runnable, long initialDelay, long period, TimeUnit unit) {
        return scheduleFixedRate(NANOSECONDS.convert(period, unit), NANOSECONDS.convert(initialDelay, unit),
                runnable);
    }

    public void scheduleAtFixedRate(FixedRateTimedFuture f, long period, TimeUnit unit) {
        f.init(NANOSECONDS.convert(period, unit), resolution, wheels);
        schedule(f);
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
        //try {
            executor.execute(r);
//        } catch (Throwable t) {
//            logger.error("{} {}", r, t);
//        }
    }

    @Override
    public void shutdown() {
        cursor.lazySet(Integer.MIN_VALUE);
        if (executor instanceof ExecutorService)
            ((ExecutorService) this.executor).shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        cursor.lazySet(Integer.MIN_VALUE);
        if (executor instanceof ExecutorService)
            return ((ExecutorService) this.executor).shutdownNow();
        else
            return List.of();
    }

    @Override
    public boolean isShutdown() {
        return cursor.getOpaque() >= 0 &&
                (!(executor instanceof ExecutorService) || ((ExecutorService) this.executor).isShutdown());
    }

    @Override
    public boolean isTerminated() {
        return cursor.getOpaque() >= 0 &&
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

    @Deprecated private <V> TimedFuture<V> scheduleOneShot(long firstDelay, Callable<V> callable) {

        if (firstDelay < resolution) {
            
            firstDelay = resolution;
        }

        int firstFireOffset = (int) (firstDelay / resolution);
        int firstFireRounds = Math.round(((float)firstDelay) / (resolution * wheels));

        TimedFuture<V> r = new OneTimedFuture<>(firstFireOffset+1, firstFireRounds, callable);
        
        
        
        
        
        
        return schedule(r);
    }


    public FixedRateTimedFuture scheduleFixedRate(long recurringTimeout,
                                                       long firstDelay,
                                                       Runnable callable) {


        FixedRateTimedFuture r = new FixedRateTimedFuture(0, callable,
                recurringTimeout, resolution, wheels);

        return scheduleFixedRate(recurringTimeout, firstDelay, r);
    }

    private FixedRateTimedFuture scheduleFixedRate(long recurringTimeout, long firstDelay, FixedRateTimedFuture r) {
        isTrue(recurringTimeout >= resolution,
                "Cannot schedule tasks for amount of time less than timer precision.");

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
        isTrue(recurringTimeout >= resolution,
                "Cannot schedule tasks for amount of time less than timer precision.");




        FixedDelayTimedFuture<V> r = new FixedDelayTimedFuture<>(0,
                callable,
                recurringTimeout, resolution, wheels,
                this::schedule);

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


    void assertRunning() {
        if (cursor.compareAndSet(-1, 0)) {
            this.loop = new Thread(this, HashedWheelTimer.class.getSimpleName() +"_" + hashCode());
            this.loop.setDaemon(daemon); 
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

}
