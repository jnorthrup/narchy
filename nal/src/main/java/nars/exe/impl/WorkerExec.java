package nars.exe.impl;

import jcog.Util;
import jcog.data.list.FasterList;
import jcog.random.SplitMix64Random;
import jcog.util.ArrayUtils;
import nars.control.Causable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static java.lang.System.nanoTime;
import static nars.time.Tense.ETERNAL;

public class WorkerExec extends ThreadedExec {

    /** value of 1 means it shares 1/N of the current work. >1 means it will take on more proportionally more-than-fair share of work, which might reduce jitter at expense of responsive */
    float workResponsibility =
            //1f;
            2f;

    //randomize the rescheduling so that the workers tend to be out of phase with each other's next rescheduling
    int rescheduleDithersMin = 16-2, rescheduleDithersMax = 16+2;

    /**
     * process sub-timeslice divisor
     * TODO auto-calculate
     */
    double granularity = 4;
    private static final long subCycleMinNS = 150_000;
    private long subCycleMaxNS;

    public WorkerExec(int threads) {
        super(threads);
    }

    public WorkerExec(int threads, boolean affinity) {
        super(threads, affinity);
    }

    @Override
    protected Supplier<Worker> loop() {
        return WorkPlayLoop::new;
    }

    private final class WorkPlayLoop implements Worker {


        private final FasterList schedule = new FasterList(inputQueueCapacityPerThread);

        Causable.Causation[] play = new Causable.Causation[0];

        private final AtomicBoolean alive = new AtomicBoolean(true);

        final SplitMix64Random rng;

        int i = 0;
        long prioLast = ETERNAL;

        private long rescheduleCycles;
        boolean reprioritize = true;


//        private final BooleanSupplier deadlineFn = this::deadline;
        private int n;

        WorkPlayLoop() {

            rng = new SplitMix64Random((31L * System.identityHashCode(this)) + nanoTime());
        }

        @Override
        public void run() {

            do {

                play(threadWorkTimePerCycle);
                sleep();

            } while (alive.get());


        }



        private void play(long playTime) {

            long start = nanoTime();
            long until = start + playTime, after = start /* assigned for safety */;

            int skip = 0;

            do {
                if (!queueSafe()) {

                    work(workResponsibility, schedule);
                }

                long now = nar.time();
                if (reprioritize || now > prioLast + rescheduleCycles) {
                    if (!prioritize(threadWorkTimePerCycle))
                        return;

                    reprioritize = false;
                    prioLast = now;
                }

                int playable = play.length; if (playable == 0) return;

                if (i + 1 >= playable) i = 0; else i++;
                Causable.Causation next = play[i];

                long sTime = next.time;

                Causable c = next.can;

                boolean played = false;
                if (sTime <= subCycleMinNS / 2 || c.sleeping()) {

                } else {

                    boolean singleton = c.singleton();
                    if (!singleton || c.busy.compareAndSet(false, true)) {

                        long before = nanoTime();

                        long useNS = Util.clampSafe(sTime / rescheduleCycles, subCycleMinNS, subCycleMaxNS);

                        next.runFor(useNS);

                        played = true;
                        after = nanoTime();
                        next.use(after - before);
                    }
                }

                if (!played && ++skip == n) {
                    reprioritize = true;
                    //break; //go to work early
                }

            } while (until > after);

//                System.out.println(
//                    this + "\tplaytime=" + Texts.timeStr(playTime) + " " +
//                        Texts.n2((((double)(after - start))/playTime)*100) + "% used"
//                );

        }

        /**
         * @param workTimeNS expected worktime nanoseconds per cycle
         * @return
         */
        private boolean prioritize(long workTimeNS) {

            /** always refresh */
            play = nar.control.active.toArray(play, Causable::timing);
            if ((n = play.length) <= 0)
                return false;

//                /** expected time will be equal to or less than the max due to various overheads on resource constraints */
//                double expectedWorkTimeNS = (((double)workTimeNS) * expectedWorkTimeFactor); //TODO meter and predict

            //TODO abstract
            int dither = nar.dtDither();
            rescheduleCycles =
                    //nar.dur(); //update current dur
                    Util.lerp(nar.random().nextFloat(), rescheduleDithersMin * dither, rescheduleDithersMax * dither);

            subCycleMaxNS = (long) ((workTimeNS) / granularity);



            if (play.length > 2)
                ArrayUtils.shuffle(play, rng); //each worker gets unique order


            //schedule
            //TODO Util.max((TimedLink.MyTimedLink m) -> m.time, play);
//            long existingTime = Util.sum((TimedLink.MyTimedLink x) -> Math.max(0, x.time), play);
//            long remainingTime = workTimeNS - existingTime;
            long minTime = -Util.max((Causable.Causation x) -> -x.time, play);
            long shift = minTime < 0 ? 1 - minTime : 0;
//            System.out.println(subCycleMinNS + " " + subCycleMaxNS /* actualCycleNS */);
            for (Causable.Causation m : play) {
                double t = workTimeNS * m.pri();
                m.add(Math.max(subCycleMinNS, (long) (shift + t * rescheduleCycles)),
                        -workTimeNS * rescheduleCycles, +workTimeNS * rescheduleCycles);
            }
//                }
            return true;
        }

//        private boolean deadline() {
//            return nanoTime() < deadline;
//        }

        void sleep() {
            long i = (long) (WorkerExec.this.threadIdleTimePerCycle * (((float)concurrency())/exe.maxThreads));
            if (i > 0) {
                Util.sleepNS(NapTimeNS);
                //Util.sleepNSwhile(i, NapTimeNS, () -> queueSafe());
            }
        }

        @Override
        public void off() {
            if (alive.compareAndSet(true,false)) {
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
