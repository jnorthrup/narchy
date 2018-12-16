package jcog.bloom;

import jcog.bloom.hash.BytesHashProvider;
import jcog.bloom.hash.HashProvider;

import java.util.function.Function;

public class MetalBloomFilter<E> {
    protected final HashProvider<E> hashProvider;
    protected final byte[] cells;
    protected final int numberOfCells;
    protected final int numberOfHashes;

    public MetalBloomFilter(Function<E,byte[]> hashProvider, int numberOfCells, int numberOfHashes) {
        this(new BytesHashProvider(hashProvider), numberOfCells, numberOfHashes);
    }

    public MetalBloomFilter(HashProvider<E> hashProvider, int numberOfCells, int numberOfHashes) {
        this.hashProvider = hashProvider;
        this.cells = new byte[numberOfCells];
        this.numberOfCells = numberOfCells;
        this.numberOfHashes = numberOfHashes;
    }

    public void add(E element) {
        add(hash(element));
    }

    /** possibly contains */
    public boolean contains(E element) {
        return contains(hash(element));
    }

    public void add(int[] indices) {
        for (int i = 0; i < numberOfHashes; i++) {
            increment(indices[i]);
        }
    }

    /** possibly contains */
    public boolean contains(int[] indices) {
        for (int i = 0; i < numberOfHashes; i++) {
            if (cells[indices[i]] > 0)
                return true;
        }

        return false;
    }

    public int[] hash(E element) {
        int[] hashes = new int[numberOfHashes];

        int h1 = hashProvider.hash1(element);
        int h2 = hashProvider.hash2(element);
        for (int i = 0; i < numberOfHashes; i++) {
            hashes[i] = Math.abs(((h1 + i * h2) % numberOfCells));
        }

        return hashes;
    }

    private void increment(int idx) {
        if (cells[idx] < Byte.MAX_VALUE) {
            cells[idx]++;
        }
    }
}
