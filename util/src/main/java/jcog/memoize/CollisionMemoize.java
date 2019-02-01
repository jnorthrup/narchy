package jcog.memoize;

import jcog.memoize.byt.ByteKey;
import systems.comodal.collision.cache.CollisionBuilder;
import systems.comodal.collision.cache.CollisionCache;

import java.util.function.Function;

public class CollisionMemoize<X,Y> extends AbstractMemoize<X,Y> {
    private final CollisionCache<X, Y> cache;

    public CollisionMemoize(CollisionCache<X,Y> c) {
        this.cache = c;
    }

//    /** probably inefficient as is */
    public static <B extends ByteKey.ByteKeyExternal,Y> CollisionMemoize<B, ByteKey.ByteKeyInternal<Y>> byteKey(int capacity, Function<B,Y> f) {


        CollisionBuilder ccc = CollisionCache
                .<ByteKey.ByteKeyInternal>withCapacity(capacity)
//                .<Key, byte[]>setLoader(
//                        guid -> loadFromDisk(guid),
//                        (guid, loaded) -> deserialize(loaded))
                .setValueType(ByteKey.ByteKeyInternal.class);

        return new CollisionMemoize<B, ByteKey.ByteKeyInternal<Y>>(ccc
                .setIsValForKey(Object::equals)
                .setLoader(f, (k, y) -> ((B)k).internal(y, 0.5f))
                .setLazyInitBuckets(true)
                .setHashCoder(Object::hashCode)
                //.setStrictCapacity(true)
                .buildPacked()
                //.buildSparse()

        ) {
            @Override
            public ByteKey.ByteKeyInternal<Y> apply(B x) {
                ByteKey.ByteKeyInternal<Y> y = super.apply(x);
                x.close();
                return y;
            }
        };

    }

    public CollisionMemoize(int capacity, Function<X,Y> f) {
        this(CollisionCache
                .<Y>withCapacity(capacity)
                .setLoader(f)
                .setLazyInitBuckets(true)
                .setHashCoder(Object::hashCode)
                //.setStrictCapacity(true)
                .buildPacked()
                //.buildSparse()
                );
    }

    @Override
    public String summary() {
        return cache.toString();
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public Y apply(X x) {
        Y y =
                //cache.getAggressive(x);
                cache.get(x);
        return y;
    }
}
