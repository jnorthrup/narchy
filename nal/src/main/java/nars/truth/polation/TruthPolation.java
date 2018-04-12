package nars.truth.polation;

import jcog.Paper;
import jcog.Skill;
import jcog.list.FasterList;
import nars.Task;
import nars.task.Revision;
import nars.task.Tasked;
import nars.truth.Truth;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

import static java.lang.Float.NaN;

/**
 * Truth Interpolation and Extrapolation of Temporal Beliefs/Goals
 * see:
 * https://en.wikipedia.org/wiki/Category:Intertemporal_economics
 * https://en.wikipedia.org/wiki/Discounted_utility
 */
@Paper
@Skill({"Interpolation", "Extrapolation"})
abstract public class TruthPolation extends FasterList<TruthPolation.TaskComponent> {

    static class TaskComponent {
        public final Task task;

        /**
         * NaN if not yet computed
         */
        public float evi = NaN;
        public float freq = NaN;

        TaskComponent(Task task) {
            this.task = task;
        }

        @Override
        public String toString() {
            return evi + "," + freq + "=" + task;
        }

        public boolean isComputed() {
            float f = freq;
            return f == f;
        }

        @Nullable public final TaskComponent update(TruthPolation p) {
            if (!isComputed()) {
                Truth tt = p.truth(task);
                if (tt != null) {
                    this.freq = tt.freq(); //not necessarily the task's reported "average" freq in case of Truthlets
                    this.evi = tt.evi();
                    return this;
                } else {
                    this.evi = -1;
                    return null; //removed
                }
            } else {
                return this.evi > 0 ? this : null;
            }


        }
    }

    public final long start;
    public final long end;
    public int dur;


    protected TruthPolation(long start, long end, int dur) {
        this.start = start;
        this.end = end;

        assert (dur > 0);
        this.dur = dur;
    }


    public void add(Task t) {
        super.add(new TaskComponent(t));
    }



    /** computes the truth value of a component */
    protected Truth truth(Task t) {
        return t.truth(start, end, dur, 0);
    }

    /** remove components contributing no evidence */
    public final TruthPolation preFilter() {
        removeIf(x -> update(x)==null);
        return this;
    }

    @Nullable protected final TaskComponent update(int i) {
        return update(get(i));
    }

    @Nullable protected final TaskComponent update(TaskComponent tc) {
        return tc.update(this);
    }

    /** removes the weakest components sharing overlapping evidence with stronger ones.
     *  should be called after all entries are added */
    public final TruthPolation filterCyclic() {
        preFilter();

        int s = size();
        if (s > 1) {
            sortThisByFloat(tc -> -tc.evi); //descending by strength
            //TODO maybe factor in originality to reduce overlap so evidence can be combined better

            //remove the weaker holder of any overlapping evidence
            LongHashSet e = new LongHashSet(s * 2);
            removeIf(tc -> {
                long[] stamp = tc.task.stamp();

                for (long ss : stamp) {
                    if (!e.add(ss))
                        return true; //overlap
                }


                return false;
            });
        }
        return this;
    }

    /** computes the final truth value */
    abstract public Truth truth();

    /** revise a temporal with a 'background' eternal truth */
    public Truth truthWithEternal(Truth temporal, Truth eternal) {
        return Revision.revise(temporal, eternal);
    }

    /**
     * blends any result with an optional eternal "background" contribution
     */
    public final Truth truthWithEternal(@Nullable Task eternalTask) {

        Truth temporal = truth();

        Truth eternal = eternalTask != null ? eternalTask.truth() : null;

        if (eternal == null)
            return temporal;
        else if (temporal == null)
            return eternal;
        else {
            return truthWithEternal(temporal, eternal);


//        float tempEvi = t.eviSum;
//        boolean someEvi = tempEvi > 0f;
//        if (topEternal != null) {
//            if (!someEvi) {
//                return new PreciseTruth(topEternal.truth()); //eternal the only authority
//            } else {
//
//                //long totalSpan = Math.max(1, t.spanEnd - t.spanStart);
//                long totalCovered = Math.max(1, t.rangeSum); //estimate
//                float temporalDensity = ((float) totalCovered) / Math.max(1, end - start);
//                float eviDecay = 1 / ((1 + tempEvi * temporalDensity));
//
//                float eteEvi = topEternal.evi();
//
//                t.accept(topEternal.freq(), eteEvi * eviDecay);
//            }
//        }
//
//        return !someEvi ? null : t.truth();
        }


    }

    public final TruthPolation add(Tasked... tasks) {
        ensureCapacity(tasks.length);
        for (Tasked t : tasks) {
            if (t != null)
                add(t);
        }
        return this;
    }

