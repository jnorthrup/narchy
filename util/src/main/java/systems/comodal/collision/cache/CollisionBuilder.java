package systems.comodal.collision.cache;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Array;
import java.util.function.*;

public final class CollisionBuilder<V> {

    static final int DEFAULT_SPARSE_BUCKET_SIZE = 4;
    static final int DEFAULT_PACKED_BUCKET_SIZE = 8;

    static final Function<?, ?> NULL_LOADER = key -> null;
    static final ToIntFunction<?> DEFAULT_HASH_CODER = key -> spread(key.hashCode());
    static final BiPredicate<?, ?> DEFAULT_IS_VAL_FOR_KEY = Object::equals;

    /**
     * Multiplied by the desired capacity to determine the hash table length.
     * Increase to reduce collisions.
     * If increasing consider lazyInitBuckets to prevent unnecessary array creation.
     */
    static final double DEFAULT_SPARSE_FACTOR = 3.0;
    static final VarHandle BUCKETS = MethodHandles.arrayElementVarHandle(Object[][].class);
    private final int capacity;
    private boolean strictCapacity = false;
    private Class<? extends V> valueType;
    private int bucketSize = 0;
    private int initCount = 5;
    private int maxCounterVal = 1_048_576;
    private boolean lazyInitBuckets = false;
    private boolean storeKeys = true;

    CollisionBuilder(final int capacity) {
        this.capacity = capacity;
    }

    /**
     * Taken from {@link java.util.concurrent.ConcurrentHashMap#spread
     * java.util.concurrent.ConcurrentHashMap}
     *
     * @see java.util.concurrent.ConcurrentHashMap#spread(int)
     */
    private static int spread(final int hash) {
        return (hash ^ (hash >>> 16));
    }

    public <K> CollisionCache<K, V> buildSparse() {
        return buildSparse(DEFAULT_SPARSE_FACTOR);
    }

    /**
     * @param sparseFactor Used to expand the size of the backing hash table to reduce collisions.
     *                     Defaults to 3.0 and has a minimum of 1.0.
     * @return A newly built {@link CollisionCache CollisionCache}.
     */
    @SuppressWarnings("unchecked")
    public <K> CollisionCache<K, V> buildSparse(final double sparseFactor) {
        return buildSparse(
                sparseFactor,
                (ToIntFunction<K>) DEFAULT_HASH_CODER,
                (BiPredicate<K, V>) DEFAULT_IS_VAL_FOR_KEY,
                (Function<K, ?>) NULL_LOADER,
                null);
    }

    <K, L> LoadingCollisionCache<K, L, V> buildSparse(
            final double sparseFactor,
            final ToIntFunction<K> hashCoder,
            final BiPredicate<K, V> isValForKey,
            final Function<K, L> loader,
            final BiFunction<K, L, V> mapper) {
        final int bucketSize = this.bucketSize > 0 ? this.bucketSize : DEFAULT_SPARSE_BUCKET_SIZE;
        final int maxCollisions = Integer.highestOneBit(bucketSize - 1) << 1;
        final int maxCollisionsShift = Integer.numberOfTrailingZeros(maxCollisions);
        final AtomicLogCounters counters = AtomicLogCounters.create(
                Integer.highestOneBit((int) (capacity * Math.max(1.0, sparseFactor)) - 1) << 1,
                initCount, maxCounterVal);
        final int hashTableLength = counters.getNumCounters() >> maxCollisionsShift;
        if (isStoreKeys()) {
            final KeyVal<K, V>[][] hashTable = createEntryHashTable(hashTableLength, maxCollisions);
            return new SparseEntryCollisionCache<>(
                    capacity,
                    strictCapacity,
                    maxCollisionsShift,
                    hashTable,
                    createEntryGetBucket(hashTable, maxCollisionsShift),
                    counters,
                    hashCoder, loader, mapper);
        }
        final V[][] hashTable = createHashTable(hashTableLength, maxCollisions);
        return new SparseCollisionCache<>(
                capacity,
                strictCapacity,
                valueType,
                maxCollisionsShift,
                hashTable,
                createGetBucket(hashTable, maxCollisionsShift),
                counters,
                hashCoder, isValForKey, loader, mapper);
    }

