package jcog.math;

import jcog.data.map.SaneObjectFloatHashMap;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;

public class CachedFloatFunction<X> extends SaneObjectFloatHashMap<X> implements FloatFunction<X> {

    private final FloatFunction<X> f;

    public CachedFloatFunction(FloatFunction<X> f) {
        super(16);
        this.f = f;
    }


    @Override
    public final float floatValueOf(X x) {
        return getIfAbsentPutWithKey(x, f);
    }

}
