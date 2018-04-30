package jcog.exe.realtime;


import jcog.Texts;
import org.HdrHistogram.ConcurrentHistogram;
import org.HdrHistogram.Histogram;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public abstract class AbstractTimerTest {

    final HashedWheelTimer timer = new HashedWheelTimer(
                    new AdmissionQueueWheelModel(128,
                    TimeUnit.MILLISECONDS.toNanos(1)),
                    waitStrategy());

    public abstract HashedWheelTimer.WaitStrategy waitStrategy();

    @BeforeEach
    public void before() {
        // TODO: run tests on different sequences
        timer.assertRunning();
    }


    @AfterEach
    public void after() throws InterruptedException {
        timer.shutdownNow();
        assertTrue(timer.awaitTermination(10, TimeUnit.SECONDS));
    }

    @Test
    public void scheduleOneShotRunnableTest() throws InterruptedException {
        AtomicInteger i = new AtomicInteger(1);
        timer.schedule((Runnable) i::decrementAndGet,
                100,
                TimeUnit.MILLISECONDS);

        Thread.sleep(300);
        assertEquals(0, i.get());
    }

    @Test
    public void testOneShotRunnableFuture() throws InterruptedException, TimeoutException, ExecutionException {
        AtomicInteger i = new AtomicInteger(1);
        long start = System.currentTimeMillis();
        assertNull(timer.schedule((Runnable) i::decrementAndGet,
                100,
                TimeUnit.MILLISECONDS)
                .get(10, TimeUnit.SECONDS));
        long end = System.currentTimeMillis();
        assertTrue(end - start >= 100);
    }

    @Test
    public void scheduleOneShotCallableTest() throws InterruptedException {
        AtomicInteger i = new AtomicInteger(1);
        timer.schedule(() -> {
                    i.decrementAndGet();
                    return "Hello";
                },
                100,
                TimeUnit.MILLISECONDS);

        Thread.sleep(300);
        assertEquals(0, i.get());
    }

    @Test
    public void testOneShotCallableFuture() throws InterruptedException, TimeoutException, ExecutionException {
        AtomicInteger i = new AtomicInteger(1);
        long start = System.currentTimeMillis();
        assertEquals("Hello", timer.schedule(() -> {
                    i.decrementAndGet();
                    return "Hello";
                },
                100,
                TimeUnit.MILLISECONDS)
                .get(10, TimeUnit.SECONDS));
        long end = System.currentTimeMillis();
        assertTrue(end - start >= 100);
    }

    @Test
    public void fixedRateFirstFireTest() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        long start = System.currentTimeMillis();
        timer.scheduleAtFixedRate(latch::countDown,
                100,
                100,
                TimeUnit.MILLISECONDS);
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        long end = System.currentTimeMillis();
        assertTrue(end - start >= 100);
    }

    @Test
    public void delayBetweenFixedRateEvents() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        List<Long> r = new ArrayList<>();
        timer.scheduleAtFixedRate(() -> {

                    r.add(System.currentTimeMillis());

                    latch.countDown();

                    if (latch.getCount() == 0)
                        return; // to avoid sleep interruptions

                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    r.add(System.currentTimeMillis());
                },
                100,
                100,
                TimeUnit.MILLISECONDS);
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        // time difference between the beginning of second tick and end of first one
        assertTrue(r.get(2) - r.get(1) <= (50 * 100)); // allow it to wiggle
    }

    @Test
    public void delayBetweenFixedDelayEvents() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        List<Long> r = new ArrayList<>();
        timer.scheduleWithFixedDelay(() -> {

                    r.add(System.currentTimeMillis());

                    latch.countDown();

                    if (latch.getCount() == 0)
                        return; // to avoid sleep interruptions

                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    r.add(System.currentTimeMillis());
                },
                100,
                100,
                TimeUnit.MILLISECONDS);
        assertTrue(latch.await(10, TimeUnit.SECONDS), ()->latch.getCount() + " should be zero");
        // time difference between the beginning of second tick and end of first one
        assertTrue(r.get(2) - r.get(1) >= 100);
    }
    @Test
    public void fixedRateSubsequentFireTest_40ms() throws InterruptedException {
        fixedDelaySubsequentFireTest(40, 40, false);
    }
    @Test
    public void fixedRateSubsequentFireTest_20ms() throws InterruptedException {
        fixedDelaySubsequentFireTest(20, 40, false);
    }

    @Test
    public void fixedDelaySubsequentFireTest_40ms() throws InterruptedException {
        fixedDelaySubsequentFireTest(40, 40, true);
    }
    @Test
    public void fixedDelaySubsequentFireTest_20ms() throws InterruptedException {
        fixedDelaySubsequentFireTest(20, 40, true);
    }

    void fixedDelaySubsequentFireTest(int delayMS, int count, boolean fixedDelayOrRate) throws InterruptedException {

        int warmup = 1;

        CountDownLatch latch = new CountDownLatch(count);
        long start = System.nanoTime();
        Histogram when = new ConcurrentHistogram(
                1_000L, //1uS
                1_000_000_000L * 4 /* 4 Sec */, 5);

        final long[] last = {start};
        Runnable task = () -> {
            long now = System.nanoTime();

            if (latch.getCount() < (count - warmup))
                when.recordValue(now - last[0]);

            last[0] = now;
            latch.countDown();
        };

        if (fixedDelayOrRate) {
            timer.scheduleWithFixedDelay(task,
                    0,
                    delayMS,
                    TimeUnit.MILLISECONDS);
        } else {
            timer.scheduleAtFixedRate(task,
                    0,
                    delayMS,
                    TimeUnit.MILLISECONDS);
        }

        assertTrue(latch.await(count, TimeUnit.SECONDS), ()->latch.getCount() + " should be zero");
        assertTrue(1 >= timer.size(), timer.size() + " tasks in wheel");

        {
            Histogram w = when.copy();
            Texts.histogramPrint(w, System.out);
            System.out.println("mean=" + Texts.timeStr(w.getMean()));
            System.out.println("max=" + Texts.timeStr(w.getMaxValue()));
            long delayNS = TimeUnit.MILLISECONDS.toNanos(delayMS);
            assertTrue(Math.abs(delayNS - w.getMean()) < delayNS / 4);
        }
        //assertTrue(end - start >= 1000);
    }

    @Test
    public void fixedRateSubsequentFireTest() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(10);
        long start = System.currentTimeMillis();
        //thre
        timer.scheduleAtFixedRate(latch::countDown,
                100,
                100,
                TimeUnit.MILLISECONDS);
        assertTrue(latch.await(10, TimeUnit.SECONDS), ()->latch.getCount() + " should be zero");
        long end = System.currentTimeMillis();
        assertTrue(end - start >= 1000, ()->end-start + "(ms) start to end");
    }

    // TODO: precision test
    // capture deadline and check the deviation from the deadline for different amounts of tasks

    // DISCLAIMER:
    // THE FOLLOWING TESTS WERE PORTED FROM NETTY. BIG PROPS TO NETTY AUTHORS FOR THEM.

    @Test
    public void testScheduleTimeoutShouldNotRunBeforeDelay() throws InterruptedException {
        final CountDownLatch barrier = new CountDownLatch(1);
        final Future timeout = timer.schedule(() -> {
            fail("This should not have run");
            barrier.countDown();
        }, 10, TimeUnit.SECONDS);
        assertFalse(barrier.await(3, TimeUnit.SECONDS));
        assertFalse(timeout.isDone(), "timer should not expire");
        // timeout.cancel(true);
    }

    @Test
    public void testScheduleTimeoutShouldRunAfterDelay() throws InterruptedException {
        final CountDownLatch barrier = new CountDownLatch(1);
        final Future timeout = timer.schedule(barrier::countDown, 2, TimeUnit.SECONDS);
        assertTrue(barrier.await(3, TimeUnit.SECONDS));
        assertTrue(timeout.isDone(), "should expire");
    }

