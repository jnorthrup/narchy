package jcog.exe;

import com.conversantmedia.util.concurrent.ConcurrentQueue;
import jcog.TODO;
import jcog.data.list.FasterList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Started by I330347 on 8/25/2016.
 * https:
 * TODO make # of worker threads growable/shrinkable dynamically
 */
public abstract class BusyPool extends AbstractExecutorService {

    protected final ConcurrentQueue  q;
    public List<Thread> workers = new FasterList<>();

    static final Logger logger = LoggerFactory.getLogger(BusyPool.class);

    /** for safety, q should be a BlockingQueue implementation but if not, ordinary Queue can be used */
    public BusyPool(int threads, ConcurrentQueue<Runnable> q) {
        this.q = q;


        for (int i = 0; i < threads; i++) {
            Thread newthread = new Thread(newWorkLoop(q));

            
            
            workers.add(newthread);
            
            newthread.start();
        }


    }

    abstract protected WorkLoop newWorkLoop(ConcurrentQueue<Runnable> q);

    @Override
    public void shutdown() {
        boolean waiting = true;
        retry:
        while (waiting) {
            
            waiting = false;
            for (Thread worker : workers) {
                if (worker.getState() != Thread.State.WAITING) {
                    waiting = true;
                    continue retry;
                }
            }
        }
        workers.stream().filter(thread -> thread.getState() == Thread.State.WAITING).forEach(Thread::interrupt);
    }

    @Override
    public List<Runnable> shutdownNow() {
        synchronized (this) {
            workers.stream().forEach(Thread::interrupt);
            List<Runnable> interrupted = new FasterList();

            Object x = null;
            while ((x = q.poll())!=null) {
                Object xx = x;
                interrupted.add(()->{ execute((Runnable)xx); }); 
            }

            return interrupted;
        }
    }

    @Override
    public boolean isShutdown() {
        throw new TODO();
    }

    @Override
    public boolean isTerminated() {
        throw new TODO();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
        throw new TODO();
    }

    @Override
    public final void execute(Runnable command) {
        queue(command);
    }



    public void queue(Object x) {
        if (!q.offer(x)) {
            if (q instanceof BlockingQueue) {
                logger.warn("{} lagged queuing {}", Thread.currentThread(), x); 
                try {
                    ((BlockingQueue) q).put(x);
                } catch (InterruptedException e) {
                    logger.error("interrupted {}", e);
                }
            } else {
                logger.warn("{} queue overflow {}", Thread.currentThread(), x);
                queueOverflow(x);
            }
        }





    }

    protected void queueOverflow(Object x) {
        throw new RuntimeException("queue overflow: " + x);
    }

    public abstract static class WorkLoop implements Runnable {

        protected final ConcurrentQueue q;


        protected WorkLoop(ConcurrentQueue q) {
            this.q = q;
        }




























    }




























}