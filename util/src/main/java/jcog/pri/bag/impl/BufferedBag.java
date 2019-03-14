package jcog.pri.bag.impl;

import jcog.data.MutableFloat;
import jcog.data.NumberX;
import jcog.pri.PriBuffer;
import jcog.pri.Prioritizable;
import jcog.pri.Prioritized;
import jcog.pri.bag.Bag;
import jcog.pri.bag.util.ProxyBag;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** concurrent buffering bag wrapper */
abstract public class BufferedBag<X,B,Y extends Prioritizable> extends ProxyBag<X,Y> {

    final AtomicBoolean busy = new AtomicBoolean(false);

    /** pre-bag accumulating buffer */
    public final PriBuffer<B> pre;

    public BufferedBag(Bag<X, Y> bag, PriBuffer<B> pre) {
        super(bag);
        this.pre = pre;
    }


    @Override
    public void clear() {
        pre.clear();
        super.clear();
    }

    @Override
    public final Bag<X, Y> commit(@Nullable Consumer<Y> update) {
        return commit(update, null);
    }

    public final Bag<X, Y> commit(@Nullable Consumer<Y> before, @Nullable Consumer<Y> after) {

        if (busy.compareAndSet(false,true)) {
            try {
                    bag.commit(before);

                    if (!pre.isEmpty()) {
                        pre.update(this::putInternal);
                        bag.commit(after); //force sort after
                    }

                //}
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
        return (Y)put((B)x, ((Prioritized)x).pri());
    }

    @Override
    public final Y put(Y b, @Nullable NumberX overflowingIgnored) {
        return put(b);
    }

    public final B put(B x, float p) {
        Y yBag = bag.get(x);
        if (yBag!=null) {
            //HACK

            if (bag instanceof ArrayBag) {
                //handles merge pressurization
                ((ArrayBag) bag).merge(yBag, (Prioritizable) x, null);
            } else if (bag instanceof HijackBag) {
                //TODO test if neceessary
                NumberX o =new MutableFloat();
                ((HijackBag)bag).merge(yBag, x, o);
                bag.depressurize(o);
            }
            else throw new UnsupportedOperationException();

            return (B) yBag;
        } else
            return pre.put(x, p);
    }

    @Override
    public final boolean isEmpty() {
        return bag.isEmpty() && pre.isEmpty();
    }

    /** internal put: custom adaptation can be implemented in subclasses */
    abstract protected void putInternal(B y, float v);

    abstract public static class DefaultBufferedBag<X,B,Y extends Prioritizable> extends BufferedBag<X,B,Y> {
        public DefaultBufferedBag(Bag<X,Y> activates, PriBuffer<B> conceptPriBuffer) {
            super(activates, conceptPriBuffer);
        }

        @Override
        public void putInternal(B b, float pri) {
                Y y = valueInternal(b, pri);
                bag.putAsync(y);
        }

        protected abstract Y valueInternal(B b, float pri);

    }

    public static class SimpleBufferedBag<X,Y extends Prioritizable> extends DefaultBufferedBag<X,Y,Y> {

        public SimpleBufferedBag(Bag<X, Y> activates, PriBuffer<Y> conceptPriBuffer) {
            super(activates, conceptPriBuffer);
        }

        @Override protected final Y valueInternal(Y c, float pri) {
            return c;
        }


    }

}
