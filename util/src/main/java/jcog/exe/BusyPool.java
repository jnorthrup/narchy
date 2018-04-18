package jcog.exe;

import com.conversantmedia.util.concurrent.ConcurrentQueue;
import jcog.TODO;
import jcog.list.FasterList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Started by I330347 on 8/25/2016.
 * https://github.com/JasonChen86899/ThreadPool/blob/master/src/main/java/HPThreadPool.java
 * TODO make # of worker threads growable/shrinkable dynamically
 */
public abstract class BusyPool extends AbstractExecutorService {

    protected final ConcurrentQueue  q;
    public List<Thread> workers = new FasterList<>();
//    final WorkLoop anonymous;
    static final Logger logger = LoggerFactory.getLogger(BusyPool.class);

    /** for safety, q should be a BlockingQueue implementation but if not, ordinary Queue can be used */
    public BusyPool(int threads, ConcurrentQueue<Runnable> q) {
        this.q = q;
//        anonymous = newWorkLoop(q);

        for (int i = 0; i < threads; i++) {
            Thread newthread = new Thread(newWorkLoop(q));

            //ProxyProduce proxyProduce = new ProxyProduce(newthread,recycleRunable);
            //Thread proxyThread =(Thread)proxyProduce.bind();
            workers.add(newthread);
            //proxyThread.start();
            newthread.start();
        }


    }

    abstract protected WorkLoop newWorkLoop(ConcurrentQueue<Runnable> q);

    @Override
    public void shutdown() {
        boolean waiting = true;
        retry:
        while (waiting) {
            //自旋的方式
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
                interrupted.add(()->{ execute((Runnable)xx); }); //TODO see if xx is sometimes not a Runnable and needs run(x)
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
                logger.warn("{} lagged queuing {}", Thread.currentThread(), x); //TODO statistics
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

//        while (!q.offer(x)) {
//            logger.error("lag"); //TODO statistics
//            anonymous.pollNext();
//        }
    }

    protected void queueOverflow(Object x) {
        throw new RuntimeException("queue overflow: " + x);
    }

    public abstract static class WorkLoop implements Runnable {

        final ConcurrentQueue q;
//        final Queue localQ = new ArrayDeque();

        protected WorkLoop(ConcurrentQueue q) {
            this.q = q;
        }

        protected void run(Object next) {
            ((Runnable) next).run();
        }

//        public void runSafe(Object next) {
//            try {
//                run(next);
//            } catch (Throwable t) {
//                logger.error("{} {}", next, t);
//            }
//        }

        protected Object pollNext() {
            return q.poll();
        }



//        protected void drain() {
//            int drained = ((DisruptorBlockingQueue) q).drainTo(localQ);
//            if (drained > 0) {
//                localQ.forEach(this::runSafe);
//                localQ.clear();
//            }
//        }


    }


    public void printInfo() {
        Iterator<Thread> iterator = workers.iterator();
        while (iterator.hasNext()) {
            Thread thread = iterator.next();
            System.out.println(thread.toString() + thread.getState().name());
        }
        System.out.println(q.size());
    }

//    private static class ProxyProduce implements InvocationHandler{
//        private Runnable runnable;
//        private Object target;
//        public ProxyProduce(Object target,Runnable r){
//            this.target = target;
//            this.runnable = r;
//        }
//        public Object bind(){
//            return Proxy.newProxyInstance(target.getClass().getClassLoader(),target.getClass().getInterfaces(),this);
//        }
//        @Override
//        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
//            Object result = method.invoke(target,args);
//            runnable.run();
//            return result;
//        }
//    }
}