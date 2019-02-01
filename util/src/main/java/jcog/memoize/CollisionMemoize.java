package jcog.memoize;

import jcog.memoize.byt.ByteKey;
import systems.comodal.collision.cache.CollisionCache;

import java.util.function.Function;

public class CollisionMemoize<X,Y> extends AbstractMemoize<X,Y> {
    private final CollisionCache<X, Y> cache;

    public CollisionMemoize(CollisionCache<X,Y> c) {
        this.cache = c;
    }

    /** probably inefficient as is */
    public static <B extends ByteKey,Y> CollisionMemoize<B, Y> get(int capacity, Function<B,Y> f) {


        return new CollisionMemoize<B,Y>(CollisionCache
                        .<Y>withCapacity(capacity)
//                .<Key, byte[]>setLoader(
//                        guid -> loadFromDisk(guid),
//                        (guid, loaded) -> deserialize(loaded))
                        //.setIsValForKey((k, v) -> k.equals(v))
                        .setLoader((B k) -> f.apply(k))
                        //.setLoader((B k) -> ((B)k).key.arrayCopy(), (B k, byte[] b)->f.apply(k))
                        .buildPacked()
        );
    }
    public CollisionMemoize(int capacity, Function<X,Y> f) {
        this(CollisionCache
                .<Y>withCapacity(capacity)
//                .<Key, byte[]>setLoader(
//                        guid -> loadFromDisk(guid),
//                        (guid, loaded) -> deserialize(loaded))
                //.setIsValForKey((guid, val) -> guid.equals(val.getGUID()))
                .setLoader(f)
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