    @SuppressWarnings("unchecked")
    public <K> CollisionCache<K, V> buildPacked() {
        return buildPacked(
                (ToIntFunction<K>) DEFAULT_HASH_CODER,
                (BiPredicate<K, V>) DEFAULT_IS_VAL_FOR_KEY,
                (Function<K, ?>) NULL_LOADER, null);
    }

    <K, L> LoadingCollisionCache<K, L, V> buildPacked(
            final ToIntFunction<K> hashCoder,
            final BiPredicate<K, V> isValForKey,
            final Function<K, L> loader,
            final BiFunction<K, L, V> mapper) {
        final int bucketSize = this.bucketSize > 0 ? this.bucketSize : DEFAULT_PACKED_BUCKET_SIZE;
        final int maxCollisions = Integer.highestOneBit(bucketSize - 1) << 1;
        final int maxCollisionsShift = Integer.numberOfTrailingZeros(maxCollisions);
        final AtomicLogCounters counters = AtomicLogCounters.create(
                Integer.highestOneBit(capacity - 1) << 1, initCount, maxCounterVal);
        final int hashTableLength = counters.getNumCounters() >> maxCollisionsShift;
        if (isStoreKeys()) {
            final KeyVal<K, V>[][] hashTable = createEntryHashTable(hashTableLength, maxCollisions);
            return new PackedEntryCollisionCache<>(
                    maxCollisionsShift,
                    hashTable,
                    createEntryGetBucket(hashTable, maxCollisionsShift),
                    counters,
                    hashCoder, loader, mapper);
        }
        final V[][] hashTable = createHashTable(hashTableLength, maxCollisions);
        return new PackedCollisionCache<>(
                valueType,
                maxCollisionsShift,
                hashTable,
                createGetBucket(hashTable, maxCollisionsShift),
                counters,
                hashCoder, isValForKey, loader, mapper);
    }

    @SuppressWarnings("unchecked")
    private <K> KeyVal<K, V>[][] createEntryHashTable(
            final int hashTableLength,
            final int maxCollisions) {
        if (lazyInitBuckets) {
            final Class<?> valueArrayType = Array
                    .newInstance(KeyVal.class, 0).getClass();
            return (KeyVal<K, V>[][]) Array.newInstance(valueArrayType, hashTableLength);
        }
        return (KeyVal<K, V>[][]) Array
                .newInstance(KeyVal.class, hashTableLength, maxCollisions);
    }

    @SuppressWarnings("unchecked")
    private <K> IntFunction<KeyVal<K, V>[]> createEntryGetBucket(final KeyVal<K, V>[][] hashTable,
                                                                 final int maxCollisionsShift) {
        return !lazyInitBuckets ? hash -> hashTable[hash]
                : hash -> {
            KeyVal<K, V>[] collisions = hashTable[hash];
            if (collisions == null) {
                collisions = (KeyVal<K, V>[]) Array
                        .newInstance(KeyVal.class, 1 << maxCollisionsShift);
                final Object witness = BUCKETS.compareAndExchange(hashTable, hash, null, collisions);
                return witness == null ? collisions : (KeyVal<K, V>[]) witness;
            }
            return collisions;
        };
    }

    @SuppressWarnings("unchecked")
    private V[][] createHashTable(final int hashTableLength, final int maxCollisions) {
        if (valueType == null) {
            throw new IllegalStateException("valueType needed.");
        }
        if (lazyInitBuckets) {
            final Class<?> valueArrayType = Array.newInstance(valueType, 0).getClass();
            return (V[][]) Array.newInstance(valueArrayType, hashTableLength);
        }
        return (V[][]) Array.newInstance(valueType, hashTableLength, maxCollisions);
    }

