package nars.exe;

import com.google.common.base.Joiner;
import jcog.Services;
import jcog.Util;
import jcog.data.bit.MetalBitSet;
import jcog.decide.Roulette;
import jcog.learn.Autoencoder;
import jcog.learn.deep.RBM;
import jcog.list.FastCoWList;
import jcog.list.FasterList;
import jcog.math.RecycledSummaryStatistics;
import jcog.util.Flip;
import nars.NAR;
import nars.control.Cause;
import nars.control.MetaGoal;
import nars.control.Traffic;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.eclipse.collections.api.block.procedure.primitive.LongLongProcedure;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.IntPredicate;
import java.util.function.LongSupplier;
import java.util.stream.IntStream;

import static jcog.Texts.n2;
import static jcog.Texts.n4;
import static jcog.Util.normalize;
import static nars.time.Tense.ETERNAL;

/**
 * decides mental activity
 */
public class Focus extends Flip<Focus.Schedule> {

    //final static Logger logger = LoggerFactory.getLogger(Focus.class);

    public final Exec.Revaluator revaluator;

    private final FastCoWList<Causable> can;

    /**
     *
     * @param startBatch - supplies the max amount of nanoseconds an execution can be calculated to
     *                   expect to run for, determining the temporal granularity of the scheduling
     *
     * @param kontinue
     * @param rng
     * @param nar
     */
    public void run(LongSupplier startBatch, BooleanSupplier kontinue, Random rng, NAR nar) {

        long maxExeTimeNS = 0;
        while ((maxExeTimeNS = startBatch.getAsLong()) >= 0) {

            if (maxExeTimeNS <= 0) {
                idle(kontinue); //empty batch
            } else {

                Focus.Schedule s = read();
                float[] sw = s.weight;
                int n = sw.length;

                if (n == 0) {
                    idle(kontinue); //empty batch
                } else {
//                    float[] iterPerSecond = s.time;
//                    float[] weight= s.weight;

                    final int[] safety = {n};
                    Causable[] can = s.can;

                    MetalBitSet active = s.active;

                    long rt = maxExeTimeNS;

                    int iterAtStart = intValue();
                    Roulette.decideRouletteWhile(n, c -> sw[c], rng, (IntPredicate) x -> {

                        if (!active.get(x)) {
                            return !active.isAllOff();
                        }

                        Causable cx = can[x];
                        AtomicBoolean cb = cx.busy;
                        if (cb != null) {
                            if (!cb.compareAndSet(false, true)) {
                                return --safety[0] < 0;
                            } else {
                                active.clear(x); //acquire
                                safety[0] = n; //reset safety count
                            }
                        }


                        int itersNext = 1;
                        double itersPrev = s.doneMean[x];

                        final double ITER_EPSILON = 0.001;
                        if (itersPrev == itersPrev && itersPrev > ITER_EPSILON) {

                            double timeNS = s.timeMean[x];
                            if (timeNS == timeNS && timeNS > 0) {
                                itersNext = (int) Math.max(1,
                                     Math.round(itersPrev * rt/timeNS )
                                );
                            }

                        }

                        //System.out.println(cx + " x " + iters + " @ " + n4(iterPerSecond[x]) + "iter/sec in " + Texts.timeStr(subTime*1E9));

                        int completed = -1;
                        try {
                            completed = cx.run(nar, itersNext);
                        } finally {
                            if (cb != null) {
                                cb.set(false); //release
                                if (completed >= 0) {
                                    active.set(x);
                                } else {
                                    //leave inactive
                                }
                            } else {
                                if (completed < 0) {
                                    active.clear(x); //set inactive
                                }
                            }
                        }

                        return intValue()==iterAtStart && kontinue.getAsBoolean();
                    });
                }
            }
        }


    }

    private void idle(BooleanSupplier kontinue) {
        int atStart = intValue();
        do { } while (kontinue.getAsBoolean() && intValue()==atStart);
    }


    public static class Schedule {

        /** value samples */
        public float[] value= ArrayUtils.EMPTY_FLOAT_ARRAY;

        /** essentially the normalized value. directly determines the frequency
         *  of an entry being sampled by the scheduler, but does not determine
         *  its rqeuested iteration allocated if selected - instead,
         *  iter, time, and jiffy does.
         * */
        public float[] weight = ArrayUtils.EMPTY_FLOAT_ARRAY;

        /** short history of time (in nanoseconds) spent */
        public DescriptiveStatistics[] time = null;

        /** short history of iter spent in the corresponding times */
        public DescriptiveStatistics[] done = null;

        /** cache for iter.getMean() and time.getMean() */
        public double[] doneMean = null;
        public double[] timeMean = null;

        final static int WINDOW = 4;

        final MetalBitSet active = new MetalBitSet.VolatileIntBitSet();

        public Causable[] can = new Causable[0];

