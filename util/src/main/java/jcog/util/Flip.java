package jcog.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/** atomic switching double buffer */
public class Flip<X> extends AtomicInteger {

    private X a;
    private X b;

    public Flip(Supplier<X> builder) {
        super(0);
        this.a = builder.get();
        this.b = builder.get();
    }

    public X write() { return (get() & 1) == 0 ? a : b;   }
    public X read() {
        return (get() & 1) == 0 ? b : a;
    }

    public X commit() {
        incrementAndGet();
        return read();
    }

}
