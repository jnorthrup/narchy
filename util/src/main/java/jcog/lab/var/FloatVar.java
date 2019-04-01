package jcog.lab.var;

import jcog.data.list.FasterList;
import jcog.func.ObjectFloatToFloatFunction;
import jcog.lab.Var;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class FloatVar<X> extends Var<X,Float> {

    private float min, max;
    private float inc;

    final List<String> unknown = new FasterList();

    public FloatVar(String id, float min, float max, float inc, Function<X, Float> get, ObjectFloatToFloatFunction<X> set) {
        super(id, get, set::value);
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
    public Float filter(Float value) {
        if (min == min) value = Math.max(min, value);
        if (max == max) value = Math.min(max, value);
        return super.filter(value);
    }

    @Override
    public List<String> unknown(Map<String,Object> hints) {
        FasterList<String> unknown = new FasterList<>(3);
        this.min = unknown(this.min, "min", hints, unknown);
        this.max = unknown(this.max, "max", hints, unknown);


        this.inc = unknown(this.inc, "inc", hints, unknown);
        if (this.inc!=this.inc && (max==max) && (min==min)) {
            
            float autoInc = ((Number)hints.getOrDefault("autoInc", Float.NaN)).floatValue();
            if (autoInc==autoInc) {
                this.inc = (max-min)/autoInc;
                unknown.removeLast();
            }
        }
        return unknown;
    }

    float unknown(float known, String val, Map<String, Object> hints, List<String> unknown) {
        if (known == known)
            return known;

        String key = id + '.' + val;
        float suggestedMin = (Float)hints.getOrDefault(key, Float.NaN);
        if (suggestedMin==suggestedMin)
            return suggestedMin;
        else {
            unknown.add(key);
            return Float.NaN;
        }
    }

    @Override
    public boolean ready() {

        return unknown.isEmpty();
    }
}