        private final long[] committed = new long[2];
        private final LongLongProcedure commiter = (timeNS, iter) -> {
            committed[0] = timeNS;
            committed[1] = iter;
        };

        @Override
        public String toString() {

            return Joiner.on("\n").join(IntStream.range(0, can.length).mapToObj(
                    x -> n4(weight[x]) + "=" + can[x] +
                            "@" + n2(value[x] * 1E3)
            ).iterator());
        }


        public void update(FastCoWList<Causable> cans) {
            final Causable[] can = this.can = cans.copy;
            if (can == null)
                return;
            int n = can.length;
            if (value.length != n) {

                value = new float[n];

                weight = new float[n];

                time = new DescriptiveStatistics[n];
                timeMean = new double[n];
                done = new DescriptiveStatistics[n];
                doneMean = new double[n];

                for (int i = 0; i < n; i++) {
                    time[i] = new DescriptiveStatistics(WINDOW);
                    done[i] = new DescriptiveStatistics(WINDOW);
                }

                assert (n < 32) : "TODO make atomic n>32 bitset";
                this.active //= MetalBitSet.bits(n);
                        .clearAll();
            }

            active.setAll();


            for (int i = 0; i < n; i++) {
                Causable c = cans.get(i);

                c.can.commit(commiter);

                long timeNS = committed[0];
                if (timeNS > 0) {
                    DescriptiveStatistics t = this.time[i];
                    t.addValue(timeNS);
                    this.timeMean[i] = t.getMean();
                    DescriptiveStatistics d = this.done[i];
                    d.addValue(committed[1]);
                    this.doneMean[i] = d.getMean();
                }

                value[i] = c.value();
                weight[i] = value[i]; //pre-normalized value
            }

            //weight[] = normalize(value[]) , with margin so the minimum value is non-zero some marginal amoutn (Margin-Max)
            float[] minmax = Util.minmax(weight);
            float lowMargin = (minmax[1] - minmax[0]) / n;
            for (int i = 0; i < n; i++)
                weight[i] = normalize(weight[i], minmax[0] - lowMargin, minmax[1]);

        }
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

        /**
         * hidden to visible neuron ratio
         */
        private float hiddenMultipler = 1f;

        float rbmStrength = 0.25f;

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
        float NOISE = 0.01f;

        public float[] next;
        public Autoencoder ae;

        /**
         * hidden to visible neuron ratio
         */
        private float hiddenMultipler = 0.05f;

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

        final RecycledSummaryStatistics[] causeSummary = new RecycledSummaryStatistics[MetaGoal.values().length];

        {
            for (int i = 0; i < causeSummary.length; i++)
                causeSummary[i] = new RecycledSummaryStatistics();
        }

        float momentum =
//                    0f;
                //0.5f;
                //0.75f;
                0.9f;
        //0.95f;

        final static double minUpdateDurs = 1f;

        long lastUpdate = ETERNAL;

        /**
         * intermediate calculation buffer
         */
        float[] val = ArrayUtils.EMPTY_FLOAT_ARRAY;

        @Override
        public void update(long time, int dur, FasterList<Cause> causes, float[] goal) {

            if (lastUpdate == ETERNAL)
                lastUpdate = time;
            double dt = (time - lastUpdate) / ((double) dur);
            if (dt < minUpdateDurs)
                return;
            lastUpdate = time;

            for (RecycledSummaryStatistics r : causeSummary) {
                r.clear();
            }

            int cc = causes.size();

            if (val.length != cc) {
                val = new float[cc];
            }

            for (int i = 0; i < cc; i++) {
                causes.get(i).commit(causeSummary);
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
                    v += goal[j] * cg[j].current;
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


    public Focus(NAR n, Exec.Revaluator r) {
        super(Schedule::new);

        this.can = new FastCoWList<>(32, Causable[]::new);

        this.revaluator = r;

        n.services.change.on((xa) -> {
            Services.Service<NAR> x = xa.getOne();
            if (x instanceof Causable) {
                Causable c = (Causable) x;
                if (xa.getTwo())
                    add(c);
                else
                    remove(c);
            }
        });

        n.onCycle(this::update);
    }


    final AtomicBoolean busy = new AtomicBoolean(false);

    public void update(NAR nar) {
        if (!busy.compareAndSet(false, true))
            return;

        try {


            Schedule s = write();
            s.update(can);

            //System.out.println(schedule.read());

            commit();

            revaluator.update(nar);

        } finally {
            busy.set(false);
        }

        //sched.
//              try {
//                sched.solve(can, dutyCycleTime);
//
//                //sched.estimatedTimeTotal(can);
//            } catch (InternalSolverError e) {
//                logger.error("{} {}", can, e);
//            }
    }


    protected void add(Causable c) {
        this.can.add(c);
    }

    protected void remove(Causable c) {
        this.can.remove(c);
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