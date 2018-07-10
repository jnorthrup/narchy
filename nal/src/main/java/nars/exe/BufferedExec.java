package nars.exe;

import jcog.TODO;
import jcog.Util;
import jcog.event.Off;
import jcog.exe.AffinityExecutor;
import jcog.exe.Exe;
import jcog.exe.realtime.FixedRateTimedFuture;
import jcog.list.FasterList;
import nars.NAR;
import nars.NARLoop;
import nars.task.ITask;
import nars.task.NALTask;
import nars.task.TaskProxy;
import nars.time.clock.RealTime;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

abstract public class BufferedExec extends UniExec {

    private final static int CAN_ITER_MAX = 4096;

    protected volatile long idleTimePerCycle;

    @Override
    public void execute(Object x) {
        if (x instanceof NALTask || x instanceof TaskProxy)
            executeNow((ITask)x);
        else
            executeLater(x);
    }

    public void executeLater(@NotNull Object x) {

        if (!in.offer(x)) {
            logger.warn("{} blocked queue on: {}", this, x);
            //in.add(x);
            executeNow(x);
        }
    }

    @Override
    public final void execute(Runnable r) {
        if (r instanceof FixedRateTimedFuture && ((((FixedRateTimedFuture)r).run instanceof NARLoop))) {
            r.run(); //high-priority
        } else {
            executeLater(r);
        }
    }

    @Override
    public final void execute(Consumer<NAR> r) {
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

    protected void onCycle(NAR nar) {

        if (nar.time instanceof RealTime) {
            double throttle = nar.loop.throttle.floatValue();
            double cycleNS = ((RealTime) nar.time).durSeconds() * 1.0E9;

            //TODO better idle calculation in each thread / worker
            idleTimePerCycle = Math.round(Util.clamp(cycleNS * (1 - throttle), 0, cycleNS));

            cpu.cycleTimeNS.set(Math.max(1, Math.round(cycleNS * throttle)));

        } else
            throw new TODO();

        nar.time.scheduled(this::executeLater);

    }

    /** work and play execution */
    protected void onCycle(List b, int concurrency) {


        if (concurrency!=1)
            throw new TODO("just need parallel execution of the 'play' phase");



        //in.drainTo(b, (int) Math.ceil(in.size() * (1f / Math.max(1, (concurrency - 1)))));
        in.clear(b::add, (int) Math.ceil(in.size() * (1f / Math.max(1, (concurrency - 1)))));


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

        b.clear();

        long dutyTimeEnd = System.nanoTime();
        long timeSliceNS = cpu.cycleTimeNS.longValue()- Math.max(0, (dutyTimeEnd - dutyTimeStart));
        double finalTimeSliceNS = Math.max(1, timeSliceNS * nar.loop.jiffy.doubleValue());
        can.forEachValue(c -> {
            if (c.c.instance.availablePermits() == 0)
                return;


            double iterTimeMean = c.iterTimeNS.getMean();
            int work;
            if (iterTimeMean == iterTimeMean) {
                double maxIters = (c.pri() * finalTimeSliceNS / (iterTimeMean / Math.max(1,c.iterations.getMean())));
                work = (maxIters == maxIters) ? (int) Math.round(Math.max(1, Math.min(CAN_ITER_MAX, maxIters))) : 1;
            } else {
                work = 1;
            }
//            System.out.println(c + " " + work);

            //int workRequested = c.;
            //b.add((Runnable) (() -> { //new NLink<Runnable>(()->{

                if (c.start()) {
                    try {
                        c.next(work);
                    } finally {
                        c.stop();
                    }
                }


            //}));

            //c.c.run(nar, WORK_PER_CYCLE, x -> b.add(x.get()));
        });
    }

//    public static class UniBufferedExec extends BufferedExec {
//        final Flip<List> buffer = new Flip<>(FasterList::new);
//
//        final AtomicBoolean cycleBusy = new AtomicBoolean();
//
//        @Override
//        protected void onCycle() {
//            super.onCycle();
//
//            if (!cycleBusy.compareAndSet(false, true))
//                return; //busy
//            try {
//                onCycle(buffer.write(), concurrent());
//            } finally {
//                cycleBusy.set(false);
//            }
//        }
//    }

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


                exe.execute(MyRunnable::new, threads);

                /** absorb system-wide tasks rather than using the default ForkJoin commonPool */
                Exe.setExecutor(this);
            }

        }


        @Override
        public void stop() {
            synchronized (this) {
                Exe.setExecutor(ForkJoinPool.commonPool()); //TODO use the actual executor replaced by the start() call instead of assuming FJP

                exe.shutdownNow();


                in.clear(this::executeNow);

                super.stop();
            }
        }


        private final class MyRunnable implements Runnable, Off {

            private final List buffer = new FasterList(1024);

            private boolean running = true;


            @Override
            public void run() {
                while (running) {
                    WorkerExec.super.onCycle(buffer, 1);

                    Util.sleepNS(idleTimePerCycle);
                }
            }

            @Override
            public synchronized void off() {
                running = false;

                //execute remaining tasks in callee's thread
                buffer.removeIf(x->{
                    executeNow(x);
                    return true;
                });
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
