package nars.exe;

import jcog.Service;
import jcog.Util;
import jcog.data.bit.AtomicMetalBitSet;
import jcog.decide.AtomicRoulette;
import jcog.learn.Autoencoder;
import jcog.learn.deep.RBM;
import jcog.list.FasterList;
import nars.NAR;
import nars.control.Cause;
import nars.control.Traffic;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.eclipse.collections.api.block.procedure.primitive.LongIntProcedure;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import static jcog.Util.normalize;
import static nars.util.time.Tense.ETERNAL;

/**
 * decides mental activity
 * this first implementation is similiar to this
 * https://en.wikipedia.org/wiki/Lottery_scheduling
 * https://www.usenix.org/legacy/publications/library/proceedings/osdi/full_papers/waldspurger.pdf
 * <p>
 * https://lwn.net/Articles/720227/
 */
public class Focus extends AtomicRoulette<Causable> {

    /** note: more granularity increases the potential dynamic range
     * (ratio of high to low prioritization). */
    protected static final int PRI_GRANULARITY = 32;

    /**
     * how quickly the iteration demand can grow from previous (max) values
     */
    //static final double IterGrowthIncrement = 1;
    static final double IterGrowthRate = 1.25f;


    private final Exec.Revaluator revaluator;


    private final NAR nar;

    double timesliceNS = 1;


    public Focus(NAR n, Exec.Revaluator r) {
        super(32, Causable[]::new);

        this.nar = n;

        this.revaluator = r;

        n.services.change.on((xa) -> {
            Service<NAR> x = xa.getOne();
            if (x instanceof Causable) {
                Causable c = (Causable) x;
                if (xa.getTwo())
                    add(c);
                else
                    remove(c);
            }
        });
        //add existing
        n.services().filter(x -> x instanceof Causable).forEach(x -> {
           add((Causable) x);
        });

        n.onCycle(this::onCycle);
    }


    public void onCycle(NAR nar) {

        commit(() -> update(nar));

        //sched.
//              try {
//                sched.solve(can, dutyCycleTime);
//
//                //sched.estimatedTimeTotal(can);
//            } catch (InternalSolverError e) {
//                logger.error("{} {}", can, e);
//            }
    }

    @Override
    protected void onAdd(Causable causable, int slot) {
        causable.scheduledID = slot;
    }

    final static int WINDOW = 8;
    private final long[] committed = new long[2];
    private final LongIntProcedure commiter = (timeNS, iter) -> {
        committed[0] = timeNS;
        committed[1] = iter;
    };



    /**
     * next value, while being computed
     */
    protected float[] value = ArrayUtils.EMPTY_FLOAT_ARRAY;

    /**
     * short history of time (in nanoseconds) spent
     */
    public DescriptiveStatistics[] time = null;
    /**
     * short history of iter spent in the corresponding times
     */
    public DescriptiveStatistics[] done = null;
    /**
     * cache for iter.getMean() and time.getMean()
     */
    double[] doneMean = null;
    long[] doneMax = null;
    double[] timeMean = null;
    int[] sliceIters = new int[0];

    final AtomicBoolean updating = new AtomicBoolean(false);

    final AtomicMetalBitSet singletonBusy = new AtomicMetalBitSet();

