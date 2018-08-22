package nars.control.channel;

import com.google.common.collect.AbstractIterator;
import jcog.TODO;
import jcog.data.list.MetalConcurrentQueue;
import jcog.pri.Priority;

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;


public class BufferedCauseChannel<X extends Priority> implements Consumer<X> {

    final MetalConcurrentQueue<X> buffer;
    private final CauseChannel<X> target;


    public BufferedCauseChannel(CauseChannel<X> c, int capacity) {
        target = c;
        buffer = new MetalConcurrentQueue(capacity);
    }


    public final void input(X x) {
        while (!buffer.offer(x)) {
            //TODO on overflow it can optionally begin batching the previous items into compound tasks
            throw new TODO();
            //buffer.poll(); //OVERFLOW
        }
    }

    /** returns false if the input was denied */
    public final boolean inputIfCapacity(X x) {
        return buffer.offer(x);
    }

    /** returns # input */
    public long input(Stream<X> x) {
        return //Math.min(buffer.capacity(),
                x.filter(Objects::nonNull).peek(this::input).count()
                //)
        ;
    }

    @SafeVarargs
    public final void input(X... xx) {
        for (X x :xx)
            input(x);
    }

    public void input(Iterator<X> xx) {
        xx.forEachRemaining(this::input);
    }

    public void input(Iterable<X> xx) {
        xx.forEach(this::input);
    }

    public void commit() {
        if (inputPending.weakCompareAndSetAcquire(false, true)) {
            target.input(inputDrainer);
        }
    }

    @Override
    public final void accept(X o) {
        input(o);
    }

    public final float value() {
        return target.value();
    }

    final AtomicBoolean inputPending = new AtomicBoolean(false);
    private final Iterable inputDrainer = ()-> new AbstractIterator() {
        @Override
        protected Object computeNext() {
            inputPending.setRelease(false);
            Object t = buffer.poll();
            if (t == null)
                endOfData();
            return t;
        }
    };

    public final boolean full() {
        return buffer.size() >= buffer.capacity();
    }

    public final short id() {
        return target.id;
    }
}