    @SuppressWarnings("unchecked")
    private IntFunction<V[]> createGetBucket(final V[][] hashTable,
                                             final int maxCollisionsShift) {
        return !lazyInitBuckets ? hash -> hashTable[hash]
                : hash -> {
            V[] collisions = hashTable[hash];
            if (collisions == null) {
                collisions = (V[]) Array.newInstance(valueType, 1 << maxCollisionsShift);
                final Object witness = BUCKETS.compareAndExchange(hashTable, hash, null, collisions);
                return witness == null ? collisions : (V[]) witness;
            }
            return collisions;
        };
    }

    /**
     * The computed hash code is used to index the backing hash table of the cache.  Hash tables are
     * always a length of some power of two.  The hash code will be masked against
     * (hashTable.length - 1) to prevent index out of bounds exceptions.
     *
     * @param hashCoder computes an integer hash code for a given key.
     * @return {@link KeyedCollisionBuilder KeyedCollisionBuilder} to continue building process.
     */
    public <K> KeyedCollisionBuilder<K, V> setHashCoder(final ToIntFunction<K> hashCoder) {
        return new KeyedCollisionBuilder<>(this, hashCoder);
    }

    /**
     * Keys will not be stored if this predicate is provided.  This is the primary motivation of
     * Collision.  The idea is allow for more cache capacity by not storing keys.
     *
     * @param isValForKey tests if a given value corresponds to the given key.
     * @return {@link KeyedCollisionBuilder KeyedCollisionBuilder} to continue building process.
     */
    public <K> KeyedCollisionBuilder<K, V> setIsValForKey(final BiPredicate<K, V> isValForKey) {
        return new KeyedCollisionBuilder<>(this, isValForKey);
    }

    /**
     * Set the loader used to initialize values if missing from the cache.  The loader may return null
     * values, the cache will simply return null as well.  The cache will provide methods to use the
     * loader either atomically or not.
     *
     * @param loader returns values for a given key.
     * @return {@link LoadingCollisionBuilder LoadingCollisionBuilder} to continue building process.
     */
    public <K> LoadingCollisionBuilder<K, V, V> setLoader(final Function<K, V> loader) {
        return setLoader(loader, (key, val) -> val);
    }

    /**
     * Set the loader and mapper used to initialize values if missing from the cache.  The loader may
     * return null values, the cache will simply return null as well.  The cache will provide methods
     * to use the loader either atomically or not.  The mapper is separated out to delay any final
     * processing/parsing until it is absolutely needed.  The mapper will never be passed a null value
     * and must not return a null value; cache performance could severely degrade.
     *
     * @param loader returns values for a given key.
     * @param mapper map loaded types to value types.
     * @param <L>    The intermediate type between loading and mapping.
     * @return {@link LoadingCollisionBuilder LoadingCollisionBuilder} to continue building process.
     */
    public <K, L> LoadingCollisionBuilder<K, L, V> setLoader(final Function<K, L> loader,
                                                             final BiFunction<K, L, V> mapper) {
        return new LoadingCollisionBuilder<>(new KeyedCollisionBuilder<>(this), loader, mapper);
    }

    public int getCapacity() {
        return capacity;
    }

    public boolean isStrictCapacity() {
        return strictCapacity;
    }

    public CollisionBuilder<V> setStrictCapacity(final boolean strictCapacity) {
        this.strictCapacity = strictCapacity;
        return this;
    }

    public Class<? extends V> getValueType() {
        return valueType;
    }

    public CollisionBuilder<V> setValueType(final Class<? extends V> valueType) {
        this.valueType = valueType;
        return this;
    }

    public int getBucketSize() {
        return bucketSize;
    }

    public CollisionBuilder<V> setBucketSize(final int bucketSize) {
        this.bucketSize = bucketSize;
        return this;
    }

    public int getInitCount() {
        return initCount;
    }

    public CollisionBuilder<V> setInitCount(final int initCount) {
        if (initCount > 32) {
            throw new IllegalStateException("Setting a large initial counter count is pointless.");
        }
        if (initCount < 0) {
            throw new IllegalStateException("Initial counter count must be >= 0.");
        }
        this.initCount = initCount;
        return this;
    }

    public int getMaxCounterVal() {
        return maxCounterVal;
    }

