package nars.task.util;

import jcog.tree.rtree.HyperRegion;
import nars.Task;
import nars.time.Tense;
import nars.truth.polation.TruthIntegration;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;

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

    /** sorts nearest to the end of a list */
    public static FloatFunction<TaskRegion> distanceFunction(TimeRange a) {

        if (a.start == Tense.ETERNAL) {
            return b -> b instanceof Task ? TruthIntegration.evi((Task)b) : (b.confMax() * b.range());
        } else if (a.start != a.end) {
            //return b -> -(Util.mean(b.minTimeTo(a.start), b.minTimeTo(a.end))) -b.range()/tableDur;
            //return b -> -(Util.mean(b.midTimeTo(a.start), b.minTimeTo(a.end))); // -b.range()/tableDur;
            return b -> -b.minTimeTo(a.start, a.end); // -b.range()/tableDur;
        } else {
            return b -> -b.minTimeTo(a.start); // -b.range()/tableDur;
        }

    }
}
