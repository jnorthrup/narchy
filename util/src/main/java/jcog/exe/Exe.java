package jcog.exe;

import com.conversantmedia.util.concurrent.MultithreadConcurrentQueue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jcog.exe.realtime.AdmissionQueueWheelModel;
import jcog.exe.realtime.HashedWheelTimer;
import jcog.net.UDPeer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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

    /** record an already-timed event */
    public static void profiled(Object what, long start, long end) {
        Profiler p = profiler;
        if (p!=null)
            p.profiled(what, start, end);
    }

    public static abstract class Profiler {

        abstract public void profiled(Object what, long startNS, long endNS);

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
                Profiler.this.profiled(r, start, end);
            }
        }
    }

    static volatile Profiler profiler = null;

    public static synchronized void setProfiler(@Nullable Profiler p) {
        profiler = p;
    }

    public static class UDPeerProfiler extends Profiler {

        final static org.slf4j.Logger logger = LoggerFactory.getLogger(UDPeerProfiler.class);

        final UDPeer p = new UDPeer();

        final AtomicBoolean busy = new AtomicBoolean();
        final MultithreadConcurrentQueue<JsonNode> out = new MultithreadConcurrentQueue(2048);

        public UDPeerProfiler() throws IOException {
            p.runFPS(10f);
        }

        @Override
        public void profiled(Object what, long startNS, long endNS) {
            if (!p.connected())
                return;

            //Map<String, Serializable> msg = Map.of("_", w, "t", new long[]{startNS, endNS,});

            ArrayNode range = JsonNodeFactory.instance.arrayNode(2);
            range.add(startNS);
            range.add(endNS);
            ObjectNode newest = JsonNodeFactory.instance.objectNode();
            newest.put("t", range);

            String w = what.toString();
            newest.put("_", w);


            if (busy.compareAndSet(false, true)) {
                int s = out.size();
                ArrayNode a = JsonNodeFactory.instance.arrayNode(s + 1);
                if (s > 0) {
                    for ( ; s > 0; s--) {
                        a.add(out.poll());
                    }
                }
                a.add(newest);
                try {
                    p.tellSome(a, 2, true);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                } finally {
                    busy.set(false);
                }
            } else {
                if (!out.offer(newest))
                    logger.warn("dropped profiling msg");
            }
        }
    }
}
