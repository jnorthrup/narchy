package nars.exe;

import jcog.Util;
import jcog.exe.AffinityExecutor;
import jcog.exe.util.RunnableForkJoin;
import jcog.list.FasterList;
import jcog.util.Flip;
import nars.NAR;
import nars.task.NALTask;
import nars.task.TaskProxy;
import nars.time.clock.RealTime;

import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class BufferedExec extends UniExec {

    final Flip<List> buffer = new Flip<>(() -> new FasterList<>());
    private long idleTimePerCycle;
    final AtomicBoolean cycleBusy = new AtomicBoolean();

    protected void updateTiming() {
        if (nar.time instanceof RealTime) {
            double throttle = nar.loop.throttle.floatValue();
            double cycleNS = ((RealTime) nar.time).durSeconds() * 1.0E9;
            cpu.cycleTimeNS.set(Math.round(cycleNS * nar.loop.jiffy.floatValue()));

            //TODO better idle calculation in each thread / worker
            idleTimePerCycle = Math.round(Util.clamp(nar.loop.periodNS() * (1 - throttle), 0, cycleNS));
        }
    }
    @Override
    public void execute(Object x) {
        if (x instanceof NALTask || x instanceof TaskProxy) {
            executeNow(x);
        }
//        else if (x instanceof NativeTask.SchedTask) {
//            ForkJoinPool.commonPool().execute(()->((ITask) x).run(nar));
        else
            executeLater(x);
    }

    public void executeLater(Object x) {

        if (!queue.offer(x)) {
            logger.info("{} blocked queue on: {}", this, x);
            queue.add(x);
        }
    }

    @Override
    public void execute(Consumer<NAR> r) {
        executeLater(r);
    }

    @Override
    public void execute(Runnable r) {
        executeLater(r);
    }

    @Override
    public boolean concurrent() {
        return true;
    }


    @Override protected void onDur() {
        super.onDur();
        sharing.commit();
    }

    protected void onCycle() {
        if (nar == null)
            return;

        updateTiming();

        onCycle(concurrent());
    }

    protected void onCycle(boolean concurrent) {
        if (!cycleBusy.compareAndSet(false, true))
            return; //busy
        try {
            List b = buffer.commit();


            int concurrency = concurrent ? Util.concurrency() :1;
            //double timeSliceNS = 100.0 * 1_000_000 * concurrency;
            long timeSliceNS = (concurrent ? concurrency : 1) * cpu.cycleTimeNS.longValue();

            can.forEachValue(c -> {

                double maxIters = 1 + (c.pri() * timeSliceNS / (c.iterTimeNS.getMean() / (1 + c.iterations.getMean())));
                int work = maxIters == maxIters ? (int) Math.max(1, Math.ceil(maxIters)) : 1;

                //int workRequested = c.;
                b.add((Runnable) (() -> { //new NLink<Runnable>(()->{

                    if (c.start()) {
                        c.next(work);
                        c.stop();
                    }


                }));

                //c.c.run(nar, WORK_PER_CYCLE, x -> b.add(x.get()));
            });

            queue.drainTo(b);

            int bn = b.size();
            switch (bn) {
                case 0:
                    return;
//                case 1:
//                    executeNow(b.get(0));
//                    break;
                default:
                    //TODO sort, distribute etc
                    if (bn > 2) {
                        ((FasterList) b).sortThisByInt(x -> x.getClass().hashCode()); //sloppy sort by type
                    }
                    if (concurrency <= 1) {
                        b.forEach(this::executeNow);
                    } else {


                        float granularity = 2;
                        (((FasterList<?>) b).chunkView((int) Math.min(concurrency, b.size() / (concurrency * granularity))))
                                .parallelStream().forEach(x -> x.forEach(this::executeNow));

//                                .forEach(c -> {
//                            execute(() -> c.forEach(this::executeNow));
//                        });

                        //Stream<Object> s = Arrays.stream(((FasterList) b).array(), 0, bn).parallel();
                        //s.forEach(this::executeNow);
                    }

                    //ForkJoinPool.commonPool().invokeAll(b);
                    //Arrays.stream(((FasterList)b).array(), 0, bn).parallel().forEach(this::executeNow);
                    //(parallel ? b.parallelStream() : b.stream()).forEach(this::executeNow);
                    break;
            }

            b.clear();

        } finally {
            cycleBusy.set(false);
        }

    }


    public static class WorkerExec extends BufferedExec {

        public final int threads;
        final AffinityExecutor exe = new AffinityExecutor();

        public WorkerExec(int threads) {
            this.threads = threads;

        }

        @Override
        public void start(NAR n) {

            synchronized (this) {
                super.start(n);


                exe.execute(() -> {
                    while (true) {
                        super.onCycle(false);
                    }
                }, threads);
            }

        }

        @Override
        public void stop() {
            synchronized (this) {
                exe.shutdownNow();
                super.stop();
            }
        }

        @Override
        protected void onCycle() {
            updateTiming();
        }
    }

    public static class ForkJoinExec extends BufferedExec {

        public final Semaphore threads;

        public ForkJoinExec(int threads) {
            this.threads = new Semaphore(threads);

        }

//        @Override
//        public void execute(Runnable r) {
//            ForkJoinPool.commonPool().execute(r);
//        }


        final class ForkJoinWorker extends RunnableForkJoin {


            @Override
            public boolean exec() {
                onCycle(false);
                //if (running) {
                fork();

                return false;
//                    return false;
//                } else {
//                    return true;
//                }

            }
        }

        @Override
        public void start(NAR n) {

            synchronized (this) {

                super.start(n);

                int i1 = threads.availablePermits(); //TODO
                for (int i = 0; i < i1; i++) {
                    spawn();
                }
            }

        }

        private void spawn() {
            ForkJoinPool.commonPool().execute((ForkJoinTask<?>) new ForkJoinWorker());
        }


        @Override
        public void stop() {
            synchronized (this) {
                //exe.shutdownNow();
                super.stop();
            }
        }

        @Override
        protected void onCycle() {
            updateTiming();
        }
    }
}
