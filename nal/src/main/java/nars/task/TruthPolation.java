package nars.task;

import jcog.Util;
import jcog.decide.DecideSoftmax;
import jcog.decide.Roulette;
import nars.Param;
import nars.Task;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import static nars.time.Tense.ETERNAL;
import static nars.truth.TruthFunctions.w2c;
import static nars.truth.TruthFunctions.w2cSafe;

/**
 * Truth Interpolation and Extrapolation of Temporal Beliefs/Goals
 * see:
 * https://en.wikipedia.org/wiki/Category:Intertemporal_economics
 * https://en.wikipedia.org/wiki/Discounted_utility
 */
public interface TruthPolation extends Consumer<Tasked> {

    PreciseTruth truth();


    /**
     * computes truth at a given time from iterative task samples
     * includes variance calculation for reduction of evidence in proportion to confusion/conflict
     * uses "waldorf method" to calculate a running variance
     * additionally, the variance is weighted by the contributor's confidences
     */
    class TruthPolationBasic implements TruthPolation {
        float eviSum, wFreqSum;
        final long start, end;
        final int dur;


        /**
         * computes an adjusted durability no larger than the minimum
         * distance to the target range provided by the input tasks.
         * uses the temporals that will be accepted later to determine the duration on a setup pass
         */
        public static TruthPolationBasic autoRange(long start, long end, int dur, Iterable<? extends Tasked> temporals) {
            if (start != ETERNAL) {
                long minDur = dur;
                for (Tasked t : temporals) {
                    long dd = t.task().minDistanceTo(start, end);

                    if (dd == 0) {
                        minDur = 0; //minimum possible
                        break;
                    } else {
                        minDur = Math.min(minDur, (int) dd);
                    }
                }
                assert (minDur < Integer.MAX_VALUE);
                dur = (int) Math.max(1, minDur);
            }
            return new TruthPolationBasic(start, end, dur);
        }

        public TruthPolationBasic(long start, long end, int dur) {
            this.start = start;
            this.end = end;
            this.dur = dur;
        }

        @Override
        public void accept(Tasked t) {
            Task task = t.task();
            Truth tt = task.truth(start, end, dur, 0);
            if (tt != null) {
                float tw = tt.evi();
                //if (tw > 0) {
                eviSum += tw;
                wFreqSum += tw * tt.freq();
                //}
            }

        }


        @Override
        public PreciseTruth truth() {

            float c = w2cSafe(eviSum);
            if (c < Param.TRUTH_EPSILON)
                return null;
            else {
                float f = (wFreqSum / eviSum);
                return new PreciseTruth(f, c);
            }


        }
    }


    class TruthPolationConf implements TruthPolation {
        float confSum, wFreqSum;
        final long start, end;
        final int dur;

        public TruthPolationConf(long start, long end, int dur) {
            this.start = start;
            this.end = end;
            this.dur = dur;
        }

        @Override
        public void accept(Tasked t) {
            Task task = t.task();
            float c = task.conf(start, end, dur);
            if (c > 0) {
                confSum += c;
                wFreqSum += c * task.freq();
            }

        }


        @Override
        public PreciseTruth truth() {
            if (confSum > 0) {
                float f = wFreqSum / confSum;
                float c = confSum;
                if (c < Param.TRUTH_EPSILON)
                    return null; //high-pass conf filter

                return new PreciseTruth(f, Util.min(1f - Param.TRUTH_EPSILON, c));

            } else {
                return null;
            }

        }
    }


    class TruthPolationGreedy implements TruthPolation {

        final long start, end;
        final int dur;
        private final Random rng;
        float bestE = Float.NEGATIVE_INFINITY;
        final FloatArrayList bestF = new FloatArrayList(4);

        public TruthPolationGreedy(long start, long end, int dur) {
            this(start, end, dur, null);
        }

        public TruthPolationGreedy(long start, long end, int dur, Random rng) {
            this.start = start;
            this.end = end;
            this.dur = dur;
            this.rng = rng;
        }

        @Override
        public void accept(Tasked t) {
            Task task = t.task();
            float e = task.evi(start, end, dur);
            if (e > bestE) {
                bestF.clear();
            }
            if (e >= bestE) {
                bestF.add(task.freq());
                bestE = e;
            }
        }


        @Override
        public PreciseTruth truth() {
            FloatArrayList f = this.bestF;
            int s = f.size();

            float g;
            switch (s) {
                case 0:
                    return null;
                case 1:
                    g = f.get(0);
                    break;
                default: {
                    Random r;
                    if (rng == null)
                        r = ThreadLocalRandom.current();
                    else
                        r = rng;
                    g = f.get(r.nextInt(s));
                    break;
                }
            }

            return new PreciseTruth(g, bestE, false);
        }
    }

