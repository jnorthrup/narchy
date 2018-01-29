package jcog.optimize;

import jcog.list.FasterList;
import org.eclipse.collections.api.block.procedure.primitive.ObjectFloatProcedure;

import java.util.List;
import java.util.Map;

public class TweakFloat<X> extends Tweak<X> {


    private float min, max;
    private float inc;

    protected TweakFloat(String id, ObjectFloatProcedure<X> apply) {
        this(id, Float.NaN, Float.NaN, Float.NaN, apply);
    }

    protected TweakFloat(String id, float min, float max, float inc, ObjectFloatProcedure<X> apply) {
        super(id, apply);
        this.min = min;
        this.max = max;
        this.inc = inc;
    }

    public float getMin() {
        return min;
    }

    public float getMax() {
        return max;
    }

    public float getInc() {
        return inc;
    }

    @Override
    public List<String> unknown(Map<String,Float> hints) {
        List<String> unknown = new FasterList<>(3);
        this.min = unknown(this.min, "min", hints, unknown);
        this.max = unknown(this.max, "max", hints, unknown);
        this.inc = unknown(this.inc, "inc", hints, unknown);
        return unknown;
    }

    float unknown(float known, String val, Map<String, Float> hints, List<String> unknown) {
        if (known == known)
            return known;

        String key = id + '.' + val;
        float suggestedMin = hints.getOrDefault(key, Float.NaN);
        if (suggestedMin==suggestedMin)
            return suggestedMin;
        else {
            unknown.add(key);
            return Float.NaN;
        }
    }

}