    protected void update(NAR nar) {

        if (!updating.compareAndSet(false, true))
            return;

        try {
            int n = choice.size();
            if (n == 0)
                return;

            if (sliceIters.length != n)
                realloc(n);

            revaluator.update(nar);

            double jiffy = nar.loop.jiffy.floatValue();
            double throttle = nar.loop.throttle.floatValue();

            //in nS
            double timePerSlice = this.timesliceNS = nar.loop.periodNS() * jiffy * throttle;// / (n / concurrency);

            for (int i = 0; i < n; i++) {
                Causable c = choice.get(i);
                if (c == null)
                    continue; //?

                c.can.commit(commiter);


                long timeNS = committed[0];
                if (timeNS > 0) {

                    DescriptiveStatistics time = this.time[i];
                    time.addValue(timeNS);

                    DescriptiveStatistics done = this.done[i];
                    done.addValue(committed[1]);

                    double timeMeanNS = this.timeMean[i] = time.getMean();

                    this.doneMean[i] = done.getMean();
                    this.doneMax[i] = Math.round(done.getMax());

                    //value per time
                    //value[i] = (float) (c.value() / (Math.max(1E3 /* 1uS in nanos */, timeMeanNS)/1E9));

                    //value
                    value[i] = c.value();

                } else {
                    //value[i] = unchanged
                    value[i] *= 0.99f; //slowly forget
                }

            }


            float[] vRange = Util.minmaxsum(value);
            float vMin = vRange[0];
            float vMax = vRange[1];
            float vSum = vRange[2];
            if (vSum < Float.MIN_NORMAL) vSum = 1; //dont divide by zero

            for (int i = 0; i < n; i++) {
                double vNorm = normalize(value[i], vMin, vMax)/vSum;

                int pri = (int) Util.clampI((PRI_GRANULARITY * vNorm), 1, PRI_GRANULARITY);



                //the iters per timeslice is determined by past measurements
                long doneMost = doneMax[i];
                double timePerIter = timeMean[i]/Math.max(0.5f, doneMean[i]);
                int iterLimit;
                if (doneMost < 1 || !Double.isFinite(timePerIter)) {
                    //assume worst case that one iteration will consume an entire timeslice
                    iterLimit = 1;
                } else {
                    iterLimit = Math.max(1,
                        (int) Math.ceil(Math.min(doneMost * IterGrowthRate, timePerSlice / timePerIter))
                    );
                }

                priGetAndSet(i, pri);
                sliceIters[i] = iterLimit;
            }
            //System.out.println();
        } finally {
            updating.set(false);
        }
    }

    private void realloc(int n) {
        //weight = new float[n];

        time = new DescriptiveStatistics[n];
        timeMean = new double[n];
        done = new DescriptiveStatistics[n];


        for (int i = 0; i < n; i++) {
            time[i] = new DescriptiveStatistics(WINDOW);
            done[i] = new DescriptiveStatistics(WINDOW);
        }

        //            assert (n < 32) : "TODO make atomic n>32 bitset";
        value = new float[n];


        doneMean = new double[n];
        doneMax = new long[n];
        sliceIters = new int[n]; //last
    }


    public boolean tryRun(int x) {
        if (singletonBusy.get(x))
            return false;

        @Nullable Causable cx = this.choice.getSafe(x);
        if (cx == null)
            return false;

//        if (sliceIters.length <= x)
//            return false;

        /** temporarily withold priority */

        boolean singleton = cx.singleton();
        int pri;
        if (singleton) {
            if (!singletonBusy.compareAndSet(x, false, true))
                return false; //someone else got this singleton

            pri = priGetAndSet(x, 0);
        } else {
            pri = pri(x);
        }

        //TODO this growth limit value should decrease throughout the cycle as each execution accumulates the total work it is being compared to
        //this will require doneMax to be an atomic accmulator for accurac



        //System.out.println(cx + " x " + iters + " @ " + n4(iterPerSecond[x]) + "iter/sec in " + Texts.timeStr(subTime*1E9));

        int completed = -1;
        try {
//            System.out.println(cx + " " + this.sliceIters[x]);
            completed = cx.run(nar, this.sliceIters[x]);
        } finally {
            if (singleton) {

                if (completed >= 0) {
                    priGetAndSetIfEquals(x, 0, pri); //release for another usage unless it's already re-activated in a new cycle
                } else {
                    //leave suspended until next commit in the next cycle
                }

                singletonBusy.clear(x);

            } else {
                if (completed < 0) {
                    priGetAndSet(x, 0); //suspend
                }
            }
        }
        return true;
    }