//    @Test
//    public void testTimerShouldThrowExceptionAfterShutdownForNewTimeouts() {
//        assertThrows(IllegalStateException.class, () -> {
//            for (int i = 0; i < 3; i++) {
//                timer.schedule(() -> {
//                }, 10, TimeUnit.MILLISECONDS);
//            }
//
//            timer.shutdown();
//            Thread.sleep(1000L); // sleep for a second
//
//            timer.schedule(() -> {
//                fail("This should not run");
//            }, 1, TimeUnit.SECONDS);
//
//        });
//    }

    @Test
    public void testTimerOverflowWheelLength() throws InterruptedException {
        final AtomicInteger counter = new AtomicInteger();

        timer.schedule(new Runnable() {
            @Override
            public void run() {
                counter.incrementAndGet();
                timer.schedule(this, 200, TimeUnit.MILLISECONDS);
            }
        }, 200, TimeUnit.MILLISECONDS);
        Thread.sleep(700);
        assertEquals(3, counter.get());
    }

    @Test
    public void testExecutionOnTime() throws InterruptedException {

        int delayTime = 250;
        int tolerance = 25;
        int maxTimeout = (delayTime) + tolerance;

        int scheduledTasks =
                //100000;
                //8 * 1024;
                500;

        final BlockingQueue<Long> queue = new LinkedBlockingQueue<>();

        for (int i = 0; i < scheduledTasks; i++) {
            final long start = System.nanoTime();

            timer.schedule(() -> {
                long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                queue.add(ms);
            }, delayTime, TimeUnit.MILLISECONDS);
        }

        for (int i = 0; i < scheduledTasks; i++) {
            long delay = queue.take();
            System.out.println(i + " " + delay);
            assertTrue(delay >= delayTime - tolerance && delay <= delayTime + tolerance,
                    () -> "Timeout + " + scheduledTasks + " delay must be " + delayTime + " < " + delay + " < " + maxTimeout);
        }
    }

}
