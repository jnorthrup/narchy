package jcog.bloom;

import jcog.bloom.hash.DefaultHashProvider;
import jcog.bloom.hash.HashProvider;

import java.util.Random;

/**
 * {@link BloomFilterBuilder}s are the entry point to builder different types of {@link LeakySet}s.
 *
 * References:
 * [1] Phillip Bradford and Michael Katehakis. 2007. A probabilistic study on combinatorial expanders and hashing. DOI=http:
 */
@Deprecated public class BloomFilterBuilder<E> {

    private int size = 1000;
    private int numberOfHashes = 3;
    private float unlearningRate = 0.0005f;
    private HashProvider<E> hashProvider = new DefaultHashProvider<>();

    private BloomFilterBuilder() {}

    /**
     * Start a new {@link BloomFilterBuilder}.
     * @return A new instance.
     */
    public static BloomFilterBuilder get() {
        return new BloomFilterBuilder();
    }

    /**
     * Set the logical size of the filter. In common publications called <i>m</i>.
     * @param size In number of fields. This translate to bytes or bits depending on the implementation.
     * @return {@link BloomFilterBuilder} For chaining.
     */
    public BloomFilterBuilder withSize(int size) {
        this.size = size;
        return this;
    }

    /**
     * Set the number of hash values that will be used to fingerprint a data element.
     * In common publications called <i>K</i>.
     * @param numberOfHashes The values will be produced by double-hashing from two different hash functions [1].
     * @return {@link BloomFilterBuilder} For chaining.
     */
    public BloomFilterBuilder withNumberOfHashes(int numberOfHashes) {
        this.numberOfHashes = numberOfHashes;
        return this;
    }

    /**
     * Set the unlearning rate to make the {@link LeakySet} stable. The unlearning rate represents
     * a percentage of filter cells that will be "unlearned" with each write operation.
     * @param unlearningRate Must be between 0.0 and 1.0.
     * @return {@link BloomFilterBuilder} For chaining.
     */
    public BloomFilterBuilder withUnlearningRate(float unlearningRate) {
        this.unlearningRate = unlearningRate;
        return this;
    }

    /**
     * Set a {@link HashProvider} to generate hash fingerprints for elements.
     * @param hashProvider
     * @return {@link BloomFilterBuilder} For chaining.
     */
    public BloomFilterBuilder withHashProvider(HashProvider<E> hashProvider) {
        this.hashProvider = hashProvider;
        return this;
    }

    /**
     * Build the instance.
     * @return Standard {@link LeakySet}.
     */
    public StableBloomFilter<E> buildFilter() {
        return new StableBloomFilter<>(size, numberOfHashes,  unlearningRate, new Random(), hashProvider);
    }

}
