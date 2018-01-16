package jcog.exe;

import jcog.TODO;
import jcog.Util;
import jcog.list.FasterList;

import java.util.*;
import java.util.concurrent.*;

/**
 * Created by I330347 on 8/25/2016.
 * https://github.com/JasonChen86899/ThreadPool/blob/master/src/main/java/HPThreadPool.java
 */
public class BusyPool extends AbstractExecutorService {

    final private BlockingQueue q;
    protected List<Thread> workers = new FasterList<>();


    public BusyPool(int threads, BlockingQueue qq) {

        this.q = qq;

        for (int i = 0; i < threads; i++) {
            Thread newthread = new Thread(newWorker(q));

            //ProxyProduce proxyProduce = new ProxyProduce(newthread,recycleRunable);
            //Thread proxyThread =(Thread)proxyProduce.bind();
            workers.add(newthread);
            //proxyThread.start();
            newthread.start();
        }

    }

    protected Worker newWorker(Queue<Runnable> q) {
        return new Worker(q);
    }

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

    public void queue(Object x) {
        q.offer(x);
    }

    protected static class Worker implements Runnable {

        final Queue q;

        protected Worker(Queue q) {
            this.q = q;
        }

        protected void run(Object next) {
            ((Runnable) next).run();
        }

        @Override
        public void run() {

//            long lastBusyNS = System.nanoTime();

            while (true) {
                Object next = null;
//                int done = 0;
                while ((next = q.poll()) != null) {
                    try {
                        run(next);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
//                    done++;
                }

//                if (done > 0)
//                    lastBusyNS = System.nanoTime();

                try {
                    idle();
                } catch (Throwable t) {
                    t.printStackTrace();
                }

            }
        }


        /**
         * called by worker thread
         */
        protected void idle() {
            Thread.yield();
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