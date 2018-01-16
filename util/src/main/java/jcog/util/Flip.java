package jcog.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/** atomic switching double buffer */
public class Flip<X> extends AtomicBoolean {

    private X a;
    private X b;

    public Flip(Supplier<X> builder) {
        this.a = builder.get();
        this.b = builder.get();
    }

    public X write() {
        return !get() ? a : b;
    }

    public Flip write(X next) {
        if (!get()) this.a = next; else  this.b = next;
        return this;
    }

//    public void writeCommit(X next) {
//        write(next).commit();
//    }

    public X commit() {
        return getAndSet(!get()) ? a : b;
    }

    public X read() {
        return get() ? a : b;
    }

}