    public CollisionBuilder<V> setMaxCounterVal(final int maxCounterVal) {
        if (maxCounterVal < 256) {
            throw new IllegalStateException("The maximum counter count should be large to increase the "
                    + "likelihood of choosing the least frequently used entry for eviction.");
        }
        this.maxCounterVal = maxCounterVal;
        return this;
    }

    public boolean isLazyInitBuckets() {
        return lazyInitBuckets;
    }

    public CollisionBuilder<V> setLazyInitBuckets(final boolean lazyInitBuckets) {
        this.lazyInitBuckets = lazyInitBuckets;
        return this;
    }

    protected boolean isStoreKeys() {
        return storeKeys;
    }

    public CollisionBuilder<V> setStoreKeys(final boolean storeKeys) {
        this.storeKeys = storeKeys;
        return this;
    }

    public static final class KeyedCollisionBuilder<K, V> {

        private final CollisionBuilder<V> delegate;
        private ToIntFunction<K> hashCoder;
        private BiPredicate<K, V> isValForKey;

        KeyedCollisionBuilder(final CollisionBuilder<V> delegate, final ToIntFunction<K> hashCoder) {
            this(delegate, hashCoder, null);
        }

        KeyedCollisionBuilder(final CollisionBuilder<V> delegate,
                              final BiPredicate<K, V> isValForKey) {
            this(delegate, null, isValForKey);
        }

        KeyedCollisionBuilder(final CollisionBuilder<V> delegate) {
            this(delegate, null, null);
        }

        KeyedCollisionBuilder(final CollisionBuilder<V> delegate, final ToIntFunction<K> hashCoder,
                              final BiPredicate<K, V> isValForKey) {
            this.delegate = delegate;
            this.hashCoder = hashCoder;
            this.isValForKey = isValForKey;
            if (isValForKey != null) {
                delegate.setStoreKeys(false);
            }
        }

        public CollisionCache<K, V> buildSparse() {
            return buildSparse(DEFAULT_SPARSE_FACTOR);
        }

        /**
         * @param sparseFactor Used to expand the size of the backing hash table to reduce collisions.
         *                     Defaults to 3.0 and has a minimum of 1.0.
         * @return A newly built {@link CollisionCache CollisionCache}.
         */
        public CollisionCache<K, V> buildSparse(final double sparseFactor) {
            return buildSparse(sparseFactor, key -> null, null);
        }

        <L> LoadingCollisionCache<K, L, V> buildSparse(
                final double sparseFactor,
                final Function<K, L> loader,
                final BiFunction<K, L, V> mapper) {
            return delegate.buildSparse(sparseFactor, getHashCoder(), getIsValForKey(), loader, mapper);
        }

        public CollisionCache<K, V> buildPacked() {
            return buildPacked(key -> null, null);
        }

        <L> LoadingCollisionCache<K, L, V> buildPacked(
                final Function<K, L> loader,
                final BiFunction<K, L, V> mapper) {
            return delegate.buildPacked(getHashCoder(), getIsValForKey(), loader, mapper);
        }

        public int getCapacity() {
            return delegate.getCapacity();
        }

        /**
         * Set the loader used to initialize values if missing from the cache.  The loader may return null
         * values, the cache will simply return null as well.  The cache will provide methods to use the
         * loader either atomically or not.
         *
         * @param loader returns values for a given key.
         * @return {@link LoadingCollisionBuilder LoadingCollisionBuilder} to continue building process.
         */
        public LoadingCollisionBuilder<K, V, V> setLoader(final Function<K, V> loader) {
            return setLoader(loader, (key, val) -> val);
        }

        /**
         * Set the loader and mapper used to initialize values if missing from the cache.  The loader may
         * return null values, the cache will simply return null as well.  The cache will provide methods
         * to use the loader either atomically or not.  The mapper is separated out to delay any final
         * processing/parsing until it is absolutely needed.  The mapper will never be passed a null value
         * and must not return a null value; cache performance could severely degrade.
         *
         * @param loader returns values for a given key.
         * @param mapper map loaded types to value types.
         * @param <L>    The intermediate type between loading and mapping.
         * @return {@link LoadingCollisionBuilder LoadingCollisionBuilder} to continue building process.
         */
        public <L> LoadingCollisionBuilder<K, L, V> setLoader(final Function<K, L> loader,
                                                              final BiFunction<K, L, V> mapper) {
            return new LoadingCollisionBuilder<>(this, loader, mapper);
        }

