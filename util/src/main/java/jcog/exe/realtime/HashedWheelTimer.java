package jcog.exe.realtime;

import jcog.Texts;
import jcog.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Hash Wheel Timer, as per the paper:
 * <p>
 * Hashed and hierarchical timing wheels:
 * http://www.cs.columbia.edu/~nahum/w6998/papers/ton97-timing-wheels.pdf
 * <p>
 * More comprehensive slides, explaining the paper can be found here:
 * http://www.cse.wustl.edu/~cdgill/courses/cs6874/TimingWheels.ppt
 * <p>
 * Hash Wheel timer is an approximated timer that allows performant execution of
 * larger amount of tasks with better performance compared to traditional scheduling.
 *
 * @author Oleksandr Petrov
 */
public class HashedWheelTimer implements ScheduledExecutorService, Runnable {



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

        abstract public void run(int wheel, HashedWheelTimer timer);

        abstract public void schedule(TimedFuture<?> r);

        abstract public void reschedule(int wheel, TimedFuture r);

        public final void schedule(TimedFuture r, int c) {
            int index = idx(c + r.getOffset(resolution) + 1);
            reschedule(index, r);
        }
        public final void scheduleUnlessImmediate(TimedFuture r, int c, HashedWheelTimer timer) {
            int offset = r.getOffset(resolution);
            if (offset>-1 || r.isPeriodic()) {
                int index = idx(c + offset + 1);
                reschedule(index, r);
            } else {
                timer.execute(r); //immediately
            }
        }
    }

    private final WheelModel model;

    public final static Logger logger = LoggerFactory.getLogger(HashedWheelTimer.class);

