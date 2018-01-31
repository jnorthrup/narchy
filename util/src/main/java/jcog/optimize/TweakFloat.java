package jcog.optimize;

import jcog.list.FasterList;
import jcog.util.ObjectFloatToFloatFunction;

import java.util.List;
import java.util.Map;

public class TweakFloat<X> extends Tweak<X> {


    private float min, max;
    private float inc;

    protected TweakFloat(String id, ObjectFloatToFloatFunction<X> apply) {
        this(id, Float.NaN, Float.NaN, Float.NaN, apply);
    }

    protected TweakFloat(String id, float min, float max, float inc, ObjectFloatToFloatFunction<X> apply) {
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
        FasterList<String> unknown = new FasterList<>(3);
        this.min = unknown(this.min, "min", hints, unknown);
        this.max = unknown(this.max, "max", hints, unknown);


        this.inc = unknown(this.inc, "inc", hints, unknown);
        if (this.inc!=this.inc && (max==max) && (min==min)) {
            //infer 'inc' if max and min are known and autoDiscrete hint is provided
            float autoInc = hints.getOrDefault("autoInc", Float.NaN);
            if (autoInc==autoInc) {
                this.inc = (max-min)/autoInc;
                unknown.removeLast();
            }
        }
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