        @SuppressWarnings("unchecked")
        public ToIntFunction<K> getHashCoder() {
            return hashCoder == null ? (ToIntFunction<K>) DEFAULT_HASH_CODER : hashCoder;
        }

        /**
         * The computed hash code is used to index the backing hash table of the cache.  Hash tables are
         * always a length of some power of two.  The hash code will be masked against
         * (hashTable.length - 1) to prevent index out of bounds exceptions.
         *
         * @param hashCoder computes an integer hash code for a given key.
         * @return {@link KeyedCollisionBuilder KeyedCollisionBuilder} to continue building process.
         */
        public KeyedCollisionBuilder<K, V> setHashCoder(final ToIntFunction<K> hashCoder) {
            this.hashCoder = hashCoder;
            return this;
        }

        @SuppressWarnings("unchecked")
        public BiPredicate<K, V> getIsValForKey() {
            return isValForKey == null ? (BiPredicate<K, V>) DEFAULT_IS_VAL_FOR_KEY : isValForKey;
        }

        /**
         * Keys will not be stored if this predicate is provided.  This is the primary motivation of
         * Collision.  The idea is allow for more cache capacity by not storing keys.
         *
         * @param isValForKey tests if a given value corresponds to the given key.
         * @return {@link KeyedCollisionBuilder KeyedCollisionBuilder} to continue building process.
         */
        public KeyedCollisionBuilder<K, V> setIsValForKey(final BiPredicate<K, V> isValForKey) {
            delegate.setStoreKeys(false);
            this.isValForKey = isValForKey;
            return this;
        }

        public boolean isStrictCapacity() {
            return delegate.isStrictCapacity();
        }

        public KeyedCollisionBuilder<K, V> setStrictCapacity(final boolean strictCapacity) {
            delegate.setStrictCapacity(strictCapacity);
            return this;
        }

        public Class<? extends V> getValueType() {
            return delegate.getValueType();
        }

        public KeyedCollisionBuilder<K, V> setValueType(final Class<V> valueType) {
            delegate.setValueType(valueType);
            return this;
        }

        public int getBucketSize() {
            return delegate.getBucketSize();
        }

        public KeyedCollisionBuilder<K, V> setBucketSize(final int bucketSize) {
            delegate.setBucketSize(bucketSize);
            return this;
        }

        public int getInitCount() {
            return delegate.getInitCount();
        }

        public KeyedCollisionBuilder<K, V> setInitCount(final int initCount) {
            delegate.setInitCount(initCount);
            return this;
        }

        public int getMaxCounterVal() {
            return delegate.getMaxCounterVal();
        }

        public KeyedCollisionBuilder<K, V> setMaxCounterVal(final int maxCounterVal) {
            delegate.setMaxCounterVal(maxCounterVal);
            return this;
        }

        public boolean isLazyInitBuckets() {
            return delegate.isLazyInitBuckets();
        }

        public KeyedCollisionBuilder<K, V> setLazyInitBuckets(final boolean lazyInitBuckets) {
            delegate.setLazyInitBuckets(lazyInitBuckets);
            return this;
        }

        public boolean isStoreKeys() {
            return delegate.isStoreKeys();
        }

        public KeyedCollisionBuilder<K, V> setStoreKeys(final boolean storeKeys) {
            delegate.setStoreKeys(storeKeys);
            return this;
        }
    }

    public static final class LoadingCollisionBuilder<K, L, V> {

        private final KeyedCollisionBuilder<K, V> delegate;
        private final Function<K, L> loader;
        private final BiFunction<K, L, V> mapper;