//    private static final String DEFAULT_TIMER_NAME = HashedWheelTimer.class.getSimpleName();


    public final long resolution;
    public final int wheels;

    private final Thread loop;
    private final Executor executor;
    private final WaitStrategy waitStrategy;

    private final AtomicInteger cursor = new AtomicInteger(0);

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
        this(null, model, waitStrategy,
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
    public HashedWheelTimer(String name, WheelModel model, WaitStrategy strategy, Executor exec) {
        this.waitStrategy = strategy;

        this.resolution = model.resolution;

        this.executor = exec;

        this.model = model;
        this.wheels = model.wheels;

        this.loop = name != null ? new Thread(this, name) : new Thread(this);
        this.loop.start();

    }
    /**
     * Rechedule a {@link TimedFuture} for the next fire
     */
    protected void _schedule(TimedFuture<?> r) {
        int c = cursor.get();
        if (c >= 0) {
            model.schedule(r, c);
        }
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

    @Override
    public void run() {


        long deadline = System.nanoTime();



        long epochTime = wheels * resolution;
        long nextEpoch = deadline;

        long tolerableLagPerEpochNS =
                //resolution;
                epochTime / 2;

        int c;
        while ((c = cursor.getAndUpdate(cc -> cc >= 0 ? (cc + 1) % wheels : Integer.MIN_VALUE)) >= 0) {

            if (c == 0) {
                //synch deadline
                long now = System.nanoTime();

                long lag = now - nextEpoch;
                if (Math.abs(lag) > tolerableLagPerEpochNS) {
                    double lagResolutions = ((double)lag)/epochTime;
                    if (lagResolutions > 5) {
                        logger.info("lag {} ({}%)", Texts.timeStr(lag), Texts.n2(100 * lagResolutions));
                    }
                    deadline = now;
                }
                nextEpoch = deadline + epochTime;
            }

            model.run(c, this);

            deadline += resolution;

            await(deadline);
        }
    }

    private void await(long deadline) {
        try {
            waitStrategy.waitUntil(deadline);
        } catch (InterruptedException e) {
            logger.error("interrupted: {}", e);
            shutdownNow();
        }
    }

    @Override public TimedFuture<?> submit(Runnable runnable) {
        return submit((TimedFuture) new Soon.Run(runnable));
    }

    public final <D> TimedFuture<D> submit(TimedFuture<D> r) {
        assertRunning();
        model.schedule(r);
        return r;
    }

    @Override
    public TimedFuture<?> schedule(Runnable runnable,
                                   long period,
                                   TimeUnit timeUnit) {
        return scheduleOneShot(TimeUnit.NANOSECONDS.convert(period, timeUnit),
                constantlyNull(runnable));
    }

    @Override
    public <V> TimedFuture<V> schedule(Callable<V> callable, long period, TimeUnit timeUnit) {
        return scheduleOneShot(TimeUnit.NANOSECONDS.convert(period, timeUnit),
                callable);
    }

    @Override
    public FixedRateTimedFuture<?> scheduleAtFixedRate(Runnable runnable, long initialDelay, long period, TimeUnit unit) {
        return scheduleFixedRate(TimeUnit.NANOSECONDS.convert(period, unit),
                TimeUnit.NANOSECONDS.convert(initialDelay, unit),
                constantlyNull(runnable));
    }

    @Override
    public FixedDelayTimedFuture<?> scheduleWithFixedDelay(Runnable runnable, long initialDelay, long delay, TimeUnit unit) {
        return scheduleFixedDelay(TimeUnit.NANOSECONDS.convert(delay, unit),
                TimeUnit.NANOSECONDS.convert(initialDelay, unit),
                constantlyNull(runnable));
    }

    @Override
    public String toString() {
        return String.format("HashedWheelTimer { Buffer Size: %d, Resolution: %d }",
                wheels,
                resolution);
    }

    /**
     * Executor Delegates
     */

    @Override
    public final void execute(Runnable command) {
        executor.execute(command);
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
        return cursor.get() >= 0 &&
                (!(executor instanceof ExecutorService) || ((ExecutorService) this.executor).isShutdown());
    }

    @Override
    public boolean isTerminated() {
        return cursor.get() >= 0 &&
                (!(executor instanceof ExecutorService) || ((ExecutorService) this.executor).isTerminated());
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return //this.loop.awaitTermination(timeout, unit) &&
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
     * Create a wrapper Function, which will "debounce" i.e. postpone the function execution until after <code>period</code>
     * has elapsed since last time it was invoked. <code>delegate</code> will be called most once <code>period</code>.
     *
     * @param delegate delegate runnable to be wrapped
     * @param period   given time period
     * @param timeUnit unit of the period
     * @return wrapped runnable
     */
    public Runnable debounce(Runnable delegate,
                             long period,
                             TimeUnit timeUnit) {
        AtomicReference<ScheduledFuture<?>> reg = new AtomicReference<>();

        return () -> {
            ScheduledFuture<?> future = reg.getAndSet(scheduleOneShot(TimeUnit.NANOSECONDS.convert(period, timeUnit),
                    () -> {
                        delegate.run();
                        return null;
                    }));
            if (future != null) {
                future.cancel(true);
            }
        };
    }

    /**
     * Create a wrapper Consumer, which will "debounce" i.e. postpone the function execution until after <code>period</code>
     * has elapsed since last time it was invoked. <code>delegate</code> will be called most once <code>period</code>.
     *
     * @param delegate delegate consumer to be wrapped
     * @param period   given time period
     * @param timeUnit unit of the period
     * @return wrapped runnable
     */
    public <T> Consumer<T> debounce(Consumer<T> delegate,
                                    long period,
                                    TimeUnit timeUnit) {
        AtomicReference<ScheduledFuture<T>> reg = new AtomicReference<>();

        return t -> {
            ScheduledFuture<T> future = reg.getAndSet(scheduleOneShot(TimeUnit.NANOSECONDS.convert(period, timeUnit),
                    () -> {
                        delegate.accept(t);
                        return t;
                    }));
            if (future != null) {
                future.cancel(true);
            }
        };
    }

    // TODO: biConsumer

    /**
     * Create a wrapper Runnable, which creates a throttled version, which, when called repeatedly, will call the
     * original function only once per every <code>period</code> milliseconds. It's easier to think about throttle
     * in terms of it's "left bound" (first time it's called within the current period).
     *
     * @param delegate delegate runnable to be called
     * @param period   period to be elapsed between the runs
     * @param timeUnit unit of the period
     * @return wrapped runnable
     */
    public Runnable throttle(Runnable delegate,
                             long period,
                             TimeUnit timeUnit) {
        AtomicBoolean alreadyWaiting = new AtomicBoolean();

        return () -> {
            if (alreadyWaiting.compareAndSet(false, true)) {
                scheduleOneShot(TimeUnit.NANOSECONDS.convert(period, timeUnit),
                        () -> {
                            delegate.run();
                            alreadyWaiting.compareAndSet(true, false);
                            return null;
                        });
            }
        };
    }

    /**
     * Create a wrapper Consumer, which creates a throttled version, which, when called repeatedly, will call the
     * original function only once per every <code>period</code> milliseconds. It's easier to think about throttle
     * in terms of it's "left bound" (first time it's called within the current period).
     *
     * @param delegate delegate consumer to be called
     * @param period   period to be elapsed between the runs
     * @param timeUnit unit of the period
     * @return wrapped runnable
     */
    public <T> Consumer<T> throttle(Consumer<T> delegate,
                                    long period,
                                    TimeUnit timeUnit) {
        AtomicBoolean alreadyWaiting = new AtomicBoolean();
        AtomicReference<T> lastValue = new AtomicReference<>();

        return val -> {
            lastValue.set(val);
            if (alreadyWaiting.compareAndSet(false, true)) {
                scheduleOneShot(TimeUnit.NANOSECONDS.convert(period, timeUnit),
                        () -> {
                            delegate.accept(lastValue.getAndSet(null));
                            alreadyWaiting.compareAndSet(true, false);
                            return null;
                        });
            }
        };
    }

    /**
     * INTERNALS
     */

    @Deprecated private <V> TimedFuture<V> scheduleOneShot(long firstDelay, Callable<V> callable) {

        if (firstDelay < resolution) {
            // round up to resolution
            firstDelay = resolution;
        }

        int firstFireOffset = (int) (firstDelay / resolution);
        int firstFireRounds = firstFireOffset / wheels;

        TimedFuture<V> r = new OneTimedFuture<>(firstFireOffset, firstFireRounds, callable);
        // We always add +1 because we'd like to keep to the right boundary of event on execution, not to the left:
        //
        // For example:
        //    |          now          |
        // res start               next tick
        // The earliest time we can tick is aligned to the right. Think of it a bit as a `ceil` function.
        return submit(r);
    }


    private <V> FixedRateTimedFuture<V> scheduleFixedRate(long recurringTimeout,
                                                          long firstDelay,
                                                          Callable<V> callable) {

        isTrue(recurringTimeout >= resolution,
                "Cannot schedule tasks for amount of time less than timer precision.");

        FixedRateTimedFuture<V> r = new FixedRateTimedFuture(0, callable,
                recurringTimeout, resolution, wheels);

        if (firstDelay > 0) {
            scheduleOneShot(firstDelay, () -> {
                model.schedule(r);
                return null;
            });
        } else {
            model.schedule(r);
        }

        return r;
    }

    private <V> FixedDelayTimedFuture<V> scheduleFixedDelay(long recurringTimeout,
                                                            long firstDelay,
                                                            Callable<V> callable) {
        isTrue(recurringTimeout >= resolution,
                "Cannot schedule tasks for amount of time less than timer precision.");

//        int offset = (int) (recurringTimeout / resolution);
//        int rounds = offset / numWheels;

        FixedDelayTimedFuture<V> r = new FixedDelayTimedFuture<>(0,
                callable,
                recurringTimeout, resolution, wheels,
                model::schedule);

        if (firstDelay > 0) {
            scheduleOneShot(firstDelay, () -> {
                model.schedule(r);
                return null;
            });
        } else {
            submit(r);
        }

        return r;
    }


    private void assertRunning() {
        if (cursor.get() < 0) {
//        if (this.loop.isTerminated()) {
            throw new IllegalStateException("Timer is not running");
        }
    }

    @FunctionalInterface
    public interface WaitStrategy {

//        WaitStrategy AdaptiveWait = (deadline) -> {
//            int spins = 0;
//            Thread t = null;
//            while (deadline >= System.nanoTime()) {
//                Util.pauseNext(spins++);
//                if ((t == null ? (t = Thread.currentThread()) : t).isInterrupted())
//                    throw new InterruptedException();
//            }
//        };

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

            long sleepTimeNanos = deadline - System.nanoTime();
            Util.sleepNS(sleepTimeNanos);
        };

        /**
         * Wait until the given deadline, deadlineNanoseconds
         *
         * @param deadlineNanoseconds deadline to wait for, in milliseconds
         */
        void waitUntil(long deadlineNanoseconds) throws InterruptedException;


    }

}
