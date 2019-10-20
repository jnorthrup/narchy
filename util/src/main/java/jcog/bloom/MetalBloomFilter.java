package jcog.bloom;

import jcog.bloom.hash.BytesHasher;
import jcog.bloom.hash.Hasher;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.IntStream;

public class MetalBloomFilter<E> {
    protected final Hasher<E> hasher;
    protected final byte[] cells;
    protected final int numberOfCells;
    protected final int numberOfHashes;

    public MetalBloomFilter(Function<E,byte[]> hashProvider, int numberOfCells, int numberOfHashes) {
        this(new BytesHasher(hashProvider), numberOfCells, numberOfHashes);
    }

    public MetalBloomFilter(Hasher<E> hasher, int numberOfCells, int numberOfHashes) {
        this.hasher = hasher;
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
        for (var i = 0; i < numberOfHashes; i++) {
            increment(indices[i]);
        }
    }

    /** possibly contains */
    public boolean contains(int[] indices) {

        var bound = numberOfHashes;
        return IntStream.range(0, bound).anyMatch(i -> cells[indices[i]] > 0);
    }

    public int[] hash(E element) {

        var h1 = hasher.hash1(element);
        var h2 = hasher.hash2(element);
        var hashes = new int[10];
        var count = 0;
        var bound = numberOfHashes;
        for (var i = 0; i < bound; i++) {
            var abs = Math.abs(((h1 + i * h2) % numberOfCells));
            if (hashes.length == count) hashes = Arrays.copyOf(hashes, count * 2);
            hashes[count++] = abs;
        }
        hashes = Arrays.copyOfRange(hashes, 0, count);

        return hashes;
    }

    private void increment(int idx) {
        if (cells[idx] < Byte.MAX_VALUE) {
            cells[idx]++;
        }
    }
}
