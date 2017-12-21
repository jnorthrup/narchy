package nars.truth;

import nars.NAR;
import nars.concept.Concept;
import nars.table.BeliefTable;
import nars.time.Tense;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;

/**
 * compact chart-like representation of a belief state at each time cycle in a range of time.
 * useful as a memoized state snapshot of a belief table
 * stored in an array of float quadruplets for each task:
 * 1) start
 * 2) end
 * 3) freq
 * 4) conf
 * 5) quality
 */
public class TruthWave {

    private static final int ENTRY_SIZE = 4;

    /**
     * start and stop interval (in cycles)
     */
    long start;
    long end;

    /**
     * sequence of triples (freq, conf, start, end) for each task; NaN for eternal
     */
    float[] truth;
    int size;
    @Nullable
    public Truth current;

    public TruthWave(int initialCapacity) {
        resize(initialCapacity);
        clear();
    }

    public void clear() {
        size = 0;
        start = end = Tense.ETERNAL;
    }

    private void resize(int cap) {
        truth = new float[ENTRY_SIZE * cap];
    }

    public TruthWave(@NotNull BeliefTable b, @NotNull NAR n) {
        this(b.size());
        set(b, n.time(), n.dur(), n, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    /**
     * clears and fills this wave with the data from a table
     */
    public void set(BeliefTable b, long now, int dur, NAR nar, long minT, long maxT) {
        int s = b.size();
        if (s == 0) {
            this.current = null;
            clear();
            return;
        }

        ensureSize(s);

        float[] t = this.truth;

        final int[] size = {0};
        b.forEachTask(false, minT, maxT, x -> {
            int ss = size[0];
            if (ss < s) { //HACK in case the table size changed since allocating above
                int j = (size[0]++) * ENTRY_SIZE;
                load(t, j, x.start(), x.end(), x);
            }
        });
        this.size = size[0];

        //compute time range
        float start = Float.POSITIVE_INFINITY;
        float end = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < size[0]; i++) {
            int ii = i * ENTRY_SIZE + 0;
            float starts = t[ii];
            if (start == start) {
                float ends = t[ii + 1];

                //float oo = (starts + ends) / 2; //midpoint

                if (ends > end) end = ends;
                if (starts < start) start = starts;
            }
        }
        this.start = (long) start;
        this.end = (long) end;
    }

    public static void load(float[] array, int index, long start, long end, @Nullable Truthed truth) {
        array[index++] = start == Tense.ETERNAL ? Float.NaN : start;
        array[index++] = end == Tense.ETERNAL ? Float.NaN : end;
        if (truth != null) {
            array[index++] = truth.freq();
            array[index++] = truth.conf();
        } else {
            array[index++] = Float.NaN;
            array[index++] = 0f;
        }
    }

    public void ensureSize(int s) {

        int c = capacity();

        if (c < s)
            resize(s);
        else {
            if (s < c) Arrays.fill(truth, 0); //TODO memfill only the necessary part of the array that won't be used
        }

    }


    /**
     * fills the wave with evenly sampled points in a time range
     */
    public void project(Concept c, boolean beliefOrGoal, long minT, long maxT, int dur, int points, NAR nar) {
        clear();

        if (minT == maxT) {
            return;
        }
        ensureSize(points);

        float dt = (maxT - minT) / ((float)points);
        long t = minT;
        float[] data = this.truth;
        int j = 0;
        int idt = Math.round(dt);
        byte punc = beliefOrGoal ? BELIEF : GOAL;
        for (int i = 0; i < points; i++) {
            long u = t + idt;
            long mid = (t + u)/2;
            load(data, (j++) * ENTRY_SIZE, mid, mid,
                    nar.truth(c, punc, mid, mid) //point
                    //nar.truth(c, punc, t, u) //range
            );
            t = (long)(t+dt);
        }
        this.current = null;
        this.size = j;
        this.start = minT;
        this.end = maxT;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public long start() {
        return start;
    }

    public long end() {
        return end;
    }

    /**
     * returns 2 element array
     */
    public float[] range(int dim) {
        final float[] min = {Float.MAX_VALUE};
        final float[] max = {Float.MIN_VALUE};
        forEach((f, c, start, end) -> {
            if (c > max[0]) max[0] = c;
            if (c < min[0]) min[0] = c;
        });
        return new float[]{min[0], max[0]};
    }


    @FunctionalInterface
    public interface TruthWaveVisitor {
        void onTruth(float f, float c, float start, float end);
    }

    public final void forEach(@NotNull TruthWaveVisitor v) {
        int n = this.size;
        float[] t = this.truth;
        int j = 0;
        for (int i = 0; i < n; i++) {
            float s = t[j++];
            float e = t[j++];
            float f = t[j++];
            float c = t[j++];
            v.onTruth(f, c, s, e);
        }
    }

    public final int capacity() {
        return truth.length / ENTRY_SIZE;
    }

//        //get min and max occurence time
//        for (Task t : beliefs) {
//            long o = t.occurrence();
//            if (o == Tense.ETERNAL) {
//                expectEternal1 += t.truth().expectationPositive();
//                expectEternal0 += t.truth().expectationNegative();
//                numEternal++;
//            }
//            else {
//                numTemporal++;
//                if (o > max) max = o;
//                if (o < min) min = o;
//            }
//        }
//
//        if (numEternal > 0) {
//            expectEternal1 /= numEternal;
//            expectEternal0 /= numEternal;
//        }
//
//        start = min;
//        end = max;
//
//        int range = length();
//        expect = new float[2][];
//        expect[0] = new float[range+1];
//        expect[1] = new float[range+1];
//
//        if (numTemporal > 0) {
//            for (Task t : beliefs) {
//                long o = t.occurrence();
//                if (o != Tense.ETERNAL) {
//                    int i = (int)(o - min);
//                    expect[1][i] += t.truth().expectationPositive();
//                    expect[0][i] += t.truth().expectationNegative();
//                }
//            }
//
//            //normalize
//            for (int i = 0; i < (max-min); i++) {
//                expect[0][i] /= numTemporal;
//                expect[1][i] /= numTemporal;
//            }
//        }
//
//    }
//
//    //TODO getFrequencyAnalysis
//    //TODO getDistribution
//
//    public int length() { return (int)(end-start); }
//
//    public void print() {
//        System.out.print("eternal=" + numEternal + ", temporal=" + numTemporal);
//
//
//        if (length() == 0) {
//            System.out.println();
//            return;
//        }
//        System.out.println(" @ " + start + ".." + end);
//
//        for (int c = 0; c < 2; c++) {
//            for (int i = 0; i < length(); i++) {
//
//                float v = expect[c][i];
//
//                System.out.print(Util.n2u(v) + ' ');
//
//            }
//            System.out.println();
//        }
//    }


    @NotNull
    @Override
    public String toString() {
        return start() + ".." + end() + ": " + Arrays.toString(truth);
    }
}
