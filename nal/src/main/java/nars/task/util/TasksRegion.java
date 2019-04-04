package nars.task.util;

import jcog.TODO;
import jcog.Util;
import nars.Task;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * dimensions:
 * 0: long time start..end
 * 1: float freq min..max
 * 2: float conf min..max
 */
public final class TasksRegion implements TaskRegion {

    private final long start, end;
    private final float freqMin, freqMax;
    private final float confMin, confMax;

    protected TasksRegion(long start, long end, float freqMin, float freqMax, float confMin, float confMax) {
        this.start = start;
        this.end = end;
        
//        if (Util.equals(freqMin, freqMax, e)) {
//            float mid = (freqMin + freqMax)/2;
//            this.freqMin = mid - e; this.freqMax = mid + e;
//        } else {
            this.freqMin = freqMin; this.freqMax = freqMax;
//        }

//        if (Util.equals(confMin, confMax, e)) {
//            float mid = (confMin + confMax)/2;
//            this.confMin = mid - e; this.confMax = mid + e;
//        } else {
            this.confMin = confMin; this.confMax = confMax;
//        }
    }

    public static TasksRegion mbr(TaskRegion r, long xs, long xe, float ef, float ec) {
        if (r instanceof Task) {
            Task tr = (Task)r;
            float trf = tr.freq(), trc = tr.conf();
            return new TasksRegion(
                    Math.min(r.start(), xs), Math.max(r.end(), xe),
                    Math.min(trf, ef),
                    Math.max(trf, ef),
                    Math.min(trc, ec),
                    Math.max(trc, ec)
            );
        } else {
            return new TasksRegion(
                    Math.min(r.start(), xs), Math.max(r.end(), xe),
                    Math.min(r.freqMin(), ef),
                    Math.max(r.freqMax(), ef),
                    Math.min(r.confMin(), ec),
                    Math.max(r.confMax(), ec)
            );
        }
    }

    @Override
    public int hashCode() {
        throw new TODO();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof TasksRegion)) return false;
        TasksRegion r = (TasksRegion)obj;
        return start==r.start() && end==r.end() &&
                Util.equals(freqMin, r.freqMin) && Util.equals(freqMax, r.freqMax) &&
                Util.equals(confMin, r.confMin) && Util.equals(confMax, r.confMax);
    }

    @Override
    public final long start() {
        return start;
    }

    @Override
    public final long end() {
        return end;
    }

    @Override
    public String toString() {
        return Arrays.toString(new double[]{start, end, freqMin, freqMax, confMin, confMax});
    }

    @Override
    public final float freqMin() {
        return freqMin;
    }
    public final float freqMax() {
        return freqMax;
    }
    public final float confMin() {
        return confMin;
    }
    @Override
    public float confMax() {
        return confMax;
    }

    @Override
    public @Nullable Task task() {
        return null;
    }

}
