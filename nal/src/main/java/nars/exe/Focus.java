package nars.exe;

import com.google.common.base.Joiner;
import jcog.Services;
import jcog.Util;
import jcog.learn.Autoencoder;
import jcog.learn.deep.RBM;
import jcog.list.FastCoWList;
import jcog.list.FasterList;
import jcog.math.RecycledSummaryStatistics;
import jcog.math.random.XoRoShiRo128PlusRandom;
import jcog.util.Flip;
import nars.NAR;
import nars.control.Cause;
import nars.control.MetaGoal;
import nars.control.Traffic;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import static jcog.Texts.n2;
import static jcog.Texts.n4;
import static jcog.Util.normalize;
import static nars.time.Tense.ETERNAL;

/**
 * decides mental activity
 */
public class Focus extends Flip<Focus.Schedule> {

    public final Exec.Revaluator revaluator;

    private final FastCoWList<Causable> can;

    public static class Schedule {
        public float[] time = ArrayUtils.EMPTY_FLOAT_ARRAY;
        //        public float[] timeNormalized = ArrayUtils.EMPTY_FLOAT_ARRAY;
        public float[] supplied = ArrayUtils.EMPTY_FLOAT_ARRAY;
        public float[] weight = ArrayUtils.EMPTY_FLOAT_ARRAY;
        public float[] iterPerSecond = ArrayUtils.EMPTY_FLOAT_ARRAY;

        /**
         * probability of selecting each can (roulette weight), updated each cycle
         */

        public Causable[] active = new Causable[0];


        @Override
        public String toString() {

            return Joiner.on("\n").join(IntStream.range(0, active.length).mapToObj(
                    x -> n4(weight[x]) + "=" + active[x] +
                            "@" + n2(time[x] * 1E3) + "uS x " + n2(supplied[x])
            ).iterator());
        }

//        private float weight(Causable c, float time) {
//            //final float MAX = canWeights.length * 2;
//            //double supply = c.can.supply();
//            //float iterationTimeMean = c.can.iterationTimeMean();
//            //float den = (float) supply * iterationTimeMean;
//
//
//            float v = c.value() / time;
//            return v;
//
////            final float TEMPERATURE = 1;
////            float x = (float) Math.exp(v * TEMPERATURE);
////            assert (Float.isFinite(x));
////            return x;
////            }
//        }

        public void update(FastCoWList<Causable> can) {
            final Causable[] active = this.active = can.copy;
            if (active == null)
                return;
            int n = active.length;
            if (time.length != n) {
                //realloc
                time = new float[n];
                supplied = new float[n];
                iterPerSecond = new float[n];
            }
            for (int i = 0; i < n; i++) {
                can.get(i).can.commit(i, time, supplied, iterPerSecond);
            }

//                float margin = 1f / n;

//                if (timeNormalized.length!=n)
//                    timeNormalized = new float[n];

            float timeMax = 0;
            for (float i : time) {
                if (i > Float.MIN_NORMAL) {
                    if (i > timeMax)
                        timeMax = i;
                }
            }

            if (timeMax < Float.MIN_NORMAL)
                timeMax = 1f; //artificial

            for (int i = 0; i < n; i++) {
                float ii = time[i];
                if (ii < Float.MIN_NORMAL) {
                    time[i] = timeMax;
                }
            }

//                if (iters < 2) {
//                    Arrays.fill(timeNormalized, 1f); //all the same
//                } else {
//                    float mean = iterSum/iters;
//                    for (int i = 0; i < n; i++) {
//                        timeNormalized[i] = normalize( normalize(time[i],
//                                0, timeMax), 0 - margin, +1f + margin);
//                    }
//                }


            weight = Util.map(n, (int i) ->
                    supplied[i] > 0 ? active[i].value() / (supplied[i]/iterPerSecond[i]) : 0, weight);

            float[] minmax = Util.minmax(weight);
            float lowMargin = (minmax[1] - minmax[0])/n;
            for (int i = 0; i < n; i++)
                weight[i] =
                        normalize(weight[i], minmax[0]-lowMargin, minmax[1])/time[i];
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
                int numHidden = Math.max(2,Math.round(hiddenMultipler * numCauses));

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

        /** intermediate calculation buffer */
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

            if (val.length!=cc) {
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

        /** subclasses can implement their own filters and post-processing of the value vector */
        protected void update(float[] val) {

        }

    }


    public Focus(NAR n, Exec.Revaluator r) {
        super(Schedule::new);

        this.can = new FastCoWList<>(32, Causable[]::new);

        this.revaluator = r;

        n.services.serviceAddOrRemove.on((xa) -> {
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