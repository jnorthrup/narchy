package nars.task.util;

import jcog.TODO;
import jcog.Texts;
import jcog.Util;
import jcog.math.Longerval;
import nars.Op;
import nars.Task;
import nars.time.Tense;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import static java.lang.Math.max;
import static java.lang.Math.min;


public final class TasksRegion extends Longerval implements TaskRegion {


    @Nullable @Override public final Task task() {
        return null;
    }

    /** discrete truth encoded extrema.  for fast and reliable comparison.
     *
     * uses the system-wide minimum Truth epsilon and encodes a freq,conf pairs in one 32-bit int.
     *
     * a and b are the corners
     */
    private final int a, b;

    public TasksRegion(long start, long end, float freqMin, float freqMax, float confMin, float confMax) {
        super(start, end);
        a = Truth.truthToInt(Math.min(freqMin, freqMax), Math.min(confMin, confMax));
        b = Truth.truthToInt(Math.max(freqMin, freqMax), Math.max(confMin, confMax));
    }

    @Override public final float freqMin() { return Truth.freq(a); }
    @Override public final float freqMax() {
        return Truth.freq(b);
    }
    @Override public final float confMin() {
        return Truth.conf(a);
    }
    @Override public final float confMax() {
        return Truth.conf(b);
    }


    @Override public final int confMinI() { return Truth.confI(a); }
    @Override public final int confMaxI() { return Truth.confI(b); }
    @Override public final int freqMinI() { return Truth.freqI(a); }
    @Override public final int freqMaxI() { return Truth.freqI(b); }

    public static TasksRegion mbr(TaskRegion r, long xs, long xe, float ef, float ec) {

        long rs = r.start();

        assert(xs!=ETERNAL && xs!=TIMELESS && rs!=ETERNAL && rs!=TIMELESS);

        long s = min(rs, xs), e = max(r.end(), xe);

        TasksRegion y = new TasksRegion(s, e,
                min(r.freqMin(), ef),
                max(r.freqMax(), ef),
                min(r.confMin(), ec),
                max(r.confMax(), ec)
        );

        return r instanceof TasksRegion ? Util.maybeEqual((TasksRegion) r, y) : y;
    }

    @Override
    public int hashCode() {
        throw new TODO();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof TasksRegion)) return false;
        TasksRegion r = (TasksRegion) obj;
        return start == r.start && end == r.end && a == r.a && b == r.b;
    }

    @Override public String toString() {

        int decimals = 3;
        return new StringBuilder(64)
                .append('@')
                .append(Tense.tStr(start, end))
                .append('[')
                .append(Texts.n(freqMin(), decimals))
                .append("..")
                .append(Texts.n(freqMax(), decimals))
                .append(Op.VALUE_SEPARATOR)
                .append(Texts.n(confMin(), decimals))
                .append("..")
                .append(Texts.n(confMax(), decimals))
                .append(Op.TRUTH_VALUE_MARK).append(']')
                .toString();
    }




}
