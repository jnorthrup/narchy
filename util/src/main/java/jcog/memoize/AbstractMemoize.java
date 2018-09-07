package jcog.memoize;

import java.util.concurrent.atomic.AtomicLong;

/** TODO use for CaffeineMemoize etc */
abstract public class AbstractMemoize<X, Y> implements Memoize<X, Y> {

    public final AtomicLong
            hit = new AtomicLong(),
            miss = new AtomicLong(),
            reject = new AtomicLong(),
            evict = new AtomicLong();


}
