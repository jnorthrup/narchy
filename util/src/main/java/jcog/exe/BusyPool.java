package jcog.exe;

import com.conversantmedia.util.concurrent.DisruptorBlockingQueue;
import jcog.TODO;
import jcog.list.FasterList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by I330347 on 8/25/2016.
 * https://github.com/JasonChen86899/ThreadPool/blob/master/src/main/java/HPThreadPool.java
 */
public abstract class BusyPool extends AbstractExecutorService {

    final private BlockingQueue q;
    public List<Thread> workers = new FasterList<>();
    final Worker anonymous;


    public BusyPool(int threads, BlockingQueue qq) {

        this.q = qq;

        anonymous = newWorker(q); //shared by callees when overflow occurrs

        for (int i = 0; i < threads; i++) {
            Thread newthread = new Thread(newWorker(q));

            //ProxyProduce proxyProduce = new ProxyProduce(newthread,recycleRunable);
            //Thread proxyThread =(Thread)proxyProduce.bind();
            workers.add(newthread);
            //proxyThread.start();
            newthread.start();
        }


    }

    abstract protected Worker newWorker(Queue<Runnable> q);

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
        workers.stream().forEach(Thread::interrupt);
        List<Runnable> interrupted = new FasterList();
        interrupted.addAll(q);
        return interrupted;
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
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        throw new TODO();
    }

    @Override
    public final void execute(Runnable command) {
        queue(command);
    }

    static final Logger logger = LoggerFactory.getLogger(BusyPool.class);

    public void queue(Object x) {
        while (!q.offer(x)) {
            //logger.error("lag"); //TODO statistics
            anonymous.poll();
        }
    }

    abstract protected static class Worker implements Runnable {

        final Queue q;
        final Queue localQ = new ArrayDeque();

        protected Worker(Queue q) {
            this.q = q;
        }

        protected void run(Object next) {
            ((Runnable) next).run();
        }


        private void runSafe(Object next) {
            try {
                run(next);
            } catch (Throwable t) {
                logger.error("{} {}", next, t);
            }
        }

        protected boolean poll() {
            Object next;
            if (null != (next = q.poll())) {
                runSafe(next);
                return true;
            }
            return false;
        }
        protected void pollWhileNotEmpty() {
            while (poll()) ;
        }

        protected void drain() {
            int drained = ((DisruptorBlockingQueue) q).drainTo(localQ);
            if (drained > 0) {
                localQ.removeIf(x -> {
                    runSafe(x);
                    return true;
                });
            }
        }


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