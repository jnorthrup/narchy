package nars.attention;

import jcog.data.NumberX;
import jcog.pri.Prioritizable;
import jcog.pri.Prioritized;
import jcog.pri.ScalarValue;
import jcog.pri.bag.Bag;
import jcog.pri.bag.util.ProxyBag;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** concurrent buffering bag wrapper */
abstract public class BufferedBag<X,B,Y> extends ProxyBag<X,Y> {

    final AtomicBoolean busy = new AtomicBoolean(false);

    public final PriBuffer<B> buffer;

    public BufferedBag(Bag<X, Y> bag, PriBuffer<B> buffer) {
        super(bag);
        this.buffer = buffer;
    }


    @Override
    public Bag<X, Y> commit(Consumer<Y> update) {

        if (busy.compareAndSet(false,true)) {
            try {
                synchronized (bag) {
                    bag.commit(update);

                    buffer.update(this::putInternal);

                    bag.commit(null); //sort
                }
            } finally {
                busy.set(false);
            }
        }

        return this;
    }

    @Override
    public final void putAsync(Y b) {
        put(b);
    }

    @Override public final Y put(Y x) {
        put((B)x, ((Prioritized)x).priElseZero());
        return x;
    }

    @Override
    public Y put(Y b, @Nullable NumberX overflowingIgnored) {
        return put(b);
    }

    public final void put(B x, float p) {
        buffer.put(x, p);
    }

    @Override
    public boolean isEmpty() {
        return bag.isEmpty() && buffer.isEmpty();
    }

    /** internal put: custom adaptation can be implemented in subclasses */
    abstract protected void putInternal(B y, float v);

    abstract public static class DefaultBufferedBag<X,B,Y extends Prioritizable> extends BufferedBag<X,B,Y> {
        public DefaultBufferedBag(Bag<X,Y> activates, PriBuffer<B> conceptPriBuffer) {
            super(activates, conceptPriBuffer);
        }
        private float min;

        @Override
        public Bag<X,Y> commit(Consumer<Y> update) {
            min = bag.size() >= bag.capacity() ? bag.priMin() : 0;
            return super.commit(update);
        }

        @Override
        public void putInternal(B b, float pri) {
            if (min <= ScalarValue.EPSILON  ||  (pri >= min || bag.contains(keyInternal(b)))) {
                //Prioritizable b will need its pri set with the provided pri that may not match, having accumulated since its first insertion
                Y y = valueInternal(b, pri);
                y.pri(pri);
                bag.putAsync(y);
            } else {
                //System.out.println("ignored: " + c + " "+n4(pri));
            }
        }


        protected abstract Y valueInternal(B b, float pri);

        protected abstract X keyInternal(B c);

    }

    abstract public static class SimpleBufferedBag<X,Y extends Prioritizable> extends DefaultBufferedBag<X,Y,Y> {

        public SimpleBufferedBag(Bag<X, Y> activates, PriBuffer<Y> conceptPriBuffer) {
            super(activates, conceptPriBuffer);
        }

        @Override protected final Y valueInternal(Y c, float pri) {
            return c;
        }

    }

}
