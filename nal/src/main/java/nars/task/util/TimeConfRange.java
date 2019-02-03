package nars.task.util;

import jcog.tree.rtree.HyperRegion;

public class TimeConfRange extends TimeRange {

    float cMin = 0, cMax = 1;

    public TimeRange set(long s, long e, float cMin, float cMax) {
        set(s, e);
        this.cMin = cMin;
        this.cMax = cMax;
        return this;
    }

    @Override
    public boolean intersects(HyperRegion x) {
        TaskRegion t = (TaskRegion)x;
        return t.intersects(start, end) && t.intersectsConf(cMin, cMax);
    }


    @Override
    public boolean contains(HyperRegion x) {
        TaskRegion t = (TaskRegion)x;
        return t.contains(start, end) && t.containsConf(cMin,cMax);
    }

}
