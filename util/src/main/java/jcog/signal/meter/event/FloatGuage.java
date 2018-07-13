package jcog.signal.meter.event;

import jcog.math.RecycledSummaryStatistics;


/**
 * Sums guaged objects, and returns
 */
public class FloatGuage extends RecycledSummaryStatistics {

    public final String id;

    public FloatGuage(String id) {
        super();
        this.id = id;
    }


}
