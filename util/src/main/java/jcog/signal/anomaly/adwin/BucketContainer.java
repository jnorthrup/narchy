package jcog.signal.anomaly.adwin;

import java.util.Arrays;

/**
 * This {@link BucketContainer} data structure is used in the Histogram and represents an array buckets.
 */
class BucketContainer {

    private BucketContainer prev;
    private BucketContainer next;

    private final int maxBuckets;
    private final Bucket[] buckets;
    private int firstBucket;
    private final int numElementsPerBucket;
    private int numBuckets;

    BucketContainer(final BucketContainer prev, final BucketContainer next, final int maxBuckets, final int elementsPerBucket) {
        this.prev = prev;
        this.next = next;
        this.maxBuckets = maxBuckets;
        this.numElementsPerBucket = elementsPerBucket;
        this.buckets = new Bucket[maxBuckets];
        this.firstBucket = 0;
        this.numBuckets = 0;
    }

    private BucketContainer(final BucketContainer originalContainer) {
        this.maxBuckets = originalContainer.maxBuckets;
        this.buckets = Arrays.copyOf(originalContainer.buckets, originalContainer.buckets.length);
        this.firstBucket = originalContainer.firstBucket;
        this.numElementsPerBucket = originalContainer.numElementsPerBucket;
        this.numBuckets = originalContainer.numBuckets;
    }

    final BucketContainer prev() {
        return this.prev;
    }

    final BucketContainer next() {
        return this.next;
    }

    int size() {
        return this.numBuckets;
    }

    int elemsPerBucket() {
        return this.numElementsPerBucket;
    }

    int capacity() {
        return this.maxBuckets;
    }

    void setNext(final BucketContainer next) {
        this.next = next;
    }

    void addBucket(Bucket newBucket) {
        assert newBucket.size() == numElementsPerBucket;
        buckets[(firstBucket + numBuckets) % maxBuckets] = newBucket;
        numBuckets++;
    }

    Bucket[] removeBuckets(int num) {
        Bucket[] resultBuckets = new Bucket[num];
        if (firstBucket <= ((firstBucket + num) % maxBuckets)) {
            System.arraycopy(buckets, firstBucket, resultBuckets, 0, num);
        } else {
            System.arraycopy(buckets, firstBucket, resultBuckets, 0, maxBuckets - firstBucket);
            System.arraycopy(buckets, 0, resultBuckets, maxBuckets - firstBucket, num - maxBuckets + firstBucket);
        }
        firstBucket = (firstBucket + num) % maxBuckets;
        numBuckets -= num;
        return resultBuckets;
    }

    Bucket bucket(int position) {
        return buckets[(firstBucket + (numBuckets - position - 1)) % maxBuckets];
    }

    BucketContainer deepCopy() {
        final BucketContainer copyThis = new BucketContainer(this);
        if(this.next!=null){
            final BucketContainer nextCopy = this.next.deepCopy();
            copyThis.next = nextCopy;
            nextCopy.prev = copyThis;
        }
        return copyThis;
    }
}
