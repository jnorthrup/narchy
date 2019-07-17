package jcog.exe.realtime;


import jcog.Texts;
import jcog.Util;
import org.HdrHistogram.ConcurrentHistogram;
import org.HdrHistogram.Histogram;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@Execution(ExecutionMode.SAME_THREAD)
abstract class AbstractTimerTest {

    private final HashedWheelTimer timer;

    {
        long resolution = TimeUnit.MILLISECONDS.toNanos(1)/4;
        int wheels = 8;

        HashedWheelTimer.WheelModel q = model(resolution, wheels);


        timer = new HashedWheelTimer(q, waitStrategy());
    }

    protected HashedWheelTimer.WheelModel model(long resolution, int wheels) {
        AdmissionQueueWheelModel admissionQueue = new AdmissionQueueWheelModel(wheels, resolution);
        return admissionQueue;
    }

    protected abstract HashedWheelTimer.WaitStrategy waitStrategy();

    @BeforeEach
    void before() {
        
        timer.assertRunning();
    }


    @AfterEach
    void after() throws InterruptedException {
        timer.shutdownNow();
        assertTrue(timer.awaitTermination(3, TimeUnit.SECONDS));
    }

    @Test
    void scheduleOneShotRunnableTest() throws InterruptedException {
        AtomicInteger i = new AtomicInteger(1);
        timer.schedule((Runnable) i::decrementAndGet,
                100,
                TimeUnit.MILLISECONDS);

        Thread.sleep(300);
        assertEquals(0, i.get());
    }

    @Test
    void testOneShotRunnableFuture() throws InterruptedException, TimeoutException, ExecutionException {
        AtomicInteger i = new AtomicInteger(1);
        long start = System.currentTimeMillis();
        assertNull(timer.schedule((Runnable) i::decrementAndGet,
                100,
                TimeUnit.MILLISECONDS)
                .get(1, TimeUnit.SECONDS));
        long end = System.currentTimeMillis();
        assertTrue(end - start >= 100);
    }

    @Test
    void scheduleOneShotCallableTest() throws InterruptedException {
        AtomicInteger i = new AtomicInteger(1);
        ScheduledFuture<String> future = timer.schedule(() -> {
                    i.decrementAndGet();
                    return "Hello";
                },
                100,
                TimeUnit.MILLISECONDS);

        Thread.sleep(300);
        assertEquals(0, i.get());
    }

    @Test
    void testOneShotCallableFuture() throws InterruptedException, ExecutionException, TimeoutException {
        AtomicInteger i = new AtomicInteger(1);
        long start = System.currentTimeMillis();
        assertEquals("Hello", timer.schedule(() -> {
                    i.decrementAndGet();
                    return "Hello";
                },
                100,
                TimeUnit.MILLISECONDS)
                .get(250, TimeUnit.MILLISECONDS));

        long end = System.currentTimeMillis();

        assertTrue(end - start >= 100);
//        assertTrue(end - start < 300);
    }