        LoadingCollisionBuilder(final KeyedCollisionBuilder<K, V> delegate, final Function<K, L> loader,
                                final BiFunction<K, L, V> mapper) {
            this.delegate = delegate;
            this.loader = loader;
            this.mapper = mapper;
        }

        public LoadingCollisionCache<K, L, V> buildSparse() {
            return buildSparse(DEFAULT_SPARSE_FACTOR);
        }

        /**
         * @param sparseFactor Used to expand the size of the backing hash table to reduce collisions.
         *                     Defaults to 3.0 and has a minimum of 1.0.
         * @return A newly built {@link LoadingCollisionCache LoadingCollisionCache}.
         */
        public LoadingCollisionCache<K, L, V> buildSparse(final double sparseFactor) {
            return delegate.buildSparse(sparseFactor, loader, mapper);
        }

        public LoadingCollisionCache<K, L, V> buildPacked() {
            return delegate.buildPacked(loader, mapper);
        }

        public int getCapacity() {
            return delegate.getCapacity();
        }

        public ToIntFunction<K> getHashCoder() {
            return delegate.getHashCoder();
        }

        /**
         * The computed hash code is used to index the backing hash table of the cache.  Hash tables are
         * always a length of some power of two.  The hash code will be masked against
         * (hashTable.length - 1) to prevent index out of bounds exceptions.
         *
         * @param hashCoder computes an integer hash code for a given key.
         * @return {@link LoadingCollisionBuilder KeyedCollisionBuilder} to continue building process.
         */
        public LoadingCollisionBuilder<K, L, V> setHashCoder(final ToIntFunction<K> hashCoder) {
            delegate.setHashCoder(hashCoder);
            return this;
        }

        public BiPredicate<K, V> getIsValForKey() {
            return delegate.getIsValForKey();
        }

        /**
         * Keys will not be stored if this predicate is provided.  This is the primary motivation of
         * Collision.  The idea is allow for more cache capacity by not storing keys.
         *
         * @param isValForKey tests if a given value corresponds to the given key.
         * @return {@link LoadingCollisionBuilder LoadingCollisionBuilder} to continue building process.
         */
        public LoadingCollisionBuilder<K, L, V> setIsValForKey(final BiPredicate<K, V> isValForKey) {
            delegate.setIsValForKey(isValForKey);
            return this;
        }

        public boolean isStrictCapacity() {
            return delegate.isStrictCapacity();
        }

        public LoadingCollisionBuilder<K, L, V> setStrictCapacity(final boolean strictCapacity) {
            delegate.setStrictCapacity(strictCapacity);
            return this;
        }

        public Class<? extends  V> getValueType() {
            return delegate.getValueType();
        }

        public LoadingCollisionBuilder<K, L, V> setValueType(final Class<V> valueType) {
            delegate.setValueType(valueType);
            return this;
        }

        public int getBucketSize() {
            return delegate.getBucketSize();
        }

        public LoadingCollisionBuilder<K, L, V> setBucketSize(final int bucketSize) {
            delegate.setBucketSize(bucketSize);
            return this;
        }

        public int getInitCount() {
            return delegate.getInitCount();
        }

        public LoadingCollisionBuilder<K, L, V> setInitCount(final int initCount) {
            delegate.setInitCount(initCount);
            return this;
        }

        public int getMaxCounterVal() {
            return delegate.getMaxCounterVal();
        }

        public LoadingCollisionBuilder<K, L, V> setMaxCounterVal(final int maxCounterVal) {
            delegate.setMaxCounterVal(maxCounterVal);
            return this;
        }

        public boolean isLazyInitBuckets() {
            return delegate.isLazyInitBuckets();
        }

        public LoadingCollisionBuilder<K, L, V> setLazyInitBuckets(final boolean lazyInitBuckets) {
            delegate.setLazyInitBuckets(lazyInitBuckets);
            return this;
        }

        public boolean isStoreKeys() {
            return delegate.isStoreKeys();
        }

        public LoadingCollisionBuilder<K, L, V> setStoreKeys(final boolean storeKeys) {
            delegate.setStoreKeys(storeKeys);
            return this;
        }
    }
}
