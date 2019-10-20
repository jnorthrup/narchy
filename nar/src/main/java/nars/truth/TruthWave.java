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

    private static final int answerDetail = 3;

    private static final int ENTRY_SIZE = 4;
    public BeliefTable table;

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
        table = null;
    }

    private void resize(int cap) {
        truth = new float[ENTRY_SIZE * cap];
    }

    public TruthWave( BeliefTable b) {
        this(b.taskCount());
        this.table = b;
        set(b, Long.MIN_VALUE, Long.MAX_VALUE);
        //TODO update range
    }

    /**
     * clears and fills this wave with the data from a table
     */
    public void set(BeliefTable b, long minT, long maxT) {
        clear();
        this.table = b;
        this.start = minT; this.end = maxT;
        var s = b.taskCount();
        if (s == 0) {
            return;
        }


        size(s);

        var t = this.truth;

        int[] size = {0};

        //long[] st = new long[]{Long.MAX_VALUE}, en = new long[]{Long.MIN_VALUE};
        b.forEachTask(minT, maxT, x -> {
            var ss = size[0];
            if (ss >= s) {
                if (NAL.DEBUG)
                    throw new WTF("truthwave capacity exceeded");
                return;
            }

            var xs = x.start();

            if (xs > maxT)
                return; //OOB
            var xe = x.end();
            if (xe < minT)
                return; //OOB

            var j = (size[0]++) * ENTRY_SIZE;
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

        var c = capacity();

        if (c < s)
            resize(s);
        else {
            
        }

    }


    /**
     * fills the wave with evenly sampled points in a time range
     */
    public void project(BeliefTable table, long minT, long maxT, int points, Term term, float dur, NAR nar) {

        clear();
        this.start = minT;
        this.end = maxT;
        if (minT == maxT)
            return;

        size(points);

        double dt, tStart;
        if (points <= 1) {
            dt = 0;
            tStart = (minT + maxT)/2.0;
        } else {
            dt = (maxT - minT) / ((double) (points - 1));
            tStart = minT;
        }


        var data = this.truth;
        var j = 0;
        var a = Answer.taskStrength(true, answerDetail, start, end, term, null, nar)
                .dur(dur);
        var tries = Math.round(answerDetail * NAL.ANSWER_TRYING);

        for (var i = 0; i < points; i++) {
            var t = tStart + i * dt;
            var s = Math.round(t - dt/2);
            var e = Math.round(t + dt/2);
            var tr = a.clear(tries).time(s, e).match(table).truth();
            if (tr!=null) {
                var mid = (s + e) / 2;
                load(data, (j++) * ENTRY_SIZE,
                        minT, maxT,
                        mid, mid,
                        tr
                );
            }
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
        float[] min = {Float.POSITIVE_INFINITY};
        float[] max = {Float.NEGATIVE_INFINITY};
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
        var n = this.size;
        var t = this.truth;
        var j = 0;
        var start = this.start;
        double totalRange = this.end-this.start;
        for (var i = 0; i < n; i++) {
            var s = t[j++];
            var e = t[j++];
            var f = t[j++];
            var c = t[j++];
            var S = start + Math.round(totalRange * s);
            var E = start + Math.round(totalRange * e);
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
