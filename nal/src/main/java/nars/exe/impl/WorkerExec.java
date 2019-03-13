package nars.exe.impl;

import jcog.Util;
import jcog.data.list.FasterList;
import jcog.random.SplitMix64Random;
import jcog.util.ArrayUtils;
import nars.exe.Causable;
import nars.exe.Exec;
import nars.exe.Valuator;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static java.lang.System.nanoTime;
import static nars.time.Tense.ETERNAL;

public class WorkerExec extends ThreadedExec {

    /** process sub-timeslice divisor */
    double granularity = 8;

    public WorkerExec(Valuator r, int threads) {
        super(r, threads);
    }

    public WorkerExec(Valuator valuator, int threads, boolean affinity) {
        super(valuator, threads, affinity);
    }

    @Override
    protected Supplier<Worker> loop() {
        return WorkPlayLoop::new;
    }

    private final class WorkPlayLoop implements Worker {

        private long subCycleMinNS = 50_000;
        private long subCycleMaxNS;

        private final FasterList schedule = new FasterList(inputQueueCapacityPerThread);

        TimedLink.MyTimedLink[] play = new TimedLink.MyTimedLink[0];

        private boolean alive = true;

        final SplitMix64Random rng;
        private long deadline;

        int i = 0;
        long prioLast = ETERNAL;
        private int n;

        private long priorityPeriod;
        boolean reprioritize = true;

        WorkPlayLoop() {
            rng = new SplitMix64Random((31L * System.identityHashCode(this)) + nanoTime());
        }

        public void run0() {

            while (alive) {

                long workTime = work(1f, schedule);

                long playTime =
                        threadWorkTimePerCycle - workTime;
                //threadWorkTimePerCycle;
                if (playTime > 0)
                    play(playTime);

                sleep();
            }
        }

        @Override
        public void run() {

            while (alive) {

                long playTime =
                        threadWorkTimePerCycle;

                if (playTime > 0)
                    play(playTime);

                work(1f, schedule);

                sleep();
            }
        }



        private final BooleanSupplier deadlineFn = this::deadline;

        private void play(long playTime) {

            n = cpu.size();
            if (n == 0)
                return;



            long start = nanoTime();
            long until = start + playTime, after = start /* assigned for safety */;


            int skip = 0;

            do {

                long now = nar.time();
                if (reprioritize || now > prioLast + priorityPeriod) {
                    reprioritize = false;


                    priorityPeriod =
                            nar.dur(); //update current dur
                    //2 * nar.dtDither();

                    prioLast = now;
                    prioritize(threadWorkTimePerCycle);
                }

                TimedLink.MyTimedLink s = play[i++];
                if (i == n) i = 0;

                long sTime = s.time;

                Causable c = s.can;

                boolean played = false;
                if (sTime <= 0 || c.sleeping()) {

                } else {

                    boolean singleton = c.singleton();
                    if (!singleton || c.busy.compareAndSet(false, true)) {

                        try {
                            long before = nanoTime();

                            long runtimeNS = Math.min(until - before, Math.min(sTime, subCycleMaxNS));
                            if (runtimeNS > 0) {

                                try {

                                    deadline = before + runtimeNS;
                                    c.next(nar, deadlineFn);
                                } catch (Throwable t) {
                                    Exec.logger.error("{} {}", this, t);
                                }

                                played = true;
                                after = nanoTime();
                                s.use(after - before);

                            }

                        } finally {
                            if (singleton)
                                c.busy.set(false);
                        }
                    }
                }

                if (!played && ++skip == n) {
                    reprioritize = true;
                    //break; //go to work early
                }

            } while ((until > after) && queueSafe());
//                System.out.println(
//                    this + "\tplaytime=" + Texts.timeStr(playTime) + " " +
//                        Texts.n2((((double)(after - start))/playTime)*100) + "% used"
//                );
        }

        private void prioritize(long workTimeNS) {

            int n = this.n;
            if (n == 0)
                return;

//                /** expected time will be equal to or less than the max due to various overheads on resource constraints */
//                double expectedWorkTimeNS = (((double)workTimeNS) * expectedWorkTimeFactor); //TODO meter and predict

            subCycleMaxNS = (long) (cycleNS / granularity);

            if (play.length != n) {
                //TODO more careful test for change
                play = new TimedLink.MyTimedLink[n];
                for (int i = 0; i < n; i++)
                    play[i] = cpu.get(i).my();
            }

            if (n > 2)
                ArrayUtils.shuffle(play, rng); //each worker gets unique order


            //schedule
            //TODO Util.max((TimedLink.MyTimedLink m) -> m.time, play);
            long minTime = -Util.max((TimedLink.MyTimedLink x) -> -x.time, play);
            long existingTime = Util.sum((TimedLink.MyTimedLink x) -> Math.max(0, x.time), play);
            long remainingTime = workTimeNS - existingTime;
//                if (remainingTime > subCycleMinNS) {
            long shift = minTime < 0 ? 1 - minTime : 0;
            for (TimedLink.MyTimedLink m : play) {
                int t = Math.round(shift + remainingTime * m.pri());
                m.add(Math.max(subCycleMinNS, t) * priorityPeriod, -workTimeNS, +workTimeNS);
            }
//                }
        }

        private boolean deadline() {
            return nanoTime() < deadline;
        }

        void sleep() {
            long i = nars.exe.impl.WorkerExec.this.threadIdleTimePerCycle;
            if (i > 0) {
                Util.sleepNSwhile(i, NapTime, nars.exe.impl.WorkerExec.this::queueSafe);
            }
        }

        @Override
        public void off() {
            if (alive) {
                synchronized (this) {
                    alive = false;

                    //execute remaining tasks in callee's thread
                    schedule.removeIf(x -> {
                        executeNow(x);
                        return true;
                    });
                }
            }
        }
    }

}