    class TruthPolationSoftMax implements TruthPolation {

        final long when;
        final int dur;
        final FloatArrayList freq = new FloatArrayList();
        final FloatArrayList conf = new FloatArrayList();

        public TruthPolationSoftMax(long when, int dur) {
            this.when = when;
            this.dur = dur;
        }

        @Override
        public void accept(Tasked t) {
            Task task = t.task();
            conf.add(task.conf(when, dur)); //TODO start,end
            freq.add(task.freq());
        }


        @Override
        public PreciseTruth truth() {
            if (!conf.isEmpty()) {
                int which = new DecideSoftmax(0f, ThreadLocalRandom.current()).decide(conf.toArray(), -1);
                float f = freq.get(which);
                float c = conf.get(which);
                return new PreciseTruth(f, c);

            } else {
                return null;
            }

        }
    }

    class TruthPolationRoulette implements TruthPolation {

        final long start, end;
        final int dur;
        final FloatArrayList freq = new FloatArrayList();
        final FloatArrayList evi = new FloatArrayList();
        private final Random rng;

        public TruthPolationRoulette(long start, long end, int dur, final Random rng) {
            this.start = start;
            this.end = end;
            this.dur = dur;
            this.rng = rng;
        }

        @Override
        public void accept(Tasked t) {
            Task task = t.task();
            evi.add(task.evi(start, end, dur));
            freq.add(task.freq());
        }


        @Override
        public PreciseTruth truth() {
            if (!evi.isEmpty()) {
                int which = Roulette.decideRoulette(freq.size(), evi::get, rng);
                float f = freq.get(which);
                float e = evi.get(which);
                return new PreciseTruth(f, e, false);

            } else {
                return null;
            }

        }
    }

    /**
     * computes truth at a given time from iterative task samples
     * includes variance calculation for reduction of evidence in proportion to confusion/conflict
     * uses "waldorf method" to calculate a running variance
     * additionally, the variance is weighted by the contributor's confidences
     */
    class TruthPolationWithVariance implements TruthPolation {
        float eviSum, wFreqSum;
        float meanSum = 0.5f, deltaSum;
        int count;

        final long when;
        final int dur;

        public TruthPolationWithVariance(long when, int dur) {
            this.when = when;
            this.dur = dur;
        }

        @Override
        public void accept(Tasked tt) {
            Task task = tt.task();
            float tw = task.evi(when, dur);

            if (tw > 0) {

                if (!task.isEternal())
                    tw = tw / (1f + ((float) task.range()) / dur); //dilute the long task in proportion to how many durations it consumes beyond point-like (=0)

                eviSum += tw;

                float f = task.freq();
                wFreqSum += tw * f;

                //        double delta = value - tmpMean;
                //        mean += delta / ++count;
                //        sSum += delta * (value - mean);
                float tmpMean = meanSum;
                float delta = f - tmpMean;
                meanSum += delta / ++count;
                deltaSum += delta * (f - meanSum) * w2c(tw); //scale the delta sum by the conf so that not all tasks contribute to the variation equally
            }

        }


        @Override
        public PreciseTruth truth() {
            if (eviSum > 0) {
                float f = wFreqSum / eviSum;

                float var =
                        deltaSum / count;

                return new PreciseTruth(f, eviSum * (1f / (1f + var)), false);

            } else {
                return null;
            }

        }
    }


//    /**
//     * returns (freq, evid) pair
//     */
//    @Nullable
//    public static PreciseTruth truthRaw(@Nullable Task topEternal, long when, int dur, @NotNull Iterable<Task> tasks) {
//
//        float[] fe = new float[2];
//
//
//        // Contribution of each task's truth
//        // use forEach instance of the iterator(), since HijackBag forEach should be cheaper
//        tasks.forEach(t -> {
//
//            float tw = t.evi(when, dur);
//
//            if (tw > 0) {
//                freqSum += tw;
//                wFreqSum += tw * t.freq();
//            }
//
//        });
//        float evidence = freqSum;
//        float freqEvi = wFreqSum;
//
//        if (topEternal != null) {
//            float ew = topEternal.evi();
//            evidence += ew;
//            freqEvi += ew * topEternal.freq();
//        }
//
//        if (evidence > 0) {
//            float f = freqEvi / evidence;
//            return new PreciseTruth(f, evidence, false);
//        } else {
//            return null;
//        }
//    }

}
