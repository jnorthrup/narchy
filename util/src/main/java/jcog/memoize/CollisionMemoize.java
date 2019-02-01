package jcog.memoize;

import jcog.memoize.byt.ByteKey;
import systems.comodal.collision.cache.CollisionCache;

import java.util.function.Function;

public class CollisionMemoize<X,Y> extends AbstractMemoize<X,Y> {
    private final CollisionCache<X, Y> cache;

    public CollisionMemoize(CollisionCache<X,Y> c) {
        this.cache = c;
    }

//    /** probably inefficient as is */
    public static <B extends ByteKey.ByteKeyExternal,Y> CollisionMemoize<B, ByteKey.ByteKeyInternal<Y>> byteKey(int capacity, Function<B,Y> f) {


        return new CollisionMemoize(CollisionCache
                .<ByteKey.ByteKeyInternal>withCapacity(capacity)
//                .<Key, byte[]>setLoader(
//                        guid -> loadFromDisk(guid),
//                        (guid, loaded) -> deserialize(loaded))
                .setValueType(ByteKey.ByteKeyInternal.class)
                .setIsValForKey((B k, ByteKey.ByteKeyInternal y) -> k.equals(y))
                .setLoader(f, (B k, Y y) -> k.internal(y, 0.5f))
                .setLazyInitBuckets(true)
                .setHashCoder(Object::hashCode)
                //.buildPacked()
                .buildSparse()

        );

    }

    public CollisionMemoize(int capacity, Function<X,Y> f) {
        this(CollisionCache
                .<Y>withCapacity(capacity)
                .setLoader(f)
                .setLazyInitBuckets(true)
                .setHashCoder(Object::hashCode)
                //.buildPacked()
                .buildSparse()
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
        return cache.get(x);
    }
}
