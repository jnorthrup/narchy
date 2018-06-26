package nars.exe;

import jcog.Util;
import jcog.exe.AffinityExecutor;
import jcog.list.FasterList;
import jcog.util.Flip;
import nars.NAR;
import nars.task.NALTask;
import nars.task.TaskProxy;
import nars.time.clock.RealTime;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

abstract public class BufferedExec extends UniExec {


    protected long idleTimePerCycle;


    protected void updateTiming() {
        if (nar.time instanceof RealTime) {
            double throttle = nar.loop.throttle.floatValue();
            double cycleNS = ((RealTime) nar.time).durSeconds() * 1.0E9;

            //TODO better idle calculation in each thread / worker
            idleTimePerCycle = Math.round(Util.clamp(cycleNS * (1 - throttle), 0, cycleNS));

            cpu.cycleTimeNS.set(Math.max(1, Math.round(cycleNS * throttle)));

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

        if (x != null && !in.offer(x)) {
            logger.info("{} blocked queue on: {}", this, x);
            in.add(x);
        }
    }

    @Override
    public void execute(Consumer<NAR> r) {
        executeLater(r);
    }


    @Override
    public boolean concurrent() {
        return true;
    }


    @Override
    protected void onDur() {
        super.onDur();
        sharing.commit();
    }

    protected void onCycle() {
        if (nar == null)
            return;

        updateTiming();

    }

    protected void onCycle(List b, boolean concurrent) {


        int concurrency = concurrent ? Util.concurrency() : 1;
        //double timeSliceNS = 100.0 * 1_000_000 * concurrency;

        long timeSliceNS = cpu.cycleTimeNS.longValue();



        in.drainTo(b, (int) Math.ceil(in.size()*(1f/concurrency)));


        long dutyTimeStart = System.nanoTime();

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
                    int chunkSize = Math.max(1, (int) Math.min(concurrency, b.size() / (concurrency * granularity)));

                    (((FasterList<?>) b).chunkView(chunkSize))
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
        long dutyTimeEnd = System.nanoTime();
        timeSliceNS = timeSliceNS - Math.max(0, (dutyTimeEnd-dutyTimeStart));

        b.clear();


        double finalTimeSliceNS = Math.max(1,timeSliceNS * nar.loop.jiffy.floatValue());
        can.forEachValue(c -> {
            int MAX_ITER = 4096;
            double iterTimeMean = c.iterTimeNS.getMean();
            int work;
            if (iterTimeMean == iterTimeMean) {
                double maxIters = (c.pri() * finalTimeSliceNS / (iterTimeMean / (c.iterations.getMean())));
                work = maxIters == maxIters ? (int) Math.round(Math.max(1, Math.min(MAX_ITER, maxIters))) : 1;
            } else {
                work = 1;
            }
//            System.out.println(c + " " + work);

            //int workRequested = c.;
            b.add((Runnable) (() -> { //new NLink<Runnable>(()->{

                if (c.start()) {
                    c.next(work);
                    c.stop();
                }


            }));

            //c.c.run(nar, WORK_PER_CYCLE, x -> b.add(x.get()));
        });
    }

    public static class UniBufferedExec extends BufferedExec {
        final Flip<List> buffer = new Flip<>(() -> new FasterList<>());

        final AtomicBoolean cycleBusy = new AtomicBoolean();

        @Override
        protected void onCycle() {
            super.onCycle();

            if (!cycleBusy.compareAndSet(false, true))
                return; //busy
            try {
                onCycle(buffer.write(), concurrent());
            } finally {
                cycleBusy.set(false);
            }
        }
    }

    public static class WorkerExec extends BufferedExec {

        public final int threads;
        final AffinityExecutor exe = new AffinityExecutor();

        boolean running;


        public WorkerExec(int threads) {
            this.threads = threads;


        }

        @Override
        public void start(NAR n) {


            synchronized (this) {

                running = true;

                super.start(n);


                exe.execute(MyRunnable::new, threads);
            }

        }


        @Override
        public void stop() {
            synchronized (this) {
                running = false;
                exe.shutdownNow();
                super.stop();
            }
        }

        @Override
        protected void onCycle() {
            updateTiming();
        }

        private class MyRunnable implements Runnable {

            final List buffer = new FasterList(1024);

            @Override
            public void run() {
                while (running) {
                    WorkerExec.super.onCycle(buffer, false);

                    Util.sleepNS(idleTimePerCycle);
                }
            }
        }
    }

//    public static class ForkJoinExec extends BufferedExec {
//
//        public final Semaphore threads;
//
//        public ForkJoinExec(int threads) {
//            this.threads = new Semaphore(threads);
//
//        }
//
////        @Override
////        public void execute(Runnable r) {
////            ForkJoinPool.commonPool().execute(r);
////        }
//
//
//        final class ForkJoinWorker extends RunnableForkJoin {
//
//
//            final List localBuffer = new FasterList(1024);
//
//            @Override
//            public boolean exec() {
//
//                    try {
//                        onCycle(localBuffer, false);
//                        //if (running) {
//                    } finally {
//                        threads.release();
//                        fork();
//                    }
//                    return false;
//
//
////                    return false;
////                } else {
////                    return true;
////                }
//
//            }
//        }
//
//        @Override
//        public void start(NAR n) {
//
//            synchronized (this) {
//
//                super.start(n);
//
//                int i1 = threads.availablePermits(); //TODO
//                for (int i = 0; i < i1; i++)
//                    spawn();
//
//            }
//
//        }
//
//        private void spawn() {
//            ForkJoinPool.commonPool().execute((ForkJoinTask<?>) new ForkJoinWorker());
//        }
//
//
//        @Override
//        public void stop() {
//            synchronized (this) {
//                //exe.shutdownNow();
//                super.stop();
//            }
//        }
//
//        @Override
//        protected void onCycle() {
//            updateTiming();
//        }
//    }
}
