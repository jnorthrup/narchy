package nars.truth;

import jcog.WTF;
import nars.NAL;
import nars.NAR;
import nars.table.BeliefTable;
import nars.task.util.Answer;
import nars.term.Term;
import nars.time.Tense;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

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

    private static final int precision = 8;

    private static final int ENTRY_SIZE = 4;

    /**
     * start and stop interval (in cycles)
     */
    private long start;
    private long end;

    /**
     * sequence of triples (freq, conf, start, end) for each task; NaN for eternal
     */
    private float[] truth;
    private int size;



    public TruthWave(int initialCapacity) {
        resize(initialCapacity);
        clear();
    }

    private void clear() {
        size = 0;
        start = end = Tense.ETERNAL;
    }

    private void resize(int cap) {
        truth = new float[ENTRY_SIZE * cap];
    }

    public TruthWave( BeliefTable b) {
        this(b.taskCount());
        set(b, Long.MIN_VALUE, Long.MAX_VALUE);
        //TODO update range
    }

    /**
     * clears and fills this wave with the data from a table
     */
    public void set(BeliefTable b, long minT, long maxT) {
        clear();
        this.start = minT; this.end = maxT;
        int s = b.taskCount();
        if (s == 0) {
            return;
        }


        size(s);

        float[] t = this.truth;

        final int[] size = {0};

        //long[] st = new long[]{Long.MAX_VALUE}, en = new long[]{Long.MIN_VALUE};
        b.forEachTask(minT, maxT, x -> {
            int ss = size[0];
            if (ss >= s) {
                if (NAL.DEBUG)
                    throw new WTF("truthwave capacity exceeded");
                return;
            }

            long xs = x.start();

            if (xs > maxT)
                return; //OOB
            long xe = x.end();
            if (xe < minT)
                return; //OOB

            int j = (size[0]++) * ENTRY_SIZE;
            load(t, j, minT, maxT, xs, xe, x);

//            if (xs < st[0]) st[0] = xs;
//            if (xe > en[0]) en[0] = xe;

        });
        this.size = size[0];


    }

    private static void load(float[] array, int index, long absStart, long absEnd, long start, long end, @Nullable Truthed truth) {

        double range = absEnd - absStart;
        array[index++] = start == Tense.ETERNAL ? Float.NaN :
                (float) (((start - absStart)) / range);
        array[index++] = end == Tense.ETERNAL ? Float.NaN :
                (float) (((end - absStart)) / range);
        if (truth != null) {
            array[index++] = truth.freq();
            array[index/*++*/] = truth.conf();
        } else {
            array[index++] = Float.NaN;
            array[index/*++*/] = 0f;
        }
    }

    private void size(int s) {

        int c = capacity();

        if (c < s)
            resize(s);
        else {
            
        }

    }


    /**
     * fills the wave with evenly sampled points in a time range
     */
    public void project(BeliefTable table, long minT, long maxT, int points, Term term, int dur, NAR nar) {

        clear();
        this.start = minT;
        this.end = maxT;
        if (minT == maxT)
            return;

        size(points);

        double dt, t;
        if (points <= 1) {
            dt = 0;
            t = (minT + maxT)/2;
        } else {
            dt = (maxT - minT) / ((float) (points - 1));
            t = minT;
        }

        int halfDT = (int) Math.round(dt/2);
        float[] data = this.truth;
        int j = 0;
        Answer a = Answer.relevant(true, precision, start, end, term, null, nar)
                .dur(dur);
        int tries = (int) Math.ceil(precision* NAL.ANSWER_COMPLETENESS);

        for (int i = 0; i < points; i++) {
            long s = Math.round(t - halfDT);
            long e = Math.round(t + halfDT);
            Truth tr = a.clear(tries).time(s, e).match(table).truth();
            if (tr!=null) {
                long mid = (s + e) / 2;
                load(data, (j++) * ENTRY_SIZE,
                        minT, maxT,
                        mid, mid,
                        tr
                );
            }

            t += dt;
        }
        this.size = j;

    }

    public boolean isEmpty() {
        return size == 0;
    }

    private long start() {
        return start;
    }

    private long end() {
        return end;
    }

    /**
     * returns 2 element array
     */
    public float[] range() {
        final float[] min = {Float.POSITIVE_INFINITY};
        final float[] max = {Float.NEGATIVE_INFINITY};
        forEach((f, c, start, end) -> {
            if (c > max[0]) max[0] = c;
            if (c < min[0]) min[0] = c;
        });
        return new float[]{min[0], max[0]};
    }


    @FunctionalInterface
    public interface TruthWaveVisitor {
        void onTruth(float f, float c, long start, long end);
    }

    public final void forEach(TruthWaveVisitor v) {
        int n = this.size;
        float[] t = this.truth;
        int j = 0;
        long start = this.start;
        double totalRange = this.end-this.start;
        for (int i = 0; i < n; i++) {
            float s = t[j++];
            float e = t[j++];
            float f = t[j++];
            float c = t[j++];
            long S = start + Math.round(totalRange * s);
            long E = start + Math.round(totalRange * e);
            v.onTruth(f, c, S, E);
        }
    }

    private int capacity() {
        return truth.length / ENTRY_SIZE;
    }












































































    
    @Override
    public String toString() {
        return start() + ".." + end() + ": " + Arrays.toString(truth);
    }
}
