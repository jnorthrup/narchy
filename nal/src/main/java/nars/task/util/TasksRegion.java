package nars.task.util;

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


    private final long start;
    private final long end;

    private final float freqMin;
    private final float freqMax;
    private final float confMin;
    private final float confMax;


    public TasksRegion(long start, long end, float freqMin, float freqMax, float confMin, float confMax) {
        this.start = start;
        this.end = end;
        this.freqMin = freqMin;
        this.freqMax = freqMax;
        this.confMin = confMin;
        this.confMax = confMax;
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
    public final float coordF(int dimension, boolean maxOrMin) {
        if (maxOrMin) {
            switch (dimension) {
                case 0:
                    return end;
                case 1:
                    return freqMax;
                case 2:
                    return confMax;
            }
        } else {
            switch (dimension) {
                case 0:
                    return start;
                case 1:
                    return freqMin;
                case 2:
                    return confMin;
            }
        }


        throw new UnsupportedOperationException();
    }


    @Override
    public @Nullable Task task() {
        return null;
    }

}
