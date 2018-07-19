package jcog.bloom;

import jcog.util.Flip;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.tuple.Tuples;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Predicate;

import static jcog.bloom.LongBitsetBloomFilter.DEFAULT_FPP;

/**
 * pair of BloomFilter which, when one or both are
 * consulted, can answer 'yes' 'no' or 'maybe' for a given input and a
 * computable predicate which it will store in both
 * <p>
 * resets the bloom filters when a capacity limit is reached
 * <p>
 * thread safe
 * <p>
 *
 * TODO add ability to consult the shadow version when the current one is in doubt
 * and only clear the shadow copy right before swapping out the active one.
 * this means 4 bloom filter comparisons for the byte[] key
 */
public class YesNoMaybe<X> extends Flip<Twin<LongBitsetBloomFilter>> {

    public final AtomicLong hitYes = new AtomicLong();
    public final AtomicLong hitNo = new AtomicLong();
    public final AtomicLong miss = new AtomicLong();

    final AtomicInteger count = new AtomicInteger();
    private final Function<X, byte[]> byter;
    private final int capacity;
    private final Predicate<X> test;

    public YesNoMaybe(Predicate<X> test, Function<X, byte[]> byter, int capacity) {
        this(test, byter, capacity, DEFAULT_FPP);
    }

    public YesNoMaybe(Predicate<X> test, Function<X, byte[]> byter, int capacity, float falsePosRate) {
        super(() ->
                Tuples.twin(
                        new LongBitsetBloomFilter(capacity, falsePosRate),
                        new LongBitsetBloomFilter(capacity, falsePosRate)
                )
        );
        this.capacity = capacity;
        this.test = test;
        this.byter = byter;
    }

    public boolean test(X x) {
        LongBitsetBloomFilter ny, nn;

        int c;
        {
            
            do {
                c = this.count.get();
                Thread.onSpinWait();
            } while (c < 0 || c >= capacity);

            Twin<LongBitsetBloomFilter> pw = read();
            ny = pw.getOne();
            nn = pw.getTwo();
        }

        byte[] b = byter.apply(x);

        boolean definiteYes = !ny.test(b);
        boolean definiteNo = !nn.test(b);
        if (definiteYes ^ definiteNo) {
            (definiteYes ? hitYes : hitNo).incrementAndGet();
            return definiteYes;
        }

        if (this.count.incrementAndGet() == capacity && this.count.compareAndSet(capacity, -1)) {
            
            Twin<LongBitsetBloomFilter> pw = write();
            pw.getOne().clear();
            pw.getTwo().clear();

            pw = commitAndGet();
            ny = pw.getOne();
            nn = pw.getTwo();

            this.count.set(0);
        }

        miss.incrementAndGet();

        boolean actual = test.test(x);

        (actual ? nn : ny).add(b); 
        return actual;
    }

    public float hitRate(boolean clear) {
        int miss = this.miss.intValue();
        int yes = hitYes.intValue();
        int no = hitNo.intValue();
        float total = miss + yes + no;
        if (total == 0) return 0;
        else if (clear) {
            this.miss.set(0); hitYes.set(0); hitNo.set(0);
        }
        return (no + yes) / total;
    }
}
