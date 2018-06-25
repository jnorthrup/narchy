package nars.exe;

import jcog.Util;
import jcog.exe.AffinityExecutor;
import jcog.list.FasterList;
import jcog.util.Flip;
import nars.NAR;
import nars.task.NALTask;
import nars.task.TaskProxy;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class BufferedExec extends UniExec {

    final Flip<List> buffer = new Flip<>(() -> new FasterList<>());

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

    final AtomicBoolean busy = new AtomicBoolean();

    protected void onCycle() {
        onCycle(concurrent());
    }

    protected void onCycle(boolean concurrent) {
        if (!busy.compareAndSet(false, true))
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
            busy.set(false);
        }
    }


    public static class AsyncExec extends BufferedExec {

        public final int threads;
        final AffinityExecutor exe = new AffinityExecutor();

        public AsyncExec(int threads) {
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
            //not called this way
        }
    }

}
