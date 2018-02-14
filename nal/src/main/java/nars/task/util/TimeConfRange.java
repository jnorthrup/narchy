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
        return t.intersectsConf(cMin, cMax) && t.intersects(start, end);
    }


    @Override
    public boolean contains(HyperRegion x) {
        TaskRegion t = (TaskRegion)x;
        return t.containsConf(cMin,cMax) && t.contains(start, end);
    }

}
