package nars.attention;

import jcog.pri.bag.Bag;
import jcog.pri.bag.util.ProxyBag;

import java.util.function.Consumer;

abstract public class BufferedBag<X,B,Y> extends ProxyBag<X,Y> {

    public final PriBuffer<B> buffer;

    public BufferedBag(Bag<X, Y> bag, PriBuffer<B> buffer) {
        super(bag);
        this.buffer = buffer;
    }

    @Override
    public Bag<X, Y> commit(Consumer<Y> update) {

        synchronized (bag) {
            bag.commit(update);

            buffer.update(this::putInternal);

            bag.commit(null); //sort
        }

        return this;
    }

    public final void put(B x, float p) {
        buffer.put(x, p);
    }

    /** internal put: custom adaptation can be implemented in subclasses */
    abstract protected void putInternal(B y, float v);
}
