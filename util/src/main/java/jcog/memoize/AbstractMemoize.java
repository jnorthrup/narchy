package jcog.memoize;

import java.util.concurrent.atomic.AtomicLong;

/** TODO use for CaffeineMemoize etc */
public abstract class AbstractMemoize<X, Y> implements Memoize<X, Y> {

    public final AtomicLong
            hit = new AtomicLong();
    public final AtomicLong miss = new AtomicLong();
    public final AtomicLong reject = new AtomicLong();
    public final AtomicLong evict = new AtomicLong();


}
