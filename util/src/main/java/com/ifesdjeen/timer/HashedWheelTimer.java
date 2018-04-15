package com.ifesdjeen.timer;

import com.conversantmedia.util.concurrent.ConcurrentQueue;
import com.conversantmedia.util.concurrent.DisruptorBlockingQueue;
import jcog.TODO;
import jcog.Texts;
import jcog.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
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

    public final static Logger logger = LoggerFactory.getLogger(HashedWheelTimer.class);

    private static final String DEFAULT_TIMER_NAME = HashedWheelTimer.class.getSimpleName();

    /** used for fast test for incoming items */
    final AtomicInteger incomingCount = new AtomicInteger();

    private final ConcurrentQueue<TimedFuture<?>> incoming = new DisruptorBlockingQueue<>(1024);


    private final Queue<TimedFuture<?>>[] wheel;
    private final int numWheels;
    private final long resolution;
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
    public HashedWheelTimer(long res, int numWheels, WaitStrategy waitStrategy) {
        this(null, res, numWheels, waitStrategy, Executors.newFixedThreadPool(1));
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
    public HashedWheelTimer(String name, long res, int numWheels, WaitStrategy strategy, Executor exec) {
        this.waitStrategy = strategy;

        this.wheel = new Queue[numWheels];

        for (int i = 0; i < numWheels; i++) {
            wheel[i] = new ArrayDeque();
        }

        this.numWheels = numWheels;

        this.resolution = res;

        this.executor = exec;

        this.loop = name!=null ? new Thread(this, name) : new Thread(this);
        this.loop.start();

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

        long toleranceNS = resolution / 2;

        long deadline = System.nanoTime();

        TimedFuture[] buffer = new TimedFuture[1024];

        int c;
        while ((c = cursor.getAndUpdate(cc -> cc >= 0 ? (cc + 1) % numWheels : Integer.MIN_VALUE))>=0) {

            if (incomingCount.get() > 0) {
                int count = incoming.remove(buffer);
                incomingCount.addAndGet(-count);
                for (int i = 0; i < count; i++) {
                    TimedFuture b = buffer[i];
                    buffer[i] = null;
                    _schedule(b, c);
                }
            }

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
            Queue<TimedFuture<?>> w = wheel[c];
            int limit = !w.isEmpty() ? w.size() : 0;
            if (limit > 0) {
                TimedFuture<?> r;
                while (limit-- > 0 && ((r = w.peek()) != null)) {

                    switch (r.state()) {
                        case CANCELLED:
                            w.poll();
                            break;
                        case READY:
                            w.poll();
                            r.execute(this);
                            break;
                        case PENDING:
                            break;

                    }
                }
            }

            deadline += resolution;

            try {
                waitStrategy.waitUntil(deadline);
            } catch (InterruptedException e) {
                logger.error("interrupted: {}", e);
                shutdownNow();
                break;
            }


        }
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
    public final void execute(Runnable command) {
        executor.execute(command);
    }

    @Override
    public void shutdown() {
        cursor.set(Integer.MIN_VALUE);
        if (executor instanceof ExecutorService)
            ((ExecutorService)this.executor).shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        cursor.set(Integer.MIN_VALUE);
        if (executor instanceof ExecutorService)
            return ((ExecutorService)this.executor).shutdownNow();
        else
            return List.of();
    }

    @Override
    public boolean isShutdown() {
        return cursor.get() >= 0 &&
                (!(executor instanceof ExecutorService) || ((ExecutorService)this.executor).isShutdown());
    }

    @Override
    public boolean isTerminated() {
        return cursor.get() >= 0 &&
                (!(executor instanceof ExecutorService) || ((ExecutorService)this.executor).isTerminated());
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return //this.loop.awaitTermination(timeout, unit) &&
                (!(executor instanceof ExecutorService) || ((ExecutorService)this.executor).awaitTermination(timeout, unit));
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return ((ExecutorService)this.executor).submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return ((ExecutorService)this.executor).submit(task, result);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return ((ExecutorService)this.executor).invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout,
                                         TimeUnit unit) throws InterruptedException {
        return ((ExecutorService)this.executor).invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return ((ExecutorService)this.executor).invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout,
                           TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return ((ExecutorService)this.executor).invokeAny(tasks, timeout, unit);
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

        TimedFuture<V> r = new OneShotTimedFuture<>(firstFireOffset, firstFireRounds, callable, firstDelay);
        // We always add +1 because we'd like to keep to the right boundary of event on execution, not to the left:
        //
        // For example:
        //    |          now          |
        // res start               next tick
        // The earliest time we can tick is aligned to the right. Think of it a bit as a `ceil` function.
        schedule(r);
        return r;
    }


    private <V> FixedRateTimedFuture<V> scheduleFixedRate(long recurringTimeout,
                                                          long firstDelay,
                                                          Callable<V> callable) {

        isTrue(recurringTimeout >= resolution,
                "Cannot schedule tasks for amount of time less than timer precision.");

        FixedRateTimedFuture<V> r = new FixedRateTimedFuture(0, callable,
                recurringTimeout, resolution, numWheels);

        if (firstDelay > 0) {
            scheduleOneShot(firstDelay, () -> {
                schedule(r);
                return null;
            });
        }  else {
            schedule(r);
        }

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

        FixedDelayTimedFuture<V> r = new FixedDelayTimedFuture<>(0, callable, recurringTimeout, rounds, offset,
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

    protected void schedule(TimedFuture<?> r) {
        boolean added = incoming.offer(r);
        if (!added) {
            throw new RuntimeException("incoming queue overloaded");
        }

        incomingCount.incrementAndGet();
    }

    /**
     * Rechedule a {@link TimedFuture} for the next fire
     */
    protected void _schedule(TimedFuture<?> r) {
        int c = cursor.get();
        if (c >= 0) {
            _schedule(r, c);
        }
    }

    private void _schedule(TimedFuture<?> r, int c) {
        add(idx(c + r.getOffset() + 1), r);
    }

    private <V> void add(int wheel, TimedFuture<V> r) {
        if (wheel < 0)
            throw new RuntimeException("wtf");

        if (!this.wheel[wheel].offer(r)) {
            throw new TODO("grow wheel capacity");
        }
    }

    private int idx(int cursor) {
        return cursor % numWheels;
    }

    private void assertRunning() {
        if (cursor.get() < 0) {
//        if (this.loop.isTerminated()) {
            throw new IllegalStateException("Timer is not running");
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
            while (deadline >= System.nanoTime()) {
                Thread.yield();
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
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
            while (deadline >= System.nanoTime()) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
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