    /**
     * uses an RBM as an adaptive associative memory to learn and reinforce the co-occurrences of the causes
     * the RBM is an unsupervised network to learn and propagate co-occurring value between coherent Causes
     */
    public static class RBMRevaluator extends DefaultRevaluator {

        private final Random rng;
        public double[] next;

        /**
         * learning iterations applied per NAR cycle
         */
        public int learning_iters = 1;

        public double learning_rate = 0.05f;

        public double[] cur;
        public RBM rbm;
        float rbmStrength = 0.25f;
        /**
         * hidden to visible neuron ratio
         */
        private final float hiddenMultipler = 1f;

        public RBMRevaluator(Random rng) {
            super();
            this.rng = rng;
        }

        @Override
        public void update(long time, int dur, FasterList<Cause> causes, float[] goal) {
            super.update(time, dur, causes, goal);

            int numCauses = causes.size();
            if (numCauses < 2)
                return;

            if (rbm == null || rbm.n_visible != numCauses) {
                int numHidden = Math.round(hiddenMultipler * numCauses);

                rbm = new RBM(numCauses, numHidden, null, null, null, rng) {
                };
                cur = new double[numCauses];
                next = new double[numCauses];
            }


            for (int i = 0; i < numCauses; i++)
                cur[i] = Util.tanhFast(causes.get(i).value());

            rbm.reconstruct(cur, next);
            rbm.contrastive_divergence(cur, learning_rate, learning_iters);

            //float momentum = 0.5f;
            //float noise = 0.1f;
            for (int i = 0; i < numCauses; i++) {
                //float j = Util.tanhFast((float) (cur[i] + next[i]));
                float j = /*((rng.nextFloat()-0.5f)*2*noise)*/ +
                        //((float) (next[i]));
                        //(float)( Math.abs(next[i]) > Math.abs(cur[i]) ? next[i] : cur[i]);
                        (float) ((1f - rbmStrength) * cur[i] + rbmStrength * next[i]);
                causes.get(i).setValue(j);
            }
        }
    }

    /**
     * denoising autoencoder revaluator
     */
    public static class AERevaluator extends DefaultRevaluator {

        private final Random rng;


        public float learning_rate = 0.1f;
        public float[] next;
        public Autoencoder ae;
        float NOISE = 0.01f;
        /**
         * hidden to visible neuron ratio
         */
        private final float hiddenMultipler = 0.05f;

        private float[] tmp;

        public AERevaluator(Random rng) {
            super();
            this.momentum = 0.9f;
            this.rng = rng;
        }

        @Override
        protected void update(float[] val) {

            int numCauses = val.length;
            if (numCauses < 2)
                return;

            if (ae == null || ae.inputs() != numCauses) {
                int numHidden = Math.max(2, Math.round(hiddenMultipler * numCauses));

                ae = new Autoencoder(numCauses, numHidden, rng);
                tmp = new float[numHidden];
            }

            next = ae.reconstruct(val, tmp, true, false);

            float err = ae.put(val, learning_rate, NOISE, 0f, true, false);

//            //float momentum = 0.5f;
//            //float noise = 0.1f;
//            for (int i = 0; i < numCauses; i++) {
//                //float j = Util.tanhFast((float) (cur[i] + next[i]));
//                float j = /*((rng.nextFloat()-0.5f)*2*noise)*/ +
//                        //((float) (next[i]));
//                        //(float)( Math.abs(next[i]) > Math.abs(cur[i]) ? next[i] : cur[i]);
//                        (float) ((1f - feedback) * cur[i] + feedback * next[i]);
//                causes.get(i).setValue(j);
//            }
        }
    }

    public static class DefaultRevaluator implements Exec.Revaluator {

        final static double minUpdateDurs = 1f;
//        final RecycledSummaryStatistics[] causeSummary = new RecycledSummaryStatistics[MetaGoal.values().length];
        float momentum =
                //0f;
                0.5f;
                //0.75f;
                //0.9f;
                //0.95f;