    @Test
    void fixedRateFirstFireTest() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        long start = System.currentTimeMillis();
        timer.scheduleAtFixedRate(latch::countDown,
                100,
                100,
                TimeUnit.MILLISECONDS);
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        long end = System.currentTimeMillis();
        assertTrue(end - start >= 100);
    }

    @Test
    void delayBetweenFixedRateEvents() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        List<Long> r = new ArrayList<>();
        int periodMS = 100;
        timer.scheduleAtFixedRate(() -> {

                    r.add(System.currentTimeMillis());

                    latch.countDown();

                    if (latch.getCount() == 0)
                        return; 

                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    r.add(System.currentTimeMillis());
                },
                periodMS,
                periodMS,
                TimeUnit.MILLISECONDS);
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        
        assertTrue(r.get(2) - r.get(1) <= (50 * periodMS));
    }

    @Test
    void delayBetweenFixedDelayEvents() {
        //CountDownLatch latch = new CountDownLatch(2);
        List<Long> r = new ArrayList<>();
        long start = System.nanoTime();
        timer.scheduleWithFixedDelay(() -> {

                    long now = System.nanoTime();
                    r.add(now);
                    System.out.println(Texts.timeStr(now - start));

//                    latch.countDown();
//
//                    if (latch.getCount() == 0)
//                        return;

//                    try {
//                        Thread.sleep(50);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }

                },
            100,
            100,
            TimeUnit.MILLISECONDS);

        Util.sleepMS(1000);
        //assertTrue(latch.await(1, TimeUnit.SECONDS), ()->latch.getCount() + " should be zero");

        assertTrue(r.size() > 6);
        assertTrue(r.size() < 15);

        Long a2 = r.get(5);
        Long a1 = r.get(4);
        assertTrue(Math.abs(a2 - a1) >= 90L*1E6 && Math.abs(a2-a1) < 110L*1E6, ()-> Texts.timeStr(a2) + " vs " + Texts.timeStr(a1));
    }

    @Test
    void fixedRateSubsequentFireTest_30ms_rate() throws InterruptedException {
        fixedDelaySubsequentFireTest(30, 20, false);
    }
    @Test
    void fixedRateSubsequentFireTest_30ms_delay() throws InterruptedException {
        fixedDelaySubsequentFireTest(30, 20, true);
    }

    @Test
    void fixedRateSubsequentFireTest_4ms() throws InterruptedException {
        fixedDelaySubsequentFireTest(4, 128, false);
    }
    @Test
    void fixedRateSubsequentFireTest_5ms() throws InterruptedException {
        fixedDelaySubsequentFireTest(5, 128, false);
    }
    @Test
    void fixedRateSubsequentFireTest_20ms() throws InterruptedException {
        fixedDelaySubsequentFireTest(20, 40, false);
    }

    @Test
    void fixedDelaySubsequentFireTest_40ms() throws InterruptedException {
        fixedDelaySubsequentFireTest(40, 40, true);
    }
    @Test
    void fixedDelaySubsequentFireTest_20ms() throws InterruptedException {
        fixedDelaySubsequentFireTest(20, 40, true);
    }

    private void fixedDelaySubsequentFireTest(int periodMS, int count, boolean fixedDelayOrRate) throws InterruptedException {

        int warmup = count/2;

        CountDownLatch latch = new CountDownLatch(count + warmup);
        Histogram when = new ConcurrentHistogram(
                1_000L, /* 1 uS */
                1_000_000_000L * 2 /* 2 Sec */, 5);

        long start = System.nanoTime();
        final long[] last = {start};
        Runnable task = () -> {
            long now = System.nanoTime();

            if (latch.getCount() < count)
                when.recordValue(now - last[0]);

            last[0] = now;
            latch.countDown();
        };

        if (fixedDelayOrRate) {
            timer.scheduleWithFixedDelay(task,
                    0,
                    periodMS,
                    TimeUnit.MILLISECONDS);
        } else {
            timer.scheduleAtFixedRate(task,
                    0,
                    periodMS,
                    TimeUnit.MILLISECONDS);
        }

        assertTrue(latch.await(count, TimeUnit.SECONDS), ()->latch.getCount() + " should be zero");
        assertTrue(1 >= timer.size(), timer.size() + " tasks in wheel");

        {
            Histogram w = when.copy();
            Texts.histogramPrint(w, System.out);
            System.out.println("mean=" + Texts.timeStr(w.getMean()));
            System.out.println("max=" + Texts.timeStr(w.getMaxValue()));
            long delayNS = TimeUnit.MILLISECONDS.toNanos(periodMS);
            double err = Math.abs(delayNS - w.getMean());
            assertTrue(err < delayNS / 4);
        }
        
    }

    @Test
    void fixedRateSubsequentFireTest() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(10);
        long start = System.currentTimeMillis();
        
        timer.scheduleAtFixedRate(latch::countDown,
                100,
                100,
                TimeUnit.MILLISECONDS);
        assertTrue(latch.await(3, TimeUnit.SECONDS), ()->latch.getCount() + " should be zero");
        long end = System.currentTimeMillis();
        assertTrue(end - start >= 900, ()->end-start + "(ms) start to end");
    }

    
    

    
    

    @Test
    void testScheduleTimeoutShouldNotRunBeforeDelay() throws InterruptedException {
        final CountDownLatch barrier = new CountDownLatch(1);
        final Future timeout = timer.schedule(() -> {
            fail("This should not have run");
            barrier.countDown();
            fail();
        }, 2, TimeUnit.SECONDS);
        assertFalse(barrier.await(1, TimeUnit.SECONDS));
        assertFalse(timeout.isDone(), "timer should not expire");
        
    }

    @Test
    void testScheduleTimeoutShouldRunAfterDelay() {
        final CountDownLatch barrier = new CountDownLatch(1);
        final Future timeout = timer.schedule(barrier::countDown, 100, TimeUnit.MILLISECONDS);
        //assertTrue(barrier.await(2, TimeUnit.SECONDS));
        Util.sleepMS(200);
        assertTrue(timeout.isDone(), "should expire");
        assertEquals(0, barrier.getCount());
    }



















    @Test
    void testTimerOverflowWheelLength() throws InterruptedException {
        final AtomicInteger counter = new AtomicInteger();

        timer.schedule(new Runnable() {
            @Override
            public void run() {
                counter.incrementAndGet();
                timer.schedule(this, 200, TimeUnit.MILLISECONDS);
            }
        }, 200, TimeUnit.MILLISECONDS);
        Thread.sleep(700);
        assertTrue(3 >= counter.get() && counter.get() < 8);
    }

    @Test
    void testExecutionOnTime() throws InterruptedException {

        int delayTime = 250;
        int tolerance = 50;
        int maxTimeout = (delayTime) + tolerance;

        int scheduledTasks =
                
                
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
