package nars.exe.impl;

import jcog.Util;
import jcog.data.list.FasterList;
import jcog.math.FloatAveragedWindow;
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

    /**
     * process sub-timeslice divisor
     * TODO auto-calculate
     */
    double granularity = 4;
    private static final long subCycleMinNS = 100_000;
    private long subCycleMaxNS;

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


        private final FasterList schedule = new FasterList(inputQueueCapacityPerThread);

        TimedLink.MyTimedLink[] play = new TimedLink.MyTimedLink[0];

        private boolean alive = true;

        final SplitMix64Random rng;
        private long deadline;

        int i = 0;
        long prioLast = ETERNAL;
        private int n;

        private long rescheduleCycles;
        boolean reprioritize = true;

        WorkPlayLoop() {

            rng = new SplitMix64Random((31L * System.identityHashCode(this)) + nanoTime());
        }

//        public void run0() {
//
//            while (alive) {
//
//                long workTime = work(1f, schedule);
//
//                long playTime =
//                        threadWorkTimePerCycle - workTime;
//                //threadWorkTimePerCycle;
//                if (playTime > 0)
//                    play(playTime);
//
//                sleep();
//            }
//        }


        //TODO use a double averaged
        final FloatAveragedWindow workTimeMean = new FloatAveragedWindow(4, 0.5f);

        @Override
        public void run() {

            do {

                long workTime = work(1, schedule);
                //TODO use time-averaged workTime
                float workTimeMean = this.workTimeMean.valueOf((float) (workTime / 1.0E6)/*, i++ % 100 == 0 */);
                long playTime =
                        (long) (threadWorkTimePerCycle - (workTimeMean * 1.0E6));

                if (playTime > 0)
                    play(playTime);

                sleep();

            } while (alive);


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
                if (reprioritize || now > prioLast + rescheduleCycles) {
                    reprioritize = false;


                    prioLast = now;
                    prioritize(threadWorkTimePerCycle);
                }

                TimedLink.MyTimedLink next = play[i++];
                if (i == n) i = 0;

                long sTime = next.time;

                Causable c = next.can;

                boolean played = false;
                if (sTime <= subCycleMinNS / 2 || c.sleeping()) {

                } else {

                    boolean singleton = c.singleton();
                    if (!singleton || c.busy.compareAndSet(false, true)) {

                        long before = nanoTime();

                        long useNS = Util.clampSafe(sTime / rescheduleCycles, subCycleMinNS, subCycleMaxNS);

                        try {

                            deadline = before + useNS;
                            c.next(nar, deadlineFn);

                        } catch (Throwable t) {
                            Exec.logger.error("{} {}", this, t);
                        } finally {
                            if (singleton)
                                c.busy.set(false);
                        }

                        played = true;
                        after = nanoTime();
                        next.use(after - before);
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

        /**
         * @param workTimeNS expected worktime nanoseconds per cycle
         */
        private void prioritize(long workTimeNS) {

            int n = this.n;
            if (n == 0)
                return;

//                /** expected time will be equal to or less than the max due to various overheads on resource constraints */
//                double expectedWorkTimeNS = (((double)workTimeNS) * expectedWorkTimeFactor); //TODO meter and predict

            //TODO abstract
            //randomize the rescheduling so that the workers tend to be out of phase with each other's next rescheduling
            rescheduleCycles =
                    //nar.dur(); //update current dur
                    Util.lerp(nar.random().nextFloat(), 1 * nar.dtDither(), 2 * nar.dtDither());

            subCycleMaxNS = (long) ((workTimeNS) / granularity);

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
//            long existingTime = Util.sum((TimedLink.MyTimedLink x) -> Math.max(0, x.time), play);
//            long remainingTime = workTimeNS - existingTime;
            long minTime = -Util.max((TimedLink.MyTimedLink x) -> -x.time, play);
            long shift = minTime < 0 ? 1 - minTime : 0;
//            System.out.println(subCycleMinNS + " " + subCycleMaxNS /* actualCycleNS */);
            for (TimedLink.MyTimedLink m : play) {
                double t = workTimeNS * m.pri();
                m.add(Math.max(subCycleMinNS, (long) (shift + t * rescheduleCycles)),
                        -workTimeNS * rescheduleCycles, +workTimeNS * rescheduleCycles);
            }
//                }
        }

        private boolean deadline() {
            return nanoTime() < deadline;
        }

        void sleep() {
            long i = nars.exe.impl.WorkerExec.this.threadIdleTimePerCycle;
            if (i > 0) {
                Util.sleepNSwhile(i, NapTimeNS, () -> queueSafe());
            }
        }

        @Override
        public void off() {
            if (alive) {
                synchronized (this) {
                    alive = false;

                    //execute remaining tasks in callee's thread
                    schedule.removeIf(x -> {
                        if (x!=null)
                            executeNow(x);
                        return true;
                    });
                }
            }
        }
    }

}