    public final TruthPolation add(Iterable<? extends Tasked> tasks) {
        tasks.forEach(this::add);
        return this;
    }

    public final TruthPolation add(Collection<? extends Tasked> tasks) {
        ensureCapacity(tasks.size());
        return add((Iterable)tasks);
    }

    public final TruthPolation add(Tasked tt) {
        add(tt.task());
        return this;
    }


//    /**
//     * computes truth at a given time from iterative task samples
//     * includes variance calculation for reduction of evidence in proportion to confusion/conflict
//     * uses "waldorf method" to calculate a running variance
//     * additionally, the variance is weighted by the contributor's confidences
//     */
//    class TruthPolationDefault implements TruthPolation {
//        public float eviSum, wFreqSum;
//        final long start, end;
//
//
//        /**
//         * computes an adjusted durability no larger than the minimum
//         * distance to the target range provided by the input tasks.
//         * uses the temporals that will be accepted later to determine the duration on a setup pass
//         *
//         //decrease the eternal evidence in proportion to the specificity of the temporal evidence
//         //in other words, eternal evidence is proportional to the range of time specified by the temporals
//         //for an approximation of the specified 'temporal density'
//         //we can use the total task duration divided by the total range they cover
//         //thus if the tasks are few and far between then eternal contributes more of its influence
//         //and if the tasks are dense and overlapping across a short range then eternal will be ignored more.
//         */
//        public TruthPolationDefault(long start, long end, int dur, Iterable<? extends Tasked> temporals, boolean computeDensity) {
//            if (start != ETERNAL) {
//                long minDur = dur;
//                for (Tasked t : temporals) {
//                    Task tt = t.task();
//                    assert(!tt.isEternal());
//
//                    long dd = tt.minDistanceTo(start, end);
//
//                    if (dd == 0) {
//                        minDur = 0; //minimum possible
//                        break;
//                    } else {
//                        minDur = Math.min(minDur, dd);
//                    }
//                    if (computeDensity) {
//                        long ts = Util.clamp(tt.start(), start, end);
//                        long te = Util.clamp(tt.end(), start, end);
//                        spanStart = Math.min(ts, spanStart);
//                        spanEnd = Math.max(te, spanEnd);
//                        rangeSum += Math.max(1, te - ts);
//                    }
//                }
//                assert (minDur < Integer.MAX_VALUE);
//                dur = (int) Math.max(1, minDur);
//            }
//            this.start = start;
//            this.end = end;
//            this.dur = dur;
//        }
//
//        @Override
//        public void accept(Tasked t) {
//            Task task = t.task();
//            Truth tt = task.truth(start, end, dur, 0);
//
//            if (tt != null) {
//                accept(tt.freq(), tt.evi());
//            }
//
//        }
//
//        public void accept(float f, float e) {
//            eviSum += e;
//            wFreqSum += e * f;
//        }
//
//        @Override
//        public PreciseTruth truth() {
//
//            float c = w2cSafe(eviSum);
//            if (c < Param.TRUTH_EPSILON)
//                return null;
//            else {
//                float f = (wFreqSum / eviSum);
//                return new PreciseTruth(f, c);
//            }
//
//
//        }
//    }
//

//    class TruthPolationConf implements TruthPolation {
//        float confSum, wFreqSum;
//        final long start, end;
//        final int dur;
//
//        public TruthPolationConf(long start, long end, int dur) {
//            this.start = start;
//            this.end = end;
//            this.dur = dur;
//        }
//
//        @Override
//        public void accept(Tasked t) {
//            Task task = t.task();
//            float c = task.conf(start, end, dur);
//            if (c > 0) {
//                confSum += c;
//                wFreqSum += c * task.freq();
//            }
//
//        }
//
//
//        @Override
//        public PreciseTruth truth() {
//            if (confSum > 0) {
//                float f = wFreqSum / confSum;
//                float c = confSum;
//                if (c < Param.TRUTH_EPSILON)
//                    return null; //high-pass conf filter
//
//                return new PreciseTruth(f, Util.min(1f - Param.TRUTH_EPSILON, c));
//
//            } else {
//                return null;
//            }
//
//        }
//    }
//
//
//    class TruthPolationGreedy implements TruthPolation {
//
//        final long start, end;
//        final int dur;
//        private final Random rng;
//        float bestE = Float.NEGATIVE_INFINITY;
//        final FloatArrayList bestF = new FloatArrayList(4);
//
//        public TruthPolationGreedy(long start, long end, int dur) {
//            this(start, end, dur, null);
//        }
//
//        public TruthPolationGreedy(long start, long end, int dur, Random rng) {
//            this.start = start;
//            this.end = end;
//            this.dur = dur;
//            this.rng = rng;
//        }
//
//        @Override
//        public void accept(Tasked t) {
//            Task task = t.task();
//            float e = task.evi(start, end, dur);
//            if (e > bestE) {
//                bestF.clear();
//            }
//            if (e >= bestE) {
//                bestF.add(task.freq());
//                bestE = e;
//            }
//        }
//
//
//        @Override
//        public PreciseTruth truth() {
//            FloatArrayList f = this.bestF;
//            int s = f.size();
//
//            float g;
//            switch (s) {
//                case 0:
//                    return null;
//                case 1:
//                    g = f.get(0);
//                    break;
//                default: {
//                    Random r;
//                    if (rng == null)
//                        r = ThreadLocalRandom.current();
//                    else
//                        r = rng;
//                    g = f.get(r.nextInt(s));
//                    break;
//                }
//            }
//
//            return new PreciseTruth(g, bestE, false);
//        }
//    }
//
//    class TruthPolationSoftMax implements TruthPolation {
//
//        final long when;
//        final int dur;
//        final FloatArrayList freq = new FloatArrayList();
//        final FloatArrayList conf = new FloatArrayList();
//
//        public TruthPolationSoftMax(long when, int dur) {
//            this.when = when;
//            this.dur = dur;
//        }
//
//        @Override
//        public void accept(Tasked t) {
//            Task task = t.task();
//            conf.add(task.conf(when, dur)); //TODO start,end
//            freq.add(task.freq());
//        }
//
//
//        @Override
//        public PreciseTruth truth() {
//            if (!conf.isEmpty()) {
//                int which = new DecideSoftmax(0f, ThreadLocalRandom.current()).decide(conf.toArray(), -1);
//                float f = freq.get(which);
//                float c = conf.get(which);
//                return new PreciseTruth(f, c);
//
//            } else {
//                return null;
//            }
//
//        }
//    }
//
//    class TruthPolationRoulette implements TruthPolation {
//
//        final long start, end;
//        final int dur;
//        final FloatArrayList freq = new FloatArrayList();
//        final FloatArrayList evi = new FloatArrayList();
//        private final Random rng;
//
//        public TruthPolationRoulette(long start, long end, int dur, final Random rng) {
//            this.start = start;
//            this.end = end;
//            this.dur = dur;
//            this.rng = rng;
//        }
//
//        @Override
//        public void accept(Tasked t) {
//            Task task = t.task();
//            evi.add(task.evi(start, end, dur));
//            freq.add(task.freq());
//        }
//
//
//        @Override
//        public PreciseTruth truth() {
//            if (!evi.isEmpty()) {
//                int which = Roulette.decideRoulette(freq.size(), evi::get, rng);
//                float f = freq.get(which);
//                float e = evi.get(which);
//                return new PreciseTruth(f, e, false);
//
//            } else {
//                return null;
//            }
//
//        }
//    }
//
//    /**
//     * computes truth at a given time from iterative task samples
//     * includes variance calculation for reduction of evidence in proportion to confusion/conflict
//     * uses "waldorf method" to calculate a running variance
//     * additionally, the variance is weighted by the contributor's confidences
//     */
//    class TruthPolationWithVariance implements TruthPolation {
//        float eviSum, wFreqSum;
//        float meanSum = 0.5f, deltaSum;
//        int count;
//
//        final long when;
//        final int dur;
//
//        public TruthPolationWithVariance(long when, int dur) {
//            this.when = when;
//            this.dur = dur;
//        }
//
//        @Override
//        public void accept(Tasked tt) {
//            Task task = tt.task();
//            float tw = task.evi(when, dur);
//
//            if (tw > 0) {
//
//                if (!task.isEternal())
//                    tw = tw / (1f + ((float) task.range()) / dur); //dilute the long task in proportion to how many durations it consumes beyond point-like (=0)
//
//                eviSum += tw;
//
//                float f = task.freq();
//                wFreqSum += tw * f;
//
//                //        double delta = value - tmpMean;
//                //        mean += delta / ++count;
//                //        sSum += delta * (value - mean);
//                float tmpMean = meanSum;
//                float delta = f - tmpMean;
//                meanSum += delta / ++count;
//                deltaSum += delta * (f - meanSum) * w2c(tw); //scale the delta sum by the conf so that not all tasks contribute to the variation equally
//            }
//
//        }
//
//
//        @Override
//        public PreciseTruth truth() {
//            if (eviSum > 0) {
//                float f = wFreqSum / eviSum;
//
//                float var =
//                        deltaSum / count;
//
//                return new PreciseTruth(f, eviSum * (1f / (1f + var)), false);
//
//            } else {
//                return null;
//            }
//
//        }
//    }


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
