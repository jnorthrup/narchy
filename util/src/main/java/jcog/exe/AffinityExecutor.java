package jcog.exe;

import jcog.WTF;
import jcog.data.list.FastCoWList;
import jcog.data.list.FasterList;
import jcog.event.Off;
import net.openhft.affinity.AffinityLock;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * uses affinity locking to pin new threads to their own unique, stable CPU core/hyperthread etc
 */
public class AffinityExecutor implements Executor {

//    private static final Logger logger = LoggerFactory.getLogger(AffinityExecutor.class);

    public final FastCoWList<Thread> threads = new FastCoWList<Thread>(Thread[]::new);
    public final Semaphore running;
    public final String id;
    public final int maxThreads;

    public AffinityExecutor(/* int maxThreads */) {
        this(Runtime.getRuntime().availableProcessors());
    }
    public AffinityExecutor(int maxThreads) {
        this(Thread.currentThread().getThreadGroup().getName(), maxThreads);
    }

    public AffinityExecutor(String id, int maxThreads) {
        this.id = id;

        this.maxThreads = maxThreads;
        this.running = new Semaphore(maxThreads);
    }

    @Override
    public final void execute(Runnable command) {
        execute(command, 1);
    }

    public final void shutdownNow() {
        stop();
    }

    public int size() {
        return maxThreads - running.availablePermits();
    }


    protected final class AffinityThread extends Thread {

        private final boolean tryPin;
        final Runnable run;

        public AffinityThread(String name, Runnable run) {
            this(name, run, true);
        }

        public AffinityThread(String name, Runnable run, boolean tryPin) {
            super(name);

            this.run = run;
            this.tryPin = tryPin;
        }

        @Override
        public void run() {


            try {
                if (tryPin) {
                    try (AffinityLock lock = AffinityLock.acquireCore()) {
                        run.run();
                    } catch (Exception e) {
//                        logger.warn("Could not acquire affinity lock; executing normally: {} ", e.getMessage());
//                        cmd.run();
                        throw new RuntimeException(e);
                    }
                } else {
                    run.run();
                }
            } finally {
                threads.remove(this);
            }

        }
    }


    static final AtomicInteger serial = new AtomicInteger(0);

    public void stop() {
        threads.removeIf(t -> {

            kill(t);

            return true;
        });
    }

    protected void kill(Thread thread) {

        running.release(1);

        Runnable t = ((AffinityThread)thread).run;
        if (t instanceof Off) {

            ((Off) t).off();

        }

        if (thread.isAlive()) thread.interrupt();

    }


    public final <R extends Runnable> void execute(R worker, int count) {
        execute((Supplier<R>)()->worker, count);
    }

    public final <R extends Runnable>  void execute(Supplier<R> worker, int count) {
        execute(worker, count, true);
    }

    public final <R extends Runnable> List<R> execute(Supplier<R> workerBuilder, int count, boolean tryPin) {

        FasterList<R> l = new FasterList(count);

        for (int i = 0; i < count; i++) {
            R w = workerBuilder.get();
            AffinityThread at = new AffinityThread(
                    id + "_" + serial.getAndIncrement(),
                    w,
                    tryPin);
            add(at);
            l.add(w);
        }
        return l;
    }

    protected void add(AffinityThread at) {

        boolean ready = running.tryAcquire();
        if (!ready)
            throw new WTF();
        threads.add(at);
        at.start();
    }
    public void remove(int i) {
        kill(threads.get(i));
    }

    private String dumpThreadInfo() {
        final StringBuilder sb = new StringBuilder();

        final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        for (Thread t : threads) {
            ThreadInfo threadInfo = threadMXBean.getThreadInfo(t.getId());
            sb.append("{");
            sb.append("name=").append(t.getName()).append(",");
            sb.append("id=").append(t.getId()).append(",");
            sb.append("state=").append(threadInfo.getThreadState()).append(",");
            sb.append("lockInfo=").append(threadInfo.getLockInfo());
            sb.append("}");
        }

        return sb.toString();
    }

    public long[] threadIDs() {
        return threads.stream().mapToLong(t -> t.getId()).toArray();
    }
}
