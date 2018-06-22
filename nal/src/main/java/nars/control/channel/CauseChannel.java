package nars.control.channel;

import jcog.pri.Priority;
import nars.control.Cause;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * metered and mixable extension of Cause base class
 */
abstract public class CauseChannel<X extends Priority> implements Consumer<X> {

    public final Cause cause;
    public final short id;

    public CauseChannel(Cause cause) {
        this.cause = cause;
        this.id = cause.id;
    }

    @Override
    public String toString() {
        return cause.name + "<-" + super.toString();
    }

    public final void input(Iterable<? extends X> xx) {
        input(xx.iterator());
    }

    public void input(Iterator<? extends X> xx) {
        xx.forEachRemaining(this::input);
    }

    public void input(Stream<? extends X> x) {
        x.forEach(this::input);
    }

    public final void input(List<? extends X> x) {
        if (x.size() == 1) {
            input(x.get(0));
        } else {
            input(x.iterator());
        }
    }

    public void input(Object... xx) {
        for (Object x : xx)
            input((X)x);
    }

    abstract public void input(X x);

    @Override
    public final void accept(@Nullable X x) {
        input(x);
    }

    public BufferedCauseChannel buffered() {
        return new BufferedCauseChannel(this);
    }

    public BufferedCauseChannel buffered(int capacity) {
        return new BufferedCauseChannel(this, capacity);
    }

//    public ThreadBufferedCauseChannel<X> threadBuffered() {
//        return new ThreadBufferedCauseChannel<>(this);
//    }

    public float value() {
        return cause.value();
    }
}
