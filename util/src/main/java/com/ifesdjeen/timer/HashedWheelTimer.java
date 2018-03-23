package com.ifesdjeen.timer;

import com.conversantmedia.util.concurrent.ConcurrentQueue;
import com.conversantmedia.util.concurrent.MultithreadConcurrentQueue;
import jcog.TODO;
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

    final static Logger logger = LoggerFactory.getLogger(HashedWheelTimer.class);

    final static int wheelCapacity = 128;

    static final long DEFAULT_RESOLUTION = TimeUnit.NANOSECONDS.convert(10, TimeUnit.MILLISECONDS);
    static final int DEFAULT_WHEEL_SIZE = 512;
    private static final String DEFAULT_TIMER_NAME = "hashed-wheel-timer";

    private final ConcurrentQueue<TimedFuture<?>>[] wheel;
    private final int numWheels;
    private final long resolution;
    private final ExecutorService loop;
    private final ExecutorService executor;
    private final WaitStrategy waitStrategy;

    private final AtomicInteger cursor = new AtomicInteger();

    /**
     * Create a new {@code HashedWheelTimer} using the given with default resolution of 10 MILLISECONDS and
     * default wheel size.
     */
    public HashedWheelTimer() {
        this(DEFAULT_RESOLUTION, DEFAULT_WHEEL_SIZE, new WaitStrategy.SleepWait());
    }

    /**
     * Create a new {@code HashedWheelTimer} using the given timer resolution. All times will rounded up to the closest
     * multiple of this resolution.
     *
     * @param resolution the resolution of this timer, in NANOSECONDS
     */
    public HashedWheelTimer(long resolution) {
        this(resolution, DEFAULT_WHEEL_SIZE, new WaitStrategy.SleepWait());
    }

    /**
     * Create a new {@code HashedWheelTimer} using the given timer resolution and wheelSize. All times will
     * rounded up to the closest multiple of this resolution.
     *
     * @param res          resolution of this timer in NANOSECONDS
     * @param numWheels    size of the Ring Buffer supporting the Timer, the larger the wheel, the less the lookup time is
     *                     for sparse timeouts. Sane default is 512.
     * @param waitStrategy strategy for waiting for the next tick
     */
    public HashedWheelTimer(long res, int numWheels, WaitStrategy waitStrategy) {
        this(DEFAULT_TIMER_NAME, res, numWheels, waitStrategy, Executors.newFixedThreadPool(1));
    }

    /**
     * Create a new {@code HashedWheelTimer} using the given timer resolution and wheelSize. All times will
     * rounded up to the closest multiple of this resolution.
     *
     * @param name              name for daemon thread factory to be displayed
     * @param resolutionInNanos resolution of this timer in NANOSECONDS
     * @param numWheels         size of the Ring Buffer supporting the Timer, the larger the wheel, the less the lookup time is
     *                          for sparse timeouts. Sane default is 512.
     * @param strategy          strategy for waiting for the next tick
     * @param exec              Executor instance to submit tasks to
     */
    public HashedWheelTimer(String name, long resolutionInNanos, int numWheels, WaitStrategy strategy, ExecutorService exec) {
        this(resolutionInNanos, numWheels, strategy, exec, new ThreadFactory() {
            final AtomicInteger i = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, name + "-" + i.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        });
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
     * @param factory   custom ThreadFactory for the main loop thread
     */
    public HashedWheelTimer(long res, int numWheels, WaitStrategy strategy, ExecutorService exec,
                            ThreadFactory factory) {
        this.waitStrategy = strategy;

        this.wheel = new ConcurrentQueue[numWheels];

        for (int i = 0; i < numWheels; i++) {
            wheel[i] = new MultithreadConcurrentQueue<>(wheelCapacity);
        }

        this.numWheels = numWheels;

        this.resolution = res;


        this.loop = Executors.newSingleThreadExecutor(factory);
        this.loop.submit(this);
        this.executor = exec;
    }

    @Override
    public void run() {
        long deadline = System.nanoTime();

        long toleranceNS = resolution / 2;

        while (true) {
            int c = cursor.getAndUpdate((cc)->(cc + 1) % numWheels);
            if (c == 0) {
                //synch deadline
                long now = System.nanoTime();

                if (deadline > now + toleranceNS) {
                    //System.out.println(deadline - nextDeadline + " ahead"); ?
                    logger.info("{} lag", Texts.timeStr(now - deadline));
                    deadline = now;
                }
            }

            // TODO: consider extracting processing until deadline for test purposes
            ConcurrentQueue<TimedFuture<?>> registrations = wheel[c];
            int limit = registrations.size();
            if (limit > 0) {
                TimedFuture<?> r;
                while (limit-- > 0 && ((r = registrations.peek()) != null)) {
                    if (r.isCancelled()) {
                        registrations.poll(); //remove this one
                    } else if (r.ready()) {
                        registrations.poll(); //remove this one
                        executor.execute(r);


                        if (!r.isCancelAfterUse()) {
                            reschedule(r);
                        }
                    } else {
                        r.decrement();
                    }
                }
            }

            deadline += resolution;

            try {
                waitStrategy.waitUntil(deadline);
            } catch (InterruptedException e) {
                return;
            }


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
    public TimedFuture<?> submit(Runnable runnable) {
        return scheduleOneShot(resolution, constantlyNull(runnable));
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
    public TimedFuture<?> scheduleWithFixedDelay(Runnable runnable, long initialDelay, long delay, TimeUnit unit) {
        return scheduleFixedDelay(TimeUnit.NANOSECONDS.convert(delay, unit),
                TimeUnit.NANOSECONDS.convert(initialDelay, unit),
                constantlyNull(runnable));
    }

    @Override
    public String toString() {
        return String.format("HashedWheelTimer { Buffer Size: %d, Resolution: %d }",
                numWheels,
                resolution);
    }

    /**
     * Executor Delegates
     */

    @Override
    public void execute(Runnable command) {
        executor.execute(command);
    }

    @Override
    public void shutdown() {
        this.loop.shutdown();
        this.executor.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        this.loop.shutdownNow();
        return this.executor.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return this.loop.isShutdown() && this.executor.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return this.loop.isTerminated() && this.executor.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return this.loop.awaitTermination(timeout, unit) && this.executor.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return this.executor.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return this.executor.submit(task, result);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return this.executor.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout,
                                         TimeUnit unit) throws InterruptedException {
        return this.executor.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return this.executor.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout,
                           TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return this.executor.invokeAny(tasks, timeout, unit);
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

    private <V> TimedFuture<V> scheduleOneShot(long firstDelay,
                                               Callable<V> callable) {
        assertRunning();
        if (firstDelay < resolution) {
            // round up to resolution
            firstDelay = resolution;
        }

        int firstFireOffset = (int) (firstDelay / resolution);
        int firstFireRounds = firstFireOffset / numWheels;

        TimedFuture<V> r = new OneShotTimedFuture<>(firstFireRounds, callable, firstDelay);
        // We always add +1 because we'd like to keep to the right boundary of event on execution, not to the left:
        //
        // For example:
        //    |          now          |
        // res start               next tick
        // The earliest time we can tick is aligned to the right. Think of it a bit as a `ceil` function.
        add(idx(cursor.get() + firstFireOffset + 1), r);
        return r;
    }

    private <V> void add(int wheel, TimedFuture<V> r) {
        if (!this.wheel[wheel].offer(r)) {
            throw new TODO("grow wheel capacity");
        }
    }

    private <V> FixedRateTimedFuture<V> scheduleFixedRate(long recurringTimeout,
                                                          long firstDelay,
                                                          Callable<V> callable) {
        assertRunning();
        isTrue(recurringTimeout >= resolution,
                "Cannot schedule tasks for amount of time less than timer precision.");

        int firstFireOffset = (int) (firstDelay / resolution);
        int firstFireRounds = firstFireOffset / numWheels;

        FixedRateTimedFuture<V> r = new FixedRateTimedFuture(firstFireRounds, callable,
                recurringTimeout, resolution, numWheels);
        add(idx(cursor.get() + firstFireOffset + 1), r);
        return r;
    }

    private <V> FixedDelayTimedFuture<V> scheduleFixedDelay(long recurringTimeout,
                                                            long firstDelay,
                                                            Callable<V> callable) {
        assertRunning();
        isTrue(recurringTimeout >= resolution,
                "Cannot schedule tasks for amount of time less than timer precision.");

        int offset = (int) (recurringTimeout / resolution);
        int rounds = offset / numWheels;

        int firstFireOffset = (int) (firstDelay / resolution);
        int firstFireRounds = firstFireOffset / numWheels;

        FixedDelayTimedFuture<V> r = new FixedDelayTimedFuture<>(firstFireRounds, callable, recurringTimeout, rounds, offset,
                this::reschedule);
        add(idx(cursor.get() + firstFireOffset + 1), r);
        return r;
    }

    /**
     * Rechedule a {@link TimedFuture} for the next fire
     */
    private void reschedule(TimedFuture<?> r) {
        r.reset();
        add(idx(cursor.get() + r.getOffset() + 1), r);
    }

    private int idx(int cursor) {
        return cursor % numWheels;
    }

    private void assertRunning() {
        if (this.loop.isTerminated()) {
            throw new IllegalStateException("Timer is not running");
        }
    }

}
