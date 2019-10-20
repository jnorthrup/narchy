package nars.task.util;

import jcog.TODO;
import jcog.Texts;
import jcog.Util;
import jcog.math.Longerval;
import nars.Op;
import nars.Task;
import nars.time.Tense;
import org.jetbrains.annotations.Nullable;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static nars.truth.Truth.hashDiscretenessCoarse;
import static nars.truth.Truth.hashDiscretenessFine;


public final class TasksRegion extends Longerval implements TaskRegion {


    @Override
    public final @Nullable Task task() {
        return null;
    }

    /** discrete truth encoded extrema.  for fast and reliable comparison.
     *
     * uses the system-wide minimum Truth epsilon and encodes a freq,conf pairs in one 32-bit int.
     *
     * a and b are the corners
     */
    private final int a;
    private final int b;

    @Deprecated public TasksRegion(long start, long end, float freqMin, float freqMax, float confMin, float confMax) {
        this(start, end,
            freqI(freqMin), freqI(freqMax),
            confI(confMin), confI(confMax)
            );
    }

    public static int confI(float conf) {
        return Util.toInt(conf, hashDiscretenessFine);
    }

    public static int freqI(float freq) {
        return Util.toInt(freq, hashDiscretenessCoarse);
    }

    TasksRegion(long start, long end, int freqIMin, int freqIMax, int confIMin, int confIMax) {
        super(start, end);
        a = (freqIMin << 24) | confIMin;
        b = (freqIMax << 24) | confIMax;
    }

    private static float freqF(int h) {
        return Util.toFloat(freqI(h), hashDiscretenessCoarse);
    }
    private static float confF(int c) {
        return Util.toFloat(confI(c), hashDiscretenessFine);
    }
    static int confI(int h) {
        return h & 0xffffff;
    }
    static int freqI(int h) {
        return h >> 24;
    }

    @Override public final float freqMin() { return freqF(a); }
    @Override public final float freqMax() { return freqF(b); }
    @Override public final float confMin() {
        return confF(a);
    }
    @Override public final float confMax() {
        return confF(b);
    }

    @Override public final int confMinI() { return confI(a); }
    @Override public final int confMaxI() { return confI(b); }
    @Override public final int freqMinI() { return freqI(a); }
    @Override public final int freqMaxI() { return freqI(b); }

    public static TasksRegion mbr(TaskRegion r, long xs, long xe, float _ef, float _ec) {

        var S = r.start();

        assert(xs!=ETERNAL && xs!=TIMELESS && xe!=ETERNAL && xe!=TIMELESS);

        var E = r.end();
        int f = r.freqMinI(), F = r.freqMaxI();
        int c = r.confMinI(), C = r.confMaxI();

        int ef = freqI(_ef), ec = confI(_ec);
        if (r instanceof TasksRegion) {
            if (xs >= S && xe <= E)
                if (ec >= c && ec <= C)
                    if (ef >= f && ef <= F)
                        return (TasksRegion) r; //conttained
        }

        return new TasksRegion(
            min(S, xs), max(E, xe),
            min(f, ef), max(F, ef),
            min(c, ec), max(C, ec)
        );

        //return r instanceof TasksRegion ? Util.maybeEqual((TasksRegion) r, y) : y;
    }

    @Override
    public int hashCode() {
        throw new TODO();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof TasksRegion))
            return false; //throw new TODO();
        var r = (TasksRegion) obj;
        return a == r.a && b == r.b && start == r.start && end == r.end;
    }

    @Override public String toString() {

        var decimals = 3;
        return '@' +
                Tense.tStr(start, end) +
                '[' +
                Texts.n(freqMin(), decimals) +
                ".." +
                Texts.n(freqMax(), decimals) +
                Op.VALUE_SEPARATOR +
                Texts.n(confMin(), decimals) +
                ".." +
                Texts.n(confMax(), decimals) +
                Op.TRUTH_VALUE_MARK + ']';
    }




}
