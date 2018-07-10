package nars.control.channel;

import com.google.common.collect.AbstractIterator;
import jcog.list.MetalConcurrentQueue;

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class BufferedCauseChannel implements Consumer {

    final MetalConcurrentQueue buffer;
    private final CauseChannel target;

    public BufferedCauseChannel(CauseChannel c) {
        this(c, 256);
    }
    public BufferedCauseChannel(CauseChannel c, int capacity) {
        target = c;
        buffer = new MetalConcurrentQueue(capacity);
    }


    public final void input(Object x) {
        while (!buffer.offer(x)) {
            buffer.poll(); //OVERFLOW
        }
    }

    /** returns false if the input was denied */
    public final boolean inputUntilBlocked(Object x) {
        return buffer.offer(x);
    }

    /** returns # input */
    public long input(Stream x) {
        return //Math.min(buffer.capacity(),
                x.filter(Objects::nonNull).peek(this::input).count()
                //)
        ;
    }

    public void input(Object... xx) {
        for (Object x :xx)
            input(x);
    }




    public void input(Iterator xx) {
        xx.forEachRemaining(this::input);
    }

    public void input(Iterable xx) {
        xx.forEach(this::input);
    }

    public void commit() {
        if (inputPending.compareAndSet(false, true)) {
            target.input(inputDrainer);
        }
    }

    @Override
    public final void accept(Object o) {
        input(o);
    }

    public final float value() {
        return target.value();
    }

    final AtomicBoolean inputPending = new AtomicBoolean(false);
    private final Iterable inputDrainer = ()-> new AbstractIterator() {
        @Override
        protected Object computeNext() {
            inputPending.set(false);
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
