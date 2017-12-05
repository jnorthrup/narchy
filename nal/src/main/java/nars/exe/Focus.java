package nars.exe;

import jcog.Services;
import jcog.Util;
import jcog.decide.Roulette;
import jcog.learn.deep.RBM;
import jcog.list.FastCoWList;
import jcog.list.FasterList;
import jcog.math.NumberException;
import jcog.math.RecycledSummaryStatistics;
import jcog.math.random.XoRoShiRo128PlusRandom;
import nars.NAR;
import nars.control.Causable;
import nars.control.Cause;
import nars.control.MetaGoal;
import nars.control.Traffic;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * decides mental activity
 */
public class Focus {

    public static final int WORK_BATCH_SIZE = 2;

    /**
     * temporal granularity unit, in seconds
     */
    public static final float JIFFY = 0.0005f;

    private final FastCoWList<Causable> can;

    /** probability of selecting each can (roulette weight), updated each cycle */
    private float[] canWeights = ArrayUtils.EMPTY_FLOAT_ARRAY;
    private Causable[] canActive;

    private final NAR nar;

    public final Exec.Revaluator revaluator;

    final Random rng = new XoRoShiRo128PlusRandom(1); //separate from NAR but not necessarily


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
        private float hiddenMultipler = 0.5f;

        float rbmStrength = 0.1f;

        public RBMRevaluator(Random rng) {
            this.rng = rng;
            momentum = 1f - rbmStrength;
        }

        @Override
        public void update(FasterList<Cause> causes, float[] goal) {
            super.update(causes, goal);

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
                        (float) (cur[i] + rbmStrength * next[i]);
                causes.get(i).setValue(j);
            }
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
                0.95f;

        @Override
        public void update(FasterList<Cause> causes, float[] goal) {

            for (RecycledSummaryStatistics r : causeSummary) {
                r.clear();
            }

            int cc = causes.size();
            for (int i = 0, causesSize = cc; i < causesSize; i++) {
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

            final float momentum = this.momentum;
            for (int i = 0, causesSize = cc; i < causesSize; i++) {
                Cause c = causes.get(i);

                Traffic[] cg = c.goalValue;

                //mix the weighted current values of each purpose, each independently normalized against the values (the reason for calculating summary statistics in previous step)
                float next = 0;
                for (int j = 0; j < goals; j++) {
                    next += goal[j] * cg[j].current;
                }

                float prev = c.value();

//                    0.99f * (1f - Util.unitize(
//                            Math.abs(next) / (1 + Math.max(Math.abs(next), Math.abs(prev)))));

                //c.setValue(Util.lerp(momentum, next, prev));
                c.setValue(momentum * prev + (1f - momentum) * next);
            }
        }

    }


    public Focus(NAR n) {
        this.can = new FastCoWList<>(32, Causable[]::new);
        this.nar = n;


        this.revaluator =
                new DefaultRevaluator();
                //new RBMRevaluator(nar.random());

        n.serviceAddOrRemove.on((xa) -> {
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

    public void work(int work) {

        float[] cw = canWeights;
        int n = cw.length;
        if (n == 0) return;

        Causable[] ca = canActive;
        if (ca.length!= n) return; //HACK in between updates, try again

        for (int i = 0; i < work; i++) {
            int x = Roulette.decideRoulette(cw, rng);
            Causable y = ca[x];
            int iters = Math.max(1, Math.round(JIFFY / y.can.iterationTimeMean()));
            y.run(nar, iters);
        }

    }

    final AtomicBoolean busy = new AtomicBoolean(false);

    public void update(NAR nar) {
        if (!busy.compareAndSet(false, true))
            return;

        try {

            //TODO these should be updated as atomic pair
            canActive = can.copy;
            canWeights = can.map(this::weight, canWeights);

//            System.out.println(values);
//            can.print();

            revaluator.update(nar.causes, nar.want);

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

    private float weight(Causable c) {
        final float TEMPERATURE = 0.05f;
        final float MAX = canWeights.length * 2;
        double supply = c.can.supply();
        float iterationTimeMean = c.can.iterationTimeMean();
        float den = (float) supply * iterationTimeMean;
        if (den < Float.MIN_NORMAL)
            return 1;
        else {
            float x = (float) Math.exp(c.value() / den * TEMPERATURE);
            if (Float.isFinite(x)) {
                return Math.min(x, MAX);
            }
            else {
                return MAX;
                //throw new NumberException("(value,cost) -> weight calculation");
            }
        }
    }

    private void add(Causable c) {
        this.can.add(c);
    }

    private void remove(Causable c) {
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