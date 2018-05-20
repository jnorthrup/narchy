package jcog.exe;

import com.fasterxml.jackson.core.JsonProcessingException;
import jcog.exe.realtime.AdmissionQueueWheelModel;
import jcog.exe.realtime.HashedWheelTimer;
import jcog.net.UDPeer;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

/** static execution context: JVM-global dispatch, logging, profiling, etc. */
public enum Exe { ;

    private final static Object lock = new Object();

    /** global timer */
    private static volatile HashedWheelTimer timer = null;
    private static volatile Executor executor = ForkJoinPool.commonPool();

    public static HashedWheelTimer timer() {
        if (timer == null) {
            synchronized (lock) {
                if (timer == null) {
                    Executor exe = Exe::invoke; //executor();
                    HashedWheelTimer.logger.info("global timer start, using {}", exe);
                    timer = new HashedWheelTimer(
                            new AdmissionQueueWheelModel(32, TimeUnit.MILLISECONDS.toNanos(1)),
                            //HashedWheelTimer.WaitStrategy.YieldingWait,
                            HashedWheelTimer.WaitStrategy.SleepWait,
                            exe);
                }
            }
        }
        return timer;
    }

    public static void invoke(Runnable r) {
        Profiler p = profiler;
        if (p == null) {
            executor.execute(r);
        } else {
            executor.execute(p.run(r));
        }
    }

    public static void invokeLater(Runnable r) {
        timer().submit(r);
    }

    public static Executor executor() {
        return executor;
    }

    public static synchronized void setExecutor(Executor e) {
        if (timer != null) {
            throw new RuntimeException("timer already started");
        }
        executor = e;
    }

    public static abstract class Profiler {

        abstract public void run(Object what, long startNS, long endNS);

        public Runnable run(Runnable r) {
            return new ProfiledRunnable(r);
        }

        private class ProfiledRunnable implements Runnable {
            private final Runnable r;

            public ProfiledRunnable(Runnable r) {
                this.r = r;
            }

            @Override
            public void run() {
                long start = System.nanoTime();
                r.run();
                long end = System.nanoTime();
                Profiler.this.run(r, start, end);
            }
        }
    }

    static volatile Profiler profiler = null;

    public static synchronized void setProfiler(@Nullable Profiler p) {
        profiler = p;
    }

    public static class UDPeerProfiler extends Profiler {
        final UDPeer p = new UDPeer();

        public UDPeerProfiler() throws IOException {
            p.runFPS(10f);
        }

        @Override
        public void run(Object what, long startNS, long endNS) {
            if (p.connected()) {
                String w = what.toString();
                Map<String, Serializable> msg = Map.of("_", w, "t", new long[]{startNS, endNS,});
                try {
                    p.tellSome(msg, 2, true);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
