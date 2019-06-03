package nars.exe.impl;

import jcog.Util;
import jcog.data.list.FasterList;
import jcog.exe.Exe;
import jcog.random.XoRoShiRo128PlusRandom;
import nars.attention.AntistaticBag;
import nars.attention.What;
import nars.control.How;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static java.lang.System.nanoTime;

public class WorkerExec extends ThreadedExec {

    private static final long subCycleMinNS = 20L * 1_000_000;
    double granularity = 8;

    /**
     * value of 1 means it shares 1/N of the current work. >1 means it will take on more proportionally more-than-fair share of work, which might reduce jitter at expense of responsive
     */
    float workResponsibility =
            1f;
            //1.5f;
            //1.618f;
            //2f;

    /**
     * process sub-timeslice divisor
     * TODO auto-calculate
     */
    private long subCycleMaxNS;

    public WorkerExec(int threads) {
        super(threads);
    }

    public WorkerExec(int threads, boolean affinity) {
        super(threads, affinity);

        Exe.setExecutor(this);
    }

    @Override
    protected void update() {
        nar.how.commit(null);
        nar.what.commit(null);
        super.update();
    }

    @Override
    protected Supplier<Worker> loop() {
        return WorkPlayLoop::new;
    }

    @Override
    public void synch() {
        if (this.exe.running() == 0) {
            in.clear(this::executeNow); //initialize
        }
    }

    private final class WorkPlayLoop implements ThreadedExec.Worker {


        final Random rng;
        private final FasterList schedule = new FasterList(inputQueueCapacityPerThread);
        private final AtomicBoolean alive = new AtomicBoolean(true);


        WorkPlayLoop() {

            rng = new XoRoShiRo128PlusRandom((31L * System.identityHashCode(this)) + nanoTime());
        }

        @Override
        public void run() {

            do {

                work(workResponsibility, schedule);

                play(threadWorkTimePerCycle);
                sleep();

            } while (alive.get());
        }


        private void play(long playTime) {

            long start = nanoTime();
            long until = start + playTime, after = start /* assigned for safety */;

            int skip = 0;
            AntistaticBag<How> H = nar.how;
            AntistaticBag<What> W = nar.what;

            do {



                long now = nar.time();

                subCycleMaxNS = Math.max(subCycleMinNS, (long) ((threadWorkTimePerCycle) / granularity));


                How h = H.sample(rng);
                if (h == null || !h.isOn()) continue; //HACK

                //if (j + 1 >= Wn) j = 0; else j++;
                int Wn = W.size();
                if (Wn == 0) return;
                What w = W.sample(rng);
                if (!w.isOn()) continue; //HACK

                boolean singleton = h.singleton();
                if (!singleton || h.busy.compareAndSet(false, true)) {

                    long before = nanoTime();

                    long useNS = //Util.lerp(h.pri() * w.pri(), subCycleMinNS, subCycleMaxNS);
                                subCycleMaxNS;
                    try {
                        h.runFor(w, useNS);
                    } finally {
                        if (singleton)
                            h.busy.set(false);
                    }

                    after = nanoTime();
                }

            } while (until > after);

//                System.out.println(
//                    this + "\tplaytime=" + Texts.timeStr(playTime) + " " +
//                        Texts.n2((((double)(after - start))/playTime)*100) + "% used"
//                );

        }

        void sleep() {
            long i = (long) (WorkerExec.this.threadIdleTimePerCycle * (((float) concurrency()) / exe.maxThreads));
            if (i > 0) {
                Util.sleepNS(NapTimeNS);
                //Util.sleepNSwhile(i, NapTimeNS, () -> queueSafe());
            }
        }

        @Override
        public void close() {
            if (alive.compareAndSet(true, false)) {
                //execute remaining tasks in callee's thread
                schedule.removeIf(x -> {
                    if (x != null)
                        executeNow(x);
                    return true;
                });
            }
        }
    }

}
