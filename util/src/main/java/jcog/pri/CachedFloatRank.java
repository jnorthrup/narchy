package jcog.pri;

import jcog.math.CachedFloatFunction;
import jcog.sort.FloatRank;

public class CachedFloatRank<X> extends CachedFloatFunction<X> implements FloatRank<X> {

    public CachedFloatRank(int sizeMin) {
        super(sizeMin);
    }

    public CachedFloatRank(int sizeMin, FloatRank<X> f) {
        super(sizeMin, f);
    }

    @Override
    public final float rank(X x, float minIgnored) {
        if (minIgnored == Float.NEGATIVE_INFINITY)
            return getIfAbsentPutWithKey(x, f);
        else {
            return getIfAbsentPutWithKey(x, (xx) -> ((FloatRank<X>) f).rank(xx, minIgnored));
        }
    }

    /** resets the value function */
    public CachedFloatRank<X> value(FloatRank<X> f) {
        clear();
        this.f = f;
        return this;
    }
}