        long lastUpdate = ETERNAL;
        /**
         * intermediate calculation buffer
         */
        float[] val = ArrayUtils.EMPTY_FLOAT_ARRAY;

//        {
//            for (int i = 0; i < causeSummary.length; i++)
//                causeSummary[i] = new RecycledSummaryStatistics();
//        }

        @Override
        public void update(long time, int dur, FasterList<Cause> causes, float[] goal) {

            if (lastUpdate == ETERNAL)
                lastUpdate = time;
            double dt = (time - lastUpdate) / ((double) dur);
            if (dt < minUpdateDurs)
                return;
            lastUpdate = time;

//            for (RecycledSummaryStatistics r : causeSummary) {
//                r.clear();
//            }

            int cc = causes.size();

            if (val.length != cc) {
                val = new float[cc];
            }

            for (int i = 0; i < cc; i++) {
                causes.get(i).commit(/*causeSummary*/);
            }


            int goals = goal.length;
//        float[] goalFactor = new float[goals];
//        for (int j = 0; j < goals; j++) {
//            float m = 1;
//                        // causeSummary[j].magnitude();
//            //strength / normalization_magnitude
//            goalFactor[j] = goal[j] / ( Util.equals(m, 0, epsilon) ? 1 : m );
//        }

            final float momentum = (float) Math.pow(this.momentum, dt);
            for (int i = 0; i < cc; i++) {
                Cause c = causes.get(i);

                Traffic[] cg = c.goal;

                //mix the weighted current values of each purpose, each independently normalized against the values (the reason for calculating summary statistics in previous step)
                float v = 0;
                for (int j = 0; j < goals; j++) {
                    v += goal[j] * cg[j].last;
                }

                //float prev = c.value();

//                    0.99f * (1f - Util.unitize(
//                            Math.abs(next) / (1 + Math.max(Math.abs(next), Math.abs(prev)))));

                //c.setValue(Util.lerp(momentum, next, prev));

//                //memory update factor: increase momentum in proportion to their relative strength
//                float ap = Math.abs(prev);
//                float an = Math.abs(next);
//                float den = an + ap;
//                float m = den > Float.MIN_NORMAL ? (ap / den) : 0f;
//                m = Util.lerp(m, momentum, 0.99f);

                float prev = val[i];
                float next = momentum * prev + (1f - momentum) * v;
                val[i] = next;
            }

            update(val);

            for (int i = 0; i < cc; i++)
                causes.get(i).setValue(val[i]);
        }

        /**
         * subclasses can implement their own filters and post-processing of the value vector
         */
        protected void update(float[] val) {

        }

    }

}

//    /**
//     * allocates what can be done
//     */
//    public void cycle(List<Can> can) {
//
//
//        NARLoop loop = nar.loop;
//
//        double nextCycleTime = Math.max(1, concurrency() - 1) * (
//                loop.isRunning() ? loop.periodMS.intValue() * 0.001 : Param.SynchronousExecution_Max_CycleTime
//        );
//
//        float throttle = loop.throttle.floatValue();
//        double dutyCycleTime = nextCycleTime * throttle * (1f - nar.exe.load());
//
//        if (dutyCycleTime > 0) {
//            nar.focus.update(nar);
//
//
//        }
//
//        final double MIN_SLEEP_TIME = 0.001f; //1 ms
//        final int sleepGranularity = 2;
//        int divisor = sleepGranularity * concurrency();
//        double sleepTime = nextCycleTime * (1f - throttle);
//        double sleepEach = sleepTime / divisor;
//        if (sleepEach >= MIN_SLEEP_TIME) {
//            int msToSleep = (int) Math.ceil(sleepTime * 1000);
//            nar.exe.add(new NativeTask.SleepTask(msToSleep, divisor));
//        }
//
//    }