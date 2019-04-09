//package nars.task.util;
//
//import jcog.tree.rtree.HyperRegion;
//
//public class TimeConfRange extends TimeRange {
//
//    public final float cMin, cMax;
//
//    public TimeConfRange(long s, long e, float cMin, float cMax) {
//        super(s, e);
//        this.cMin = cMin;
//        this.cMax = cMax;
//    }
//
//    @Override
//    public boolean intersects(HyperRegion x) {
//        TaskRegion t = (TaskRegion)x;
//        return t.intersects(start, end) && t.intersectsConf(cMin, cMax);
//    }
//
//    @Override
//    public boolean contains(HyperRegion x) {
//        TaskRegion t = (TaskRegion)x;
//        return t.contains(start, end) && t.containsConf(cMin,cMax);
//    }
//
//}
