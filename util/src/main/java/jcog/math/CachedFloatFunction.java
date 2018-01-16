package jcog.math;

import org.eclipse.collections.api.block.HashingStrategy;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMapWithHashingStrategy;

public class CachedFloatFunction<X> extends ObjectFloatHashMapWithHashingStrategy<X> implements FloatFunction<X> {

    private static final HashingStrategy IDENTITY_STRATEGY = new HashingStrategy() {
        @Override
        public int computeHashCode(Object object) {
            return object.hashCode();
        }

        @Override
        public boolean equals(Object object1, Object object2) {
            return object1 == object2;
        }
    };

    private final FloatFunction<X> f;

    public CachedFloatFunction(int size, FloatFunction<X> f) {
        super(IDENTITY_STRATEGY, size);
        this.f = f;
    }


    @Override
    public final float floatValueOf(X x) {
        return getIfAbsentPutWithKey(x, f);
    }

}
